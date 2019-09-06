/*
 * SendService.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.instrument.runtime;

import android.app.Service;
import android.app.job.JobParameters;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

public class SendService extends Service implements IStopService {
  SendImpl<SendService> impl;

  private SendImpl<SendService> impl() {
    if (this.impl == null) {
      this.impl = new SendImpl<>(this);
    }
    return this.impl;
  }

  @Override
  public final void onCreate() {
    impl().onCreate();
    super.onCreate();
  }

  @Override
  public final void onDestroy() {
    impl().onDestroy();
    super.onDestroy();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    impl();
    return null;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    return impl().onStartCommand(intent, flags, startId);
  }

  @Override
  public boolean callServiceStopSelfResult(int startId) {
    return stopSelfResult(startId);
  }

  @Override
  public void callJobServiceFinished(JobParameters jobParameters, boolean needsReschedule) {
    throw new UnsupportedOperationException();
  }
}
