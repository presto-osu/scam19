/*
 * NIntentFilterSetContentOpNode.java - part of the GATOR project
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
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.intent.NIntentFilterNode;
import edu.osu.cse.presto.android.gator.gui.intent.IntentFilter;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

import java.util.Set;

public class NIntentFilterSetContentOpNode extends NOpNode {
  public static final SootMethod CREATE_INTENT_FILTER_MTD =
          NIntentFilterNode.CLASS.getMethod("android.content.IntentFilter create(java.lang.String,java.lang.String)");
  public static final SootMethod NO_PARAM_CONSTRUCTOR =
          NIntentFilterNode.CLASS.getMethod("void <init>()");
  public static final SootMethod ACTION_CONSTRUCTOR =
          NIntentFilterNode.CLASS.getMethod("void <init>(java.lang.String)");
  public static final SootMethod ACTION_DATATYPE_CONSTRUCTOR =
          NIntentFilterNode.CLASS.getMethod("void <init>(java.lang.String,java.lang.String)");
  public static final SootMethod OTHER_FILTER_CONSTRUCTOR =
          NIntentFilterNode.CLASS.getMethod("void <init>(android.content.IntentFilter)");

  public NNode action;
  public NNode dataType;
  public NNode otherFilter;

  public Set<String> reachActionSet = Sets.newHashSet();
  public Set<String> reachDataTypeSet = Sets.newHashSet();
  public Set<IntentFilter> reachFilterSet = Sets.newHashSet();

  public NIntentFilterSetContentOpNode(NNode action, NNode dataType, NNode otherFilter,
                                       Pair<Stmt, SootMethod> callSite) {
    super(callSite, false);
    this.action = action;
    this.dataType = dataType;
    this.otherFilter = otherFilter;
    if (action != null) action.addEdgeTo(this);
    if (dataType != null) dataType.addEdgeTo(this);
    if (otherFilter != null) otherFilter.addEdgeTo(this);
  }

  @Override
  public boolean hasReceiver() {
    return false;
  }

  @Override
  public boolean hasParameter() {
    return false;
  }

  @Override
  public boolean hasLhs() {
    return false;
  }
}
