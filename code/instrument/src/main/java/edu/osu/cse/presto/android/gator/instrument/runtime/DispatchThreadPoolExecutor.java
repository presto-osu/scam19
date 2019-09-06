/*
 * DispatchThreadPoolExecutor.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.instrument.runtime;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DispatchThreadPoolExecutor extends ThreadPoolExecutor {
  DispatchThreadPoolExecutor() {
    super(1, 1, 2, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
    this.setThreadFactory(new DispatchThreadFactory());
    this.allowCoreThreadTimeOut(true);
  }

  @Override
  protected final <T> RunnableFuture<T> newTaskFor(Runnable runnable, T t) {
    return new DispatchFutureTask<>(runnable, t);
  }
}
