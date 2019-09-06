/*
 * NSetIntentContentOpNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */
package edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.wtg;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.osu.cse.presto.android.gator.Logger;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NNode;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NOpNode;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NVarNode;
import edu.osu.cse.presto.android.gator.gui.intent.IntentField;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

import java.util.Map;
import java.util.Set;

public class NSetIntentContentOpNode extends NOpNode {
  private boolean hasLhs = false;

  private Map<NNode, Set<IntentField>> nn2contentMap;

  public NSetIntentContentOpNode(NNode lhsNode, NNode rcvNode,
      Pair<Stmt, SootMethod> callSite) {
    super(callSite, false);
    if (rcvNode == null) {
      Logger.err(getClass().getSimpleName(), "the rcvNode for SetIntentContentOpNode: " + this + " is null");
    }
    rcvNode.addEdgeTo(this);
    if (lhsNode != null) {
      this.addEdgeTo(lhsNode);
      rcvNode.addEdgeTo(lhsNode);
      hasLhs = true;
    }
    nn2contentMap = Maps.newHashMap();
  }

  public void addContent(NNode node, IntentField content) {
    Set<IntentField> fields = this.nn2contentMap.get(node);
    if (fields == null) {
      fields = Sets.newHashSet();
      this.nn2contentMap.put(node, fields);
    }
    fields.add(content);
  }

  public Set<IntentField> getContent(NNode node) {
    return this.nn2contentMap.get(node);
  }

  public Map<NNode, Set<IntentField>> getContentMap() {
    return this.nn2contentMap;
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
  public NVarNode getLhs() {
    if (hasLhs) {
      return (NVarNode)this.succ.get(0);
    } else {
      return null;
    }
  }
  @Override
  public boolean hasLhs() {
    return hasLhs;
  }
}
