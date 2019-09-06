/*
 * ApkXMLParser.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */
package edu.osu.cse.presto.android.gator.xml;

// This is work-in-progress. Once this is done and some additional minor setting
// tweaks are done, the analysis will be ready to analyze APK files directly.
public class ApkXMLParser extends DefaultXMLParser {
  private static ApkXMLParser theInstance;

  public static synchronized ApkXMLParser v() {
    if (theInstance == null) {
      theInstance = new ApkXMLParser();
    }
    return theInstance;
  }

  @Override
  protected String getMenuFilePath(String project, String menuId, boolean isSys) {
    if (!isSys) {
      project = project.substring(0, project.indexOf(".apk"));
    }
    return super.getMenuFilePath(project, menuId, isSys);
  }

  @Override
  protected String getLayoutFilePath(String project, String layoutId, boolean isSys) {
    if (!isSys) {
      project = project.substring(0, project.indexOf(".apk"));
    }
    return super.getLayoutFilePath(project, layoutId, isSys);
  }
}
