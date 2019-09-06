/*
 * NIntegerConstantNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.gui.flowgraph.nodes;

import soot.Scene;
import soot.SootClass;

public class NIntegerConstantNode extends NObjectNode {
  public Integer value;

  @Override
  public SootClass getClassType() {
    return Scene.v().getSootClass("java.lang.Integer");
  }

  @Override
  public String toString() {
    return "IntegerConst[" + value + "]" + id;
  }
}
