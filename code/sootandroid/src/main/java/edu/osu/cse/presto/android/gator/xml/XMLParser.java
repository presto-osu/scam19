/*
 * XMLParser.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */
package edu.osu.cse.presto.android.gator.xml;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.osu.cse.presto.android.gator.Configs;
import edu.osu.cse.presto.android.gator.Logger;
import soot.Scene;
import soot.SootClass;
import soot.toolkits.scalar.Pair;

import java.util.*;

public interface XMLParser {
  abstract class AbstractXMLParser implements XMLParser {
    Map<String, ActivityLaunchMode> activityAndLaunchModes = Maps.newHashMap();

    @Override
    public ActivityLaunchMode getLaunchMode(String activityClassName) {
      return activityAndLaunchModes.get(activityClassName);
    }

    protected String appPkg;

    protected final ArrayList<String> activities = Lists.newArrayList();
    protected final ArrayList<String> services = Lists.newArrayList();

    // hailong:
    public final Map<String, String> activityAndParentActivity = Maps.newHashMap();

    protected SootClass mainActivity;

    @Override
    public SootClass getMainActivity() {
      return mainActivity;
    }

    @Override
    public Iterator<String> getActivities() {
      return activities.iterator();
    }

    @Override
    public int getNumberOfActivities() {
      return activities.size();
    }

    @Override
    public String getAppPackageName() {
      return appPkg;
    }

    // hailong:
    @Override
    public List<String> getParentChain(String root) {
      String parent = activityAndParentActivity.get(root);
      if (parent == null) return Lists.newArrayList();
      List<String> parentChain = getParentChain(parent);
      parentChain.add(parent);
      return parentChain;
    }
  }

  class Helper {
    // These are declared in the manifest, but no corresponding .java/.class
    // files are available. Most likely, code was deleted while manifest has
    // not been updated.
    static Set<String> astridMissingActivities = Sets.newHashSet(
        "com.todoroo.astrid.tags.reusable.FeaturedListActivity",
        "com.todoroo.astrid.actfm.TagViewWrapperActivity",
        "com.todoroo.astrid.actfm.TagCreateActivity",
        "com.todoroo.astrid.gtasks.auth.GtasksAuthTokenProvider",
        "com.todoroo.astrid.reminders.NotificationWrapperActivity");
    static Set<String> nprMissingActivities = Sets.newHashSet(
        "com.crittercism.NotificationActivity");

    public static String getClassName(String classNameFromXml, String appPkg) {
      if (classNameFromXml == null || appPkg == null
              || classNameFromXml.isEmpty() || appPkg.isEmpty()) {
        Logger.warn("Class name or package name is empty!");
        return null;
      }
      if ('.' == classNameFromXml.charAt(0)) {
        classNameFromXml = appPkg + classNameFromXml;
      }
      if (!classNameFromXml.contains(".")) {
        classNameFromXml = appPkg + "." + classNameFromXml;
      }
      if (Configs.benchmarkName.equals("Astrid")
          && astridMissingActivities.contains(classNameFromXml)) {
        return null;
      }
      if (Configs.benchmarkName.equals("NPR")
          && nprMissingActivities.contains(classNameFromXml)) {
        return null;
      }
      if (Scene.v().getSootClass(classNameFromXml).isPhantom()) {
        System.out.println("[WARNNING] : "+ classNameFromXml +
            " is declared in AndroidManifest.xml, but phantom.");
        return null;
       // throw new RuntimeException(
       //     classNameFromXml + " is declared in AndroidManifest.xml, but phantom.");
      }
      return classNameFromXml;
    }
  }

  // A hacky factory method. It's good enough since the number of possible
  // XMLParser implementations is very limited. We may even end up with only
  // one and push the diff logic into the existing parser.
  class Factory {
    public static XMLParser getXMLParser() {
      if (Configs.apkMode) {
        return ApkXMLParser.v();
      }
      return DefaultXMLParser.v();
    }
  }
  // === layout, id, string, menu xml files

  // R.layout.*
  Set<Integer> getApplicationLayoutIdValues();

  Set<Integer> getSystemLayoutIdValues();

  Integer getSystemRLayoutValue(String layoutName);

  String getApplicationRLayoutName(Integer value);

  String getSystemRLayoutName(Integer value);

  // R.menu.*
  Set<Integer> getApplicationMenuIdValues();

  Set<Integer> getSystemMenuIdValues();

  String getApplicationRMenuName(Integer value);

  String getSystemRMenuName(Integer value);

  // R.id.*
  Set<Integer> getApplicationRIdValues();

  Set<Integer> getSystemRIdValues();

  Integer getSystemRIdValue(String idName);

  String getApplicationRIdName(Integer value);

  String getSystemRIdName(Integer value);

  // hailong:
  // R.drawable.*
  Set<Integer> getRDrawableIdValues();

  Integer getRDrawableIdValue(String name);

  String getRDrawableIdName(Integer value);

  Set<Integer> getSysRDrawableIdValues();

  Integer getSysRDrawableIdValue(String name);

  String getSysRDrawableIdName(Integer value);

  // hailong:
  // R.xml.*
  Set<Integer> getXmlIdValues();

  Integer getXmlIdValue(String name);

  String getXmlIdName(Integer value);

  // R.string.*
  Set<Integer> getStringIdValues();

  String getRStringName(Integer value);

  String getStringValue(Integer idValue);

  // === AndroidManifest.xml
  SootClass getMainActivity();

  Iterator<String> getActivities();

  Iterator<String> getServices();

  int getNumberOfActivities();

  String getAppPackageName();

  enum ActivityLaunchMode {
    standard,
    singleTop,
    singleTask,
    singleInstance
  }

  ActivityLaunchMode getLaunchMode(String activityClassName);

  // === APIs for layout xml files

  // Given a view id, find static abstraction of the matched view.
  AndroidView findViewById(Integer id);

  // retrieve callbacks defined in xml
  Map<Integer, Pair<String, Boolean>> retrieveCallbacks();


  // hailong:
  List<String> getParentChain(String root);
}
