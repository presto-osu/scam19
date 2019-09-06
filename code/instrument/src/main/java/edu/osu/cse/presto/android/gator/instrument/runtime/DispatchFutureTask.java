/*
 * DispatchFutureTask.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.instrument.runtime;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.concurrent.FutureTask;

public class DispatchFutureTask<T> extends FutureTask<T> {
  private final String TAG = "presto.ga.rt." + DispatchFutureTask.class.getSimpleName();

  DispatchFutureTask(@NonNull Runnable runnable, T result) {
    super(runnable, result);
  }

  @Override
  protected final void setException(Throwable throwable) {
    Log.w(TAG, "DispatchThreadPoolExecutor: job failed with " + throwable);
    super.setException(throwable);
  }
}
