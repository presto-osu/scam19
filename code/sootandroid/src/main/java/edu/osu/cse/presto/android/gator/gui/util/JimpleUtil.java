/*
 * JimpleUtil.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */
package edu.osu.cse.presto.android.gator.gui.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import edu.osu.cse.presto.android.gator.Logger;
import edu.osu.cse.presto.android.gator.MethodNames;
import soot.*;
import soot.jimple.*;
import soot.shimple.Shimple;
import soot.tagkit.LineNumberTag;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class JimpleUtil implements MethodNames {
  edu.osu.cse.presto.android.gator.Hierarchy hier;

  private static JimpleUtil instance;

  public static synchronized JimpleUtil v(edu.osu.cse.presto.android.gator.Hierarchy hier) {
    if (instance == null) {
      instance = new JimpleUtil();
      instance.s2m = Maps.newHashMap();
      instance.exprToStmt = Maps.newHashMap();
      instance.s2hash = Maps.newHashMap();
      instance.s2siteString = Maps.newHashMap();
      instance.hier = hier;
    }
    return instance;
  }

  public static synchronized JimpleUtil v() {
    return v(edu.osu.cse.presto.android.gator.Hierarchy.v());
  }

  // /////////////////////////////////////////
  // General Jimple utils
  // Assume "l = ..."
  public Local lhsLocal(Stmt s) {
    return (Local) ((DefinitionStmt) s).getLeftOp();
  }

  // Assume "... = ..."
  public Value lhs(Stmt s) {
    return ((DefinitionStmt) s).getLeftOp();
  }

  public Local receiver(Stmt s) {
    return (Local) ((InstanceInvokeExpr) s.getInvokeExpr()).getBase();
  }

  public Local receiver(InvokeExpr ie) {
    return (Local) ((InstanceInvokeExpr) ie).getBase();
  }

  public Local thisLocal(SootMethod m) {
    IdentityStmt first;
    synchronized (m) {
      try {
        first = (IdentityStmt) m.retrieveActiveBody().getUnits().iterator().next();
      } catch (Exception e) {
        Logger.warn(e.getMessage());
        return null;
      }
    }
    if (!(first.getRightOp() instanceof ThisRef)) {
      throw new RuntimeException();
    }
    return lhsLocal(first);
  }

  // /////////////////////////////////////////
  // General Shimple utils
  public boolean containsPhi(Stmt ds) {
    return Shimple.getPhiExpr(ds) != null;
  }

  /**
   * Returns the local variable corresponding to the n-th parameter in the
   * specified method. The counting starts from 0. For an instance method and
   * n=0, this method is equivalent to thisLocal().
   *
   * @param method the specified method
   * @param index  specifies the position of the parameter
   * @return
   */
  public Local localForNthParameter(SootMethod method, int index) {
    Iterator<Unit> stmts = null;
    synchronized (method) {
      stmts = method.retrieveActiveBody().getUnits().iterator();
    }
    for (int i = 0; i < index; i++) {
      stmts.next();
    }
    Stmt idStmt = (Stmt) stmts.next();
    if (!(idStmt instanceof DefinitionStmt)) {
      System.out.println("--- " + method);
      System.out.println(method.retrieveActiveBody());
    }
    return lhsLocal(idStmt);
  }

  // /////////////////////////////////////////
  // App-specific recording
  public Map<Stmt, SootMethod> s2m;

  // hailong: statement to hash code
  // the hash code is computed against a string starting
  // with a method signature that the statement is in,
  // followed by the line number of the statement in the method,
  // followed by the content of the statement.
  // TODO: is signature and line number enough?
  // TODO: alternative hash code algorithm? ELF?
  public Map<Stmt, Integer> s2hash;
  private Map<Stmt, String> s2siteString;

  public SootMethod lookup(Stmt s) {
    return s2m.get(s);
  }

  public int getHash(Stmt s) {
    return s2hash.get(s);
  }

  public String getSiteString(final Stmt sStmt) {
    return this.s2siteString.get(sStmt);
  }

  public void record(Stmt s, SootMethod m) {
    s2m.put(s, m);
    int lineNumber = getLineNumberInMethod(s, m);
    if (lineNumber == -1)
      Logger.err();
    String tmp = String.format("%s %s %s", m.getSignature(),
            String.valueOf(lineNumber), s.toString());
    s2hash.put(s, tmp.hashCode()); // only positive integers
    this.s2siteString.put(s, String.format("%s %s %s@%s", tmp.hashCode(), lineNumber, s, m.getSignature()));
  }

  public void record(Stmt s, SootMethod m, int lineNumber) {
    s2m.put(s, m);
    if (lineNumber == -1)
      Logger.err();
    String tmp = String.format("%s %s %s", m.getSignature(),
            String.valueOf(lineNumber), s.toString());
    int hash = tmp.hashCode() & 0x7FFFFFFF;
    s2hash.put(s, hash); // only positive integers
    this.s2siteString.put(s, String.format("%s %s %s@%s", hash, lineNumber, s, m.getSignature()));
  }

  public int getLineNumberInMethod(Stmt s, SootMethod m) {
    Body b = m.getActiveBody();
    Iterator<Unit> iter = b.getUnits().iterator();
    int index = -1;
    while (iter.hasNext()) {
      Stmt ts = (Stmt) iter.next();
      index += 1;
      if (ts.equals(s))
        break;
    }
    return index;
  }

  public Map<Expr, Stmt> exprToStmt;

  public Stmt lookup(Expr e) {
    return exprToStmt.get(e);
  }

  public void record(Expr e, Stmt s) {
    exprToStmt.put(e, s);
  }

  public String toString(Expr e) {
    Stmt s = lookup(e);
    SootMethod m = lookup(s);
    return s + " @ " + m;
  }

  public Set<Value> getReturnValues(SootMethod method) {
    Preconditions.checkArgument(method.isConcrete());

    Set<Value> returnValues = Sets.newHashSet();
    Body body = method.retrieveActiveBody();
    Iterator<Unit> stmts = body.getUnits().iterator();
    while (stmts.hasNext()) {
      Stmt d = (Stmt) stmts.next();
      if (!(d instanceof ReturnStmt)) {
        continue;
      }
      Value retval = ((ReturnStmt) d).getOp();
      returnValues.add(retval);
    }
    return returnValues;
  }

  // Analysis-specific
  public boolean interesting(Type t) {
    if (t instanceof ArrayType) {
      return interesting(((ArrayType) t).baseType);
    }
    return t instanceof IntType || t instanceof RefType;
  }

  public int getLineNumber(Unit u) {
    int lineNumber = -1;
    LineNumberTag tag = (LineNumberTag) u.getTag("LineNumberTag");
    if (tag != null) {
      lineNumber = tag.getLineNumber();
    }
    return lineNumber;
  }

  // /////////////////////////////////////////
  // Android-specific
  public Value getLayoutId(Stmt s) {
    InvokeExpr ie = s.getInvokeExpr();
    SootMethod m = ie.getMethod();
    SootClass c = m.getDeclaringClass();
    String sig = m.getSignature();
    String subsig = m.getSubSignature();
    if (subsig.equals(setContentViewSubSig)) {
      if (hier.libActivityClasses.contains(c)
          || hier.applicationActivityClasses.contains(c)) {
        return ie.getArg(0);
      }
    }
    if (sig.equals(layoutInflaterInflate)
        || sig.equals(layoutInflaterInflateBool)) {
      return ie.getArg(0);
    }
    if (sig.equals(viewCtxInflate)) {
      return ie.getArg(1);
    }
    return null;
  }

  /**
   * Returns the set of methods in the specified interface. When the parameter
   * is not an interface (as expected), return an empty set.
   *
   * @param interfaceType the interface specified in SootClass
   * @return set of the methods in the specified interface
   */
  public Set<SootMethod> getMethodsInInterface(SootClass interfaceType) {
    if (interfaceType.isInterface()) {
      return Sets.newHashSet(interfaceType.getMethods());
    } else {
      return Collections.emptySet();
    }
  }

  public void writeAllJimples() {
    File tempDir = Files.createTempDir();
    String absPath = tempDir.getAbsolutePath();
    ExecutorService executor = Executors.newFixedThreadPool(4);
    for (final SootClass cls : Scene.v().getApplicationClasses()) {
      String clsName = cls.getName();
      String fileName = absPath + "/" + clsName + ".jimple";
      final File jimpleFile = new File(fileName);
      executor.submit(new Runnable() {
        @Override
        public void run() {
          PrintWriter out = null;
          try {
            out = new PrintWriter(new FileWriter(jimpleFile));
            for (SootField f : cls.getFields()) {
              out.println("!!! " + f.getSignature());
              out.println();
            }
            for (SootMethod m : cls.getMethods()) {
              out.println("--- " + m.getSignature());
              if (m.isConcrete()) {
                out.println(m.retrieveActiveBody());
              }
              out.println();
            }
            out.flush();
            System.out.print(".");
          } catch (IOException e) {
            e.printStackTrace();
          } finally {
            try {
              if (out != null) {
                out.close();
              }
            } catch (Exception ex) {
              ex.printStackTrace();
            }
          }
        }
      });
    }
    try {
      executor.shutdown();
      executor.awaitTermination(30, TimeUnit.MINUTES);
      System.out.println("\nJimple code saved to " + absPath);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
