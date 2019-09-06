/*
 * NMenuIdNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */
package edu.osu.cse.presto.android.gator.gui.flowgraph.nodes;

import edu.osu.cse.presto.android.gator.gui.IDNameExtractor;

public class NMenuIdNode extends NIdNode {
  public NMenuIdNode(Integer i) {
    super(i, "MID");
  }

  @Override
  public String getIdName() {
    return IDNameExtractor.v().menuIdName(i);
  }
}
