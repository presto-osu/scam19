/*
 * NCreatePendingIntentOpNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.pendingintent.op;

import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NOpNode;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NVarNode;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.pendingintent.NPendingIntentNode;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

public class NCreatePendingIntentOpNode extends NOpNode {
  public NPendingIntentNode pendingIntentNode;
  NVarNode lhs, recv;

  public NCreatePendingIntentOpNode(NVarNode recv, NVarNode lhs, NPendingIntentNode pendingIntent,
                                    Pair<Stmt, SootMethod> callSite) {
    super(callSite, false);
    this.recv = recv;
    if (recv != null) recv.addEdgeTo(this);
    this.pendingIntentNode = pendingIntent;
    this.addEdgeTo(pendingIntent);
    this.lhs = lhs;
    if (lhs != null) pendingIntent.addEdgeTo(lhs);
  }

  @Override
  public boolean hasReceiver() {
    return recv != null;
  }

  @Override
  public NVarNode getReceiver() {
    if (hasReceiver()) return recv;
    return super.getReceiver();
  }

  @Override
  public boolean hasParameter() {
    return false;
  }

  @Override
  public boolean hasLhs() {
    return lhs != null;
  }

  @Override
  public NVarNode getLhs() {
    if (hasLhs()) return lhs;
    return super.getLhs();
  }
}
