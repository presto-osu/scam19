/*
 * NGoogleAnalyticsEventBuilderBuildOpNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.analytics.op;

import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NNode;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NOpNode;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

public class NGoogleAnalyticsEventBuilderBuildOpNode extends NOpNode {
  public NGoogleAnalyticsEventBuilderBuildOpNode(NNode builderNode,
                                                 NNode bundleNode,
                                                 Pair<Stmt, SootMethod> callSite) {
    super(callSite, false);
    builderNode.addEdgeTo(this);
    this.addEdgeTo(bundleNode);
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
  public boolean hasLhs() {
    return true;
  }
}
