/*
 * NGoogleAnalyticsTimingBuilderNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.analytics;

import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NAllocNode;
import soot.SootMethod;
import soot.jimple.Expr;
import soot.jimple.Stmt;

public class NGoogleAnalyticsTimingBuilderNode extends NAllocNode {
  public Stmt allocStmt;
  public SootMethod allocMethod;

  public NGoogleAnalyticsTimingBuilderNode(Expr e, Stmt allocStmt, SootMethod allocMethod) {
    this.e = e;
    this.allocStmt = allocStmt;
    this.allocMethod = allocMethod;
  }
}
