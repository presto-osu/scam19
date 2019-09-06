/*
 * SootUtil.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.instrument.util;

import edu.osu.cse.presto.android.gator.instrument.runtime.Proxy;
import soot.*;
import soot.jimple.Jimple;

public class SootUtil {
  private static final String TAG = SootUtil.class.getSimpleName();
  private static SootUtil instance;

  public static synchronized SootUtil v() {
    if (instance == null) {
      instance = new SootUtil();
      instance.PROXY_CLS =
              Scene.v().getSootClass(Proxy.class.getName());
      instance.PROXY_RECORD_TRACKER_INT_MTD =
              instance.PROXY_CLS.getMethod("void recordTracker(int,com.google.android.gms.analytics.Tracker)");
      instance.PROXY_RECORD_TRACKER_STR_MTD =
              instance.PROXY_CLS.getMethod("void recordTracker(java.lang.String,com.google.android.gms.analytics.Tracker)");
      instance.PROXY_NEW_TRACKER_STR_MTD =
              instance.PROXY_CLS.getMethod("com.google.android.gms.analytics.Tracker newTracker(java.lang.String,com.google.android.gms.analytics.GoogleAnalytics)");
      instance.PROXY_HIT_MTD =
              instance.PROXY_CLS.getMethod("void hit(java.util.Map,com.google.android.gms.analytics.Tracker)");
      instance.PROXY_INIT_MTD =
              instance.PROXY_CLS.getMethod("void init(android.content.Context,com.google.android.gms.analytics.GoogleAnalytics)");
      instance.PROXY_PRINT_STATUS_MTD =
              instance.PROXY_CLS.getMethod("void printStatus()");
      instance.PROXY_RECORD_AUTO_TRACKING_MTD =
              instance.PROXY_CLS.getMethod("void recordAutoTracking(boolean,com.google.android.gms.analytics.Tracker)");

      instance.SET_CLS =
              Scene.v().getSootClass("java.util.Set");
      instance.SET_ADD_MTD =
              instance.SET_CLS.getMethod("boolean add(java.lang.Object)");

      instance.MAP_CLS =
              Scene.v().getSootClass("java.util.Map");
      instance.MAP_PUT_MTD =
              instance.MAP_CLS.getMethod("java.lang.Object put(java.lang.Object,java.lang.Object)");
    }
    return instance;
  }

  // classes
  public SootClass PROXY_CLS;
  public SootClass SET_CLS;
  public SootClass MAP_CLS;

  // methods
  public SootMethod PROXY_RECORD_TRACKER_INT_MTD;
  public SootMethod PROXY_RECORD_TRACKER_STR_MTD;
  public SootMethod PROXY_RECORD_AUTO_TRACKING_MTD;
  public SootMethod PROXY_NEW_TRACKER_STR_MTD;
  public SootMethod PROXY_HIT_MTD;
  public SootMethod PROXY_INIT_MTD;
  public SootMethod PROXY_PRINT_STATUS_MTD;
  public SootMethod SET_ADD_MTD;
  public SootMethod MAP_PUT_MTD;
  public static final String TRACKER_SEND_SIG =
          "<com.google.android.gms.analytics.Tracker: void send(java.util.Map)>";
  public static final String GOOGLE_ANALYTICS_GET_INSTANCE_SIG =
          "<com.google.android.gms.analytics.GoogleAnalytics: com.google.android.gms.analytics.GoogleAnalytics getInstance(android.content.Context)>";
  public static final String LOG_I_SIG =
          "<android.util.Log: int i(java.lang.String,java.lang.String)>";
  public static final String GOOGLE_ANALYTICS_NEW_TRACKER_INT_SIG =
          "<com.google.android.gms.analytics.GoogleAnalytics: com.google.android.gms.analytics.Tracker newTracker(int)>";
  public static final String GOOGLE_ANALYTICS_NEW_TRACKER_STR_SIG =
          "<com.google.android.gms.analytics.GoogleAnalytics: com.google.android.gms.analytics.Tracker newTracker(java.lang.String)>";
  public static final String TRACKER_ENABLE_AUTO_ACTIVITY_TRACKING_SIG =
          "<com.google.android.gms.analytics.Tracker: void enableAutoActivityTracking(boolean)>";

  // helper functions
  public boolean isLibraryClass(SootClass cls) {
    String name = cls.getName();
    if (name.startsWith("android")
            || name.startsWith("com.android")
            || name.startsWith("com.google")
            || name.startsWith("com.crashlytics")
            || name.startsWith("com.fasterxml")
            || name.startsWith("com.jakewharton")
            || name.startsWith("io.fabric")
            || name.startsWith("okhttp3")
            || name.startsWith("okio")
            || name.startsWith("org.json")
            || name.startsWith("retrofit")
            || name.startsWith("rx")
            || name.startsWith("com.urbanairship")
            || name.startsWith("com.squareup.picasso")
            || name.startsWith("com.facebook")
            || name.startsWith("javax")
            || name.startsWith("org.apache.http")
            || name.startsWith("junit")
            || name.startsWith("org.junit"))
      return true;
    return false;
  }

  public Local getTempLocal(Body body, String type, String name) {
    Local tmp = Jimple.v().newLocal(name, RefType.v(type));
    body.getLocals().add(tmp);
    return tmp;
  }
}
