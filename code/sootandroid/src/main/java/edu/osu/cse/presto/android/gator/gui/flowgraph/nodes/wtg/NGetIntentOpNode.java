/*
 * NGetIntentOpNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */
package edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.wtg;

import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NNode;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NOpNode;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NVarNode;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

public class NGetIntentOpNode extends NOpNode {
  public NGetIntentOpNode(NNode lhsNode, NNode rcvNode, Pair<Stmt, SootMethod> callSite) {
    super(callSite, false);
    rcvNode.addEdgeTo(this);
    this.addEdgeTo(lhsNode);
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
    return false;
  }

  @Override
  public boolean hasLhs() {
    return true;
  }

  @Override
  public NVarNode getLhs() {
    return (NVarNode) this.succ.get(0);
  }
}
