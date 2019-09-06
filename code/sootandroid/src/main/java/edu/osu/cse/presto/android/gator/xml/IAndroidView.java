/*
 * IAndroidView.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */
package edu.osu.cse.presto.android.gator.xml;

public interface IAndroidView {
  IAndroidView deepCopy();

  void setParent(AndroidView parent);
}