/*
 * StatsClient.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.transformation.clients;

import edu.osu.cse.presto.android.gator.Logger;
import edu.osu.cse.presto.android.gator.transformation.Transformation;
import soot.*;

public class StatsClient implements Transformation.Client {
  private static String TAG = StatsClient.class.getSimpleName();

  @Override
  public void run() {
    int numCls = 0;
    int numMtd = 0;
    int numStm = 0;
    for (SootClass cls : Scene.v().getClasses()) {
      if (!cls.isConcrete()) continue;
      numCls += 1;
      for (SootMethod mtd : cls.getMethods()) {
        if (!mtd.isConcrete()) continue;
        numMtd += 1;
        Body body = mtd.retrieveActiveBody();
        for (Unit u : body.getUnits()) {
          numStm += 1;
        }
      }
    }
    Logger.info(TAG, "#Class=" + numCls);
    Logger.info(TAG, "#Method=" + numMtd);
    Logger.info(TAG, "#Stmts=" + numStm);
  }
}
