/*
 * NGoogleAnalyticsTrackerSendOpNode.java - part of the GATOR project
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

public class NGoogleAnalyticsTrackerSendOpNode extends NOpNode {
  public NGoogleAnalyticsTrackerSendOpNode(NNode trackerNode, NNode bundleNode, Pair<Stmt, SootMethod> callSite) {
    super(callSite, false);
    trackerNode.addEdgeTo(this);
    bundleNode.addEdgeTo(this);
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
    return false;
  }
}
