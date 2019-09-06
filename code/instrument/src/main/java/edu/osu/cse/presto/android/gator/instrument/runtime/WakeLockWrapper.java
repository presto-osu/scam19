/*
 * WakeLockWrapper.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.instrument.runtime;

import android.content.Context;
import android.os.PowerManager;

public class WakeLockWrapper {
  private final PowerManager.WakeLock mWakeLock;

  public WakeLockWrapper(Context context, String name) {
    this.mWakeLock = ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, name);
  }

  public void acquire(long timeout) {
    this.mWakeLock.acquire(timeout);
  }

  public void release() {
    try {
      this.mWakeLock.release();
    } catch (RuntimeException ignored) {
    }
  }

  public void setReferenceCounted(boolean b) {
    this.mWakeLock.setReferenceCounted(b);
  }

  public boolean isHeld() {
    return mWakeLock.isHeld();
  }
}
