/*
 * Universe.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.ga;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

@XmlRootElement
public class Universe implements Serializable {
  static final long serialVersionUID = 66666L;

  public boolean autoTracking;
  public Set<String> name = Sets.newHashSet();
  public Map<String, String> act2name = Maps.newHashMap();
}
