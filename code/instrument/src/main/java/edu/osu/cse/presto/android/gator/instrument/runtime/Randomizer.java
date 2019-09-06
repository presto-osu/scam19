/*
 * Randomizer.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.instrument.runtime;

import android.util.Log;
import com.google.android.gms.analytics.Tracker;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class Randomizer {
  private final String TAG = "presto.ga.rt." + Randomizer.class.getSimpleName();

  private final DatabaseController mDatabaseController;

  private final Random mRand = new Random();

  private static int HIT_TIME_ADJUSTMENT_MILLISEC = 6000 * 2; // 6 sec

  public Randomizer(DatabaseController dbController) {
    this.mDatabaseController = dbController;
  }

//  Map<Double, Map<String, Integer>> epsilon2map = new ConcurrentHashMap<>();

  private void storeAndEnqueueHit(Queue<HitInfo> pendingRandomizedHitQueue, Tracker tracker,
                                  String name, Map<String, String> actualHitMap, double epsilon, int user) {
//    if (Proxy.experimentMode) {
//      Map<String, Integer> name2num = epsilon2map.getOrDefault(epsilon, new ConcurrentHashMap<>());
//      int num = name2num.getOrDefault(name, 0) % Proxy.scaleEvents;
//      name2num.put(name, num + 1);
//      epsilon2map.put(epsilon, name2num);
//      if (num != 0) return;
//    }

    Log.i(TAG, "\trandomized hit: " + name);
    mDatabaseController.incrementRandomViews(name, epsilon, user);

    Map<String, String> newMap = new HashMap<>(actualHitMap);
    newMap.put("&cd", Proxy.experimentMode ? Proxy.PREFIX + "_" + name : name);
    newMap.put("&ht", String.valueOf(System.currentTimeMillis() + mRand.nextInt(HIT_TIME_ADJUSTMENT_MILLISEC) - 6000));

    long id = mDatabaseController.storeRandomizedHit(tracker, newMap);
    HitInfo hit = new HitInfo(id, tracker, newMap);
    pendingRandomizedHitQueue.add(hit);
    Log.i(TAG, "\tEnqueue " + hit + " (q#=" + pendingRandomizedHitQueue.size() + ")");
  }

  private double NOT_HIT_PROBABILITY(double epsilon) {
    return 1 / (1 + Math.exp(epsilon / 2));
  }

  private double HIT_PROBABILITY(double epsilon) {
    return Math.exp(epsilon / 2) / (1 + Math.exp(epsilon / 2));
  }

  public void randomize(Queue<HitInfo> pendingRandomizedHitQueue, Tracker tracker, Map<String, String> actualHitMap) {
    String currentName = actualHitMap.get("&cd");
    long actualViewsSoFar = mDatabaseController.actualViewsSoFar();
    Log.i(TAG, "screenview#" + actualViewsSoFar + ": " + currentName + " " + actualHitMap);

//    for (int user = 0; user < Proxy.simulateUserNum; user++) {
//      double epsilon = Proxy.experimentMode ? 0.25 : Proxy.EPSILON;
//      while (epsilon <= Proxy.EPSILON) {
    double epsilon = Proxy.EPSILON;
    int user = 0;
    if (actualHitMap.get("presto_new_name").equals("true")) {
      for (int j = 1; j < actualViewsSoFar; j++) {
        if (mRand.nextDouble() <= NOT_HIT_PROBABILITY(epsilon)) {
          storeAndEnqueueHit(pendingRandomizedHitQueue, tracker, currentName, actualHitMap, epsilon, user);
        }
      }
    }
    for (String name : Proxy.V) {
      if (name.equals(currentName)) {
        if (mRand.nextDouble() <= HIT_PROBABILITY(epsilon)) {
          storeAndEnqueueHit(pendingRandomizedHitQueue, tracker, name, actualHitMap, epsilon, user);
        }
      } else if (mRand.nextDouble() <= NOT_HIT_PROBABILITY(epsilon)) {
        storeAndEnqueueHit(pendingRandomizedHitQueue, tracker, name, actualHitMap, epsilon, user);
      }
//        }
//        epsilon *= 2;
    }
//    }
  }
}
