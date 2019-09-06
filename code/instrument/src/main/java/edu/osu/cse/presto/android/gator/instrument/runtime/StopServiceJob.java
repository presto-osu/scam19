/*
 * StopServiceJob.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.instrument.runtime;

import android.app.job.JobParameters;
import android.os.Build;
import android.util.Log;

public class StopServiceJob implements Runnable {
  private final String TAG = "presto.ga.rt." + StopServiceJob.class.getSimpleName();
  private final JobParameters jobParameters;
  private final IStopService service;
  Integer startId;

  public StopServiceJob(IStopService service, Integer startId, JobParameters jobParameters) {
    this.startId = startId;
    this.jobParameters = jobParameters;
    this.service = service;
  }

  @Override
  public void run() {
    if (startId != null) {
      if (service.callServiceStopSelfResult(startId)) {
        Log.i(TAG, "SendService processed last dispatch request");
      }
      return;
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      Log.i(TAG, "SendJobService processed last dispatch request");
      service.callJobServiceFinished(jobParameters, false);
    }
  }
}
