/*
 * IntentFilterManager.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */
package edu.osu.cse.presto.android.gator.gui.intent;

import edu.osu.cse.presto.android.gator.Logger;
import soot.toolkits.scalar.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class IntentFilterManager {
  // launcher intent
  private Pair<String, IntentFilter> launcherFilter = null;
  // map [ActivityName, ActivityFilter]
  private final Map<String, Set<IntentFilter>> filterMap = new HashMap<String, Set<IntentFilter>>();
  private static IntentFilterManager manager;

  private IntentFilterManager() {}

  public static synchronized IntentFilterManager v() {
    if (manager == null) {
      manager = new IntentFilterManager();
    }
    return manager;
  }

  public void addFilter(String activity, IntentFilter filter, boolean enabled) {
    filter.enabled = enabled;
    addFilter(activity, filter);
  }

  public void addFilter(String activity, IntentFilter filter) {
    Set<IntentFilter> filters = filterMap.get(activity);
    if (filters == null) {
      filters = new HashSet<IntentFilter>();
      filterMap.put(activity, filters);
    }
    filters.add(filter);
    if (filter.isLauncherFilter()) {
      if (launcherFilter != null) {
        Logger.err(getClass().getSimpleName(), "define multiple launcher: " + filter + ", activity: " + activity);
      }
      launcherFilter = new Pair<String, IntentFilter>(activity, filter);
    }
  }

  public Map<String, Set<IntentFilter>> getAllFilters() {
    return this.filterMap;
  }

  public Pair<String, IntentFilter> getLauncherFilter() {
    return launcherFilter;
  }
}
