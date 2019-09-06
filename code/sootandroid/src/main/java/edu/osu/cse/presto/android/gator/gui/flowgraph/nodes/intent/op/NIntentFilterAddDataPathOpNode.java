/*
 * NIntentFilterAddDataPathOpNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.intent.op;

import com.google.common.collect.Lists;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NNode;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NVarNode;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.intent.NExtOpNode;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

import java.util.List;

public class NIntentFilterAddDataPathOpNode extends NExtOpNode {
  public static final SootMethod METHOD = Scene.v().getMethod("<android.content.IntentFilter: void addDataPath(java.lang.String,int)>");

  public NIntentFilterAddDataPathOpNode(NVarNode receiver, NNode param1, NNode param2,
                                        Pair<Stmt, SootMethod> callSite) {
    super(callSite, false);
    receiver.addEdgeTo(this);
    param1.addEdgeTo(this);
    param2.addEdgeTo(this);
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
  public boolean hasParameters() {
    return true;
  }

  @Override
  public List<NNode> getParameters() {
    return Lists.newArrayList(getPredecessor(1), getPredecessor(2));
  }

  @Override
  public boolean hasLhs() {
    return false;
  }
}
