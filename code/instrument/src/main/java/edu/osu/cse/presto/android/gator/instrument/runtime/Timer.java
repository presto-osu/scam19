/*
 * Timer.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.instrument.runtime;

public class Timer {
  private long mStartTIme;

  public void start() {
    mStartTIme = System.currentTimeMillis();
  }

  public void clear() {
    mStartTIme = 0L;
  }

  public boolean checkTimeLimit(long l) {
    return mStartTIme == 0L || System.currentTimeMillis() - mStartTIme > l;
  }
}
