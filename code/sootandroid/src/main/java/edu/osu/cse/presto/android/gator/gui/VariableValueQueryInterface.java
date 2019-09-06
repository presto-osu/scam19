/*
 * VariableValueQueryInterface.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */
package edu.osu.cse.presto.android.gator.gui;

import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NIdNode;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NObjectNode;
import soot.Local;

import java.util.Set;

// For a given GUI-related variable (GUI object, ID, activity, etc), return the
// set of values this variable may reference.
public interface VariableValueQueryInterface {
  Set<NIdNode> idVariableValues(Local local);

  Set<NObjectNode> activityVariableValues(Local local);

  Set<NObjectNode> guiVariableValues(Local local);

  Set<NObjectNode> listenerVariableValues(Local local);
}
