/*
 * NTaskStackBuilderAddIntentOpNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.pendingintent.op;

import com.google.common.collect.Sets;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NOpNode;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NVarNode;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

import java.util.List;
import java.util.Set;

public class NTaskStackBuilderAddIntentOpNode extends NOpNode {
  public Boolean addParent = false;
  public Boolean addIntent = false;
  public Set<List<SootClass>> intents;
  NVarNode lhs, recv;

  public NTaskStackBuilderAddIntentOpNode(NVarNode recv, NVarNode lhs,
                                          Pair<Stmt, SootMethod> callSite) {
    super(callSite, false);
    this.recv = recv;
    if (recv != null) recv.addEdgeTo(this);
    this.lhs = lhs;
    if (lhs != null) recv.addEdgeTo(lhs);
    intents = Sets.newHashSet();
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
