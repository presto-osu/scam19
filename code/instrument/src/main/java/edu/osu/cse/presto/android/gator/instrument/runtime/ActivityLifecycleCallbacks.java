/*
 * ActivityLifecycleCallbacks.java - part of the GATOR project
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
import android.os.Bundle;
import com.google.android.gms.analytics.Tracker;

import java.util.HashMap;
import java.util.Map;

@TargetApi(value = 14)
public class ActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
  private Tracker tracker;
  private Proxy proxy;

  public ActivityLifecycleCallbacks(Proxy proxy, Tracker tracker) {
    this.tracker = tracker;
    this.proxy = proxy;
  }

  @Override
  public void onActivityStarted(Activity activity) {
    Map<String, String> map = new HashMap<>();
    map.put("&t", "screenview");
    map.put("&cd", proxy.getName(activity));
    Proxy.hit(map, tracker);
  }

  @Override
  public void onActivityStopped(Activity activity) {

  }

  @Override
  public void onActivityCreated(Activity activity, Bundle bundle) {

  }

  @Override
  public void onActivityResumed(Activity activity) {

  }

  @Override
  public void onActivityPaused(Activity activity) {

  }

  @Override
  public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

  }

  @Override
  public void onActivityDestroyed(Activity activity) {

  }
}
