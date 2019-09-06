/*
 * IStopService.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.instrument.runtime;

import android.app.job.JobParameters;

public interface IStopService {
  boolean callServiceStopSelfResult(int startId);

  void callJobServiceFinished(JobParameters jobParameters, boolean needsReschedule);
}
