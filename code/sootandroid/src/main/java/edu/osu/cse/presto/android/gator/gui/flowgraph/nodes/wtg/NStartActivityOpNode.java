/*
 * NStartActivityOpNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */
package edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.wtg;

import edu.osu.cse.presto.android.gator.Logger;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NNode;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NOpNode;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NVarNode;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

public class NStartActivityOpNode extends NOpNode {
  SootMethod startActivityMethod;

  public NStartActivityOpNode(NNode activityNode, NNode intentNode,
                              Pair<Stmt, SootMethod> callSite) {
    super(callSite, false);
    activityNode.addEdgeTo(this);
    intentNode.addEdgeTo(this);
    Stmt s = callSite.getO1();
    startActivityMethod = s.getInvokeExpr().getMethod();
    // sanity check
    if (getReceiver() == null) {
      Logger.err(getClass().getSimpleName(), "NStartActivityOpNode should have receiver node");
    }
  }
  @Override
  public NVarNode getReceiver() {
    return (NVarNode) this.pred.get(0);
  }

  @Override
  public boolean hasReceiver() {
    return true;
  }
  @Override
  public boolean hasParameter() {
    return true;
  }
  @Override
  public NNode getParameter() {
    return this.pred.get(1);
  }
  @Override
  public boolean hasLhs() {
    return false;
  }
}
