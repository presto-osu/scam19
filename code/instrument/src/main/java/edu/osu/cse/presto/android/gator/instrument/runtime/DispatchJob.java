/*
 * DispatchJob.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.instrument.runtime;

import com.google.android.gms.analytics.Tracker;

import java.util.Map;

public class DispatchJob implements Runnable {

  private final Dispatcher dispatcher;
  private Tracker tracker = null;
  private Map<String, String> map = null;

  DispatchJob(Dispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

  DispatchJob(Dispatcher dispatcher, Tracker tracker, Map<String, String> map) {
    this.dispatcher = dispatcher;
    this.tracker = tracker;
    this.map = map;
  }

  @Override
  public void run() {
    Dispatcher.checkIfInWorkerThread();
    if (tracker != null && map != null) {
      dispatcher.enqueueHit(tracker, map);
      return;
    }
    dispatcher.deliverLocalRandomizedHits();
  }
}
