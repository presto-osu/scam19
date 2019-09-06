/*
 * NRegisterReceiverOpNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.intent.op;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NNode;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NVarNode;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.intent.NExtOpNode;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.intent.NIntentNode;
import edu.osu.cse.presto.android.gator.gui.intent.IntentFilter;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

import java.util.List;
import java.util.Set;

public class NRegisterReceiverOpNode extends NExtOpNode {
  public static final SootMethod CONTENT_REGISTER_RECEIVER_TWO_PARAM =
          Scene.v().getMethod("<android.content.ContextWrapper: android.content.Intent registerReceiver(android.content.BroadcastReceiver,android.content.IntentFilter)>");
  public static final SootMethod CONTEXT_CONTENT_REGISTER_RECEIVER_TWO_PARAM =
          Scene.v().getMethod("<android.content.Context: android.content.Intent registerReceiver(android.content.BroadcastReceiver,android.content.IntentFilter)>");
  public static final SootMethod CONTENT_REGISTER_RECEIVER_FOUR_PARAM =
          Scene.v().getMethod("<android.content.ContextWrapper: android.content.Intent registerReceiver(android.content.BroadcastReceiver,android.content.IntentFilter,java.lang.String,android.os.Handler)>");
  public static final SootMethod CONTEXT_CONTENT_REGISTER_RECEIVER_FOUR_PARAM =
          Scene.v().getMethod("<android.content.Context: android.content.Intent registerReceiver(android.content.BroadcastReceiver,android.content.IntentFilter,java.lang.String,android.os.Handler)>");
  public static SootMethod LOCAL_BROADCAST_MANAGER_REGISTER_RECEIVER = null;
  static {
    try {
      SootClass LOCAL_BROADCAST_MANAGER = Scene.v().getSootClass("android.support.v4.content.LocalBroadcastManager");
      if (LOCAL_BROADCAST_MANAGER != null)
        LOCAL_BROADCAST_MANAGER_REGISTER_RECEIVER = LOCAL_BROADCAST_MANAGER.getMethod("void registerReceiver(android.content.BroadcastReceiver,android.content.IntentFilter)");
    } catch (Exception e) {
      SootClass LOCAL_BROADCAST_MANAGER = new SootClass("android.support.v4.content.LocalBroadcastManager");
      Type br = Scene.v().getType("android.content.BroadcastReceiver");
      Type intentFilter = Scene.v().getType("android.content.IntentFilter");
      LOCAL_BROADCAST_MANAGER_REGISTER_RECEIVER = new SootMethod("registerReceiver", Lists.newArrayList(br, intentFilter), Scene.v().getType("void"));
      LOCAL_BROADCAST_MANAGER.addMethod(LOCAL_BROADCAST_MANAGER_REGISTER_RECEIVER);
    }
  }

  public NNode broadcastReceiver;
  public NNode intentFilter;
  public NVarNode lhs;

  public Set<IntentFilter> filterSet = Sets.newHashSet();
  public Set<String > broadcastReceiverSet = Sets.newHashSet();

  public NRegisterReceiverOpNode(NVarNode rcv, NNode broadcastReceiver, NNode intentFilter, NVarNode lhs, NIntentNode intent,
                                 Pair<Stmt, SootMethod> callSite) {
    super(callSite, false);
    rcv.addEdgeTo(this);
    this.broadcastReceiver = broadcastReceiver;
    if (broadcastReceiver != null) broadcastReceiver.addEdgeTo(this);
    this.intentFilter = intentFilter;
    intentFilter.addEdgeTo(this);
    this.lhs = lhs;
    if (lhs != null) {
      this.addEdgeTo(intent);
      intent.addEdgeTo(lhs);
    }
  }

  @Override
  public boolean hasParameters() {
    return true;
  }

  @Override
  public List<NNode> getParameters() {
    List<NNode> ret = Lists.newArrayList();
    if (broadcastReceiver != null) ret.add(broadcastReceiver);
    ret.add(intentFilter);
    return ret;
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
  public boolean hasLhs() {
    return lhs != null;
  }

  @Override
  public NVarNode getLhs() {
    if (hasLhs()) return lhs;
    return super.getLhs();
  }
}
