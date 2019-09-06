/*
 * DispatchThreadFactory.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.instrument.runtime;

import android.support.annotation.NonNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DispatchThreadFactory implements ThreadFactory {
  private static final AtomicInteger threadId = new AtomicInteger();

  @Override
  public final Thread newThread(@NonNull Runnable runnable) {
    int n = threadId.incrementAndGet();
    return new DispatchThread(runnable, "gasend-" + n);
  }
}
