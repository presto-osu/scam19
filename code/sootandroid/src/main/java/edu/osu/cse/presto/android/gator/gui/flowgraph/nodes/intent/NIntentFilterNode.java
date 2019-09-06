/*
 * NIntentFilterNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.intent;

import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NObjectNode;
import edu.osu.cse.presto.android.gator.gui.intent.IntentFilter;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Expr;
import soot.jimple.Stmt;

public class NIntentFilterNode extends NObjectNode {
  public static final SootClass CLASS = Scene.v().getSootClass("android.content.IntentFilter");

  public Stmt allocStmt;
  public SootMethod allocMethod;

  public IntentFilter filter = new IntentFilter();

  public NIntentFilterNode(Expr e, Stmt allocStmt, SootMethod allocMethod) {
    this.allocStmt = allocStmt;
    this.allocMethod = allocMethod;
  }

  @Override
  public SootClass getClassType() {
    return CLASS;
  }

  @Override
  public String toString() {
    return "IntentFilter[" + filter+ "]" + id;
  }
}
