/*
 * NLauncherNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */
package edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.wtg;

import soot.Scene;
import soot.SootClass;

public class NLauncherNode extends NSpecialNode {

  SootClass fakeClass;

  private NLauncherNode() {
    fakeClass = new SootClass("presto.android.gui.stubs.PrestoFakeLauncherNodeClass");
    Scene.v().addClass(fakeClass);
  }

  public String toString() {
    return "LAUNCHER_NODE[]" + id;
  }

  @Override
  public SootClass getClassType() {
    return fakeClass;
  }

  public final static NLauncherNode LAUNCHER = new NLauncherNode();
}
