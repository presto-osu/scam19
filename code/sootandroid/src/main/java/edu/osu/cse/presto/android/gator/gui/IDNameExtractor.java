/*
 * IDNameExtractor.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */
package edu.osu.cse.presto.android.gator.gui;

import edu.osu.cse.presto.android.gator.xml.XMLParser;

public class IDNameExtractor {
  private final String UNKNOWN = "";

  private final XMLParser xmlParser;
  private IDNameExtractor() {
    xmlParser = XMLParser.Factory.getXMLParser();
  }

  private static IDNameExtractor instance;
  public static synchronized IDNameExtractor v() {
    if (instance == null) {
      instance = new IDNameExtractor();
    }
    return instance;
  }

  public boolean isUnknown(String name) {
    // NOTE: use == for equality check on purpose.
    return name == UNKNOWN;
  }

  public String idName(Integer id) {
    String name = xmlParser.getApplicationRIdName(id);
    if (name != null) {
      return name;
    }
    name = xmlParser.getSystemRIdName(id);
    if (name != null) {
      return name;
    }
    name = xmlParser.getApplicationRLayoutName(id);
    if (name != null) {
      return name;
    }
    name = xmlParser.getSystemRLayoutName(id);
    if (name != null) {
      return name;
    }
    name = xmlParser.getRStringName(id);
    if (name != null) {
      return name;
    }
    // hailong:
    name = xmlParser.getRDrawableIdName(id);
    if (name != null) {
      return name;
    }
    // hailong:
    name = xmlParser.getSysRDrawableIdName(id);
    if (name != null) {
      return name;
    }
    // hailong:
    name = xmlParser.getXmlIdName(id);
    if (name != null) {
      return name;
    }
    return UNKNOWN;
  }

  public String menuIdName(Integer id) {
    String name = xmlParser.getApplicationRMenuName(id);
    if (name == null) {
      name = xmlParser.getSystemRMenuName(id);
      if (name == null) {
        name = UNKNOWN;
      }
    }
    return name;
  }
}
