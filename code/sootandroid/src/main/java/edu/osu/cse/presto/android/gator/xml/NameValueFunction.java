/*
 * NameValueFunction.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */
package edu.osu.cse.presto.android.gator.xml;

import java.util.Map;

public abstract class NameValueFunction {
  public void feed(String name, int val) {
  }

  public static NameValueFunction mapInvMap(final Map<String, Integer> map,
      final Map<Integer, String> invMap) {

    return new NameValueFunction() {
      public void feed(String name, int val) {
        map.put(name, val);
        invMap.put(val, name);
      }
    };
  }
}
