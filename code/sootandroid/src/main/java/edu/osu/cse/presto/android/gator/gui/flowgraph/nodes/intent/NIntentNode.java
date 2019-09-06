/*
 * NIntentNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.intent;

import com.google.common.collect.Sets;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NAllocNode;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Expr;
import soot.jimple.Stmt;

import java.util.Set;

public class NIntentNode extends NAllocNode {
  public Stmt allocStmt;
  public SootMethod allocMethod;
  public Set<SootClass> tgt;
  public boolean alloc;

  public NIntentNode(Expr e, Stmt allocStmt, SootMethod allocMethod, boolean alloc) {
    this.e = e;
    this.allocStmt = allocStmt;
    this.allocMethod = allocMethod;
    this.alloc = alloc;
    this.tgt = Sets.newHashSet();
  }

  @Override
  public String toString() {
    String pre;
    if (alloc) pre = "NEW";
    else pre = "ARTIFICIAL";
    return pre + ".Intent[" + tgt + "]" + id;
  }

  @Override
  public SootClass getClassType() {
    return Scene.v().getSootClass("android.content.Intent");
  }
}
