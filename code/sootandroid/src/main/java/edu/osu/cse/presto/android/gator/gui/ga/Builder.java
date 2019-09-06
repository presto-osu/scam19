/*
 * Builder.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.gui.ga;

import com.google.common.collect.Sets;
import edu.osu.cse.presto.android.gator.Configs;
import edu.osu.cse.presto.android.gator.Hierarchy;
import edu.osu.cse.presto.android.gator.Logger;
import edu.osu.cse.presto.android.gator.ga.Universe;
import edu.osu.cse.presto.android.gator.gui.GUIAnalysisOutput;
import edu.osu.cse.presto.android.gator.gui.flowgraph.Flowgraph;
import edu.osu.cse.presto.android.gator.gui.flowgraph.GAFlowgraph;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.*;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.analytics.op.*;
import edu.osu.cse.presto.android.gator.gui.util.GraphUtil;
import edu.osu.cse.presto.android.gator.xml.ApktoolResXMLReader;
import soot.SootClass;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.StringWriter;
import java.util.Map;
import java.util.Set;

public class Builder {
  String TAG = Builder.class.getSimpleName();

  GraphUtil graphUtil = GraphUtil.v();

  public void build(GUIAnalysisOutput output) {
    Flowgraph flowgraph = output.getFlowgraph();
    if (!(flowgraph instanceof GAFlowgraph)) {
      Logger.err();
    }

    GAFlowgraph gaFlowgraph = (GAFlowgraph) flowgraph;

    boolean autoTracking = false;
    Set<NXmlIdNode> gaXmlIdNodes = Sets.newHashSet();

    Set<NOpNode> instances = NOpNode.getNodes(NGoogleAnalyticsGetInstanceOpNode.class);
    Logger.info(TAG, "google.getInstance.num=" + instances.size());
    Set<NOpNode> trackers = NOpNode.getNodes(NGoogleAnalyticsNewTrackerOpNode.class);
    Logger.info(TAG, "google.tracker.num=" + trackers.size());
    int id = 0;
    for (NNode trackerNode : trackers) {
      id += 1;
      String tag = "google.tracker." + id;
      // 1. determine string or integer flow to newTracker(x)
      // if integer, check xml for <bool name="ga_autoActivityTracking">true</bool>
      for (NNode paramNode : graphUtil.backwardReachableNodes(trackerNode)) {
        if (paramNode instanceof NStringConstantNode) {
          Logger.info(TAG, tag + ".string:::" + paramNode);
        } else if (paramNode instanceof NStringIdNode) {
          Logger.info(TAG, tag + ".string:::String[" +
                  gaFlowgraph.xmlUtil.getStringValue(((NStringIdNode) paramNode).getIdValue())
                  + "]:::" + paramNode);
        } else if (paramNode instanceof NXmlIdNode) {
          Logger.info(TAG, tag + ".integer:::" + paramNode);
          String autoString = ApktoolResXMLReader.v().readXmlFirst(((NXmlIdNode) paramNode).getIdName(),
                  "bool", "ga_autoActivityTracking");
          gaXmlIdNodes.add((NXmlIdNode) paramNode);
          if (autoString != null && autoString.equalsIgnoreCase("true")) {
            Logger.info(TAG, tag + ".xml.auto.true");
            autoTracking = true;
          }
        }
      }
      // 2. check x.enableAutoActivityTracking(y)
      for (NNode autoOpNode : graphUtil.forwardReachableNodes(trackerNode)) {
        if (autoOpNode instanceof NGoogleAnalyticsAutoActivityTrackingOpNode) {
          for (NNode paramNode : graphUtil.backwardReachableNodes(autoOpNode)) {
            if (paramNode instanceof NIntegerConstantNode) {
              if (((NIntegerConstantNode) paramNode).value == 1) { // true
                Logger.info(TAG, tag + ".call.auto.true");
                autoTracking = true;
              }
            }
          }
        }
      }
    }

    Logger.info(TAG, "3. check features ====================================================");
    Logger.info(TAG, "#ScreenViewBuilder=" + gaFlowgraph.allNGoogleAnalyticsScreenViewBuilderNodes.size());
    Logger.info(TAG, "#EventBuilder=" + gaFlowgraph.allNGoogleAnalyticsEventBuilderNodes.size());
    Logger.info(TAG, "#ExceptionBuilder=" + gaFlowgraph.allNGoogleAnalyticsExceptionBuilderNodes.size());
    Logger.info(TAG, "#SocialBuilder=" + gaFlowgraph.allNGoogleAnalyticsSocialBuilderNodes.size());
    Logger.info(TAG, "#TimingBuilder=" + gaFlowgraph.allNGoogleAnalyticsTimingBuilderNodes.size());

    Logger.info(TAG, "4. call sites ====================================================");
    int i = 0;
    for (NOpNode node : NOpNode.getNodes(NGoogleAnalyticsSetScreenNameOpNode.class)) {
      Logger.info(TAG, ++i + ".setScreenName.callsite: " + node.callSite);
    }
    i = 0;
    for (NOpNode node : NOpNode.getNodes(NGoogleAnalyticsTrackerSendOpNode.class)) {
      Logger.info(TAG, ++i + ".send.callsite: " + node.callSite);
    }
    i = 0;
    for (NOpNode node : NOpNode.getNodes(NGoogleAnalyticsNewTrackerOpNode.class)) {
      Logger.info(TAG, ++i + ".newTracker.callsite: " + node.callSite);
    }
    i = 0;
    for (NOpNode node : NOpNode.getNodes(NGoogleAnalyticExceptionBuilderBuildOpNode.class)) {
      Logger.info(TAG, ++i + ".ExceptionBuilder.build.callsite: " + node.callSite);
    }
    i = 0;
    for (NOpNode node : NOpNode.getNodes(NGoogleAnalyticsEventBuilderBuildOpNode.class)) {
      Logger.info(TAG, ++i + ".EventBuilder.build.callsite: " + node.callSite);
    }
    i = 0;
    for (NOpNode node : NOpNode.getNodes(NGoogleAnalyticsScreenViewBuilderBuildOpNode.class)) {
      Logger.info(TAG, ++i + ".ScreenViewBuilder.build.callsite: " + node.callSite);
    }
    i = 0;
    for (NOpNode node : NOpNode.getNodes(NGoogleAnalyticsSocialBuilderBuildOpNode.class)) {
      Logger.info(TAG, ++i + ".SocialBuilder.build.callsite: " + node.callSite);
    }
    i = 0;
    for (NOpNode node : NOpNode.getNodes(NGoogleAnalyticsTimingBuilderBuildOpNode.class)) {
      Logger.info(TAG, ++i + ".TimingBuilder.build.callsite: " + node.callSite);
    }
    i = 0;
    for (NOpNode node : NOpNode.getNodes(NGoogleAnalyticsAutoActivityTrackingOpNode.class)) {
      Logger.info(TAG, ++i + ".autoActivityTracking.callsite: " + node.callSite);
    }

    Logger.info(TAG, "5. check screen names ====================================================");
    int numTrackers = NOpNode.getNodes(NGoogleAnalyticsNewTrackerOpNode.class).size();
    Logger.info("google.tracker.num", "" + numTrackers);
    int numAutoTrackingOpNodes = NOpNode.getNodes(NGoogleAnalyticsAutoActivityTrackingOpNode.class).size();
    Logger.info("google.autotracking.num", "" + numAutoTrackingOpNodes);

    Universe nu = new Universe();
    nu.autoTracking = autoTracking;
    if (nu.autoTracking) {
      for (NXmlIdNode xmlIdNode : gaXmlIdNodes) {
        Map<String, String> act2name = ApktoolResXMLReader.v().readXml(xmlIdNode.getIdName(), "screenName");
        nu.act2name.putAll(act2name);
      }
      for (SootClass act : Hierarchy.v().applicationActivityClasses) {
        if (!act.isConcrete()) continue;
        String name = act.getName();
        if (name.startsWith("android.support")) continue;
        if (!nu.act2name.containsKey(name)) {
          nu.act2name.put(name, name);
        }
      }
    }
    if (numTrackers == 1 && numAutoTrackingOpNodes == 0) {
      log(nu, gaFlowgraph, "google.onetracker.noauto.setscreenname.string");
    } else if (numAutoTrackingOpNodes == 0) {
      log(nu, gaFlowgraph, "google.noauto.setscreenname.string");
    } else {
      log(nu, gaFlowgraph, "google.setscreenname.string");
    }
    Logger.info(TAG, dumpXml(nu));

    if (!Configs.flowgraphOutput.isEmpty()) {
      gaFlowgraph.dump(Configs.flowgraphOutput);
    }

    Logger.info(TAG, "========================Finished========================");
  }

  void log(Universe nu, GAFlowgraph gaFlowgraph, String tag) {
    for (NNode node : NOpNode.getNodes(NGoogleAnalyticsSetScreenNameOpNode.class)) {
      int strings = 0;
      for (NNode n : GraphUtil.v().backwardReachableNodes(node)) {
        if (n instanceof NStringConstantNode) {
          nu.name.add(((NStringConstantNode) n).value);
          strings += 1;
        } else if (n instanceof NStringIdNode) {
          nu.name.add(gaFlowgraph.xmlUtil.getStringValue(((NStringIdNode) n).getIdValue()));
          strings += 1;
//        } else if (n instanceof NClassConstantNode) {
//          nu.name.add(((NClassConstantNode) n).myClass.getShortJavaStyleName());
//          strings += 1;
//        } else if (n instanceof NActivityNode) {
//          nu.name.add(((NActivityNode) n).getClassType().getShortJavaStyleName());
//          strings += 1;
//        } else if (n instanceof NGetClassOpNode) {
//          for (NNode pred : GraphUtil.v().backwardReachableNodes(n)) {
//            if (pred instanceof NClassConstantNode) {
//              nu.name.add(((NClassConstantNode) pred).myClass.getShortJavaStyleName());
//              strings += 1;
//            } else if (pred instanceof NActivityNode) {
//              nu.name.add(((NActivityNode) pred).getClassType().getShortJavaStyleName());
//              strings += 1;
//            }
//          }
        }
      }
      if (strings == 1) {
        Logger.info(tag + ".one", "# = " + strings);
      } else if (strings == 0) {
        Logger.info(tag + ".none", "no string constant");
      } else {
        Logger.info(tag + ".multi", "multiple string constants");
      }
    }
  }

  private String dumpXml(Universe nu) {
    String xmlString = null;
    try {
      JAXBContext jc = JAXBContext.newInstance(Universe.class);
      Marshaller m = jc.createMarshaller();
      m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      if (Configs.gaScreenNameXmlOutputFile.isEmpty()) {
        StringWriter sw = new StringWriter();
        m.marshal(nu, sw);
        xmlString = sw.toString();
      } else {
        File xmlOutputFile = new File(Configs.gaScreenNameXmlOutputFile);
        xmlOutputFile.setWritable(true, false);
        xmlOutputFile.setReadable(true, false);
//        xmlOutputFile.setExecutable(true, false);
        m.marshal(nu, xmlOutputFile);
        xmlString = "Universe XML is saved to " + Configs.gaScreenNameXmlOutputFile;
      }
    } catch (JAXBException e) {
      e.printStackTrace();
    }
    return xmlString;
  }
}
