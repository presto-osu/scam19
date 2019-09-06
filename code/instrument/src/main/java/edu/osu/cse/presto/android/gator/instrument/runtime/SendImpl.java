/*
 * SendImpl.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.instrument.runtime;

import android.annotation.TargetApi;
import android.app.Service;
import android.app.job.JobParameters;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

public class SendImpl<T extends Context> {
  private final String TAG = "presto.ga.rt." + SendImpl.class.getSimpleName();
  private final Handler mHandler;
  private final T service;

  SendImpl(@NonNull T t) {
    service = t;
    mHandler = new Handler();
  }

  public void onCreate() {
    Log.i(TAG, service.getClass().getSimpleName() + " is starting up");
  }

  public void onDestroy() {
    Log.i(TAG, service.getClass().getSimpleName() + " is shutting down");
  }

  public int onStartCommand(Intent intent, int flags, int startId) {
    try {
      synchronized (SendReceiver.sLock) {
        if (SendReceiver.sWakelock != null && SendReceiver.sWakelock.isHeld()) {
          SendReceiver.sWakelock.release();
        }
      }
    } catch (SecurityException ignored) {
    }
    if (intent == null) {
      Log.w(TAG, "SendService started with null intent");
      return Service.START_NOT_STICKY;
    }
    String string = intent.getAction();
    Log.i(TAG, "SendService called. startId: " + startId + ", action: " + string);
    if (SendReceiver.SEND_ACTION.equals(string)) {
      dispatch(startId, null);
    }
    return Service.START_NOT_STICKY;
  }

  private void dispatch(final Integer startId, final JobParameters jobParameters) {
    try {
      Proxy.getInstance().getDispatcher().deliver();
    } catch (Exception ignored) {
    }
    // stop service
    mHandler.post(new StopServiceJob((IStopService) service, startId, jobParameters));
  }

  @TargetApi(Build.VERSION_CODES.N)
  public boolean onStartJob(JobParameters jobParameters) {
    String string = jobParameters.getExtras().getString("action");
    Log.i(TAG, "SendJobService called. Action: " + string);
    if (SendReceiver.SEND_ACTION.equals(string)) {
      dispatch(null, jobParameters);
    }
    return true;
  }

}
