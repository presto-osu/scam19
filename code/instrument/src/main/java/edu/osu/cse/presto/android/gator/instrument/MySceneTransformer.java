/*
 * MySceneTransformer.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.instrument;

import com.google.common.collect.Queues;
import edu.osu.cse.presto.android.gator.Logger;
import edu.osu.cse.presto.android.gator.instrument.util.SootUtil;
import edu.osu.cse.presto.android.gator.instrument.util.TransformationUtil;
import soot.*;
import soot.jimple.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MySceneTransformer extends SceneTransformer {
  final String TAG = MySceneTransformer.class.getSimpleName();
  TransformationUtil util = TransformationUtil.v();

  public MySceneTransformer() {
    Logger.verb(TAG, "MySceneTransformer created.");
  }


  @Override
  protected void internalTransform(String phaseName, Map<String, String> options) {
    int numCls = 0, numMtd = 0, numStm = 0;
    for (SootClass cls : Scene.v().getApplicationClasses()) {
      if (cls.isInterface())
        continue;
      // trick to avoid crashing by removing casting
      // CastExpr will be translated to check-cast in DEX
      // remove it could remove run-time type checking
      String clsName = cls.getName();
      if (clsName.startsWith("com.google.android.gms.phenotype.Phenotype") ||
              clsName.startsWith("com.google.android.gms.internal.zzcue") ||
              clsName.startsWith("com.google.android.gms.auth.api.Auth") ||
              clsName.startsWith("com.google.android.gms.drive.Drive") ||
              clsName.startsWith("com.google.android.gms.internal.zzarc") ||
              clsName.startsWith("com.google.android.gms.wearable.Wearable") ||
              clsName.startsWith("com.google.android.gms.plus.Plus") ||
              clsName.startsWith("com.google.android.gms.")) {
        for (SootMethod mtd : cls.getMethods()) {
          if (!mtd.isConcrete() || !mtd.getSubSignature().equals("void <clinit>()")) continue;
          Body jbody = mtd.retrieveActiveBody();
          for (final Iterator<Unit> iter = jbody.getUnits().snapshotIterator(); iter.hasNext(); ) {
            Stmt stmt = (Stmt) iter.next();
            if (stmt instanceof AssignStmt) {
              Value rop = ((AssignStmt) stmt).getRightOp();
              if (rop instanceof CastExpr) {
                ((AssignStmt) stmt).setRightOp(((CastExpr) rop).getOp());
              }
            }
          }
        }
      }
      // some app doesn't have Tracker.get
      // we add it if they don't
      if (clsName.equals("com.google.android.gms.analytics.Tracker")) {
        SootMethod getMtd = cls.getMethodUnsafe("java.lang.String get(java.lang.String)");
        if (getMtd == null) {
          SootMethod setMtd = cls.getMethodUnsafe("void set(java.lang.String,java.lang.String)");
          if (setMtd == null) {
            Logger.warn(TAG, "Tracker does not have set method.");
          } else {
            InstanceFieldRef mapField = null;
            Body jbody = setMtd.retrieveActiveBody();
            for (final Iterator<Unit> iter = jbody.getUnits().snapshotIterator(); iter.hasNext(); ) {
              Stmt stmt = (Stmt) iter.next();
//              Logger.info(TAG, "........... " + stmt);
              if (stmt instanceof AssignStmt) {
                Value rop = ((AssignStmt) stmt).getRightOp();
                if (rop.getType().toString().equals("java.util.Map")) {
                  mapField = (InstanceFieldRef) rop;
                }
              }
            }

            getMtd = new SootMethod("get",
                    Arrays.asList(new Type[]{RefType.v("java.lang.String")}),
                    RefType.v("java.lang.String"), Modifier.PUBLIC);
            cls.addMethod(getMtd);
            jbody = Jimple.v().newBody(getMtd);
            getMtd.setActiveBody(jbody);

            Local thisLocal = Jimple.v().newLocal("r0", RefType.v(cls));
            jbody.getLocals().addFirst(thisLocal);
            jbody.getUnits().add(Jimple.v().newIdentityStmt(thisLocal,
                    Jimple.v().newThisRef(RefType.v(cls))));

            Local argLocal = Jimple.v().newLocal("r1", RefType.v("java.lang.String"));
            jbody.getLocals().add(argLocal);
            jbody.getUnits().add(Jimple.v().newIdentityStmt(argLocal,
                    Jimple.v().newParameterRef(RefType.v("java.lang.String"), 0)));

            Local mapLocal = Jimple.v().newLocal("l1", RefType.v("java.util.Map"));
            jbody.getLocals().add(mapLocal);
            jbody.getUnits().add(Jimple.v().newAssignStmt(mapLocal,
                    Jimple.v().newInstanceFieldRef(thisLocal, mapField.getFieldRef())));

            Local retLocal = Jimple.v().newLocal("l2", RefType.v("java.lang.Object"));
            jbody.getLocals().add(retLocal);
            SootMethod mapGetMtd = Scene.v().getMethod("<java.util.Map: java.lang.Object get(java.lang.Object)>");
            jbody.getUnits().add(Jimple.v().newAssignStmt(retLocal,
                    Jimple.v().newInterfaceInvokeExpr(mapLocal, mapGetMtd.makeRef(), argLocal)));

            Local castRetLocal = Jimple.v().newLocal("l3", RefType.v("java.lang.String"));
            jbody.getLocals().addFirst(castRetLocal);
            jbody.getUnits().add(Jimple.v().newAssignStmt(castRetLocal,
                    Jimple.v().newCastExpr(retLocal, RefType.v("java.lang.String"))));

            jbody.getUnits().add(Jimple.v().newReturnStmt(castRetLocal));

            for (final Iterator<Unit> iter = jbody.getUnits().snapshotIterator(); iter.hasNext(); ) {
              Stmt stmt = (Stmt) iter.next();
//              Logger.info(TAG, "+++++++++++ " + stmt);
            }
          }
        } else {
          Body jbody = getMtd.retrieveActiveBody();
          for (final Iterator<Unit> iter = jbody.getUnits().snapshotIterator(); iter.hasNext(); ) {
            Stmt stmt = (Stmt) iter.next();
//            Logger.info(TAG, "=========== " + stmt);
          }
        }
      }

      if (SootUtil.v().isLibraryClass(cls))
        continue;
      if (cls.getName().startsWith("edu.osu.cse.presto.android.gator.instrument")) {
        // TODO: insert static results
        if (cls.getName().endsWith("runtime.Proxy")) {
          util.instrumentRuntimeProxy(cls);
        }
        continue;
      }
      numCls += 1;
      for (SootMethod mtd : Queues.newConcurrentLinkedQueue(cls.getMethods())) {
        if (!mtd.isConcrete()) continue;
        numMtd += 1;
        Body jbody = mtd.retrieveActiveBody();
        for (final Iterator<Unit> iter = jbody.getUnits().snapshotIterator(); iter.hasNext(); ) {
          numStm += 1;
          Stmt stmt = (Stmt) iter.next();
          if (stmt.containsInvokeExpr()) {
            InvokeExpr expr = stmt.getInvokeExpr();
            SootMethod callee;
            try {
              callee = expr.getMethod();
            } catch (SootMethodRefImpl.ClassResolutionFailedException ignored) {
              Logger.warn(TAG, "Skip analysis of " + expr);
              continue;
            }
            if (callee.getSignature().equals(SootUtil.TRACKER_SEND_SIG)) {
              // replace Tracker.send()
              if (!(expr instanceof InstanceInvokeExpr)) {
                // should not happen
                Logger.err(TAG, "Not InstanceInvokeExpr for " + SootUtil.TRACKER_SEND_SIG);
                continue;
              }
              Logger.info(TAG, "Replace Tracker.send()");
              List<Value> args = expr.getArgs(); // contains the map
              args.add(((InstanceInvokeExpr) expr).getBase()); // add tracker
              Stmt toInsert = Jimple.v().newInvokeStmt(
                      Jimple.v().newStaticInvokeExpr(SootUtil.v().PROXY_HIT_MTD.makeRef(), args));
              jbody.getUnits().insertAfter(toInsert, stmt);
              jbody.getUnits().remove(stmt);
              Logger.info(TAG, "Replace " + stmt + " with " + toInsert);
            } else if (callee.getSignature().equals(SootUtil.TRACKER_ENABLE_AUTO_ACTIVITY_TRACKING_SIG)) {
              // replace Tracker.enableAutoActivityTracking
              if (!(expr instanceof InstanceInvokeExpr)) {
                continue;
              }
              List<Value> args = expr.getArgs(); // contains the boolean
              args.add(((InstanceInvokeExpr) expr).getBase()); // add tracker
              Stmt toInsert = Jimple.v().newInvokeStmt(
                      Jimple.v().newStaticInvokeExpr(SootUtil.v().PROXY_RECORD_AUTO_TRACKING_MTD.makeRef(), args));
              jbody.getUnits().insertAfter(toInsert, stmt);
              jbody.getUnits().remove(stmt);
              Logger.info(TAG, "Replace " + stmt + " with " + toInsert);
            } else if (callee.getSignature().equals(SootUtil.GOOGLE_ANALYTICS_GET_INSTANCE_SIG)) {
              // record GoogleAnalytics.getInstance()
              if (!(stmt instanceof AssignStmt)) {
                // should not happen
                Logger.err(TAG, "Not AssignStmt for " + SootUtil.GOOGLE_ANALYTICS_GET_INSTANCE_SIG);
                continue;
              }
              List<Value> args = expr.getArgs(); // contains the context
              args.add(((AssignStmt) stmt).getLeftOp()); // add instance
              Stmt toInsert = Jimple.v().newInvokeStmt(
                      Jimple.v().newStaticInvokeExpr(SootUtil.v().PROXY_INIT_MTD.makeRef(), args));
              Logger.info(TAG, "Insert " + toInsert + " after " + stmt);
              jbody.getUnits().insertAfter(toInsert, stmt);
            } else if (callee.getSignature().equals(SootUtil.GOOGLE_ANALYTICS_NEW_TRACKER_INT_SIG)) {
              // record GoogleAnalytics.newTracker()
              if (!(stmt instanceof AssignStmt)) {
                // should not happen
                Logger.err(TAG, "Not AssignStmt for " + SootUtil.GOOGLE_ANALYTICS_NEW_TRACKER_INT_SIG);
                continue;
              }
              List<Value> args = expr.getArgs(); // contains the context
              args.add(((AssignStmt) stmt).getLeftOp()); // add instance
              Stmt toInsert = Jimple.v().newInvokeStmt(
                      Jimple.v().newStaticInvokeExpr(SootUtil.v().PROXY_RECORD_TRACKER_INT_MTD.makeRef(), args));
              Logger.info(TAG, "Insert " + toInsert + " after " + stmt);
              jbody.getUnits().insertAfter(toInsert, stmt);

              if (!(expr instanceof InstanceInvokeExpr)) {
                // should not happen
                Logger.err(TAG, "Not InstanceInvokeExpr for " + SootUtil.GOOGLE_ANALYTICS_NEW_TRACKER_INT_SIG);
                continue;
              }

              // replace original tracker
              StringConstant myGAId = StringConstant.v(Configs.myGAId);
              Expr toReplace = Jimple.v().newStaticInvokeExpr(SootUtil.v().PROXY_NEW_TRACKER_STR_MTD.makeRef(),
                      myGAId, ((InstanceInvokeExpr) expr).getBase());
              Logger.info(TAG, "Replace " + ((AssignStmt) stmt).getRightOp() + " with " + toReplace);
              ((AssignStmt) stmt).setRightOp(toReplace);
            } else if (callee.getSignature().equals(SootUtil.GOOGLE_ANALYTICS_NEW_TRACKER_STR_SIG)) {
              // record GoogleAnalytics.newTracker()
              if (!(stmt instanceof AssignStmt)) {
                // should not happen
                Logger.err(TAG, "Not AssignStmt for " + SootUtil.GOOGLE_ANALYTICS_NEW_TRACKER_STR_SIG);
                continue;
              }
              List<Value> args = expr.getArgs(); // contains the original id
              args.add(((AssignStmt) stmt).getLeftOp()); // add instance
              Stmt toInsert = Jimple.v().newInvokeStmt(
                      Jimple.v().newStaticInvokeExpr(SootUtil.v().PROXY_RECORD_TRACKER_STR_MTD.makeRef(), args));
              Logger.info(TAG, "Insert " + toInsert + " after " + stmt);
              jbody.getUnits().insertAfter(toInsert, stmt);

              // replace original tracker
              StringConstant myGAId = StringConstant.v(Configs.myGAId);
              Expr toReplace = Jimple.v().newStaticInvokeExpr(SootUtil.v().PROXY_NEW_TRACKER_STR_MTD.makeRef(),
                      myGAId, ((InstanceInvokeExpr) expr).getBase());
              Logger.info(TAG, "Replace " + ((AssignStmt) stmt).getRightOp() + " with " + toReplace);
              ((AssignStmt) stmt).setRightOp(toReplace);
            }
          }
        }
      }
    }

    Logger.stat("#Classes: " + numCls);
    Logger.stat("#Methods: " + numMtd);
    Logger.stat("#Statements: " + numStm);
  }
}
