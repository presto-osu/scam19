/*
 * NNullNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.wtg;

import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NNode;

public class NNullNode extends NNode {
  public static NNullNode nullNode = new NNullNode();

  private NNullNode() {}
}