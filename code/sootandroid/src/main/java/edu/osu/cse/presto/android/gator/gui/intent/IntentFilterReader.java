/*
 * IntentFilterReader.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */
package edu.osu.cse.presto.android.gator.gui.intent;

import edu.osu.cse.presto.android.gator.Configs;
import edu.osu.cse.presto.android.gator.Logger;
import edu.osu.cse.presto.android.gator.gui.util.PatternMatcher;
import edu.osu.cse.presto.android.gator.gui.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Paths;

public class IntentFilterReader {
  private static IntentFilterReader theInst;
  private IntentFilterManager filterManager = IntentFilterManager.v();
  private boolean read = false;
  private Util util;

  private IntentFilterReader(Util util) {
    this.util = util;
  }

  static synchronized IntentFilterReader v(Util util) {
    if (theInst == null) {
      theInst = new IntentFilterReader(util);
    }
    return theInst;
  }

  public void read() {
    if (read) {
      Logger.err(getClass().getSimpleName(), "intent filter has read the AndroidManifest already");
    }
    read = true;
    readManifest();
  }

  private void readManifest() {
    String fn;
    if (Configs.apkMode)
      fn = Paths.get(Configs.apktoolDir, // hailong: apktool directory
              "AndroidManifest.xml").toString();
    else if (Configs.useAndroidStudio)
      fn = Paths.get(Configs.project, "app", "src", "main", "AndroidManifest.xml").toString();
    else
      fn = Paths.get(Configs.project, "AndroidManifest.xml").toString();
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

    DocumentBuilder dBuilder = null;
    Document doc = null;
    try {
      dBuilder = dbFactory.newDocumentBuilder();
      doc = dBuilder.parse(fn);
    } catch (SAXException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    }
    Node root = doc.getElementsByTagName("manifest").item(0);
    String appPkg = root.getAttributes().getNamedItem("package").getTextContent();

    Node appNode = doc.getElementsByTagName("application").item(0);
    NodeList nodes = appNode.getChildNodes();
    for (int i = 0; i < nodes.getLength(); ++i) {
      Node n = nodes.item(i);
      String eleName = n.getNodeName();
      if ("activity".equals(eleName) || "receiver".equals(eleName)) {
        NamedNodeMap m = n.getAttributes();
        String cls = m.getNamedItem("android:name").getTextContent();
        if ('.' == cls.charAt(0)) {
          cls = appPkg + cls;
        }
        boolean isEnabled = true;
        Node enabledNode = m.getNamedItem("android:enabled");
        if (enabledNode != null) {
          isEnabled = Boolean.parseBoolean(enabledNode.getTextContent());
        }
        // record the information for activity filters
        NodeList filterNodes = n.getChildNodes();
        for (int idx = 0; idx < filterNodes.getLength(); idx++) {
          Node filterNode = filterNodes.item(idx);
          if (filterNode.getNodeName().equals("intent-filter")) {
            Node actionNode = filterNode.getFirstChild();
//              IntentFilter filter = new IntentFilter(util);
            IntentFilter filter = new IntentFilter();
            // assume no duplicated intent filter for any activity
            while (actionNode != null) {
              if (actionNode.getNodeName().equals("action")) {
                String actionName = actionNode.getAttributes().getNamedItem("android:name").getTextContent();
                filter.addAction(actionName);
              } else if (actionNode.getNodeName().equals("category")) {
                String category = actionNode.getAttributes().getNamedItem("android:name").getTextContent();
                filter.addCategory(category);
              } else if (actionNode.getNodeName().equals("data")) {
                {
                  Node mTypeNode = actionNode.getAttributes().getNamedItem("android:mimeType");
                  String mType = mTypeNode == null ? null : mTypeNode.getTextContent();
                  if (mType != null) {
                    filter.addDataType(mType);
                  }
                }
                {
                  Node scheNode = actionNode.getAttributes().getNamedItem("android:scheme");
                  String scheme = scheNode == null ? null : scheNode.getTextContent();
                  if (scheme != null) {
                    filter.addDataScheme(scheme);
                  }
                }
                {
                  Node hostNode = actionNode.getAttributes().getNamedItem("android:host");
                  String host = hostNode == null ? null : hostNode.getTextContent();
                  Node portNode = actionNode.getAttributes().getNamedItem("android:port");
                  String port = portNode == null ? null : portNode.getTextContent();
                  if (host != null || port != null) {
                    filter.addDataAuthority(host, port);
                  }
                }
                {
                  Node pathNode = actionNode.getAttributes().getNamedItem("android:path");
                  String path = pathNode == null ? null : pathNode.getTextContent();
                  if (path != null) {
                    filter.addDataPath(path, PatternMatcher.PATTERN_LITERAL);
                  }
                }
                {
                  Node pathNode = actionNode.getAttributes().getNamedItem("android:pathPrefix");
                  String path = pathNode == null ? null : pathNode.getTextContent();
                  if (path != null) {
                    filter.addDataPath(path, PatternMatcher.PATTERN_PREFIX);
                  }
                }
                {
                  Node pathNode = actionNode.getAttributes().getNamedItem("android:pathPattern");
                  String path = pathNode == null ? null : pathNode.getTextContent();
                  if (path != null) {
                    filter.addDataPath(path, PatternMatcher.PATTERN_SIMPLE_GLOB);
                  }
                }
              }
              actionNode = actionNode.getNextSibling();
            }
            filterManager.addFilter(cls, filter, isEnabled);
          }
        }
      }
    }
  }
}
