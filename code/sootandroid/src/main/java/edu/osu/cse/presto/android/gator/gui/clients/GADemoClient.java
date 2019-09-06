/*
 * GADemoClient.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.gui.clients;

import edu.osu.cse.presto.android.gator.gui.GUIAnalysisClient;
import edu.osu.cse.presto.android.gator.gui.GUIAnalysisOutput;
import edu.osu.cse.presto.android.gator.gui.ga.Builder;

public class GADemoClient implements GUIAnalysisClient {
  @Override
  public void run(GUIAnalysisOutput output) {
    Builder builder = new Builder();
    builder.build(output);
  }
}
