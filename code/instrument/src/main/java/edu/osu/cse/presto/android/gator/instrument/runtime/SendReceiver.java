/*
 * SendReceiver.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.instrument.runtime;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SendReceiver extends BroadcastReceiver {
  private final String TAG = "presto.ga.rt." + SendReceiver.class.getSimpleName();
  public static final String SEND_ACTION = "edu.osu.cse.presto.gator.runtime.SEND";
  public static final Object sLock = new Object();
  public static WakeLockWrapper sWakelock;

  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent == null) {
      Log.w(TAG, "SendReceiver called with null intent");
      return;
    }
    String string = intent.getAction();
    Log.i(TAG, "SendReceiver got " + string);
    if (SEND_ACTION.equals(string)) {
      Intent intent2 = new Intent(SEND_ACTION);
      intent2.setComponent(new ComponentName(context, SendService.class));
      intent2.setAction(SEND_ACTION);
      synchronized (sLock) {
        context.startService(intent2);
        try {
          if (sWakelock == null) {
            sWakelock = new WakeLockWrapper(context, "Presto GA WakeLock");
            sWakelock.setReferenceCounted(false);
          }
          sWakelock.acquire(10000L); // 10 seconds
        } catch (SecurityException securityException) {
          Log.e(TAG, "SendService at risk of not starting. For more reliable analytics, add the WAKE_LOCK permission to your manifest.");
        }
      }
    }
  }
}
