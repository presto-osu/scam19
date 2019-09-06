/*
 * SendJobService.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.instrument.runtime;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.N)
public class SendJobService extends JobService implements IStopService {
  SendImpl<SendJobService> impl;

  private SendImpl<SendJobService> impl() {
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

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    return impl().onStartCommand(intent, flags, startId);
  }

  @Override
  public boolean onStartJob(JobParameters params) {
    return impl().onStartJob(params);
  }

  @Override
  public boolean onStopJob(JobParameters params) {
    return false;
  }

  @Override
  public boolean callServiceStopSelfResult(int startId) {
    return stopSelfResult(startId);
  }

  @Override
  public void callJobServiceFinished(JobParameters jobParameters, boolean needsReschedule) {
    jobFinished(jobParameters, false);
  }
}
