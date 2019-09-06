/*
 * ApktoolResXMLReader.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.xml;

import com.google.common.collect.Maps;
import edu.osu.cse.presto.android.gator.Configs;
import edu.osu.cse.presto.android.gator.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileNotFoundException;
import java.util.Map;

public class ApktoolResXMLReader {
  static final String TAG = ApktoolResXMLReader.class.getSimpleName();
  String resDir = Configs.apktoolDir + "/res/values/";

  private static ApktoolResXMLReader ourInstance = new ApktoolResXMLReader();

  public static ApktoolResXMLReader v() {
    return ourInstance;
  }

  private ApktoolResXMLReader() {
  }

  public void readIds(String expectedType, NameValueFunction nvf) {
    String file = resDir + "public.xml";
    Document doc;
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      doc = dBuilder.parse(file);
    } catch (FileNotFoundException fnfe) {
      Logger.warn(expectedType + " cannot be read: " + file + " not found.");
      Logger.warn("Please use ApkTool to decode/decompress the APK first.");
      return;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    Logger.verb(TAG, "--- Reading " + file);
    NodeList nodes = doc.getElementsByTagName("public");
    if (nodes == null) {
      return;
    }
    for (int i = 0; i < nodes.getLength(); i++) {
      Node n = nodes.item(i);
      NamedNodeMap attrs = n.getAttributes();
      String type = attrs.getNamedItem("type").getTextContent();
      if (!type.equals(expectedType)) continue;
      String name = attrs.getNamedItem("name").getTextContent();
      String id = attrs.getNamedItem("id").getTextContent();
      nvf.feed(name, Integer.decode(id));
    }
  }

  /**
   * Specific for Google Analytics.
   *
   * @author Hailong Zhang
   */
  public String readXmlFirst(String xmlName, String expectedType, String expectedName) {
    if (!xmlName.endsWith(".xml"))
      xmlName = xmlName + ".xml";
    String file = Configs.apktoolDir + "/res/xml/" + xmlName;
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    Document doc = null;
    try {
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      doc = dBuilder.parse(file);
    } catch (FileNotFoundException e) {
      Logger.warn(expectedType + " cannot be read: " + file + " not found.");
      Logger.warn("Please use ApkTool to decode/decompress the APK first.");
      System.exit(-1);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    NodeList nodes = doc.getElementsByTagName(expectedType);
    if (nodes == null) {
      return null;
    }
    for (int i = 0; i < nodes.getLength(); i++) {
      Node n = nodes.item(i);
      NamedNodeMap attrs = n.getAttributes();
      String name = attrs.getNamedItem("name").getTextContent();
      if (!name.equals(expectedName)) continue;
      return n.getTextContent();
    }
    return null;
  }

  /**
   * @return map from name to value
   * @author Hailong Zhang
   */
  public Map<String, String> readXml(String xmlName, String expectedType) {
    if (!xmlName.endsWith(".xml"))
      xmlName = xmlName + ".xml";
    Map<String, String> ret = Maps.newHashMap();
    String file = Configs.apktoolDir + "/res/xml/" + xmlName;
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    Document doc = null;
    try {
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      doc = dBuilder.parse(file);
    } catch (FileNotFoundException e) {
      Logger.warn(expectedType + " cannot be read: " + file + " not found.");
      Logger.warn("Please use ApkTool to decode/decompress the APK first.");
      System.exit(-1);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    NodeList nodes = doc.getElementsByTagName(expectedType);
    if (nodes == null) {
      return null;
    }
    for (int i = 0; i < nodes.getLength(); i++) {
      Node n = nodes.item(i);
      NamedNodeMap attrs = n.getAttributes();
      String name = attrs.getNamedItem("name").getTextContent();
      ret.put(name, n.getTextContent());
    }
    return ret;
  }
}
