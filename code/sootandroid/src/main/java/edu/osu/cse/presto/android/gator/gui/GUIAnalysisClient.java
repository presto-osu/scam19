/*
 * GUIAnalysisClient.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */
package edu.osu.cse.presto.android.gator.gui;

import org.atteo.classindex.IndexSubclasses;

@IndexSubclasses
public interface GUIAnalysisClient {
  void run(GUIAnalysisOutput output);
}
