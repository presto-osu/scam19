/*
 * Configs.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */
package edu.osu.cse.presto.android.gator;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import soot.Scene;
import soot.SootClass;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;

public class Configs {
  public static String benchmarkName;

  // root of the project directory
  public static String project;
  public static boolean apkMode;

  // root directory of Android SDK
  public static String sdkDir;

  public static String apiLevel;
  public static int numericApiLevel;

  public static String sysProj;

  public static String bytecodes;

  public static ArrayList<String> depJars;
  public static ArrayList<String> extLibs;

  // full path to android.jar
  public static String android;

  // jre jars
  public static String jre = "";

  // xml file describing listeners
  public static String listenerSpecFile = "";

  // --- boolean flags
  public static boolean verbose = false;

  public static boolean guiAnalysis;

  // hailong: run simple transformation instead of GUI analysis
  public static boolean transformation;

  public static Set<String> debugCodes = Sets.newHashSet();

  public static Set<String> clients = Sets.newHashSet();

  public static boolean withCHA = false;

  // hailong:
  public static boolean withSPARK = false;

  // [wtg analysis] xml file describing the calls related with wtg
  public static String wtgSpecFile = "";

  // [wtg analysis] turn on/off implicit intent resolution
  public static boolean implicitIntent = false;

  // [wtg analysis] turn on/off context-sensitive resolution on event handler
  // i.e. provide GUI object while analyzing the target window
  public static boolean resolveContext = true;

  // [wtg analysis] turn on/off time tracking running time for whole execution
  public static boolean trackWholeExec = false;

  // [wtg analysis] generate edges related to rotate, power and home
  public static boolean hardwareEvent = true;

  // [wtg analysis] number of threads building wtg edges
  public static int workerNum = 16;

  // [wtg analysis] detect resource leak
  public static int detectLeak = -1;

  // [wtg analysis] backward traversal depth to find successor
  public static int sDepth = 4;

  // [wtg analysis] handle asynchronous operations specially
  public static AsyncOpStrategy asyncStrategy = AsyncOpStrategy.Default_EventHandler_Async;

  // [wtg analysis] generate test cases
  public static boolean genTestCase = false;

  // [test generation] allow loop in wtg forward traversal
  public static boolean allowLoop = false;

  // [test generation] path exploration length
  public static int epDepth = 3;

  // [test generation] test cases generation strategy
  public static TestGenStrategy testGenStrategy = null;

  public static boolean instrument = false;

  // Mock testing flags
  public static boolean mockScene = false;

  // arguments for clients
  public static Set<String> clientParams = Sets.newHashSet();

  // Path output file name
  public static String pathoutfilename = "";

  public static String monitoredClass = "";

  public static boolean useAndroidStudio = false;

  public static String internalConstFile = Configs.class.getClassLoader().getResource("com_android_internal_R").getFile();

  public static boolean gaEnabled = false;
  public static boolean async = false;
  public static String gaScreenNameXmlOutputFile = "";
  public static String flowgraphOutput = "";
  public static String apktoolDir;

  public static void processing() {
    if (project.endsWith(".apk")) {
      if (useAndroidStudio) {
        Logger.warn("In APK mode, disable useAndroidStudio.");
        useAndroidStudio = false;
      }
      apkModeProcessing();
      return;
    }

    if (useAndroidStudio) {
      // hailong:
      bytecodes = project + "/app/build/intermediates/classes/debug/";
    } else {
      bytecodes = project + "/bin/classes";
    }

    depJars = Lists.newArrayList();
    File f;
    if (useAndroidStudio) {
      f = new File(project + "/libs");
    } else {
      f = new File(project + "/app/libs");
    }
    if (f.exists()) {
      File[] files = f.listFiles();
      for (File file : files) {
        String fn = file.getName();
        if (fn.endsWith(".jar")) {
          depJars.add(file.getAbsolutePath());
        }
      }
    }

    if (!useAndroidStudio) {
      Properties prop = new Properties();
      try {
        prop.load(new FileReader(project + "/project.properties"));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      // check target
      String tgt = prop.getProperty("target");
      if (tgt.startsWith(GOOGLE_API_PREFIX)) {
        tgt = "android-" + tgt.substring(GOOGLE_API_PREFIX.length());
      }
      if (!apiLevel.equals(tgt)) {
        System.err.println("Specified: " + apiLevel + ", project used: " + tgt);
        //System.exit(-1);
      }
      numericApiLevel = Integer.parseInt(apiLevel.substring("android-".length()));
      sysProj = Configs.sdkDir + "/platforms/" + Configs.apiLevel + "/data";
      // read libraries
      int i = 1;
      String lib;
      extLibs = Lists.newArrayList();
      while ((lib = prop.getProperty("android.library.reference." + i)) != null) {
        i++;
        extLibs.add(project + "/" + lib);
      }
    } else {
      numericApiLevel = Integer.parseInt(apiLevel.substring("android-".length()));
      sysProj = Configs.sdkDir + "/platforms/" + Configs.apiLevel + "/data";
      extLibs = Lists.newArrayList();
    }

    validate();
  }

  static void apkModeProcessing() {
    apkMode = true;
    bytecodes = project;

    sysProj = Configs.sdkDir + "/platforms/" + Configs.apiLevel + "/data";
//    Options.v().set_force_android_jar(Configs.android);
//    Options.v().set_android_jars(Configs.sdkDir + "/platforms");
//    Options.v().set_src_prec(Options.src_prec_apk);
//    Options.v().set_process_multiple_dex(true); // allow multiple dex files
    Scene.v().addBasicClass("java.lang.System", SootClass.SIGNATURES);
    Scene.v().addBasicClass("java.lang.Object", SootClass.BODIES);

    // make validate() happy
    depJars = Lists.newArrayList();
    extLibs = Lists.newArrayList();

    validate();
  }

  final static String GOOGLE_API_PREFIX = "Google Inc.:Google APIs:";

  public static void validate() {
    Class<Configs> cls = Configs.class;
    for (Field f : cls.getFields()) {
      if (f.getType().equals(String.class)) {
        try {
          Object res = f.get(null);
          if (res == null) {
            System.err.println("[Configs] You need to set `Configs."
                    + f.getName() + "'");
            System.exit(-1);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  public static int getAndroidAPILevel() {
    Preconditions.checkNotNull(apiLevel);
    if (apiLevel.startsWith("android-")) {
      return Integer.parseInt(apiLevel.substring(8));
    } else if (apiLevel.startsWith("google-")) {
      return Integer.parseInt(apiLevel.substring(7));
    } else {
      return -1;
    }
  }

  public static String getClientParamCode(String subStr) {
    if (Configs.clientParams.size() == 0) {
      return null;
    }
    for (String curStr : Configs.clientParams) {
      if (curStr.indexOf(subStr) == 0) {
        return new String(curStr);
      }
    }
    return null;
  }

  public enum TestGenStrategy {
    All_Window_Coverage,
    All_Edge_Coverage,
    Feasible_Edge_Coverage,
  }

  public enum AsyncOpStrategy {
    Default_EventHandler_Async,     // handle Activity.runOnUiThread and View.post as part of event handler
    Default_Special_Async,         // handle Activity.runOnUiThread and View.post as special event
    All_Special_Async,              // handle all the ops defined in the wtg.xml as part of event handler
    All_EventHandler_Async        // handle all the ops defined in the wtg.xml as special event
  }
}
