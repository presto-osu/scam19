/*
 * HitInfo.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.instrument.runtime;

import com.google.android.gms.analytics.Tracker;

import java.util.Map;

public class HitInfo {
  Tracker tracker;
  Map<String, String> map;
  long dbId;

  HitInfo(long dbId, Tracker tracker, Map<String, String> map) {
    this.dbId = dbId;
    this.tracker = tracker;
    this.map = map;
  }

  @Override
  public String toString() {
    return "[" + dbId + ": " + map + "]";
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) return false;
    if (o instanceof HitInfo) {
      return dbId == ((HitInfo) o).dbId;
    }
    return false;
  }
}
