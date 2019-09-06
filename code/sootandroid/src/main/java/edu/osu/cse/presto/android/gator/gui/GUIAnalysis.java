/*
 * GUIAnalysis.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */
package edu.osu.cse.presto.android.gator.gui;

import com.google.common.collect.Sets;
import edu.osu.cse.presto.android.gator.Configs;
import edu.osu.cse.presto.android.gator.Hierarchy;
import edu.osu.cse.presto.android.gator.Logger;
import edu.osu.cse.presto.android.gator.gui.flowgraph.Flowgraph;
import edu.osu.cse.presto.android.gator.gui.flowgraph.GAFlowgraph;
import edu.osu.cse.presto.android.gator.xml.XMLParser;
import org.atteo.classindex.ClassIndex;

import java.util.Date;
import java.util.Set;

/**
 * The entrypoint class for our GUI analysis. See run() for details.
 */
public class GUIAnalysis {
  public static final String DEFAULT_CLIENT_PACKAGE =
      "edu.osu.cse.presto.android.gator.gui.clients";

  public Hierarchy hier;
  public XMLParser xmlParser;

  public Set<Integer> allLayoutIds = Sets.newHashSet();
  public Set<Integer> allMenuIds = Sets.newHashSet();
  public Set<Integer> allWidgetIds = Sets.newHashSet();
  public Set<Integer> allStringIds = Sets.newHashSet();
  public Set<Integer> allXmlIds = Sets.newHashSet();

  // hailong:
  public Set<Integer> allDrawableIds = Sets.newHashSet();

  public Flowgraph flowgraph;
  public FixpointSolver fixpointSolver;
  public VariableValueQueryInterface variableValueQueryInterface;

  GUIAnalysis(Hierarchy hier, XMLParser xmlParser) {
    this.hier = hier;
    this.xmlParser = xmlParser;
  }

  private static GUIAnalysis instance;

  public static synchronized GUIAnalysis v() {
    if (instance == null) {
      instance = new GUIAnalysis(Hierarchy.v(), XMLParser.Factory.getXMLParser());
    }
    return instance;
  }

  /**
   * Populate ID containers with information from XML files, and print out some
   * statistics as a sanity check. The ID containers are used in the
   * construction of flowgraph.
   */
  public void populateIDContainers() {
    // First, the layout ids
    allLayoutIds.addAll(xmlParser.getApplicationLayoutIdValues());
    allLayoutIds.addAll(xmlParser.getSystemLayoutIdValues());
    // Next, the menu ids (similarly to layouts, could be inflated):
    allMenuIds.addAll(xmlParser.getApplicationMenuIdValues());
    allMenuIds.addAll(xmlParser.getSystemMenuIdValues());
    // And the widget ids
    allWidgetIds.addAll(xmlParser.getApplicationRIdValues());
    allWidgetIds.addAll(xmlParser.getSystemRIdValues());
    allStringIds.addAll(xmlParser.getStringIdValues());
    allXmlIds.addAll(xmlParser.getXmlIdValues());
    // hailong:
    allDrawableIds.addAll(xmlParser.getRDrawableIdValues());
    allDrawableIds.addAll(xmlParser.getSysRDrawableIdValues());

    System.out.println(
        "[XML] Layout Ids: " + allLayoutIds.size() + ", Menu Ids: " + allMenuIds.size()
            + ", Widget Ids: " + allWidgetIds.size() + ", String Ids: " + allStringIds.size());
    System.out.println("[XML] MainActivity: " + xmlParser.getMainActivity());
  }

  public void run() {
    System.out.println("[GUIAnalysis] Start @ " + new Date());
    long startTime = System.nanoTime();

    // 0. Populate IDs
    populateIDContainers();

    // 1. Build flow graph
    // flowgraph = new Flowgraph(hier, allLayoutIds, allMenuIds, allWidgetIds, allStringIds);
    if (Configs.gaEnabled) {
      flowgraph =
              new GAFlowgraph(hier, allLayoutIds, allMenuIds, allWidgetIds, allStringIds, allDrawableIds, allXmlIds);
    } else {
      flowgraph =
          new Flowgraph(hier, allLayoutIds, allMenuIds, allWidgetIds, allStringIds, allDrawableIds);
    }
    flowgraph.build();

    // 2. Fix-point computation
//    fixpointSolver = new FixpointSolver(flowgraph);
//    fixpointSolver.solve();

    // 3. Variable value query interface
    variableValueQueryInterface = DemandVariableValueQuery.v(flowgraph, fixpointSolver);

    // 4. Construct the output
    GUIAnalysisOutput output = new DefaultGUIAnalysisOutput(this);

    long estimatedTime = System.nanoTime() - startTime;
    output.setRunningTimeInNanoSeconds(estimatedTime);
    System.out.println("[GUIAnalysis] End: " + (estimatedTime * 1.0e-09) + " sec");

    // 5. Client analyses
    executeClientAnalyses(output);
  }

  void executeClientAnalyses(GUIAnalysisOutput output) {
    Set<GUIAnalysisClient> clients = Sets.newHashSet();
    readClientAnalysisSpecification(clients);

    for (GUIAnalysisClient client : clients) {
      Class<? extends GUIAnalysisClient> clientClass = client.getClass();
      String clientName;
      if (clientClass.getPackage().getName().equals(DEFAULT_CLIENT_PACKAGE)) {
        clientName = clientClass.getSimpleName();
      } else {
        clientName = clientClass.getName();
      }

      System.out.println("[" + clientName + "] Start");
      long startTime = System.nanoTime();
      client.run(output);
      long estimatedTime = System.nanoTime() - startTime;
      System.out.println("[" + clientName + "] End: " + (estimatedTime * 1.0e-09) + " sec");
    }
  }

  void readClientAnalysisSpecification2(Set<GUIAnalysisClient> clients) {
    for (Class<?> klass : ClassIndex.getSubclasses(GUIAnalysisClient.class)) {
      String klassName = klass.getSimpleName();
      if (Configs.clients.contains(klassName)) {
        Object newInstance = null;
        try {
          newInstance = klass.newInstance();
        } catch (Exception e) {
          Logger.warn("Cannot create an instance for `" + klassName + "'");
        }
        if (newInstance != null) {
          if (newInstance instanceof GUIAnalysisClient) {
            clients.add((GUIAnalysisClient) newInstance);
          } else {
            Logger.warn("`" + klassName + "' does not implement GUIAnalysisClient");
          }
        }
      }
    }
  }

  // TODO(tony): use fancy annotation mechanism to manage client specification
  // so that shorter names can be registered with clients.
  // NOTE(tony): we assume each client is specified once (i.e., no duplication)
  @Deprecated
  void readClientAnalysisSpecification(Set<GUIAnalysisClient> clients) {
    for (String clientSpec : Configs.clients) {
      Class<?> klass = null;
      try {
        klass = Class.forName(clientSpec);
      } catch (Exception e1) {
        if (!clientSpec.contains(".")) {
          // prepend default package
          try {
            klass = Class.forName(DEFAULT_CLIENT_PACKAGE + "." + clientSpec);
          } catch (Exception ex) {
            // ignore
          }
        }
      }
      if (klass == null) {
        System.out.println("[WARNING] Cannot find client `" + clientSpec + "'");
      } else {
        Object newInstance = null;
        try {
          newInstance = klass.newInstance();
        } catch (Exception e) {
          System.out.println("[WARNING] Cannot create an instance for `" + clientSpec + "'");
        }
        if (newInstance != null) {
          if (newInstance instanceof GUIAnalysisClient) {
            clients.add((GUIAnalysisClient) newInstance);
          } else {
            System.out.println(
                "[WARNING] `" + clientSpec + "' does not implement GUIAnalysisClient");
          }
        }
      }
    }
  }
}
