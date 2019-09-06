/*
 * NClassConstantNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */
package edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.wtg;

import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NObjectNode;
import soot.Scene;
import soot.SootClass;

public class NClassConstantNode extends NObjectNode {
  public SootClass myClass;
  public NClassConstantNode(SootClass cls) {
    myClass = cls;
  }

  public String toString() {
    return "CLASSCONST[" + myClass + "]" + id;
  }

  @Override
  public SootClass getClassType() {
    return Scene.v().getSootClass("java.lang.Class");
  }
}
