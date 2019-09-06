/*
 * NTaskStackBuilderNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.pendingintent;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NObjectNode;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.pendingintent.op.NTaskStackBuilderAddIntentOpNode;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Expr;
import soot.jimple.Stmt;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class NTaskStackBuilderNode extends NObjectNode {
  public Stmt allocStmt;
  public SootMethod allocMethod;
  private Map<Integer, Set<List<SootClass>>> id2intents;

  public NTaskStackBuilderNode(Expr e, Stmt allocStmt, SootMethod allocMethod) {
    this.allocStmt = allocStmt;
    this.allocMethod = allocMethod;
    id2intents = Maps.newTreeMap();
  }

  public Set<List<SootClass>> getIntents() {
    Set<List<SootClass>> ret = Sets.newHashSet();
    Set<List<List<SootClass>>> intermediate = Sets.cartesianProduct(Lists.newArrayList(id2intents.values()));
    for (List<List<SootClass>> intents : intermediate) {
      ret.add(Lists.newArrayList(Iterables.concat(intents)));
    }
    return ret;
  }

  public void addIntents(NTaskStackBuilderAddIntentOpNode addIntentOpNode) {
    id2intents.put(addIntentOpNode.id, addIntentOpNode.intents);
  }

  @Override
  public SootClass getClassType() {
    return soot.Scene.v().getSootClass("android.support.v4.app.TaskStackBuilder");
  }
}
