/*
 * Main.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */
package edu.osu.cse.presto.android.gator;

import org.junit.Assert;
import soot.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Map;

/**
 * The main class for SootAndroid.
 */
public class Main {

  public static void main(String[] args) {
    Debug.v().setStartTime();
    parseArgs(args);
    checkAndPrintEnvironmentInformation(args);
    setupAndInvokeSoot();
  }

  /**
   * Parse the command line arguments for flag values. We, intentionally, do not
   * use any flag library so that everything is explicit and clear.
   *
   * @param args the command line arguments passed from main().
   */
  public static void parseArgs(String[] args) {
    for (int i = 0; i < args.length; i++) {
      String s = args[i];
      if ("-project".equals(s)) {
        Configs.project = args[++i];
      } else if ("-benchmarkName".equals(s)) {
        Configs.benchmarkName = args[++i];
      } else if ("-sdkDir".equals(s)) {
        Configs.sdkDir = args[++i];
      } else if ("-apiLevel".equals(s)) {
        Configs.apiLevel = args[++i];
      } else if ("-android".equals(s)) {
        Configs.android = args[++i];
      } else if ("-jre".equals(s)) {
        Configs.jre = args[++i];
      } else if ("-guiAnalysis".equals(s)) {
        if (Configs.transformation) {
          Logger.err("Cannot set 'guiAnalysis' and 'transformation' at the same time");
        }
        Configs.guiAnalysis = true;
      } else if ("-transformation".equals(s)) {
        // hailong:
        if (Configs.guiAnalysis) {
          Logger.err("Cannot set 'guiAnalysis' and 'transformation' at the same time");
        }
        Configs.transformation = true;
      } else if ("-verbose".equals(s)) {
        Configs.verbose = true;
        Logger.setVerbose(true);
      } else if ("-debugCode".equals(s)) {
        Configs.debugCodes.add(args[++i]);
      } else if ("-client".equals(s)) {
        Configs.clients.add(args[++i]);
      } else if ("-withCHA".equals(s)) {
        Configs.withCHA = true;
      } else if ("-withSPARK".equals(s)) {
        // hailong:
        Configs.withSPARK = true;
      } else if ("-listenerSpecFile".equals(s)) {
        Configs.listenerSpecFile = args[++i];
      } else if ("-wtgSpecFile".equals(s)) {
        Configs.wtgSpecFile = args[++i];
      } else if ("-implicitIntent".equals(s)) {
        Configs.implicitIntent = true;
      } else if ("-instrument".equals(s)) {
        Configs.instrument = true;
      } else if ("-resolveContext".equals(s)) {
        Configs.resolveContext = true;
      } else if ("-trackWholeExec".equals(s)) {
        Configs.trackWholeExec = true;
      } else if ("-worker".equals(s)) {
        Configs.workerNum = Integer.parseInt(args[++i]);
        Assert.assertTrue("[Error]: number of workers should be >= 1", Configs.workerNum > 0);
      } else if ("-mockScene".equals(s)) {
        Configs.mockScene = true;
      } else if ("-hardwareEvent".equals(s)) {
        Configs.hardwareEvent = true;
      } else if ("-detectLeak".equals(s)) {
        Configs.detectLeak = Integer.parseInt(args[++i]);
      } else if ("-strategy".equals(s)) {
        Configs.testGenStrategy = Configs.TestGenStrategy.valueOf(args[++i]);
        Assert.assertNotNull("[Error]: strategy should not be null", Configs.testGenStrategy);
      } else if ("-succDepth".equals(s)) {
        Configs.sDepth = Integer.parseInt(args[++i]);
        Assert.assertTrue(Configs.sDepth > 0);
      } else if ("-allowLoop".equals(s)) {
        Configs.allowLoop = true;
      } else if ("-epDepth".equals(s)) {
        Configs.epDepth = Integer.parseInt(args[++i]);
      } else if ("-clientParam".equals(s)) {
        Configs.clientParams.add(args[++i]);
      } else if ("-async".equals(s)) {
        Configs.asyncStrategy = Configs.AsyncOpStrategy.valueOf(args[++i]);
      } else if ("-genTestCase".equals(s)) {
        Configs.genTestCase = true;
      } else if ("-outputFile".equals(s)) {
        Configs.pathoutfilename = args[++i];
      } else if ("-monitoredClass".equals(s)) {
        Configs.monitoredClass = args[++i];
      } else if ("-useAndroidStudio".equals(s)) {
        Configs.useAndroidStudio = true;
      } else if ("-gaEnabled".equals(s)) {
        Configs.gaEnabled = true;
      } else if ("-async".equals(s)) {
        Configs.async = true;
      } else if ("-gaXmlOutput".equals(s)) {
        if (!Configs.gaEnabled)
          Logger.err("Please enable GA features with '-gaEnabled'.");
        Configs.gaScreenNameXmlOutputFile = args[++i];
      } else if ("-flowgraphOutput".equals(s)) {
        Configs.flowgraphOutput = args[++i];
      } else if ("-apktoolDir".equals(s)) {
        Configs.apktoolDir = args[++i];
      } else {
        throw new RuntimeException("Unknown option: " + s);
      }
    }
    Configs.processing();
  }

  /**
   * Print out information about execution environment.
   *
   * @param args
   */
  static void checkAndPrintEnvironmentInformation(String[] args) {
    // Now, we only support *nix systems. SootAndroid has been tested on Ubuntu
    // and Mac OS X.
    String OS = System.getProperty("os.name").toLowerCase();
    if (OS.indexOf("win") >= 0) {
      throw new RuntimeException("Windows detected!");
    }

    // Print out some information about the execution commands.
    if (Configs.verbose) {
      // The complete Java command
      String cmd = System.getProperty("sun.java.command");
      System.out.println("\n[command] " + cmd);

      // The VM arguments
      RuntimeMXBean RuntimemxBean = ManagementFactory.getRuntimeMXBean();
      List<String> arguments = RuntimemxBean.getInputArguments();
      for (String a : arguments) {
        System.out.print("  [VM-Arg] " + a);
      }
      System.out.println("\n");

      // The arguments after main class is specified
      for (String s : args) {
        System.out.println("  [MAIN-ARG] " + s);
      }
      System.out.println("\n");
    }
  }

  /**
   * Computes the classpath to be used by soot.
   */
  static String computeClasspath() {
    // Compute classpath
    StringBuffer classpathBuffer =
            new StringBuffer(Configs.android);
    if (!Configs.jre.isEmpty())
      classpathBuffer.append(":" + Configs.jre);
    for (String s : Configs.depJars) {
      classpathBuffer.append(":" + s);
    }

    // TODO(tony): add jar files of third-party libraries if necessary
    for (String s : Configs.extLibs) {
      classpathBuffer.append(":" + s + "/bin/classes");
    }

    return classpathBuffer.toString();
  }

  /**
   * Invoke soot.Main.main() with proper arguments.
   */
  static void setupAndInvokeSoot() {
    // Setup an artificial phase to call into our analysis entrypoint. We can
    // run it with or without call graph construction (CHA is chosen here).
    if (Configs.apkMode) {
      String classpath = computeClasspath();
      Logger.verb("SETUP", "classpath : " + classpath);
      String packName = "wstp";
      String phaseName = String.format("%s.%s", packName, "gui");
      if (Configs.withSPARK) {
        String[] sootArgs = {
                "-src-prec", "apk",
                "-process-multiple-dex",
                "-android-jars", Configs.sdkDir + "/platforms",
                "-ws",
                "-p", "jb", "stabilize-local-names:true",
                "-p", phaseName, "enabled:true",
                "-f", "n",
                "-keep-line-number",
                "-allow-phantom-refs",
                "-process-dir", Configs.bytecodes,
                "-p", "cg", "all-reachable:true",
                "-p", "cg.spark", "enabled:true",
        };
        setupAndInvokeSootHelper(packName, phaseName, sootArgs);
      } else {
        String[] sootArgs = {
                "-src-prec", "apk",
                "-process-multiple-dex",
                "-android-jars", Configs.sdkDir + "/platforms",
                "-ws",
                "-p", "jb", "stabilize-local-names:true",
                "-p", phaseName, "enabled:true",
                "-f", "n",
                "-keep-line-number",
                "-allow-phantom-refs",
                "-process-dir", Configs.bytecodes,
                "-p", "cg", "all-reachable:true",
        };
        setupAndInvokeSootHelper(packName, phaseName, sootArgs);
      }
    } else {
      String classpath = computeClasspath();
      Logger.verb("SETUP", "classpath : " + classpath);
      if (Configs.withCHA) {
        String packName = "wjtp";
        String phaseName = "wjtp.gui";
        String[] sootArgs = {
                "-w",
                "-p", "cg", "all-reachable:true",
                "-p", "cg.cha", "enabled:true",
                "-p", phaseName, "enabled:true",
                "-f", "n",
                "-keep-line-number",
                "-allow-phantom-refs",
                "-process-dir", Configs.bytecodes,
                "-cp", classpath,
        };
        setupAndInvokeSootHelper(packName, phaseName, sootArgs);
      } else {
        String packName = "cg";
        String phaseName = "cg.gui";
        String[] sootArgs = {
                "-w",
                "-p", phaseName, "enabled:true",
                "-f", "n",
                "-keep-line-number",
                "-allow-phantom-refs",
                //      "-coffi",
                "-process-dir", Configs.bytecodes,
                "-cp", classpath,
        };
        setupAndInvokeSootHelper(packName, phaseName, sootArgs);
      }
    }
  }

  /**
   * Prepare a soot plugin that calls into our analysis entrypoint, and then
   * invoke soot with the plugin enabled.
   */
  static void setupAndInvokeSootHelper(String packName, String phaseName,
                                       String[] sootArgs) {
    // Create the phase and add it to the pack
    Pack pack = PackManager.v().getPack(packName);
    pack.add(new Transform(phaseName, new SceneTransformer() {
      @Override
      protected void internalTransform(String phaseName,
                                       Map<String, String> options) {
        AnalysisEntrypoint.v().run();
      }
    }));

    // Print out arguments to Soot for debugging
    if (Configs.verbose) {
      for (String s : sootArgs) {
        System.out.println("  [SOOT-ARG] " + s);
      }
    }

    Logger.info("Gator", "Start analyzing " + Configs.project);

    // Finally, invoke Soot
    soot.Main.main(sootArgs);
  }
}
