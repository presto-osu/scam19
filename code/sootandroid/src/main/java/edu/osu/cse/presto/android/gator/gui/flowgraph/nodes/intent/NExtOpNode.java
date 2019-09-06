/*
 * NExtOpNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.intent;

import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NNode;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NOpNode;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

import java.util.List;

// helper class, extended NOpNode
public abstract class NExtOpNode extends NOpNode {
  public NExtOpNode(Pair<Stmt, SootMethod> callSite, boolean artificial) {
    super(callSite, artificial);
  }

  public abstract boolean hasParameters();

  public List<NNode> getParameters() {
    throw new RuntimeException("Fail to get parameters for " + this.toString());
  }
}
