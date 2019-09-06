/*
 * NListenerAllocNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */
package edu.osu.cse.presto.android.gator.gui.flowgraph.nodes;

import soot.SootClass;

public class NListenerAllocNode extends NAllocNode {
  public SootClass c;
  public NListenerAllocNode(SootClass c) {
    this.c = c;
  }
  public String toString() {
    return "NEWLISTENER[" + c + "]" + id;
  }
}
