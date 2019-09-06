/*
 * GAUtil.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.gui.util;

import com.google.common.collect.Maps;
import edu.osu.cse.presto.android.gator.Configs;
import edu.osu.cse.presto.android.gator.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.jimple.Stmt;
import soot.jimple.spark.ondemand.genericutil.HashSetMultiMap;
import soot.util.NumberedString;

import java.util.Map;
import java.util.Set;

/**
 * Utilities for Google Analytics APIs.
 *
 * @see <a href="https://developers.google.com/analytics/devguides/collection/android/v4">Analytics in Android</a>
 * @see <a href="https://developers.google.com/analytics/devguides/collection/protocol/v1/devguide">Measurement Protocol </a>
 */
public class GAUtil extends IntentUtil {
  // <class, <subsig, name_pos>>
  private final Map<SootClass, Map<String, Integer>> googleAnalyticsSetScreenNameMethods = Maps.newHashMap();
  // <class, <subsig, bundle_pos>>
  private final Map<SootClass, Map<String, Integer>> googleAnalyticsTrackerSendMethods = Maps.newHashMap();
  // <class, <subsig, pos>>
  private final Map<SootClass, Map<String, Integer>> googleAnalyticsNewTrackerMethods = Maps.newHashMap();
  // <class, <subsig, pos>>
  private final Map<SootClass, Map<String, Integer>> googleAnalyticsAutoActivityTrackingMethods = Maps.newHashMap();
  // <class, <subsig>>
  private final HashSetMultiMap<SootClass, String> googleAnalyticsScreenViewBuilderBuildMethods = new HashSetMultiMap<>();
  // <class, <subsig>>
  private final HashSetMultiMap<SootClass, String> googleAnalyticsExceptionBuilderBuildMethods = new HashSetMultiMap<>();
  // <class, <subsig>>
  private final HashSetMultiMap<SootClass, String> googleAnalyticsTimingBuilderBuildMethods = new HashSetMultiMap<>();
  // <class, <subsig>>
  private final HashSetMultiMap<SootClass, String> googleAnalyticsSocialBuilderBuildMethods = new HashSetMultiMap<>();
  // <class, <subsig>>
  private final HashSetMultiMap<SootClass, String> googleAnalyticsEventBuilderBuildMethods = new HashSetMultiMap<>();
  // <class, <subsig, context_pos>>
  private final Map<SootClass, Map<String, Integer>> googleAnalyticsGetInstanceMethods = Maps.newHashMap();


  public static synchronized GAUtil v() {
    if (util == null) {
      util = new GAUtil();
      util.readFromSpecificationFile(Configs.wtgSpecFile);
    }
    return (GAUtil) util;
  }

  public boolean isLibraryClass(SootClass c) {
    String name = c.getName();
    if (name.startsWith("com.google.android.")
            || name.startsWith("com.google.gson")
            || name.startsWith("com.google.zxing")
            || name.startsWith("com.google.common.")
            || name.startsWith("com.google.ads.")
            || name.startsWith("com.google.")
            || name.startsWith("com.crashlytics")
            || name.startsWith("com.urbanairship")
            || name.startsWith("com.facebook")
            || name.startsWith("com.samsung.android.sdk")
            || name.startsWith("android.")
            || name.startsWith("com.android.")
            || name.startsWith("com.amazonaws.")
            || name.startsWith("com.actionbarsherlock.")
            || name.startsWith("org.jsoup")
            || name.startsWith("com.alibaba.fastjson.")
            || name.startsWith("org.apache.")
            || name.startsWith("com.microsoft.appcenter.")
            || name.startsWith("rx.")
            || name.startsWith("retrofit.")
            || name.startsWith("retrofit2.")
            || name.startsWith("org.slf4j.")
            || name.startsWith("okio.")
            || name.startsWith("okhttp3.")
            || name.startsWith("kotlin.")
            || name.startsWith("io.reactivex.")
            || name.startsWith("io.fabric.")
            || name.startsWith("com.flurry.")
            || name.startsWith("com.fasterxml.jackson.")
            || name.startsWith("com.viewpagerindicator.")) {
      return true;
    }
    return false;
  }

  @Override
  protected void readByType(String type, Node role) {
    switch (type) {
      case "googleAnalyticsGetInstance":
        readGoogleAnalyticsRole(role, googleAnalyticsGetInstanceMethods, "contextPos");
        break;
      case "googleAnalyticsSetScreenName":
        readGoogleAnalyticsRole(role, googleAnalyticsSetScreenNameMethods, "namePos");
        break;
      case "googleAnalyticsTrackerSend":
        readGoogleAnalyticsRole(role, googleAnalyticsTrackerSendMethods, "bundlePos");
        break;
      case "googleAnalyticsNewTracker":
        readGoogleAnalyticsRole(role, googleAnalyticsNewTrackerMethods, "pos");
        break;
      case "googleAnalyticsAutoActivityTracking":
        readGoogleAnalyticsRole(role, googleAnalyticsAutoActivityTrackingMethods, "pos");
        break;
      case "googleAnalyticsScreenViewBuilderBuild":
        readGoogleAnalyticsRole(role, googleAnalyticsScreenViewBuilderBuildMethods);
        break;
      case "googleAnalyticsExceptionBuilderBuild":
        readGoogleAnalyticsRole(role, googleAnalyticsExceptionBuilderBuildMethods);
        break;
      case "googleAnalyticsTimingBuilderBuild":
        readGoogleAnalyticsRole(role, googleAnalyticsTimingBuilderBuildMethods);
        break;
      case "googleAnalyticsSocialBuilderBuild":
        readGoogleAnalyticsRole(role, googleAnalyticsSocialBuilderBuildMethods);
        break;
      case "googleAnalyticsEventBuilderBuild":
        readGoogleAnalyticsRole(role, googleAnalyticsEventBuilderBuildMethods);
        break;
      case "googleAnalyticsCampaignOps":
        readGoogleAnalyticsCampaignOpsRole(role);
        break;
      case "googleAnalyticsEcommerceOps":
        readGoogleAnalyticsEcommerceOpsRole(role);
        break;
      case "googleAnalyticsCustomOps":
        readGoogleAnalyticsCustomOpsRole(role);
        break;
      default:
        super.readByType(type, role);
        break;
    }
  }


  /////////////////////////////////////////
  ///// check calls
  /////////////////////////////////////////
  public boolean isGoogleAnalyticsGetInstanceCall(Stmt s) {
    return isGoogleAnalyticsCall(s, googleAnalyticsGetInstanceMethods);
  }

  public boolean isGoogleAnalyticsScreenViewBuilderBuildCall(Stmt s) {
    return isGoogleAnalyticsCall(s, googleAnalyticsScreenViewBuilderBuildMethods);
  }

  public boolean isGoogleAnalyticsExceptionBuilderBuildCall(Stmt s) {
    return isGoogleAnalyticsCall(s, googleAnalyticsExceptionBuilderBuildMethods);
  }

  public boolean isGoogleAnalyticsTimingBuilderBuildCall(Stmt s) {
    return isGoogleAnalyticsCall(s, googleAnalyticsTimingBuilderBuildMethods);
  }

  public boolean isGoogleAnalyticsSocialBuilderBuildCall(Stmt s) {
    return isGoogleAnalyticsCall(s, googleAnalyticsSocialBuilderBuildMethods);
  }

  public boolean isGoogleAnalyticsEventBuilderBuildCall(Stmt s) {
    return isGoogleAnalyticsCall(s, googleAnalyticsEventBuilderBuildMethods);
  }


  public boolean isGoogleAnalyticsTrackerSendCall(Stmt s) {
    return isGoogleAnalyticsCall(s, googleAnalyticsTrackerSendMethods);
  }


  public boolean isGoogleAnalyticsNewTrackerCall(Stmt s) {
    return isGoogleAnalyticsCall(s, googleAnalyticsNewTrackerMethods);
  }


  public boolean isGoogleAnalyticsSetScreenNameCall(Stmt s) {
    return isGoogleAnalyticsCall(s, googleAnalyticsSetScreenNameMethods);
  }


  public boolean isGoogleAnalyticsAutoActivityTrackingCall(Stmt s) {
    return isGoogleAnalyticsCall(s, googleAnalyticsAutoActivityTrackingMethods);
  }

  private boolean isGoogleAnalyticsCall(Stmt s, HashSetMultiMap<SootClass, String> methods) {
    if (!s.containsInvokeExpr()) {
      return false;
    }
    SootMethodRef mtd = s.getInvokeExpr().getMethodRef();
    SootClass declz = mtd.declaringClass();
    NumberedString mtdSig = mtd.getSubSignature();
    for (SootClass clz : methods.keySet()) {
      boolean is = hier.isSubclassOf(declz, clz);
      if (is) {
        Set<String> subsigs = methods.get(clz);
        is = subsigs.contains(mtdSig.getString());
        if (is) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isGoogleAnalyticsCall(Stmt s, Map<SootClass, Map<String, Integer>> methods) {
    if (!s.containsInvokeExpr()) {
      return false;
    }
    SootMethodRef mtd = s.getInvokeExpr().getMethodRef();
    SootClass declz = mtd.declaringClass();
    NumberedString mtdSig = mtd.getSubSignature();
    for (SootClass clz : methods.keySet()) {
      boolean is = hier.isSubclassOf(declz, clz);
      if (is) {
        Map<String, Integer> subsigPair = methods.get(clz);
        is = subsigPair.containsKey(mtdSig.getString());
        if (is) {
          return true;
        }
      }
    }
    return false;
  }

  /////////////////////////////////////////
  ///// get parameter position
  /////////////////////////////////////////
  public Integer getGoogleAnlayticsGetInstanceContextPos(Stmt s) {
    return getGoogleAnlayticsArgPos(s, googleAnalyticsGetInstanceMethods, "we can not find the context position");
  }

  public Integer getGoogleAnlayticsAutoActivityTrackingParamPos(Stmt s) {
    return getGoogleAnlayticsArgPos(s, googleAnalyticsAutoActivityTrackingMethods, "we can not find the param position");
  }


  public Integer getGoogleAnlayticsNewTrackerParamPos(Stmt s) {
    return getGoogleAnlayticsArgPos(s, googleAnalyticsNewTrackerMethods, "we can not find the param position");
  }


  public Integer getGoogleAnlayticsSetScreenNamePos(Stmt s) {
    return getGoogleAnlayticsArgPos(s, googleAnalyticsSetScreenNameMethods, "we can not find the name position");
  }


  public Integer getGoogleAnlayticsTrackerSendMapPos(Stmt s) {
    return getGoogleAnlayticsArgPos(s, googleAnalyticsTrackerSendMethods, "we can not find the map position");
  }


  public Integer getGoogleAnlayticsArgPos(Stmt s, Map<SootClass, Map<String, Integer>> map, String errorMsg) {
    if (!s.containsInvokeExpr()) {
      return null;
    }
    SootMethod mtd = s.getInvokeExpr().getMethod();
    SootClass declz = mtd.getDeclaringClass();
    String mtdSig = mtd.getSubSignature();
    for (SootClass clz : map.keySet()) {
      boolean is = hier.isSubclassOf(declz, clz);
      if (is) {
        Map<String, Integer> subsigPair = map.get(clz);
        is = subsigPair.containsKey(mtdSig);
        if (is) {
          return subsigPair.get(mtdSig);
        }
      }
    }
    Logger.err(getClass().getSimpleName(), "For " + mtdSig + ", " + errorMsg);
    return null;
  }


  /////////////////////////////////////////
  ///// read xml
  /////////////////////////////////////////
  void readGoogleAnalyticsRole(Node role, Map<SootClass, Map<String, Integer>> map, String posTag) {
    NodeList classes = role.getChildNodes();
    for (int i = 0; i < classes.getLength(); i++) {
      Node classNode = classes.item(i);
      if (!classNode.getNodeName().equals("class")) {
        continue;
      }
      String classType = classNode.getAttributes().getNamedItem("type")
              .getNodeValue();
      SootClass sc = Scene.v().getSootClass(classType);
      if (sc == null) {
        continue;
      }
      NodeList invocations = classNode.getChildNodes();
      for (int j = 0; j < invocations.getLength(); j++) {
        Node invocation = invocations.item(j);
        if (!invocation.getNodeName().equals("invocation")) {
          continue;
        }
        String subsig = invocation.getAttributes().getNamedItem("subsig")
                .getNodeValue();
        Map<String, Integer> subsigPair = map.get(sc);
        if (subsigPair == null) {
          subsigPair = Maps.newHashMap();
          map.put(sc, subsigPair);
        }
        NodeList args = invocation.getChildNodes();
        for (int k = 0; k < args.getLength(); k++) {
          Node arg = args.item(k);
          if (!arg.getNodeName().equals("arg")) {
            continue;
          }
          String pos = arg.getAttributes().getNamedItem(posTag).getNodeValue();
          subsigPair.put(subsig, Integer.parseInt(pos));
        }
      }
    }
  }


  void readGoogleAnalyticsRole(Node role, HashSetMultiMap<SootClass, String> methods) {
    NodeList classes = role.getChildNodes();
    for (int i = 0; i < classes.getLength(); i++) {
      Node classNode = classes.item(i);
      if (!classNode.getNodeName().equals("class")) {
        continue;
      }
      String classType = classNode.getAttributes().getNamedItem("type")
              .getNodeValue();
      SootClass sc = Scene.v().getSootClass(classType);
      if (sc == null) {
        continue;
      }
      NodeList invocations = classNode.getChildNodes();
      for (int j = 0; j < invocations.getLength(); j++) {
        Node invocation = invocations.item(j);
        if (!invocation.getNodeName().equals("invocation")) {
          continue;
        }
        String subsig = invocation.getAttributes().getNamedItem("subsig")
                .getNodeValue();
        methods.put(sc, subsig);
      }
    }
  }

  // TODO:
  private void readGoogleAnalyticsCustomOpsRole(Node role) {
  }

  // TODO:
  private void readGoogleAnalyticsEcommerceOpsRole(Node role) {
  }

  // TODO:
  private void readGoogleAnalyticsCampaignOpsRole(Node role) {
  }
}
