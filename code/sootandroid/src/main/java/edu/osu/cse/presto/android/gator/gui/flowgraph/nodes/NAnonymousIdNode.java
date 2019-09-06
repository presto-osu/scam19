/*
 * NAnonymousIdNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */
package edu.osu.cse.presto.android.gator.gui.flowgraph.nodes;

public class NAnonymousIdNode extends NIdNode {
  public NAnonymousIdNode(Integer i) {
    super(i, "AID");
  }

  @Override
  public String getIdName() {
    return "ANONYMOUS";
  }
}