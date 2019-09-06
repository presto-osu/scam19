/*
 * NIntentFilterAddActionOpNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.intent.op;

import com.google.common.collect.Sets;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NNode;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NOpNode;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NVarNode;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

import java.util.Set;

public class NIntentFilterAddActionOpNode extends NOpNode {
  public static final SootMethod METHOD = Scene.v().getMethod("<android.content.IntentFilter: void addAction(java.lang.String)>");

  public Set<String> reachActionSet = Sets.newHashSet();

  public NIntentFilterAddActionOpNode(NVarNode receiver, NNode action,
                                      Pair<Stmt, SootMethod> callSite) {
    super(callSite, false);
    receiver.addEdgeTo(this);
    action.addEdgeTo(this);
  }

  @Override
  public boolean hasReceiver() {
    return true;
  }

  @Override
  public NVarNode getReceiver() {
    return (NVarNode) getPredecessor(0);
  }

  @Override
  public boolean hasParameter() {
    return true;
  }

  @Override
  public NNode getParameter() {
    return getPredecessor(1);
  }

  @Override
  public boolean hasLhs() {
    return false;
  }
}
