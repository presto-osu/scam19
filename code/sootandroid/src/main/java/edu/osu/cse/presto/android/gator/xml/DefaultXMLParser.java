/*
 * DefaultXMLParser.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */
package edu.osu.cse.presto.android.gator.xml;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.osu.cse.presto.android.gator.Configs;
import edu.osu.cse.presto.android.gator.Logger;
import org.w3c.dom.*;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.toolkits.scalar.Pair;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Paths;
import java.util.*;

/*
 * This is a re-design of the xml parsing component.
 */
class DefaultXMLParser extends XMLParser.AbstractXMLParser {
  @Override
  public Integer getSystemRIdValue(String idName) {
    return sysRIdMap.get(idName);
  }

  @Override
  public Integer getSystemRLayoutValue(String layoutName) {
    return sysRLayoutMap.get(layoutName);
  }

  @Override
  public String getApplicationRLayoutName(Integer value) {
    return invRLayoutMap.get(value);
  }

  @Override
  public String getSystemRLayoutName(Integer value) {
    return invSysRLayoutMap.get(value);
  }

  @Override
  public AndroidView findViewById(Integer id) {
    AndroidView res = id2View.get(id);
    if (res != null) {
      return res;
    }

    res = sysId2View.get(id);
    if (res != null) {
      return res;
    }
    return null;
  }

  @Override
  public Set<Integer> getApplicationLayoutIdValues() {
    return invRLayoutMap.keySet();
  }

  @Override
  public Set<Integer> getSystemLayoutIdValues() {
    return invSysRLayoutMap.keySet();
  }

  @Override
  public Set<Integer> getApplicationMenuIdValues() {
    return invRMenuMap.keySet();
  }

  @Override
  public Set<Integer> getSystemMenuIdValues() {
    return invSysRMenuMap.keySet();
  }

  @Override
  public String getApplicationRMenuName(Integer value) {
    return invRMenuMap.get(value);
  }

  @Override
  public String getSystemRMenuName(Integer value) {
    return invSysRMenuMap.get(value);
  }

  @Override
  public Set<Integer> getApplicationRIdValues() {
    return invRIdMap.keySet();
  }

  @Override
  public Set<Integer> getSystemRIdValues() {
    return invSysRIdMap.keySet();
  }

  @Override
  public String getApplicationRIdName(Integer value) {
    return invRIdMap.get(value);
  }

  @Override
  public String getSystemRIdName(Integer value) {
    return invSysRIdMap.get(value);
  }

  @Override
  public Set<Integer> getStringIdValues() {
    return invRStringMap.keySet();
  }

  @Override
  public String getRStringName(Integer value) {
    return invRStringMap.get(value);
  }

  @Override
  public String getStringValue(Integer idValue) {
    return intAndStringValues.get(idValue);
  }

  @Override
  public Iterator<String> getServices() {
    return services.iterator();
  }

  // hailong:
  @Override
  public Set<Integer> getRDrawableIdValues() {
    return invRDrawableMap.keySet();
  }

  // hailong:
  @Override
  public Integer getRDrawableIdValue(String name) {
    return rDrawableMap.get(name);
  }

  // hailong:
  @Override
  public String getRDrawableIdName(Integer value) {
    return invRDrawableMap.get(value);
  }

  // hailong:
  @Override
  public Set<Integer> getSysRDrawableIdValues() {
    return invSysRDrawableMap.keySet();
  }

  // hailong:
  @Override
  public Integer getSysRDrawableIdValue(String name) {
    return sysRDrawableMap.get(name);
  }

  // hailong:
  @Override
  public String getSysRDrawableIdName(Integer value) {
    return invSysRDrawableMap.get(value);
  }

  // hailong: ga
  @Override
  public Set<Integer> getXmlIdValues() {
    return invRXmlMap.keySet();
  }

  // hailong: ga
  @Override
  public Integer getXmlIdValue(String name) {
    return rXmlMap.get(name);
  }

  // hailong: ga
  @Override
  public String getXmlIdName(Integer value) {
    return invRXmlMap.get(value);
  }

  //================================================

  private static final boolean debug = false;

  protected static DefaultXMLParser theInst;

  protected DefaultXMLParser() {
    doIt();
  }

  static synchronized DefaultXMLParser v() {
    if (theInst == null) {
      theInst = new DefaultXMLParser();
    }
    return theInst;
  }

  // === implementation details
  protected void doIt() {
    Logger.info("XMLParser", "Start reading");

    long startTime = System.nanoTime();
    readManifest();
    readRFile();

    // Strings must be read first
    readStrings();

    // hailong: disable as we don't need layout info
    // Then, layout and menu. Later, we may need to read preference as well.
//    readLayout();
//    readMenu();

    long estimatedTime = System.nanoTime() - startTime;
    Logger.info("XMLParser", "End reading: " + (estimatedTime * 1.0e-09) + " sec");
  }

  protected void readManifest() {
    String fn = Configs.project + "/AndroidManifest.xml";
    if (Configs.useAndroidStudio)
      fn = Paths.get(Configs.project, "app", "src", "main", "AndroidManifest.xml").toString();
    if (Configs.apkMode)
      fn = Paths.get(Configs.apktoolDir, "AndroidManifest.xml").toString();
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(fn);
      Node root = doc.getElementsByTagName("manifest").item(0);
      appPkg = root.getAttributes().getNamedItem("package").getTextContent();

      Node appNode = doc.getElementsByTagName("application").item(0);
      NodeList nodes = appNode.getChildNodes();
      for (int i = 0; i < nodes.getLength(); ++i) {
        Node n = nodes.item(i);
        String eleName = n.getNodeName();
        if ("activity".equals(eleName)) {
          NamedNodeMap m = n.getAttributes();
          String cls = Helper.getClassName(
                  m.getNamedItem("android:name").getTextContent(), appPkg);
          if (cls == null) {
            continue;
          }
          activities.add(cls);

          // hailong:
          Node parentActivityNode = m.getNamedItem("android:parentActivityName");
          if (parentActivityNode != null) {
            String parentActivity = Helper.getClassName(
                    parentActivityNode.getTextContent(), appPkg);
            activityAndParentActivity.put(cls, parentActivity);
          }

          if (isMainActivity(n)) {
            assert mainActivity == null;
            mainActivity = Scene.v().getSootClass(cls);
          }

          ActivityLaunchMode launchMode = ActivityLaunchMode.standard;
          Node launchModeNode = m.getNamedItem("android:launchMode");
          if (launchModeNode != null) {
            launchMode = ActivityLaunchMode.valueOf(
                    launchModeNode.getTextContent());
          }
          activityAndLaunchModes.put(cls, launchMode);
        }

        if ("service".equals(eleName)) {
          NamedNodeMap m = n.getAttributes();
          String partialClassName = m.getNamedItem("android:name").getTextContent();

          String cls = Helper.getClassName(partialClassName, appPkg);
          services.add(cls);

          if (Configs.verbose) {
            Logger.verb("XML", "Service: " + cls);
          }


        }

      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  protected void retriveIntentFilters(Node node) {
    NodeList list = node.getChildNodes();
    for (int i = 0; i < list.getLength(); i++) {
      Node n = list.item(i);
      String s = n.getNodeName();

      if (!s.equals("intent-filter"))
        continue;


    }
  }

  protected boolean isMainActivity(Node node) {
    assert "activity".equals(node.getNodeName());
    NodeList list = node.getChildNodes();
    for (int i = 0; i < list.getLength(); i++) {
      Node n = list.item(i);
      String s = n.getNodeName();
      if (!s.equals("intent-filter")) {
        continue;
      }
      if (isMainIntent(n)) {
        return true;
      }
    }
    return false;
  }

  protected boolean isMainIntent(Node node) {
    assert "intent-filter".equals(node.getNodeName());
    boolean isMain = false;
    boolean isLauncher = false;
    NodeList list = node.getChildNodes();
    for (int i = 0; i < list.getLength(); i++) {
      Node n = list.item(i);
      String s = n.getNodeName();
      if ("action".equals(s)) {
        NamedNodeMap m = n.getAttributes();
        String action = m.getNamedItem("android:name").getTextContent();
        if ("android.intent.action.MAIN".equals(action)) {
          isMain = true;
        }
      } else if ("category".equals(s)) {
        NamedNodeMap m = n.getAttributes();
        String category = m.getNamedItem("android:name").getTextContent();
        if ("android.intent.category.LAUNCHER".equals(category)) {
          isLauncher = true;
        }
      }
    }
    return isMain && isLauncher;
  }

  // --- END

  // --- R files

  // <R.id field, its const val>
  protected HashMap<String, Integer> rIdMap;
  protected HashMap<Integer, String> invRIdMap;
  protected HashMap<String, Integer> sysRIdMap;
  protected HashMap<Integer, String> invSysRIdMap;

  // hailong:
  // <R.drawable field, its const val>
  protected Map<String, Integer> rDrawableMap;
  protected Map<Integer, String> invRDrawableMap;
  protected Map<String, Integer> sysRDrawableMap;
  protected Map<Integer, String> invSysRDrawableMap;

  // <R.layout field, its const val>
  protected HashMap<String, Integer> rLayoutMap;
  protected HashMap<Integer, String> invRLayoutMap;
  protected HashMap<String, Integer> sysRLayoutMap;
  protected HashMap<Integer, String> invSysRLayoutMap;

  // <R.menu field, its const val>
  protected HashMap<String, Integer> rMenuMap;
  protected HashMap<Integer, String> invRMenuMap;
  protected HashMap<String, Integer> sysRMenuMap;
  protected HashMap<Integer, String> invSysRMenuMap;

  // <R.string field, its const val>
  protected HashMap<String, Integer> rStringMap;
  protected HashMap<Integer, String> invRStringMap;

  // <R.xml field, its const val>
  protected HashMap<String, Integer> rXmlMap;
  protected HashMap<Integer, String> invRXmlMap;

  protected final HashMap<String, Integer> sysRStringMap = Maps.newHashMap();
  protected final HashMap<Integer, String> invSysRStringMap = Maps.newHashMap();

  // <int const val, string val in xml>
  protected HashMap<Integer, String> intAndStringValues;
  // <R.string field, its string val>
  protected HashMap<String, String> rStringAndStringValues;

  protected final HashMap<Integer, String> sysIntAndStringValues = Maps.newHashMap();
  protected final HashMap<String, String> sysRStringAndStringValues = Maps.newHashMap();

  protected void readRFile() {
    rIdMap = Maps.newHashMap();
    invRIdMap = Maps.newHashMap();
    rDrawableMap = Maps.newHashMap();
    invRDrawableMap = Maps.newHashMap();
    rLayoutMap = Maps.newHashMap();
    invRLayoutMap = Maps.newHashMap();
    rStringMap = Maps.newHashMap();
    invRStringMap = Maps.newHashMap();
    rMenuMap = Maps.newHashMap();
    invRMenuMap = Maps.newHashMap();
    rXmlMap = Maps.newHashMap();
    invRXmlMap = Maps.newHashMap();

    // hailong:
    // in apk mode, all ids are stored in res/values/public.xml
    if (Configs.apkMode) {
      ApktoolResXMLReader.v().readIds("id", NameValueFunction.mapInvMap(rIdMap, invRIdMap));
      ApktoolResXMLReader.v().readIds("drawable", NameValueFunction.mapInvMap(rDrawableMap, invRDrawableMap));
      ApktoolResXMLReader.v().readIds("layout", NameValueFunction.mapInvMap(rLayoutMap, invRLayoutMap));
      ApktoolResXMLReader.v().readIds("string", NameValueFunction.mapInvMap(rStringMap, invRStringMap));
      ApktoolResXMLReader.v().readIds("menu", NameValueFunction.mapInvMap(rMenuMap, invRMenuMap));
      ApktoolResXMLReader.v().readIds("xml", NameValueFunction.mapInvMap(rXmlMap, invRXmlMap));
    }

    // R.id
    final String rIdClass = appPkg + ".R$id";
    readIntConstFields(rIdClass, NameValueFunction.mapInvMap(rIdMap, invRIdMap));

    //Moded by Haowei
//    for (String indice : rIdMap.keySet()){
//      Logger.verb("App_R", "Key: " + indice + " Value: "+ rIdMap.get(indice));
//    }
    //Mod end

    sysRIdMap = Maps.newHashMap();
    invSysRIdMap = Maps.newHashMap();
    final String sysRIdClass = "android.R$id";
    NameValueFunction sysRIdNVF = NameValueFunction.mapInvMap(sysRIdMap,
            invSysRIdMap);
    readIntConstFields(sysRIdClass, sysRIdNVF);

    final String internalSysRIdClass = "com.android.internal.R$id";
    SootClass idCls = Scene.v().getSootClass(internalSysRIdClass);
    if (idCls.isPhantom()) {
      ResourceConstantHelper.loadConstFromResFile(sysRIdNVF, "id", Configs.internalConstFile);
    } else {
      readIntConstFields(internalSysRIdClass, sysRIdNVF);
    }

    //Moded by Haowei
//    for (String indice : sysRIdMap.keySet()){
//      Logger.verb("Sys_R", "Key: " + indice + " Value: "+ sysRIdMap.get(indice));
//    }

//    for (String indice : invSysRIdMap.keySet()){
//      Logger.verb("invSys_R", "Key: " + indice + " Value: "+ invSysRIdMap.get(indice));
//    }
    //Mod end

    // hailong:
    // R.drawable
    final String rDrawableClass = appPkg + ".R$drawable";
    readIntConstFields(rDrawableClass, NameValueFunction.mapInvMap(rDrawableMap, invRDrawableMap));

    // hailong:
    sysRDrawableMap = Maps.newHashMap();
    invSysRDrawableMap = Maps.newHashMap();
    final String sysRDrawableClass = "android.R$drawable";
    readIntConstFields(sysRDrawableClass, NameValueFunction.mapInvMap(sysRDrawableMap, invSysRDrawableMap));

    // R.layout
    final String rLayoutClass = appPkg + ".R$layout";
    readIntConstFields(rLayoutClass,
            NameValueFunction.mapInvMap(rLayoutMap, invRLayoutMap));

    sysRLayoutMap = Maps.newHashMap();
    invSysRLayoutMap = Maps.newHashMap();
    final String sysRLayoutClass = "android.R$layout";
    NameValueFunction sysRLayoutNVF = NameValueFunction.mapInvMap(
            sysRLayoutMap, invSysRLayoutMap);
    readIntConstFields(sysRLayoutClass, sysRLayoutNVF);
    final String internalSysRLayoutClass = "com.android.internal.R$layout";
    idCls = Scene.v().getSootClass(internalSysRLayoutClass);
    if (idCls.isPhantom())
      ResourceConstantHelper.loadConstFromResFile(sysRLayoutNVF, "layout", Configs.internalConstFile);
    else
      readIntConstFields(internalSysRLayoutClass, sysRLayoutNVF);

    //Moded by Haowei
//    for (String indice : sysRLayoutMap.keySet()){
//      Logger.verb("SysRLayout_R", "Key: " + indice + " Value: "+ sysRLayoutMap.get(indice));
//    }
    //Mod end

    // R.menu
    final String rMenuClass = appPkg + ".R$menu";
    // it may not exist
    String menuResDir = Configs.project + "/res/menu";
    if (Configs.apkMode) menuResDir = Configs.apktoolDir + "/res/menu"; // hailong:
    if (new File(menuResDir).exists()) {
      readIntConstFields(rMenuClass,
              NameValueFunction.mapInvMap(rMenuMap, invRMenuMap));
    }
    sysRMenuMap = Maps.newHashMap();
    invSysRMenuMap = Maps.newHashMap();
    if (Configs.numericApiLevel > 10) {
      NameValueFunction sysRMenuNVF = NameValueFunction.mapInvMap(sysRMenuMap,
              invSysRMenuMap);
      readIntConstFields("android.R$menu", sysRMenuNVF);
      ResourceConstantHelper.loadConstFromResFile(sysRMenuNVF, "menu", Configs.internalConstFile);
      readIntConstFields("com.android.internal.R$menu", sysRMenuNVF);
    }

    // R.string
    final String rStringClass = appPkg + ".R$string";
    String valuesDir = Configs.project + "/res/values";
    if (Configs.apkMode) valuesDir = Configs.apktoolDir + "/res/values/string.xml"; // hailong:
    if (new File(valuesDir).exists()) {
      readIntConstFields(rStringClass,
              NameValueFunction.mapInvMap(rStringMap, invRStringMap));
    }
    NameValueFunction sysRStringNVF =
            NameValueFunction.mapInvMap(sysRStringMap, invSysRStringMap);
    readIntConstFields("android.R$string", sysRStringNVF);
    ResourceConstantHelper.loadConstFromResFile(sysRStringNVF, "string", Configs.internalConstFile);
    readIntConstFields("com.android.internal.R$string", sysRStringNVF);
  }

  protected static void readIntConstFields(String clsName, NameValueFunction nvf) {
    SootClass idCls = Scene.v().getSootClass(clsName);
    // This particular R$* class is not used. Should be system R class though.
    if (idCls.isPhantom()) {
      if (Configs.verbose) {
        System.out.println("[DEBUG] " + clsName + " is phantom!");
      }
      return;
    }
    //Moded by Haowei
    //Logger.verb("readIntConst", idCls.toString());
    //End mod
    for (SootField f : idCls.getFields()) {
      if (f.getTag("IntegerConstantValueTag") == null) continue;
      String tag = f.getTag("IntegerConstantValueTag").toString();
      int val = Integer.parseInt(tag.substring("ConstantValue: ".length()));
      // hailong:
      // String name = idCls.getName() + "." + f.getName();
      String name = f.getName();
      nvf.feed(name, val);
    }
  }

  // --- END

  // --- read layout files
  protected static final String ID_ATTR = "android:id";
  protected static final String TEXT_ATTR = "android:text";
  protected static final String TITLE_ATTR = "android:title";

  protected static int nonRId = -0x7f040000;

  protected HashMap<Integer, AndroidView> id2View;
  protected HashMap<Integer, AndroidView> sysId2View;

//  protected void readLayout() {
//    id2View = Maps.newHashMap();
//    readLayout(Configs.project, invRLayoutMap, id2View);
//
//    sysId2View = Maps.newHashMap();
//    readLayout(Configs.sysProj, invSysRLayoutMap, sysId2View);
//
//    resolveIncludes(Configs.project, invRLayoutMap, id2View);
//    resolveIncludes(Configs.sysProj, invSysRLayoutMap, sysId2View);
//  }

  // TODO: due to the way we implement resolveIncludes(), now we need
  // to change findViewById.
  protected void resolveIncludes(String proj, HashMap<Integer, String> nameMap,
                                 HashMap<Integer, AndroidView> viewMap) {

    HashMap<String, AndroidView> name2View = Maps.newHashMap();
    for (Map.Entry<Integer, String> entry : nameMap.entrySet()) {
      String name = entry.getValue();
      AndroidView view = viewMap.get(entry.getKey());
      name2View.put(name, view);
    }
    boolean isSys = (viewMap == sysId2View);
    LinkedList<AndroidView> work = Lists.newLinkedList();
    work.addAll(viewMap.values());
    while (!work.isEmpty()) {
      AndroidView view = work.remove();
      for (int i = 0; i < view.getNumberOfChildren(); i++) {
        IAndroidView child = view.getChildInternal(i);
        if (child instanceof AndroidView) {
          work.add((AndroidView) child);
          continue;
        }
        IncludeAndroidView iav = (IncludeAndroidView) child;
        String layoutId = iav.layoutId;
        AndroidView tgt = name2View.get(layoutId);
        if (tgt == null) {
          // not exist, let's get it on-demand
          String file = getLayoutFilePath(proj, layoutId, isSys);
          if (file == null) {
            System.err.println("[WARNING] Unknown layout " + layoutId
                    + " included by " + view.getOrigin());
            continue;
          }
          tgt = new AndroidView();
          tgt.setParent(view, i);
          tgt.setOrigin(file);
          readLayout(file, tgt, isSys);
          int newId = nonRId--;
          viewMap.put(newId, tgt);
          nameMap.put(newId, layoutId);
        } else {
          tgt = (AndroidView) tgt.deepCopy();
          tgt.setParent(view, i);
        }
        Integer includeeId = iav.includeeId;
        if (includeeId != null) {
          tgt.setId(includeeId.intValue());
        }
        work.add(tgt);
      }
    }
  }

  protected void readLayout(String proj, HashMap<Integer, String> in,
                            HashMap<Integer, AndroidView> out) {
    if (debug) {
      System.out.println("*** read layout of " + proj);
    }
    boolean isSys = (invSysRLayoutMap == in);
    assert Configs.project.equals(proj) ^ isSys;

    for (Map.Entry<Integer, String> entry : in.entrySet()) {
      Integer layoutFileId = entry.getKey();
      String layoutFileName = entry.getValue();
      AndroidView root = new AndroidView();
      out.put(layoutFileId, root);

      String file = getLayoutFilePath(proj, layoutFileName, isSys);
      if (file == null) {
        System.err.println("[WARNING] Cannot find " + layoutFileName
                + ".xml in " + proj);
        continue;
      }

      readLayout(file, root, isSys);
    }
  }

  protected void readLayout(String file, AndroidView root, boolean isSys) {
    Document doc;
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      doc = dBuilder.parse(file);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    Element rootElement = doc.getDocumentElement();
    // In older versions, Preference could be put in layout folder and we do
    // not support Prefernce yet.
    if (rootElement.getTagName().equals("PreferenceScreen")) {
      return;
    }

    LinkedList<Pair<Node, AndroidView>> work = Lists.newLinkedList();
    work.add(new Pair<Node, AndroidView>(rootElement, root));
    while (!work.isEmpty()) {
      Pair<Node, AndroidView> p = work.removeFirst();
      Node node = p.getO1();
      AndroidView view = p.getO2();
      view.setOrigin(file);

      NamedNodeMap attrMap = node.getAttributes();
      if (attrMap == null) {
        System.out.println(file + "!!!" + node.getClass() + "!!!"
                + node.toString() + "!!!" + node.getTextContent());
      }
      // Retrieve view id (android:id)
      Node idNode = attrMap.getNamedItem(ID_ATTR);
      int guiId = -1;
      String id = null;
      if (idNode != null) {
        String txt = idNode.getTextContent();
        Pair<String, Integer> pair = parseAndroidId(txt, isSys);
        id = pair.getO1();
        Integer guiIdObj = pair.getO2();
        if (guiIdObj == null) {
          if (debug) {
            System.err.println("[WARNING] unresolved android:id " + id + " in "
                    + file);
          }
        } else {
          guiId = guiIdObj.intValue();
        }
      }

      // Retrieve view type
      String guiName = node.getNodeName();
      if ("view".equals(guiName)) {
        guiName = attrMap.getNamedItem("class").getTextContent();
      } else if (guiName.equals("MenuItemView")) {
        // FIXME(tony): this is an "approximation".
        guiName = "android.view.MenuItem";
      }

      if (debug) {
        System.out.println(guiName + " (" + guiId + ", " + id + ")");
      }

      //Retrieve callback (android:onClick)
      if (guiId != -1) {
        String callback = readAndroidCallback(attrMap, "android:onClick");
        if (callback != null) {
          Pair<String, Boolean> pair = new Pair<String, Boolean>(callback, false);
          this.callbacksXML.put(guiId, pair);
        }
      }

      // Retrieve text (android:text)
      String text = readAndroidTextOrTitle(attrMap, TEXT_ATTR);

      view.save(guiId, text, guiName);

      NodeList children = node.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        Node newNode = children.item(i);
        String nodeName = newNode.getNodeName();
        if ("#comment".equals(nodeName)) {
          continue;
        }
        if ("#text".equals(nodeName)) {
          // possible for XML files created on a different operating system
          // than the one our analysis is run on
          continue;
        }
        if (nodeName.equals("requestFocus")) {
          continue;
        }
        if (!newNode.hasAttributes() && !"TableRow".equals(nodeName)
                && !"View".equals(nodeName)) {
          System.err.println("[WARNING] no attribute node "
                  + newNode.getNodeName());
        }

        if (newNode.getNodeName().equals("include")) {
          attrMap = newNode.getAttributes();
          String layoutTxt = attrMap.getNamedItem("layout").getTextContent();
          String layoutId = null;
          if (layoutTxt.startsWith("@layout/")) {
            layoutId = layoutTxt.substring("@layout/".length());
          } else if (layoutTxt.startsWith("@android:layout/")) {
            layoutId = layoutTxt.substring("@android:layout/".length());
          } else {
            if (debug) {
              throw new RuntimeException("[WARNING] Unhandled layout id "
                      + layoutTxt);
            }
            continue;
          }
          Integer includeeId = null;
          id = null;
          idNode = attrMap.getNamedItem(ID_ATTR);
          if (idNode != null) {
            String txt = idNode.getTextContent();
            Pair<String, Integer> pair = parseAndroidId(txt, isSys);
            id = pair.getO1();
            Integer guiIdObj = pair.getO2();
            if (guiIdObj == null) {
              if (debug) {
                System.err.println("[WARNING] unresolved android:id " + id
                        + " in " + file);
              }
            } else {
              includeeId = guiIdObj;
            }
          }

          // view.saveInclude(layoutId, includeeId);
          IncludeAndroidView iav = new IncludeAndroidView(layoutId, includeeId);
          iav.setParent(view);
        } else {
          AndroidView newView = new AndroidView();
          newView.setParent(view);
          work.add(new Pair<Node, AndroidView>(newNode, newView));
        }
      }
    }
  }

  protected String getLayoutFilePath(String project, String layoutId,
                                     boolean isSys) {
    // special cases
    if ("keyguard_eca".equals(layoutId)) {
      // its real name is defined in values*/alias.xml
      // for our purpose, we can simply hack it
      assert isSys;
      // use the value for portrait
      String ret = project + "/res/layout/keyguard_emergency_carrier_area.xml";
      assert new File(ret).exists() : "ret=" + ret;
      return ret;
    }
    if ("status_bar_latest_event_ticker_large_icon".equals(layoutId)
            || "status_bar_latest_event_ticker".equals(layoutId)
            || "keyguard_screen_status_land".equals(layoutId)
            || "keyguard_screen_status_port".equals(layoutId)) {
      assert isSys;
      String ret = findFileExistence(project + "/res", "layout", layoutId + ".xml");
/*
      String ret = project + "/res/layout-sw600dp/" + layoutId + ".xml";
      if (!new File(ret).exists()) {
        ret = project + "/res/layout-sw720dp/" + layoutId + ".xml";
      }
*/
      assert new File(ret).exists() : "ret=" + ret;
      return ret;
    }
    ArrayList<String> projectDirs = Lists.newArrayList();
    projectDirs.add(project);
    if (!isSys) {
      projectDirs.addAll(Configs.extLibs);
    }
/*
    for (String proj : projectDirs) {
      String file = proj + "/res/layout/" + layoutId + ".xml";
      if (!new File(file).exists()) {
        file = proj + "/res/layout-port/" + layoutId + ".xml";
        if (!new File(file).exists()) {
          file = proj + "/res/layout-land/" + layoutId + ".xml";
          if (!new File(file).exists()) {
            file = proj + "/res/layout-sw600dp/" + layoutId + ".xml";
            if (!new File(file).exists()) {
              file = proj + "/res/layout-sw720dp/" + layoutId + ".xml";
              if (!new File(file).exists()) {
                file = proj + "/res/layout-finger/" + layoutId + ".xml";
                if (!new File(file).exists()) {
                  file = null;
                }
              }
            }
          }
        }
      }
      if (file != null) {
        return file;
      }
    }
*/
    for (String proj : projectDirs) {
      String file;
      if (isSys || !Configs.useAndroidStudio)
        file = findFileExistence(proj + "/res", "layout", layoutId + ".xml");
      else
        // TODO(hailong): may be wrong: file = findFileExistence(proj + "/app/src/main/res", "layout", layoutId + ".xml");
        file = findFileExistence(proj + "/app/build/intermediates/res/merged/debug", "layout", layoutId + ".xml");
      if (file == null) {
        continue;
      }
      if (new File(file).exists()) {
        return file;
      }
    }
    return null;
  }

  protected String readAndroidCallback(NamedNodeMap attrMap, String callback) {
    Node node = attrMap.getNamedItem(callback);
    if (node == null) {
      return null;
    }
    String refOrValue = node.getTextContent();
    if (debug) {
      System.out.println("  * `" + refOrValue + "' -> `" + refOrValue + "'");
    }
    return refOrValue;
  }

  protected Pair<String, Integer> parseAndroidId(String txt, boolean isSys) {
    String id = null;
    Integer guiIdObj = null;
    if ("@+android:id/internalEmpty".equals(txt)) {
      id = "internalEmpty";
      guiIdObj = sysRIdMap.get(id);
    } else if (txt.startsWith("@id/android:")) {
      id = txt.substring(12);
      guiIdObj = sysRIdMap.get(id);
    } else if (txt.startsWith("@+id/android:")
            || txt.startsWith("@+android:id/")) { // handle old code
      id = txt.substring(13);
      guiIdObj = sysRIdMap.get(id);
    } else if (txt.startsWith("@+id")) {
      id = txt.substring(5);
      if (isSys) {
        guiIdObj = sysRIdMap.get(id);
      } else {
        guiIdObj = rIdMap.get(id);
      }
    } else if (txt.startsWith("@id/")) {
      id = txt.substring(4);
      if (isSys) {
        guiIdObj = sysRIdMap.get(id);
      } else {
        guiIdObj = rIdMap.get(id);
      }
    } else if (txt.startsWith("@android:id")) {
      id = txt.substring(12);
      guiIdObj = sysRIdMap.get(id);
    } else {
      if (debug) throw new RuntimeException("[WARNING] Unhandled android:id prefix " + txt);
    }
    return new Pair<String, Integer>(id, guiIdObj);
  }

  // --- END

  // --- read menu*/*.xml
  protected void readMenu() {
//    readMenu(Configs.project, invRMenuMap, id2View);
//    readMenu(Configs.sysProj, invSysRMenuMap, sysId2View);
  }

  protected void readMenu(String proj, HashMap<Integer, String> map,
                          HashMap<Integer, AndroidView> viewMap) {
    boolean isSys = (map == invSysRMenuMap);
    assert proj.equals(Configs.project) ^ isSys;

    for (Map.Entry<Integer, String> e : map.entrySet()) {
      Integer val = e.getKey();
      String name = e.getValue();
      AndroidView root = new AndroidView();
      viewMap.put(val, root);
      String file = getMenuFilePath(proj, name, isSys);
      if (file == null) {
        System.err.println("Unknown menu " + name + " for " + proj);
        continue;
      }
      root.setOrigin(file);
      Logger.verb(DefaultXMLParser.class.getSimpleName(), "--- reading " + file);

      readMenu(file, root, isSys);
    }
  }

  protected void readMenu(String file, AndroidView root, boolean isSys) {
    Document doc;
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      doc = dBuilder.parse(file);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    LinkedList<Pair<Node, AndroidView>> worklist = Lists.newLinkedList();
    worklist.add(new Pair<Node, AndroidView>(doc.getDocumentElement(), root));
    root = null;
    while (!worklist.isEmpty()) {
      Pair<Node, AndroidView> pair = worklist.remove();
      Node node = pair.getO1();
      AndroidView view = pair.getO2();
      NamedNodeMap attrMap = node.getAttributes();
      Node idNode = attrMap.getNamedItem(ID_ATTR);
      int guiId = -1;
      String id = null;
      if (idNode != null) {
        String txt = idNode.getTextContent();
        Pair<String, Integer> p = parseAndroidId(txt, isSys);
        id = p.getO1();
        Integer guiIdObj = p.getO2();
        if (guiIdObj == null) {
          if (debug) {
            System.err.println("[WARNING] unresolved android:id " + id + " in "
                    + file);
          }
          guiId = nonRId--; // negative value to indicate it is a unique id but
          // we don't know its value
          if (isSys) {
            sysRIdMap.put(id, guiId);
            invSysRIdMap.put(guiId, id);
          } else {
            rIdMap.put(id, guiId);
            invRIdMap.put(guiId, id);
          }
        } else {
          guiId = guiIdObj.intValue();
        }
      }

      // FIXME(tony): this is an "approximation"
      String guiName = node.getNodeName();
      if (guiName.equals("menu")) {
        guiName = "android.view.Menu";
      } else if (guiName.equals("item")) {
        guiName = "android.view.MenuItem";
      } else if (guiName.equals("group")) {
        // TODO(tony): we might want to create a special fake class to
        // represent menu groups. But for now, let's simply pretend it's
        // a ViewGroup. Also, print a warning when we do see <group>
        if (Configs.verbose) {
          System.out.println("[TODO] <group> used in " + file);
        }
        guiName = "android.view.ViewGroup";
      } else {
        if (debug) throw new RuntimeException("Unhandled menu tag " + guiName);
        continue;
      }
      if (debug) {
        System.out.println(guiName + " (" + guiId + ", " + id + ")");
      }
      String text = readAndroidTextOrTitle(attrMap, TITLE_ATTR);

      view.save(guiId, text, guiName);
      NodeList children = node.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        Node newNode = children.item(i);
        String nodeName = newNode.getNodeName();
        if ("#comment".equals(nodeName)) {
          continue;
        }
        if ("#text".equals(nodeName)) {
          // possible for XML files created on a different operating system
          // than the one our analysis is run on
          continue;
        }

        AndroidView newView = new AndroidView();
        // FIXME: we assume that every node has attributes, may be wrong
        if (!newNode.hasAttributes()) {
          continue;
        } else {
          NamedNodeMap attrs = newNode.getAttributes();
          for (int idx = 0; idx < attrs.getLength(); idx += 1) {
            Node attr = attrs.item(idx);
            String name = attr.getNodeName();
            String value = attr.getNodeValue();
            newView.addAttr(name, value);
          }
        }
        newView.setParent(view);
        worklist.add(new Pair<Node, AndroidView>(newNode, newView));
      }
    }
  }

  protected String getMenuFilePath(String project, String menuId,
                                   boolean isSys) {
    ArrayList<String> projectDirs = Lists.newArrayList();
    projectDirs.add(project);
    if (!isSys) {
      projectDirs.addAll(Configs.extLibs);
    }

    for (String proj : projectDirs) {
      String file = findFileExistence(proj + "/res", "menu", menuId + ".xml");
/*
      String file = proj + "/res/menu/" + menuId + ".xml";
      if (!new File(file).exists()) {
        file = null;
      }
*/
      if (file != null) {
        return file;
      }
    }
    return null;
  }
  // --- END

  // --- read values/*.xml
  protected void readStrings() {
    if (debug) {
      Logger.verb("DefaultXMLParser", "Reading strings from " + Configs.apktoolDir + "/res");
    }
    intAndStringValues = Maps.newHashMap();
    rStringAndStringValues = Maps.newHashMap();
    for (String file : getStringXMLFilePaths(Configs.apktoolDir + "/res", false)) {
      readStrings(file, intAndStringValues, rStringAndStringValues, rStringMap);
    }

    for (String file : getStringXMLFilePaths(Configs.apktoolDir + "/res", true)) {
      readStrings(file, sysIntAndStringValues, sysRStringAndStringValues, sysRStringMap);
    }
  }

  final static String SYS_ANDROID_STRING_REF = "@android:string/";
  final static int SYS_ANDROID_STRING_REF_LENGTH =
          SYS_ANDROID_STRING_REF.length();

  final static String ANOTHER_SYS_ANDROID_STRING_REF = "@*android:string/";
  final static int ANOTHER_SYS_ANDROID_STRING_REF_LENGTH =
          ANOTHER_SYS_ANDROID_STRING_REF.length();

  final static String MOSTLY_APP_ANDROID_STRING_REF = "@string/";
  final static int MOSTLY_APP_ANDROID_STRING_REF_LENGTH =
          MOSTLY_APP_ANDROID_STRING_REF.length();

  String convertAndroidTextToString(String androidText) {
    if (androidText.isEmpty()) {
      return null;
    }
    // Is it string ref
    if (androidText.charAt(0) == '@') {
      if (androidText.startsWith(SYS_ANDROID_STRING_REF)) {
        return sysRStringAndStringValues.get(
                androidText.substring(SYS_ANDROID_STRING_REF_LENGTH));
      }
      if (androidText.startsWith(ANOTHER_SYS_ANDROID_STRING_REF)) {
        return sysRStringAndStringValues.get(
                androidText.substring(ANOTHER_SYS_ANDROID_STRING_REF_LENGTH));
      }
      if (androidText.startsWith(MOSTLY_APP_ANDROID_STRING_REF)) {
        String stringName =
                androidText.substring(MOSTLY_APP_ANDROID_STRING_REF_LENGTH);
        String result = rStringAndStringValues.get(stringName);
        if (result == null) {
          result = sysRStringAndStringValues.get(stringName);
        }
        return result;
      }
      // Workaround for a weird case in XBMC
      return null;
      //throw new RuntimeException("Unknown android:text format " + androidText);
    } else {
      return androidText;
    }
  }

  String readAndroidTextOrTitle(NamedNodeMap attrMap, String attributeName) {
    Node textNode = attrMap.getNamedItem(attributeName);
    String text = null;
    if (textNode != null) {
      String refOrValue = textNode.getTextContent();
      text = convertAndroidTextToString(refOrValue);
      if (debug) {
        System.out.println("  * `" + refOrValue + "' -> `" + text + "'");
      }
    }
    return text;
  }

  protected void readStrings(String file, HashMap<Integer, String> idAndStrings,
                             HashMap<String, String> stringFieldAndStrings,
                             HashMap<String, Integer> stringFieldAndIds) {
    Document doc;
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      doc = dBuilder.parse(file);
    } catch (Exception ex) {
      if (debug)
        throw new RuntimeException(ex);
      return;
    }
    Logger.verb(DefaultXMLParser.class.getSimpleName(), "--- reading " + file);
    NodeList nodes = doc.getElementsByTagName("string");
    if (nodes == null) {
      return;
    }
    for (int i = 0; i < nodes.getLength(); i++) {
      Node n = nodes.item(i);
      if (!"string".equals(n.getNodeName())) {
        throw new RuntimeException();
      }
      NamedNodeMap attrs = n.getAttributes();
      String stringName = attrs.getNamedItem("name").getTextContent();
      NodeList childNodes = n.getChildNodes();
      String stringValue;
      if (childNodes.getLength() == 0) {
        stringValue = "";
      } else {
        stringValue = eliminateQuotes(childNodes.item(0).getTextContent());
      }
      stringFieldAndStrings.put(stringName, stringValue);

      Integer idValueObj = stringFieldAndIds.get(stringName);
      if (idValueObj == null) {
        if (debug) {
          throw new RuntimeException("Unknown string node " + stringName
                  + " defined in " + file);
        }
      } else {
        idAndStrings.put(idValueObj, stringValue);
      }
    }
  }

  protected String eliminateQuotes(String s) {
    int len = s.length();
    if (len > 1 && s.charAt(0) == '"' && s.charAt(len - 1) == '"') {
      return s.substring(1, len - 1);
    }
    return s;
  }

  /*
   *  Usually the file name is strings.xml, but it technically can be anything.
   *  For now, let's read strings.xml and strings-*.xml.
   */
  protected ArrayList<String> getStringXMLFilePaths(String project, boolean isSys) {
    ArrayList<String> projectDirs = Lists.newArrayList();
    projectDirs.add(project);
    if (!isSys) {
      projectDirs.addAll(Configs.extLibs);
    }
    ArrayList<String> xmlFiles = Lists.newArrayList();
    for (String proj : projectDirs) {
      String valuesDirectoryName = proj + "/values/";
      File valuesDirectory = new File(valuesDirectoryName);
      if (!valuesDirectory.exists()) {
        Logger.warn(
                "[WARNING] Directory " + valuesDirectory + " does not exist!");
        return Lists.newArrayList();
      }
      for (String file : valuesDirectory.list()) {
        if (file.equals("strings.xml")
                || (file.startsWith("strings-") && file.endsWith(".xml"))) {
          xmlFiles.add(valuesDirectoryName + file);
        }
      }
    }
    return xmlFiles;
  }
  // --- END

  // === END

  protected static String findFileExistence(String folderName, String dirName, String tgtFileName) {
    File folder = new File(folderName);
    // hailong:
    if (folder.listFiles() == null)
      return null;
    for (File subFolder : folder.listFiles()) {
      if (subFolder.isDirectory()) {
        String subDirName = subFolder.getName();
        if (subDirName.length() < dirName.length()) {
          continue;
        }
        if (subDirName.startsWith(dirName)) {
          for (File subFile : subFolder.listFiles()) {
            if (subFile.getName().equals(tgtFileName))
              return folderName + "/" + subDirName + "/" + tgtFileName;
          }
        }
      }
    }
    return null;
  }

  // record callbacks defined in xml
  protected HashMap<Integer, Pair<String, Boolean>> callbacksXML = new HashMap<Integer, Pair<String, Boolean>>();

  @Override
  public Map<Integer, Pair<String, Boolean>> retrieveCallbacks() {
    return callbacksXML;
  }
}
