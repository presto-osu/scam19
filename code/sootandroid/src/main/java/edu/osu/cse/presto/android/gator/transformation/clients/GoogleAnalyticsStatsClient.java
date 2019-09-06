/*
 * GoogleAnalyticsStatsClient.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.transformation.clients;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.osu.cse.presto.android.gator.Logger;
import edu.osu.cse.presto.android.gator.transformation.Transformation;
import edu.osu.cse.presto.android.gator.xml.ApktoolResXMLReader;
import edu.osu.cse.presto.android.gator.xml.NameValueFunction;
import soot.*;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Sources;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class GoogleAnalyticsStatsClient implements Transformation.Client {
  final String TAG = GoogleAnalyticsStatsClient.class.getSimpleName();

  @Override
  public void run() {
    Stream<SootClass> clsStream = Scene.v().getApplicationClasses().parallelStream();
    clsStream.forEach(new Consumer<SootClass>() {
      @Override
      public void accept(SootClass sootClass) {
        if (!sootClass.isConcrete())
          return;
        if (sootClass.getName().startsWith("com.google.android")
                || sootClass.getName().startsWith("android.support")) {
          return;
        }
        Stream<SootMethod> mtdStream = sootClass.getMethods().parallelStream();
        mtdStream.forEach(new Consumer<SootMethod>() {
          @Override
          public void accept(SootMethod sootMethod) {
            if (!sootMethod.isConcrete())
              return;
            Body b = sootMethod.retrieveActiveBody();

            Stream<Unit> stmtStream = b.getUnits().parallelStream();
            stmtStream.forEach(new Consumer<Unit>() {
              @Override
              public void accept(Unit unit) {
                Stmt currentStmt = (Stmt) unit;
                if (currentStmt.containsInvokeExpr()) {
                  InvokeExpr ie = currentStmt.getInvokeExpr();
                  SootMethodRef stm = ie.getMethodRef(); // static target
                  if (stm.getSignature().equals("<com.google.android.gms.analytics.GoogleAnalytics: com.google.android.gms.analytics.Tracker newTracker(int)>")) {
                    Value v = ie.getArg(0);
                    if (!(v instanceof IntConstant)) {
                      Logger.info(TAG, "newtracker.int.notconstant:::" + currentStmt);
                      return;
                    }
                    Logger.info(TAG, "newtracker.int.constant:::" + currentStmt);
                    int resId = ((IntConstant) v).value;
                    HashMap<String, Integer> rXmlMap = Maps.newHashMap();
                    HashMap<Integer, String> invRXmlMap = Maps.newHashMap();
                    ApktoolResXMLReader.v().readIds("xml", NameValueFunction.mapInvMap(rXmlMap, invRXmlMap));
                    String autoString = ApktoolResXMLReader.v().readXmlFirst(invRXmlMap.get(resId), "bool", "ga_autoActivityTracking");
                    if (autoString != null && autoString.equalsIgnoreCase("true")) {
                      Logger.info(TAG, "auto.true.xml");
                    }
                  } else if (stm.getSignature().equals("<com.google.android.gms.analytics.GoogleAnalytics: com.google.android.gms.analytics.Tracker newTracker(java.lang.String)>")) {
                    Logger.info(TAG, "newtracker.string");
                  } else if (stm.getSignature().equals("<com.google.android.gms.analytics.Tracker: void setScreenName(java.lang.String)>")) {
                    Logger.info(TAG, "setscreenname");
                  } else if (stm.getSignature().equals("<com.google.android.gms.analytics.Tracker: void send(java.util.Map)>")) {
                    Logger.info(TAG, "send");
                  } else if (stm.getSignature().equals("<com.google.android.gms.analytics.HitBuilders$ScreenViewBuilder: java.util.Map build()>")) {
                    Logger.info(TAG, "build.ScreenViewBuilder");
                  } else if (stm.getSignature().equals("<com.google.android.gms.analytics.HitBuilders$ExceptionBuilder: java.util.Map build()>")) {
                    Logger.info(TAG, "build.ExceptionBuilder");
                  } else if (stm.getSignature().equals("<com.google.android.gms.analytics.HitBuilders$TimingBuilder: java.util.Map build()>")) {
                    Logger.info(TAG, "build.TimingBuilder");
                  } else if (stm.getSignature().equals("<com.google.android.gms.analytics.HitBuilders$SocialBuilder: java.util.Map build()>")) {
                    Logger.info(TAG, "build.SocialBuilder");
                  } else if (stm.getSignature().equals("<com.google.android.gms.analytics.HitBuilders$EventBuilder: java.util.Map build()>")) {
                    Logger.info(TAG, "build.EventBuilder");
                  } else if (stm.getSubSignature().getString().equals("com.google.android.gms.analytics.HitBuilders$HitBuilder setCampaignParamsFromUrl(java.lang.String)")) {
                    Logger.info(TAG, "campaign");
                  } else if (stm.getSubSignature().getString().equals("com.google.android.gms.analytics.HitBuilders$HitBuilder addProduct(com.google.android.gms.analytics.ecommerce.Product)")
                          || stm.getSubSignature().getString().equals("com.google.android.gms.analytics.HitBuilders$HitBuilder addPromotion(com.google.android.gms.analytics.ecommerce.Promotion)")
                          || stm.getSubSignature().getString().equals("com.google.android.gms.analytics.HitBuilders$HitBuilder addImpression(com.google.android.gms.analytics.ecommerce.Product,java.lang.String)")
                          || stm.getSubSignature().getString().equals("com.google.android.gms.analytics.HitBuilders$HitBuilder setProductAction(com.google.android.gms.analytics.ecommerce.ProductAction)")
                          || stm.getSubSignature().getString().equals("com.google.android.gms.analytics.HitBuilders$HitBuilder setPromotionAction(java.lang.String)")) {
                    Logger.info(TAG, "ecommerce");
                  } else if (stm.getSubSignature().getString().equals("com.google.android.gms.analytics.HitBuilders$HitBuilder setCustomDimension(int,java.lang.String)")
                          || stm.getSubSignature().getString().equals("com.google.android.gms.analytics.HitBuilders$HitBuilder setCustomMetric(int,float)")) {
                    Logger.info(TAG, "custom");
                  }
                }
              }
            });
          }
        });
      }
    });
  }

  private Set<String> tracked = Sets.newConcurrentHashSet();

  void track(String tag, SootMethodRef stm) {
    if (tracked.contains(tag))
      return;
    tracked.add(tag);
    SootMethod mtd = stm.resolve();
    Set<List<SootMethod>> traces = Sets.newHashSet();

    final CallGraph cg = Scene.v().getCallGraph();
    track(cg, mtd, Lists.newLinkedList(), traces, Sets.newHashSet());
    for (List<SootMethod> trace : traces) {
      Logger.info(TAG, tag + ".trace: " + trace.toString());
    }
  }

  void track(CallGraph cg, SootMethod tgt, List<SootMethod> trace, Set<List<SootMethod>> traces, Set<SootMethod> visited) {
    if (visited.contains(tgt)) {
      return;
    }
    visited.add(tgt);

    Sources incoming = new Sources(cg.edgesInto(tgt));
    if (!incoming.hasNext()) {
      traces.add(Lists.newArrayList(trace));
      return;
    }
    while (incoming.hasNext()) {
      SootMethod src = (SootMethod) incoming.next();
      if (src.getDeclaringClass().getJavaPackageName().startsWith("android.support")
              || src.getDeclaringClass().getJavaPackageName().startsWith("com.google.android.gms")) {
        continue;
      }
      trace.add(0, src);
      track(cg, src, trace, traces, visited);
      trace.remove(0);
    }
  }
}
