/*
 * IntentUtil.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.gui.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import edu.osu.cse.presto.android.gator.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

import javax.lang.model.SourceVersion;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class IntentUtil extends Util {
  private Map<SootClass, Multimap<String, Integer>> createPendingIntentMethods = Maps.newHashMap();
  private Map<SootClass, Set<String>> createTaskStackBuilderMethods = Maps.newHashMap();
  private Map<SootClass, Map<String, Pair<Integer, Boolean>>> taskStackBuilderAddIntentMethods = Maps.newHashMap();


  @Override
  protected void readByType(String type, Node role) {
    switch (type) {
      case "createPendingIntent":
        readCreatePendingIntentRole(role);
        break;
      case "createTaskStackBuilder":
        readCreateTaskStackBuilder(role);
        break;
      case "taskStackBuilderAddIntent":
        readTaskStackBuilderAddIntent(role);
        break;
      default:
        super.readByType(type, role);
        break;
    }
  }

  private void readTaskStackBuilderAddIntent(Node role) {
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
        Logger.err(getClass().getSimpleName(), "can not find soot class for "
                + classType);
      }
      NodeList invocations = classNode.getChildNodes();
      for (int j = 0; j < invocations.getLength(); j++) {
        Node invocation = invocations.item(j);
        if (!invocation.getNodeName().equals("invocation")) {
          continue;
        }
        Map<String, Pair<Integer, Boolean>> subsigPair = taskStackBuilderAddIntentMethods.get(sc);
        if (subsigPair == null) {
          subsigPair = Maps.newHashMap();
          taskStackBuilderAddIntentMethods.put(sc, subsigPair);
        }
        NodeList args = invocation.getChildNodes();
        for (int k = 0; k < args.getLength(); k++) {
          Node arg = args.item(k);
          if (!arg.getNodeName().equals("arg")) {
            continue;
          }
          Integer intentPos = Integer.parseInt(arg.getAttributes().getNamedItem("intentPos").getNodeValue());
          Boolean parent = Boolean.parseBoolean(arg.getAttributes().getNamedItem("parent").getNodeValue());
          String subsig = invocation.getAttributes().getNamedItem("subsig").getNodeValue();
          Pair<Integer, Boolean> posAndParent = new Pair<Integer, Boolean>(intentPos, parent);
          subsigPair.put(subsig, posAndParent);
        }
      }
    }
  }

  public boolean isTaskStackBuilderAddIntentCall(Stmt s) {
    if (!s.containsInvokeExpr()) {
      return false;
    }
    SootMethod mtd = s.getInvokeExpr().getMethod();
    SootClass declz = mtd.getDeclaringClass();
    String mtdSig = mtd.getSubSignature();
    for (SootClass clz : taskStackBuilderAddIntentMethods.keySet()) {
      boolean is = hier.isSubclassOf(declz, clz);
      if (is) {
        Map<String, Pair<Integer, Boolean>> subsigs = taskStackBuilderAddIntentMethods.get(clz);
        is = subsigs.keySet().contains(mtdSig);
        if (is) {
          return true;
        }
      }
    }
    return false;
  }

  public Pair<Integer, Boolean> getTaskStackBuilderAddIntentFields(Stmt s) {
    if (!s.containsInvokeExpr()) {
      return null;
    }
    SootMethod mtd = s.getInvokeExpr().getMethod();
    SootClass declz = mtd.getDeclaringClass();
    String mtdSig = mtd.getSubSignature();
    for (SootClass clz : taskStackBuilderAddIntentMethods.keySet()) {
      boolean is = hier.isSubclassOf(declz, clz);
      if (is) {
        Map<String, Pair<Integer, Boolean>> subsigPair = taskStackBuilderAddIntentMethods.get(clz);
        is = subsigPair.containsKey(mtdSig);
        if (is) {
          return subsigPair.get(mtdSig);
        }
      }
    }
    Logger.err(getClass().getSimpleName(), "we can not find the content fields for call: " + mtd);
    return null;
  }

  private void readCreateTaskStackBuilder(Node role) {
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
        Logger.err(getClass().getSimpleName(), "can not find soot class for "
                + classType);
      }
      NodeList invocations = classNode.getChildNodes();
      for (int j = 0; j < invocations.getLength(); j++) {
        Node invocation = invocations.item(j);
        if (!invocation.getNodeName().equals("invocation")) {
          continue;
        }
        String subsig = invocation.getAttributes().getNamedItem("subsig").getNodeValue();
        Set<String> subsigs = createTaskStackBuilderMethods.get(sc);
        if (subsigs == null) {
          subsigs = Sets.newHashSet();
          createTaskStackBuilderMethods.put(sc, subsigs);
        }
        subsigs.add(subsig);
      }
    }
  }

  public boolean isCreateTaskStackBuilderCall(Stmt s) {
    if (!s.containsInvokeExpr()) {
      return false;
    }
    SootMethod mtd = s.getInvokeExpr().getMethod();
    SootClass declz = mtd.getDeclaringClass();
    String mtdSig = mtd.getSubSignature();
    for (SootClass clz : createTaskStackBuilderMethods.keySet()) {
      boolean is = hier.isSubclassOf(declz, clz);
      if (is) {
        Set<String> subsigs = createTaskStackBuilderMethods.get(clz);
        is = subsigs.contains(mtdSig);
        if (is) {
          return true;
        }
      }
    }
    return false;
  }

  private void readCreatePendingIntentRole(Node role) {
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
        Logger.err(getClass().getSimpleName(), "can not find soot class for "
                + classType);
      }
      NodeList invocations = classNode.getChildNodes();
      for (int j = 0; j < invocations.getLength(); j++) {
        Node invocation = invocations.item(j);
        if (!invocation.getNodeName().equals("invocation")) {
          continue;
        }
        String subsig = invocation.getAttributes().getNamedItem("subsig")
                .getNodeValue();
        NodeList args = invocation.getChildNodes();
        for (int k = 0; k < args.getLength(); k++) {
          Node arg = args.item(k);
          if (!arg.getNodeName().equals("arg")) {
            continue;
          }
          String intentPos = arg.getAttributes().getNamedItem("intentPos").getNodeValue();
          Multimap<String, Integer> subsigPair = createPendingIntentMethods.get(sc);
          if (subsigPair == null) {
            subsigPair = HashMultimap.create();
            createPendingIntentMethods.put(sc, subsigPair);
          }
          subsigPair.put(subsig, Integer.parseInt(intentPos));
        }
      }
    }
  }

  public boolean isCreatePendingIntentCall(Stmt s) {
    if (!s.containsInvokeExpr()) {
      return false;
    }
    SootMethod mtd = s.getInvokeExpr().getMethod();
    SootClass declz = mtd.getDeclaringClass();
    String mtdSig = mtd.getSubSignature();
    for (SootClass clz : createPendingIntentMethods.keySet()) {
      boolean is = hier.isSubclassOf(declz, clz);
      if (is) {
        Multimap<String, Integer> subsigPair = createPendingIntentMethods.get(clz);
        is = subsigPair.containsKey(mtdSig);
        if (is) {
          return true;
        }
      }
    }
    return false;
  }

  public Collection<Integer> getCreatePendingIntentFields(Stmt s) {
    if (!s.containsInvokeExpr()) {
      return null;
    }
    SootMethod mtd = s.getInvokeExpr().getMethod();
    SootClass declz = mtd.getDeclaringClass();
    String mtdSig = mtd.getSubSignature();
    for (SootClass clz : createPendingIntentMethods.keySet()) {
      boolean is = hier.isSubclassOf(declz, clz);
      if (is) {
        Multimap<String, Integer> subsigPair = createPendingIntentMethods.get(clz);
        is = subsigPair.containsKey(mtdSig);
        if (is) {
          return subsigPair.get(mtdSig);
        }
      }
    }
    Logger.err(getClass().getSimpleName(), "we can not find the content fields for call: " + mtd);
    return null;
  }

  public boolean isValidClassName(String name) {
    return SourceVersion.isName(name);
  }
}
