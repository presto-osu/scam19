/*
 * Dispatcher.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.instrument.runtime;

import android.util.Log;
import com.google.android.gms.analytics.Tracker;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Dispatcher {
  private final String TAG = "presto.ga.rt." + Dispatcher.class.getSimpleName();
  private final DispatchThreadPoolExecutor mDeliverExecutor = new DispatchThreadPoolExecutor();
  private final DispatchThreadPoolExecutor mDispatchExecutor = new DispatchThreadPoolExecutor();
  private final Queue<HitInfo> mPendingRandomizedHitQueue = new ConcurrentLinkedQueue<>();
  private final DatabaseController mDbController;
  private final Randomizer mRandomizer;
  private final long INTERVAL_BETWEEN_SENDS = 2000; // 2 seconds
  private final long MAX_HITS_PER_DISPATCH = 20;

  Dispatcher(DatabaseController databaseController, Randomizer randomizer) {
    this.mDbController = databaseController;
    this.mRandomizer = randomizer;
  }

  public void dispatch(Tracker tracker, Map<String, String> map) {
    mDispatchExecutor.submit(new DispatchJob(this, tracker, map));
  }

  void enqueueHit(Tracker tracker, Map<String, String> map) {
    long id = mDbController.storeHit(tracker, map);
    mRandomizer.randomize(mPendingRandomizedHitQueue, tracker, map);
    if (!Proxy.experimentMode)
      mDbController.deleteHit(id);
  }

  public void deliver() {
    if (!Proxy.experimentMode)
      mDeliverExecutor.submit(new DispatchJob(this));
  }

  public static void checkIfInWorkerThread() {
    if (!(Thread.currentThread() instanceof DispatchThread)) {
      throw new IllegalStateException("Call expected from DispatchThread");
    }
  }

  boolean enqueueSavedRandomizedHits() {
    if (mDbController.numberOfStoredHits() > 0L) {
      for (HitInfo hit : mDbController.readHits(MAX_HITS_PER_DISPATCH)) {
        enqueueHit(hit.tracker, hit.map);
      }
      return true;
    }
    if (mDbController.numberOfStoredRandomizedHits() > 0L) {
      Log.i(TAG, "Enqueue randomized hits (db#=" + mDbController.numberOfStoredRandomizedHits() + ")");
      for (HitInfo hit : mDbController.readRandomizedHits(MAX_HITS_PER_DISPATCH)) {
        mPendingRandomizedHitQueue.offer(hit);
        Log.i(TAG, "\t" + hit + " (q#=" + mPendingRandomizedHitQueue.size() + ")");
      }
      return true;
    }
    return false;
  }

  public void deliverLocalRandomizedHits() {
    if (mPendingRandomizedHitQueue.isEmpty() && !enqueueSavedRandomizedHits())
      return;
    while (!mPendingRandomizedHitQueue.isEmpty()) {
      try {
        Thread.sleep(INTERVAL_BETWEEN_SENDS); // delay between two sends
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      HitInfo hitInfo = mPendingRandomizedHitQueue.poll();
      if (hitInfo != null && hitInfo.tracker != null) {
        hitInfo.tracker.send(hitInfo.map);
        mDbController.deleteRandomizedHit(hitInfo.dbId);
        Log.i(TAG, "Send hit: " + hitInfo + " (q#=" + mPendingRandomizedHitQueue.size() + ")");
      }
    }
    deliver();
  }
}
