/*
 * Main.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.instrument;

import com.google.common.collect.Lists;
import edu.osu.cse.presto.android.gator.Logger;
import edu.osu.cse.presto.android.gator.instrument.runtime.*;
import edu.osu.cse.presto.android.gator.instrument.util.ApkUtil;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.Transform;
import soot.options.Options;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
  static final String TAG = Main.class.getSimpleName();

  public static void main(final String[] args) {
    Configs.apkPath = args[0];
    Logger.setVerbose(args[1]);
    Configs.sdkPlatformsPath = args[2];
    Configs.runtimeClsDir = args[3];
    String[] parts = Configs.apkPath.split("/");
    Configs.apkName = parts[parts.length - 1];
    Path sootOutApkPath = Paths.get("sootOutput", Configs.apkName);
    Path maniOutApkPath = Paths.get("sootOutput", "mani", Configs.apkName);

    Configs.myGAId = args[4];
    Configs.dataUniverseXmlPath = args[5];
    Configs.runtimeDb = args[6];

    Configs.randomization = Boolean.parseBoolean(args[7]);
    Configs.epsilon = Double.parseDouble(args[8]);
    Configs.experiment = Boolean.parseBoolean(args[9]);
//    Configs.simulateUserNum = Integer.parseInt(args[10]);
//    Configs.scaleEvents = Integer.parseInt(args[11]);

    try {
      Files.delete(sootOutApkPath);
      Files.delete(maniOutApkPath);
    } catch (IOException ignored) {
    }

    runJTP(Configs.apkPath, Configs.sdkPlatformsPath);

    ApkUtil.v().repack(sootOutApkPath, maniOutApkPath);
    Logger.info(TAG, "Saved to " + maniOutApkPath);
  }

  // jimple transformation pack
  static void runJTP(final String apkPath, final String platformDir) {
    settings(apkPath, platformDir);

    PackManager.v().getPack("wjtp").add(new Transform("wjtp.myInstrumenter", new MySceneTransformer()));

    final String[] sootArgs = {
            "-w",
            "-process-multiple-dex",
            "-p", "jb", "stabilize-local-names:true",
            "-keep-line-number",
            "-allow-phantom-refs",};

    soot.Main.main(sootArgs);
  }

  static void settings(final String apkPath, final String platformDir) {
    // prefer Android APK files// -src-prec apk
    Options.v().set_src_prec(Options.src_prec_apk);
    // output as APK, too//-f J
    Options.v().set_output_format(Options.output_format_dex);
    //    Options.v().set_output_format(Options.output_format_jimple);
    // set Android platform jars and apk
    Options.v().set_android_jars(platformDir);
    Options.v().set_process_dir(Lists.newArrayList(apkPath));

    // load instrument helper class
    Options.v().set_prepend_classpath(true);
    Options.v().set_soot_classpath(Configs.runtimeClsDir);

    // sometimes using array to parse args does not work
    Options.v().set_process_multiple_dex(true);
    Options.v().set_whole_program(true);
    Options.v().set_allow_phantom_refs(true);

    Scene.v().loadClass(Proxy.class.getName(), SootClass.HIERARCHY).setApplicationClass();
    Scene.v().loadClass(Dispatcher.class.getName(), SootClass.HIERARCHY).setApplicationClass();
    Scene.v().loadClass(HitInfo.class.getName(), SootClass.HIERARCHY).setApplicationClass();
    Scene.v().loadClass(DispatchFutureTask.class.getName(), SootClass.HIERARCHY).setApplicationClass();
    Scene.v().loadClass(DispatchJob.class.getName(), SootClass.HIERARCHY).setApplicationClass();
    Scene.v().loadClass(DispatchThread.class.getName(), SootClass.HIERARCHY).setApplicationClass();
    Scene.v().loadClass(DispatchThreadFactory.class.getName(), SootClass.HIERARCHY).setApplicationClass();
    Scene.v().loadClass(DispatchThreadPoolExecutor.class.getName(), SootClass.HIERARCHY).setApplicationClass();
    Scene.v().loadClass(IStopService.class.getName(), SootClass.HIERARCHY).setApplicationClass();
    Scene.v().loadClass(Scheduler.class.getName(), SootClass.HIERARCHY).setApplicationClass();
    Scene.v().loadClass(SendImpl.class.getName(), SootClass.HIERARCHY).setApplicationClass();
    Scene.v().loadClass(SendJobService.class.getName(), SootClass.HIERARCHY).setApplicationClass();
    Scene.v().loadClass(SendReceiver.class.getName(), SootClass.HIERARCHY).setApplicationClass();
    Scene.v().loadClass(SendService.class.getName(), SootClass.HIERARCHY).setApplicationClass();
    Scene.v().loadClass(StopServiceJob.class.getName(), SootClass.HIERARCHY).setApplicationClass();
    Scene.v().loadClass(WakeLockWrapper.class.getName(), SootClass.HIERARCHY).setApplicationClass();
    Scene.v().loadClass(SQLiteOpenHelperWrapper.class.getName(), SootClass.HIERARCHY).setApplicationClass();
    Scene.v().loadClass(DatabaseController.class.getName(), SootClass.HIERARCHY).setApplicationClass();
    Scene.v().loadClass(Timer.class.getName(), SootClass.HIERARCHY).setApplicationClass();
    Scene.v().loadClass(ActivityLifecycleCallbacks.class.getName(), SootClass.HIERARCHY).setApplicationClass();
    Scene.v().loadClass(Randomizer.class.getName(), SootClass.HIERARCHY).setApplicationClass();
  }

}
