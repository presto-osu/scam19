/*
 * TransformationUtil.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.instrument.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import edu.osu.cse.presto.android.gator.Logger;
import edu.osu.cse.presto.android.gator.ga.Universe;
import edu.osu.cse.presto.android.gator.instrument.Configs;
import edu.osu.cse.presto.android.gator.instrument.runtime.Proxy;
import soot.*;
import soot.jimple.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Iterator;
import java.util.Set;

public class TransformationUtil {
  private final String TAG = TransformationUtil.class.getSimpleName();

  private static TransformationUtil instance;

  public static synchronized TransformationUtil v() {
    if (instance == null) {
      instance = new TransformationUtil();
    }
    return instance;
  }

  private Universe readDataUniverse() {
    Preconditions.checkNotNull(Configs.dataUniverseXmlPath);
    try {
      JAXBContext jc = JAXBContext.newInstance(Universe.class);
      Unmarshaller m = jc.createUnmarshaller();
      return (Universe) m.unmarshal(new File(Configs.dataUniverseXmlPath));
    } catch (JAXBException e) {
      e.printStackTrace();
    }
    throw new RuntimeException("Error reading data universe.");
  }

  private Set<String> readRuntimeDataUniverse() {
    Set<String> ret = Sets.newHashSet();
    if (Configs.runtimeDb != null && !Configs.runtimeDb.equals("<no-db>")) {
      Path dbPath = Paths.get(Configs.runtimeDb).toAbsolutePath();
      Connection connection = null;
      try {
        // create a database connection
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        Statement statement = connection.createStatement();
        statement.setQueryTimeout(30);  // set timeout to 30 sec.
        ResultSet rs = statement.executeQuery("SELECT name FROM screen_names");
        while (rs.next()) {
          // read the result set
          String name = rs.getString("name");
          System.out.println("name = " + name);
          ret.add(name);
        }
      } catch (SQLException e) {
        // if the error message is "out of memory",
        // it probably means no database file is found
        System.err.println(e.getMessage());
      } finally {
        try {
          if (connection != null)
            connection.close();
        } catch (SQLException e) {
          // connection close failed.
          System.err.println(e);
        }
      }
    }
    return ret;
  }

  public void instrumentRuntimeProxy(SootClass proxyCls) {
    if (!Configs.randomization)
      return;
    SootMethod proxyInitMtd = SootUtil.v().PROXY_INIT_MTD;
    Body jbody = proxyInitMtd.retrieveActiveBody();
    for (final Iterator<Unit> iter = jbody.getUnits().snapshotIterator(); iter.hasNext(); ) {
      Stmt point = (Stmt) iter.next();
      if (point.containsInvokeExpr()) {
        InvokeExpr expr = point.getInvokeExpr();
        SootMethod callee = expr.getMethod();
        if (callee.getSignature().equals(SootUtil.LOG_I_SIG)) {
          Universe universe = readDataUniverse();
          universe.name.addAll(readRuntimeDataUniverse());

          SootField insField = proxyCls.getField("edu.osu.cse.presto.android.gator.instrument.runtime.Proxy instance");
          final Local insLocal = SootUtil.v().getTempLocal(jbody, Proxy.class.getName(), "tmp_ins");
          Stmt toInsert = Jimple.v().newAssignStmt(insLocal, Jimple.v().newStaticFieldRef(insField.makeRef()));
          Logger.info(TAG, "Insert " + toInsert + " after " + point);
          jbody.getUnits().insertAfter(toInsert, point);
          point = toInsert;

          SootField epsilonField = proxyCls.getField("double EPSILON");
          toInsert = Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(epsilonField.makeRef()), DoubleConstant.v(Configs.epsilon));
          Logger.info(TAG, "Insert " + toInsert + " after " + point);
          jbody.getUnits().insertAfter(toInsert, point);
          point = toInsert;

          SootField randField = proxyCls.getField("boolean enableRandomization");
          toInsert = Jimple.v().newAssignStmt(Jimple.v().newInstanceFieldRef(insLocal, randField.makeRef()), IntConstant.v(1));
          Logger.info(TAG, "Insert " + toInsert + " after " + point);
          jbody.getUnits().insertAfter(toInsert, point);
          point = toInsert;

          SootField vField = proxyCls.getField("java.util.Set V");
          final Local vLocal = SootUtil.v().getTempLocal(jbody, "java.util.Set", "tmp_v");
          toInsert = Jimple.v().newAssignStmt(vLocal, Jimple.v().newStaticFieldRef(vField.makeRef()));
          Logger.info(TAG, "Insert " + toInsert + " after " + point);
          jbody.getUnits().insertAfter(toInsert, point);
          point = toInsert;

          for (String item : universe.name) {
            Expr e = Jimple.v().newInterfaceInvokeExpr(vLocal, SootUtil.v().SET_ADD_MTD.makeRef(), StringConstant.v(item));
            toInsert = Jimple.v().newInvokeStmt(e);
            Logger.info(TAG, "Insert " + toInsert + " after " + point);
            jbody.getUnits().insertAfter(toInsert, point);
            point = toInsert;
          }

          SootField mField = proxyCls.getField("java.util.Map act2name");
          final Local mLocal = SootUtil.v().getTempLocal(jbody, "java.util.Map", "tmp_m");
          toInsert = Jimple.v().newAssignStmt(mLocal, Jimple.v().newInstanceFieldRef(insLocal, mField.makeRef()));
          Logger.info(TAG, "Insert " + toInsert + " after " + point);
          jbody.getUnits().insertAfter(toInsert, point);
          point = toInsert;

          for (String act : universe.act2name.keySet()) {
            Expr e = Jimple.v().newInterfaceInvokeExpr(mLocal, SootUtil.v().MAP_PUT_MTD.makeRef(),
                    StringConstant.v(act), StringConstant.v(universe.act2name.get(act)));
            toInsert = Jimple.v().newInvokeStmt(e);
            Logger.info(TAG, "Insert " + toInsert + " after " + point);
            jbody.getUnits().insertAfter(toInsert, point);
            point = toInsert;
          }

          if (universe.autoTracking) {
            SootField trackingField = proxyCls.getField("boolean enableAutoTracking");
            toInsert = Jimple.v().newAssignStmt(Jimple.v().newInstanceFieldRef(insLocal, trackingField.makeRef()), IntConstant.v(1));
            Logger.info(TAG, "Insert " + toInsert + " after " + point);
            jbody.getUnits().insertAfter(toInsert, point);
            point = toInsert;
          }

          if (Configs.experiment) {
            SootField expField = proxyCls.getField("boolean experimentMode");
            toInsert = Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(expField.makeRef()), IntConstant.v(1));
            Logger.info(TAG, "Insert " + toInsert + " after " + point);
            jbody.getUnits().insertAfter(toInsert, point);
            point = toInsert;

//            SootField simField = proxyCls.getField("int simulateUserNum");
//            toInsert = Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(simField.makeRef()), IntConstant.v(Configs.simulateUserNum));
//            Logger.info(TAG, "Insert " + toInsert + " after " + point);
//            jbody.getUnits().insertAfter(toInsert, point);
//            point = toInsert;
//
//            SootField scaleField = proxyCls.getField("int scaleEvents");
//            toInsert = Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(scaleField.makeRef()), IntConstant.v(Configs.scaleEvents));
//            Logger.info(TAG, "Insert " + toInsert + " after " + point);
//            jbody.getUnits().insertAfter(toInsert, point);
//            point = toInsert;
          }
          break;
        }
      }
    }
  }

}
