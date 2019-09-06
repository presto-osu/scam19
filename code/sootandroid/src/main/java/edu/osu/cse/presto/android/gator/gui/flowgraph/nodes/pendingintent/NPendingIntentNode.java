/*
 * NPendingIntentNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.pendingintent;

import com.google.common.collect.Sets;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NObjectNode;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Expr;
import soot.jimple.Stmt;

import java.util.List;
import java.util.Set;

// Pending intent
public class NPendingIntentNode extends NObjectNode {
  public Stmt allocStmt;
  public SootMethod allocMethod;
  public Set<List<SootClass>> intents;

  public NPendingIntentNode(Expr e, Stmt allocStmt, SootMethod allocMethod) {
    this.allocStmt = allocStmt;
    this.allocMethod = allocMethod;
    intents = Sets.newHashSet();
  }

  public Set<SootClass> getPrimaryIntents() {
    Set<SootClass> primaryIntent = Sets.newHashSet();
    for (List<SootClass> i : intents) {
      primaryIntent.add(i.get(i.size() - 1));
    }
    return primaryIntent;
  }

  @Override
  public SootClass getClassType() {
    return Scene.v().getSootClass("android.app.PendingIntent");
  }

  @Override
  public String toString() {
    return "PendingIntent[" + intents + "]" + id;
  }
}
