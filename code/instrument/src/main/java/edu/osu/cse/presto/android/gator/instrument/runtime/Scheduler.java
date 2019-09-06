/*
 * Scheduler.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.instrument.runtime;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.util.Log;

public class Scheduler {
  private static final String TAG = "presto.ga.rt." + Scheduler.class.getSimpleName();

  long SCHEDULE_ALARM_MILLIS = 60000; // 1 minute delay for actual sending

  private boolean hasScheduled;
  private final AlarmManager mAlarmManager;
  private final Context mAppContext;
  private Integer mJobId;

  Scheduler(Context context) {
    mAppContext = context;
    mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
  }

  public void schedule() {
    cancel();
    hasScheduled = true;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      ComponentName componentName = new ComponentName(mAppContext, SendJobService.class);
      JobScheduler jobScheduler = (JobScheduler) mAppContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);
      JobInfo.Builder builder = new JobInfo.Builder(getJobId(), componentName);
      builder.setMinimumLatency(SCHEDULE_ALARM_MILLIS);
      builder.setOverrideDeadline(SCHEDULE_ALARM_MILLIS << 1);
//      builder.setPeriodic(SCHEDULE_ALARM_MILLIS);
      PersistableBundle persistableBundle = new PersistableBundle();
      persistableBundle.putString("action", SendReceiver.SEND_ACTION);
      builder.setExtras(persistableBundle);
      JobInfo jobInfo = builder.build();
      try {
        jobScheduler.schedule(jobInfo);
        Log.v(TAG, "Scheduling upload with JobScheduler. JobID: " + getJobId());
        return;
      } catch (IllegalArgumentException ex) {
        Log.w(TAG, "SendJobService not available: " + ex.getMessage());
      }
    }
    Log.v(TAG, "Scheduling upload with AlarmManager");
    mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + SCHEDULE_ALARM_MILLIS,
            SCHEDULE_ALARM_MILLIS,
            getSendReceiver());
  }

  private PendingIntent getSendReceiver() {
    Intent intent = new Intent(SendReceiver.SEND_ACTION);
    intent.setComponent(new ComponentName(mAppContext, SendReceiver.class));
    return PendingIntent.getBroadcast(mAppContext, 0, intent, 0);
  }

  public void cancel() {
    hasScheduled = false;
    mAlarmManager.cancel(getSendReceiver());
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      JobScheduler jobScheduler = (JobScheduler) mAppContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);
      Log.v(TAG, "Cancelling job. JobID: " + getJobId());
      jobScheduler.cancel(getJobId());
    }
  }

  private int getJobId() {
    if (mJobId == null) {
      String string = String.valueOf(mAppContext.getPackageName());
      mJobId = (string.length() != 0 ? "presto".concat(string) : "presto").hashCode();
    }
    return mJobId;
  }
}
