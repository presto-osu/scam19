/*
 * Proxy.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.instrument.runtime;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.Log;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Proxy {
  private static final String TAG = "presto.ga.rt." + Proxy.class.getSimpleName();

  private final String PRESTO_PREFIX = "presto_";
  private final String PRESTO_POSTFIX = "";

  public static String PREFIX;
  public static Set<String> V;
  public static double EPSILON;
  public static boolean experimentMode = false;
//  public static int simulateUserNum = 1;
//  public static int scaleEvents = 1;

  private static Proxy instance;
  private GoogleAnalytics mGoogleAnalytics;
  private Map<String, String> act2name;
  private Context mAppContext;
  private Scheduler mScheduler;
  private Dispatcher mDispatcher;
  private DatabaseController mDatabaseController;
  private Randomizer mRandomizer;
  private Map<String, Tracker> trackers = new ConcurrentHashMap<>();
  private Map<Tracker, String> trackersInv = new ConcurrentHashMap<>();
  private Set<WeakReference<String>> originalTrackerSettings = new HashSet<>();
  private boolean enableRandomization = false;
  private boolean enableAutoTracking = false;

  private Proxy(Context context, GoogleAnalytics googleAnalytics) {
    mGoogleAnalytics = googleAnalytics;
    mScheduler = new Scheduler(context.getApplicationContext());
    mDatabaseController = new DatabaseController(this, context.getApplicationContext());
    mAppContext = context.getApplicationContext();
    PREFIX = PRESTO_PREFIX + context.getPackageName() + PRESTO_POSTFIX;
    V = new HashSet<>(mDatabaseController.readScreenNames());
    act2name = new HashMap<>();
    mRandomizer = new Randomizer(mDatabaseController);
    mDispatcher = new Dispatcher(mDatabaseController, mRandomizer);
  }

  public static synchronized Proxy getInstance() {
    if (instance == null) {
      Log.e(TAG, "getInstance: Not initialized.");
      throw new RuntimeException();
    }
    return instance;
  }

  public Dispatcher getDispatcher() {
    return mDispatcher;
  }

  public static synchronized void init(Context context, GoogleAnalytics googleAnalytics) {
    if (instance != null) {
      return;
    }
    instance = new Proxy(context, googleAnalytics);
    Log.i(TAG, "Init @ " + context.getPackageName());

    // TODO: code will be inserted here to fill the universe

    if (instance.enableAutoTracking) {
      V.addAll(instance.act2name.values());
    }
    printStatus();
    instance.mScheduler.schedule();
  }

  public static synchronized void printStatus() {
    Log.i(TAG, "\tRandomized: " + instance.enableRandomization);
    if (instance.enableRandomization) Log.i(TAG, "\tEpsilon: " + EPSILON);
    Log.i(TAG, "\tAuto-tracking: " + instance.enableAutoTracking);
    Log.i(TAG, "\tActual Views: " + instance.mDatabaseController.actualViewsSoFar());
    Log.i(TAG, "\tUniverse: #=" + V.size() + " " + V);
//    if (experimentMode) {
//      Log.i(TAG, "\tExperiment Mode: #users=" + simulateUserNum + ", scale=1/" + scaleEvents);
//    }
  }

  public static void recordTracker(int xmlId, Tracker tracker) {
    Log.i(TAG, "Original tracker: " + xmlId + "; current tracker: " + tracker.get("&tid"));
//    Log.i(TAG, "New tracker: " + arg + "@" + tracker);
    if (instance == null) {
      Log.e(TAG, "recordTracker: Not initialized.");
      return;
    }
    instance.originalTrackerSettings.add(new WeakReference<>(String.valueOf(xmlId)));
//    instance.trackers.put(String.valueOf(arg), tracker);
//    instance.trackersInv.put(tracker, String.valueOf(arg));
  }

  public static void recordTracker(String gaId, Tracker tracker) {
    Log.i(TAG, "Original tracker: " + gaId + "; current tracker: " + tracker.get("&tid"));
//    Log.i(TAG, "New tracker: " + arg + "@" + tracker);
    if (instance == null) {
      Log.e(TAG, "recordTracker: Not initialized.");
      return;
    }
    instance.originalTrackerSettings.add(new WeakReference<>(gaId));
//    instance.trackers.put(arg, tracker);
//    instance.trackersInv.put(tracker, arg);
  }

  public static synchronized Tracker newTracker(String gaId, GoogleAnalytics ga) {
    if (instance == null) {
      Log.e(TAG, "newTracker: Not initialized.");
      return null;
    }
    if (instance.trackers.containsKey(gaId)) {
      return instance.trackers.get(gaId);
    }
    Tracker tracker = ga.newTracker(gaId);
    instance.trackers.put(gaId, tracker);
    instance.trackersInv.put(tracker, gaId);
    Log.i(TAG, "New tracker: " + gaId + " " + tracker);

    tracker.enableAutoActivityTracking(false);
    if (instance.enableAutoTracking) {
      if (instance.mAppContext instanceof Application) {
        enableReportActivity((Application) instance.mAppContext, tracker);
      }
    }

    return tracker;
  }

  public static void recordAutoTracking(boolean enableAutoTracking, Tracker tracker) {
    Log.i(TAG, "Auto Activity tracking " + (enableAutoTracking ? "enabled" : "disabled") + " on " + tracker);
  }

  @TargetApi(value = 14)
  private static void enableReportActivity(Application application, Tracker tracker) {
    application.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks(instance, tracker));
  }

  public String getName(Activity activity) {
    return act2name.getOrDefault(activity.getClass().getCanonicalName(), activity.getClass().getCanonicalName());
  }

  public String getTrackingId(Tracker tracker) {
    return trackersInv.get(tracker);
  }

  public Tracker getTracker(String id) {
    return trackers.get(id);
  }

  public static void hit(final Map<String, String> map, final Tracker tracker) {
    if (instance == null) {
      Log.e(TAG, "hit: Not initialized.");
      return;
    }
    Log.i(TAG, "Hit: " + map);
    String type = map.get("&t");
    if (type.equals("screenview")) {
      String currentName = map.getOrDefault("&cd", tracker.get("&cd")); // content description
      if (currentName == null) {
        Log.i(TAG, "\tnull screen name, just send");
        tracker.send(map);
        return;
      }

      instance.mDatabaseController.incrementActualViews(currentName);

      if (!instance.enableRandomization) {
        if (!V.contains(currentName)) {
          V.add(currentName);
          instance.mDatabaseController.storeNewScreenName(currentName);
        }
        tracker.send(map);
        List<String> screenNames = instance.mDatabaseController.readScreenNames();
        Log.i(TAG, "\trandomization disabled, names: #=" + screenNames.size() + " " + screenNames);
        return;
      }

      Map<String, String> newMap = new HashMap<>(map);
      newMap.put("&cd", currentName);

      if (!V.contains(currentName)) {
        Log.i(TAG, "\tnew screen name, add " + currentName + " to universe");
        instance.mDatabaseController.storeNewScreenName(currentName);
        V.add(currentName);
        newMap.put("presto_new_name", "true");
      } else {
        newMap.put("presto_new_name", "false");
      }

      instance.mDispatcher.dispatch(tracker, newMap);
      instance.mScheduler.schedule();
    } else {
      tracker.send(map);
    }
  }
}

