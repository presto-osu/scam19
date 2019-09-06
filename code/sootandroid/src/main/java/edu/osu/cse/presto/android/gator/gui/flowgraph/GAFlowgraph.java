/*
 * GAFlowgraph.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.gui.flowgraph;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.osu.cse.presto.android.gator.Configs;
import edu.osu.cse.presto.android.gator.Hierarchy;
import edu.osu.cse.presto.android.gator.Logger;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.*;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.analytics.*;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.analytics.op.*;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.wtg.NClassConstantNode;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.wtg.NGetClassOpNode;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.wtg.NNullNode;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.wtg.NPhiNode;
import edu.osu.cse.presto.android.gator.gui.util.GAUtil;
import edu.osu.cse.presto.android.gator.gui.util.GraphUtil;
import edu.osu.cse.presto.android.gator.gui.util.Util;
import soot.*;
import soot.jimple.*;
import soot.shimple.PhiExpr;
import soot.shimple.Shimple;
import soot.toolkits.scalar.Pair;
import soot.toolkits.scalar.ValueUnitPair;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class GAFlowgraph extends Flowgraph {
  String TAG = GAFlowgraph.class.getSimpleName();

  // mapping: container local -> stmt reading container element
  private final Map<Local, Set<Stmt>> varsAtContainerRead = Maps.newHashMap();
  // mapping: container local -> stmt writing container element
  private final Map<Local, Set<Stmt>> varsAtContainerWrite = Maps.newHashMap();

  // hailong:
  public Set<Integer> allXmlIds;
  public Map<Integer, NXmlIdNode> allNXmlIdNodes = Maps.newHashMap();

  // hailong: google analytics
  public Set<NGoogleAnalyticsScreenViewBuilderNode> allNGoogleAnalyticsScreenViewBuilderNodes;
  public Set<NGoogleAnalyticsExceptionBuilderNode> allNGoogleAnalyticsExceptionBuilderNodes;
  public Set<NGoogleAnalyticsTimingBuilderNode> allNGoogleAnalyticsTimingBuilderNodes;
  public Set<NGoogleAnalyticsSocialBuilderNode> allNGoogleAnalyticsSocialBuilderNodes;
  public Set<NGoogleAnalyticsEventBuilderNode> allNGoogleAnalyticsEventBuilderNodes;

  public Map<SootClass, NClassConstantNode> allNClassConstantNodes = Maps.newHashMap();

  public Map<Long, NLongConstantNode> allNLongConstantNodes = Maps.newHashMap();

  // utility class
  GAUtil gaUtil = GAUtil.v();

  public GAFlowgraph(Hierarchy hier, Set<Integer> allLayoutIds, Set<Integer> allMenuIds, Set<Integer> allWidgetIds, Set<Integer> allStringIds, Set<Integer> allDrawableIds, Set<Integer> allXmlIds) {
    super(hier, allLayoutIds, allMenuIds, allWidgetIds, allStringIds, allDrawableIds);
    this.allXmlIds = allXmlIds;
    allNGoogleAnalyticsScreenViewBuilderNodes = Sets.newHashSet();
    allNGoogleAnalyticsExceptionBuilderNodes = Sets.newHashSet();
    allNGoogleAnalyticsTimingBuilderNodes = Sets.newHashSet();
    allNGoogleAnalyticsSocialBuilderNodes = Sets.newHashSet();
    allNGoogleAnalyticsEventBuilderNodes = Sets.newHashSet();
  }

  @Override
  public void build() {
    buildIdNodes();
    processFrameworkManagedCallbacks();
    processApplicationClasses();

    // Additional manipulation (a.k.a, post-processing)
    buildFlowThroughContainer();

    // Resolve one-level array-refs. We may want to refine this if later we
    // find it necessary
    resolveArrayRefs();

    if (Configs.gaEnabled)
      return;

    // Deal with recorded dialog and its builder calls
    // WARNING: the order of the following two calls cannot be changed!!!
    processAllRecordedDialogCalls();

    checkAndPatchRootlessActivities();
    checkAndPatchRootlessDialogs();

    // For each ListActivity, model its onListItemClick
    patchListActivity();

    // Deal with list views and list adapters
    processRecordedListViewCalls();

    // TabHost, TabSpec...
    processTabHostRelatedCalls();

    processFlowFromSetListenerToEventHandlers();
  }

  @Override
  void processActivityCallbacks(SootClass c) {
    if (c.isAbstract()) {
      return;
    }
    // Connect activity node to <this> of callback methods
    Set<SootMethod> callbacks = hier.frameworkManaged.get(c);
    for (SootMethod callbackPrototype : callbacks) {
      String subsig = callbackPrototype.getSubSignature();
      SootClass matched = hier.matchForVirtualDispatch(subsig, c);
      if (matched == null) {
        System.out.println("[WARNING] " + subsig + " does not exist for " + c);
        continue;
      }
      if (!matched.isApplicationClass()) {
        continue;
      }
      SootMethod callback = matched.getMethod(subsig);
      Local thisLocal = jimpleUtil.thisLocal(callback);
      NActivityNode actNode = activityNode(c);
      actNode.addEdgeTo(varNode(thisLocal), null);
    }
  }

  @Override
  public void buildIdNodes() {
    super.buildIdNodes();
    for (Integer i : allXmlIds) {
      xmlIdNode(i);
    }
  }

  private NXmlIdNode xmlIdNode(Integer xmlId) {
    Preconditions.checkNotNull(xmlId);
    NXmlIdNode xmlIdNode = allNXmlIdNodes.get(xmlId);
    if (xmlIdNode == null) {
      xmlIdNode = new NXmlIdNode(xmlId);
      allNXmlIdNodes.put(xmlId, xmlIdNode);
      allNNodes.add(xmlIdNode);
    }
    return xmlIdNode;
  }

  @Override
  protected void processApplicationClasses() {
    long totalLines = 0;
    long totalClz = 0, totalMtd = 0;
    // Now process each "ordinary" statements
    for (SootClass c : hier.appClasses) {
      if (gaUtil.isLibraryClass(c)) {
        continue;
      }
      if (Util.v().isIgnoredClass(c)) {
        continue;
      }
      Logger.verb(TAG, "Class processed: " + c.getName());
      totalClz += 1;
      ImmutableList methods = ImmutableList.copyOf(c.getMethods());
      for (Iterator<SootMethod> iter = methods.iterator(); iter.hasNext(); ) {
        //        if (number != c.getMethodCount()) {
        //          Logger.warn("hailong", "-- class: " + c.getName() + ", #methods: " + c.getMethods());
        //        }
        currentMethod = iter.next();
        if (!currentMethod.isConcrete()) {
          continue;
        }
        totalMtd += 1;
        Body b = currentMethod.retrieveActiveBody();
        Iterator<Unit> stmts = b.getUnits().snapshotIterator();
        int lineNo = -1;
        while (stmts.hasNext()) {
          currentStmt = (Stmt) stmts.next();
          totalLines += 1;
          jimpleUtil.record(currentStmt, currentMethod, ++lineNo); // remember the method
          if (currentStmt instanceof ReturnVoidStmt) {
            continue;
          }
          if (currentStmt instanceof ThrowStmt) {
            continue;
          }
          if (currentStmt instanceof GotoStmt) {
            continue;
          }
          if (currentStmt instanceof BreakpointStmt) {
            continue;
          }
          if (currentStmt instanceof NopStmt) {
            continue;
          }
          if (currentStmt instanceof RetStmt) {
            continue;
          }
          if (currentStmt instanceof IfStmt) {
            continue;
          }
          if (currentStmt instanceof TableSwitchStmt) {
            continue;
          }
          if (currentStmt instanceof LookupSwitchStmt) {
            continue;
          }
          if (currentStmt instanceof MonitorStmt) {
            continue;
          }

//          if (currentStmt.containsInvokeExpr() &&
//                  currentStmt.toString().contains("GAUtil: void track(")) {
//          if (currentMethod.getSignature().contains(
//                  "edu.osu.cse.presto.ga.GAUtil2: void track")) {
//            Logger.info("............", currentStmt.toString());
//          }

          if (processStmt(currentStmt)) {
            continue;
          }

          // Some "special" handling of calls
          if (currentStmt.containsInvokeExpr()) {
            InvokeExpr ie = currentStmt.getInvokeExpr();
            SootMethod stm = ie.getMethod(); // static target

            //            // Model Android framework calls
            //            NOpNode opNode = createOpNode(currentStmt);
            //            if (opNode != null && opNode != NOpNode.NullNode) {
            //              allNNodes.add(opNode);
            //              continue;
            //            }
            //            // It is an operation node, but with missing parameters. So, there
            //            // is no point continue matching other cases.
            //            if (opNode == NOpNode.NullNode) {
            //              continue;
            //            }
            // Other interesting calls
//            recordInterestingCalls(currentStmt);

            // flow graph edges at non-virtual calls
            if (ie instanceof StaticInvokeExpr || ie instanceof SpecialInvokeExpr) {
              if (stm.getDeclaringClass().isApplicationClass()) {
                processFlowAtCall(currentStmt, stm);
              }
              continue;
            }

            // flow graph edges at virtual calls
            Local rcv_var = jimpleUtil.receiver(ie);
            Type rcv_t = rcv_var.getType();
            // could be ArrayType, for clone() calls
            if (!(rcv_t instanceof RefType)) {
              continue;
            }

            // handle simple container with subclasses of java.util.List and java.util.Map
            recordReadWriteContainer(currentStmt, currentMethod);

            SootClass stc = ((RefType) rcv_t).getSootClass();
            Set<SootClass> concreteSubtypes = hier.getConcreteSubtypes(stc);
            if (concreteSubtypes != null) {
              for (SootClass sub : concreteSubtypes) {
                SootMethod trg = hier.virtualDispatch(stm, sub);
                if (trg != null && trg.getDeclaringClass().isApplicationClass()) {
                  processFlowAtCall(currentStmt, trg);
                } else if (isInteresting(currentStmt, trg)) {
                  argsFlowToReceiver(currentStmt, trg);
                }
              }
            }
            continue;
          } // the statement was a call

          // assignment (but not with a call; calls are already handled)
          if (!(currentStmt instanceof DefinitionStmt)) {
            continue;
          }
          DefinitionStmt ds = (DefinitionStmt) currentStmt;
          Value lhs = ds.getLeftOp();
          // filter based on types
          if (!jimpleUtil.interesting(lhs.getType()) && !jimpleUtil.containsPhi(currentStmt)
                  && !(lhs.getType() instanceof ByteType)) {
            continue;
          }
          Value rhs = ds.getRightOp();
          if (rhs instanceof CaughtExceptionRef) {
            continue;
          }
          // parameter passing taken care of by processFlowAtCall
          if (rhs instanceof ThisRef || rhs instanceof ParameterRef) {
            continue;
          }
          // remember array refs for later resolution
          if (lhs instanceof ArrayRef) {
            Value x = ((ArrayRef) lhs).getBase();
            if (x instanceof Local) {
              recordVarAtArrayRefWrite((Local) x, currentStmt);
            }
            continue;
          }
          if (rhs instanceof ArrayRef) {
            Value x = ((ArrayRef) rhs).getBase();
            if (x instanceof Local) {
              recordVarAtArrayRefRead((Local) x, currentStmt);
            }
            continue;
          }
          NNode nn_lhs = simpleNode(lhs);
          NNode nn_rhs = simpleNode(rhs);
          // record for debugging purpose
          if (nn_rhs instanceof NAllocNode) {
            jimpleUtil.record(((NAllocNode) nn_rhs).e, currentStmt);
          }
          // create the flow edge
          if (nn_lhs != null && nn_rhs != null) {
            nn_rhs.addEdgeTo(nn_lhs, currentStmt);
            if (nn_rhs instanceof NAllocNode) {
              NAllocNode an = (NAllocNode) nn_rhs;
              // special treatment for "run" methods
              if (an.e instanceof NewExpr) {
                SootClass cl = ((NewExpr) an.e).getBaseType().getSootClass();
                if (cl.declaresMethod("void run()")) {
                  SootMethod rn = cl.getMethod("void run()");
                  Local thisLocal = jimpleUtil.thisLocal(rn);
                  if (thisLocal != null)
                    an.addEdgeTo(varNode(thisLocal), currentStmt);
                }
              }
            }
          }
        } // all statements in the method body
      } // all methods in an application class
    } // all application classes
    Logger.info("STAT.col.B", "# Soot classes processed: " + totalClz);
    Logger.info("STAT.col.C", "# Soot methods processed: " + totalMtd);
    Logger.info("STAT.col.D", "# Jimple statements processed: " + totalLines);
  }

  private boolean isInteresting(Stmt s, SootMethod tgt) {
    if (!(s instanceof DefinitionStmt))
      return false;
    SootMethod mtd = Scene.v().getMethod("<android.content.res.Resources: int getColor(int)>");
    if (tgt != null && tgt.equals(mtd))
      return true;
    mtd = Scene.v().getMethod("<android.content.Context: java.lang.String getString(int)>");
    if (tgt != null && tgt.equals(mtd))
      return true;
    mtd = Scene.v().getMethod("<android.content.Context: java.lang.String getString(int,java.lang.Object[])>");
    if (tgt != null && tgt.equals(mtd))
      return true;
    mtd = Scene.v().getMethod("<android.content.Context: java.lang.CharSequence getText(int)>");
    return tgt != null && tgt.equals(mtd);
  }

  private void argsFlowToReceiver(Stmt s, SootMethod m) {
    Local lhs_at_call = jimpleUtil.lhsLocal(s);
    NNode lhsNode = varNode(lhs_at_call);
    //    Local receiver = jimpleUtil.receiver(s);
    //    NNode receiverNode = varNode(receiver);
    //    receiverNode.addEdgeTo(lhsNode, s);
    InvokeExpr ie = s.getInvokeExpr();
    for (Value arg : ie.getArgs()) {
      NNode argNode = simpleNode(arg);
      argNode.addEdgeTo(lhsNode, s);
    }
  }

  @Override
  public NOpNode createOpNode(Stmt s) {
    if (!s.containsInvokeExpr()) {
      return null;
    }

//    {
//      NOpNode opNode = createWriteContainerOpNode(s);
//      if (opNode != null)
//        return opNode;
//    }
//
//    {
//      NOpNode opNode = createReadContainerOpNode(s);
//      if (opNode != null)
//        return opNode;
//    }

    // object.getClass()
    {
      NOpNode getClass = createGetClassOpNode(s);
      if (getClass != null) {
        return getClass;
      }
    }

    // START Google >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    {
      NOpNode opNode = createGoogleAnalyticsSetScreenNameOpNode(s);
      if (opNode != null)
        return opNode;
    }
    {
      NOpNode opNode = createGoogleAnalyticsTrackerSendOpNode(s);
      if (opNode != null)
        return opNode;
    }
    {
      NOpNode opNode = createGoogleAnalyticsNewTrackerOpNode(s);
      if (opNode != null)
        return opNode;
    }
    {
      NOpNode opNode = createGoogleAnalyticsAutoActivityTrackingOpNode(s);
      if (opNode != null)
        return opNode;
    }
    {
      NOpNode opNode = createGoogleAnalyticsScreenViewBuilderBuildOpNode(s);
      if (opNode != null)
        return opNode;
    }
    {
      NOpNode opNode = createGoogleAnalyticsEventBuilderBuildOpNode(s);
      if (opNode != null)
        return opNode;
    }
    {
      NOpNode opNode = createGoogleAnalyticsExceptionBuilderBuildOpNode(s);
      if (opNode != null)
        return opNode;
    }
    {
      NOpNode opNode = createGoogleAnalyticsSocialBuilderBuildOpNode(s);
      if (opNode != null)
        return opNode;
    }
    {
      NOpNode opNode = createGoogleAnalyticsTimingBuilderBuildOpNode(s);
      if (opNode != null)
        return opNode;
    }
    {
      NOpNode opNode = createGoogleAnalyticsGetInstanceOpNode(s);
      if (opNode != null)
        return opNode;
    }
    // END Google <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
    if (Configs.gaEnabled)
      return null;
    return super.createOpNode(s);
  }

  //////////////////////////////////////////////////////////////////


  // hailong: google analytics
  public NGoogleAnalyticsScreenViewBuilderNode screenViewBuilderNode(Expr expr, Stmt allocStmt, SootMethod allocMethod) {
    NGoogleAnalyticsScreenViewBuilderNode node = new NGoogleAnalyticsScreenViewBuilderNode(expr, allocStmt, allocMethod);
    allNNodes.add(node);
    allNGoogleAnalyticsScreenViewBuilderNodes.add(node);
    return node;
  }

  // hailong: google analytics
  public NGoogleAnalyticsExceptionBuilderNode exceptionBuilderNode(Expr expr, Stmt allocStmt, SootMethod allocMethod) {
    NGoogleAnalyticsExceptionBuilderNode node = new NGoogleAnalyticsExceptionBuilderNode(expr, allocStmt, allocMethod);
    allNNodes.add(node);
    allNGoogleAnalyticsExceptionBuilderNodes.add(node);
    return node;
  }

  // hailong: google analytics
  public NGoogleAnalyticsSocialBuilderNode socialBuilderNode(Expr expr, Stmt allocStmt, SootMethod allocMethod) {
    NGoogleAnalyticsSocialBuilderNode node = new NGoogleAnalyticsSocialBuilderNode(expr, allocStmt, allocMethod);
    allNNodes.add(node);
    allNGoogleAnalyticsSocialBuilderNodes.add(node);
    return node;
  }

  // hailong: google analytics
  public NGoogleAnalyticsEventBuilderNode eventBuilderNode(Expr expr, Stmt allocStmt, SootMethod allocMethod) {
    NGoogleAnalyticsEventBuilderNode node = new NGoogleAnalyticsEventBuilderNode(expr, allocStmt, allocMethod);
    allNNodes.add(node);
    allNGoogleAnalyticsEventBuilderNodes.add(node);
    return node;
  }

  // hailong: google analytics
  public NGoogleAnalyticsTimingBuilderNode timingBuilderNode(Expr expr, Stmt allocStmt, SootMethod allocMethod) {
    NGoogleAnalyticsTimingBuilderNode node = new NGoogleAnalyticsTimingBuilderNode(expr, allocStmt, allocMethod);
    allNNodes.add(node);
    allNGoogleAnalyticsTimingBuilderNodes.add(node);
    return node;
  }
  // --------------------------------------------------------------

  @Override
  public NObjectNode allocNodeOrSpecialObjectNode(Expr e) {
    if (e instanceof NewExpr) {
      SootClass type = ((RefType) e.getType()).getSootClass();
      if (hier.isSubclassOf(type, Scene.v().getSootClass("com.google.android.gms.analytics.HitBuilders$ScreenViewBuilder"))) {
        jimpleUtil.record(e, currentStmt);
        return screenViewBuilderNode(e, currentStmt, currentMethod);
      } else if (hier.isSubclassOf(type, Scene.v().getSootClass("com.google.android.gms.analytics.HitBuilders$ExceptionBuilder"))) {
        jimpleUtil.record(e, currentStmt);
        return exceptionBuilderNode(e, currentStmt, currentMethod);
      } else if (hier.isSubclassOf(type, Scene.v().getSootClass("com.google.android.gms.analytics.HitBuilders$TimingBuilder"))) {
        jimpleUtil.record(e, currentStmt);
        return timingBuilderNode(e, currentStmt, currentMethod);
      } else if (hier.isSubclassOf(type, Scene.v().getSootClass("com.google.android.gms.analytics.HitBuilders$SocialBuilder"))) {
        jimpleUtil.record(e, currentStmt);
        return socialBuilderNode(e, currentStmt, currentMethod);
      } else if (hier.isSubclassOf(type, Scene.v().getSootClass("com.google.android.gms.analytics.HitBuilders$EventBuilder"))) {
        jimpleUtil.record(e, currentStmt);
        return eventBuilderNode(e, currentStmt, currentMethod);
      }
    }
    return super.allocNodeOrSpecialObjectNode(e);
  }

  public NPhiNode phiNode(Value jimpleValue) {
    PhiExpr phiExpr = (PhiExpr) jimpleValue;
    NPhiNode ret = new NPhiNode(phiExpr);
    allNNodes.add(ret);
    for (ValueUnitPair p : phiExpr.getArgs()) {
      Value arg = p.getValue();
      simpleNode(arg).addEdgeTo(ret);
    }
    return ret;
  }

  public NClassConstantNode classConstNode(SootClass cls) {
    NClassConstantNode x = allNClassConstantNodes.get(cls);
    if (x != null) {
      return x;
    }
    x = new NClassConstantNode(cls);
    allNClassConstantNodes.put(cls, x);
    allNNodes.add(x);
    return x;
  }

  public NLongConstantNode longConstantNode(Long value) {
    Preconditions.checkNotNull(value);
    NLongConstantNode intConstantNode = allNLongConstantNodes.get(value);
    if (intConstantNode == null) {
      intConstantNode = new NLongConstantNode();
      intConstantNode.value = value;
      allNLongConstantNodes.put(value, intConstantNode);
      allNNodes.add(intConstantNode);
    }
    return intConstantNode;
  }

  @Override
  public NNode simpleNode(Value jimpleValue) {
    if (jimpleValue instanceof NullConstant) {
      allNNodes.add(NNullNode.nullNode);
      return NNullNode.nullNode;
    }
    if (jimpleValue instanceof IntConstant) {
      Integer integerConstant = ((IntConstant) jimpleValue).value;
      if (allXmlIds.contains(integerConstant)) {
        return xmlIdNode(integerConstant);
      }
    }
    if (jimpleValue instanceof LongConstant) {
      return longConstantNode(((LongConstant) jimpleValue).value);
    }
    if (Shimple.isPhiExpr(jimpleValue)) {
      return phiNode(jimpleValue);
    }
    if (jimpleValue instanceof OrExpr) {
      Value l = ((OrExpr) jimpleValue).getOp1();
      Value r = ((OrExpr) jimpleValue).getOp2();
      if (l instanceof IntConstant && r instanceof IntConstant) {
        return simpleNode(((IntConstant) l).or((IntConstant) r));
      } else {
        //        Logger.err("L=" + l.getClass() + ", R=" + r.getClass());
      }
    } else if (jimpleValue instanceof AndExpr) {
      Value l = ((AndExpr) jimpleValue).getOp1();
      Value r = ((AndExpr) jimpleValue).getOp2();
      if (l instanceof IntConstant && r instanceof IntConstant) {
        return simpleNode(((IntConstant) l).and((IntConstant) r));
      }
    } else if (jimpleValue instanceof ClassConstant) {
      String clsName = null;
      clsName = ((ClassConstant) jimpleValue).value.replace('/', '.');
      if (clsName.charAt(0) == '[') {
        // if it is constant array class, we ignore it
        // e.g., in DaoReflectionHelpers of astrid,
        // we got stmt like return getStaticFieldByReflection(model, Property[].class, "PROPERTIES");
        return null;
      }
      if (clsName.endsWith(";") && clsName.startsWith("L")) {
        clsName = clsName.substring(1, clsName.length() - 1);
        clsName = clsName.trim();
      }
      SootClass sc = Scene.v().getSootClass(clsName);
      return classConstNode(sc);
    }
    return super.simpleNode(jimpleValue);
  }

  // >>>>>>>>>>>>>>>>>>>>>>> copy from flowgraphbuilder >>>>>>>>>>>>>>>>>>>>>>>
  private boolean processStmt(Stmt s) {
    boolean success = createPropagation(s);
    if (success) {
      return true;
    }
    NOpNode opNode = createOpNode(s);
    if (opNode != null) {
      allNNodes.add(opNode);
      return true;
    }
//    modelUntrackedValue(s);
    return false;
  }

  public NNode lookupNode(Value x) {
    if (x instanceof FieldRef) {
      return lookupFieldNode(((FieldRef) x).getField());
    }
    if (x instanceof Local) {
      return lookupVarNode((Local) x);
    }
    if (x instanceof ClassConstant) {
      String clsName = null;
      clsName = ((ClassConstant) x).value.replace('/', '.');
      if (clsName.charAt(0) == '[') {
        // if it is constant array class, we ignore it
        // e.g., in DaoReflectionHelpers of astrid,
        // we got stmt like return getStaticFieldByReflection(model, Property[].class, "PROPERTIES");
        return null;
      }
      SootClass sc = Scene.v().getSootClass(clsName);
      return this.allNClassConstantNodes.get(sc);
    }
    if (x instanceof StringConstant) {
      String value = ((StringConstant) x).value;
      return allNStringConstantNodes.get(value);
    }
    // if it is new Intent or createIntentCall
    if (allNAllocNodes.containsKey(x)) {
      return allNAllocNodes.get(x);
    }
    return null;
  }

  private boolean createPropagation(Stmt s) {
    if (!(s instanceof DefinitionStmt) || !s.containsInvokeExpr()) {
      return false;
    }
    InvokeExpr ie = s.getInvokeExpr();
//    SootMethodRef calleeMtdRef = ie.getMethodRef();
//    calleeMtdRef.resolve()
    SootMethod callee = ie.getMethod();
    Map<String, Integer> fields = null;
    if (gaUtil.isIntentPropagationCall(s)) {
      fields = gaUtil.getIntentPropagationFields(s);
    } else if (gaUtil.isValuePropagationCall(s)) {
      fields = gaUtil.getValuePropagationFields(s);
    } else {
      return false;
    }
    Integer src = fields.get("srcPosition");
    Integer tgt = fields.get("tgtPosition");
    if (src == null) {
      Logger.err(getClass().getSimpleName(),
              "you have not specified the source for propagation call to " + callee);
    } else if (tgt == null) {
      Logger.err(getClass().getSimpleName(),
              "you have not specified the target for propagation call to " + callee);
    }
    NNode srcNode = null, tgtNode = null;
    if (src == 0) {
      Local rcvLocal = jimpleUtil.receiver(ie);
      srcNode = simpleNode(rcvLocal);
    } else if (src > 0) {
      Value arg = ie.getArg(src - 1);
      srcNode = simpleNode(arg);
    } else {
      Logger.err(getClass().getSimpleName(),
              "the source intent idx should not be less than 0 for propagation call to " + callee);
    }
    if (tgt == -1) {
      Local lhsLocal = jimpleUtil.lhsLocal(s);
      tgtNode = simpleNode(lhsLocal);
    } else {
      Logger.err(getClass().getSimpleName(),
              "the tgt intent idx should be -1 for propagation call to " + callee);
    }
    srcNode.addEdgeTo(tgtNode);
    return true;
  }

  private NOpNode createGetClassOpNode(Stmt s) {
    if (!(s instanceof DefinitionStmt)) {
      return null;
    }
    InvokeExpr ie = s.getInvokeExpr();
    if (!gaUtil.isGetClassCall(s)) {
      return null;
    }
    Integer srcPos = gaUtil.getGetClassField(s);
    if (srcPos == null) {
      return null;
    }
    Value srcLocal = null;
    if (srcPos < 0) {
      Logger.err(getClass().getSimpleName(),
              "can not find the src for get class opnode at stmt: " + s);
    } else if (srcPos == 0) {
      srcLocal = jimpleUtil.receiver(ie);
    } else {
      srcLocal = ie.getArg(srcPos - 1);
    }
    NNode srcNode = simpleNode(srcLocal);
    Pair<Stmt, SootMethod> callsite = new Pair<Stmt, SootMethod>(s, jimpleUtil.lookup(s));
    Local lhsLocal = jimpleUtil.lhsLocal(s);
    NNode lhsNode = simpleNode(lhsLocal);
    return new NGetClassOpNode(lhsNode, srcNode, callsite);
  }

  private void buildFlowThroughContainer() {
    GraphUtil graphUtil = GraphUtil.v();
    for (Expr e : allNAllocNodes.keySet()) {
      if (!(e.getType() instanceof RefType)) {
        continue;
      }
      Set<Stmt> writes = Sets.newHashSet();
      Set<Stmt> reads = Sets.newHashSet();
      Set<NNode> reachedContainers = graphUtil.reachableNodes(allNAllocNodes.get(e));
      for (NNode reachedContainer : reachedContainers) {
        if (!(reachedContainer instanceof NVarNode)) {
          continue;
        }
        NVarNode varNode = (NVarNode) reachedContainer;
        if (varsAtContainerRead.containsKey(varNode.l)) {
          reads.addAll(varsAtContainerRead.get(varNode.l));
        }
        if (varsAtContainerWrite.containsKey(varNode.l)) {
          writes.addAll(varsAtContainerWrite.get(varNode.l));
        }
      }
      for (Stmt src : writes) {
        Integer srcPos = gaUtil.getWriteContainerField(src);
        if (srcPos == null) {
          Logger.verb(getClass().getSimpleName(), "the target of write container stmt can not be found: " + src);
          continue;
        }
        NNode sn = null;
        if (srcPos < 0) {
          sn = varNode(jimpleUtil.lhsLocal(src));
        } else {
          sn = simpleNode(src.getInvokeExpr().getArg(srcPos - 1));
        }
        if (sn == null) {
          continue;
        }
        for (Stmt tgt : reads) {
          Integer tgtPos = gaUtil.getReadContainerField(tgt);
          if (tgtPos == null) {
            Logger.verb(getClass().getSimpleName(), "the target of read container stmt can not be found: " + tgt);
            continue;
          }
          NNode tn = null;
          if (tgtPos < 0) {
            if (tgt instanceof DefinitionStmt) {
              tn = varNode(jimpleUtil.lhsLocal(tgt));
            }
          } else {
            tn = simpleNode(tgt.getInvokeExpr().getArg(tgtPos - 1));
          }
          if (tn == null) {
            continue;
          }
          sn.addEdgeTo(tn);
        }
      }
    }
  }

  private void recordReadWriteContainer(Stmt s, SootMethod callingMethod) {
    if (s == null || !s.containsInvokeExpr()) {
      return;
    }
    InvokeExpr ie = s.getInvokeExpr();
    if (!(ie instanceof InstanceInvokeExpr)) {
      return;
    }
    Local rcv = jimpleUtil.receiver(ie);
    if (rcv == null) {
      return;
    }
    Type type = rcv.getType();
    if (!(type instanceof RefType)) {
      return;
    }
    if (gaUtil.isWriteContainerCall(s)) {
      recordVarAtContainerWrite(rcv, s);
    } else if (gaUtil.isReadContainerCall(s)) {
      recordVarAtContainerRead(rcv, s);
    }
  }

  private void recordVarAtContainerRead(Local rcv, Stmt s) {
    Set<Stmt> stmts = varsAtContainerRead.get(rcv);
    if (stmts == null) {
      stmts = Sets.newHashSet();
      varsAtContainerRead.put(rcv, stmts);
    }
    stmts.add(s);
  }

  private void recordVarAtContainerWrite(Local rcv, Stmt s) {
    Set<Stmt> stmts = varsAtContainerWrite.get(rcv);
    if (stmts == null) {
      stmts = Sets.newHashSet();
      varsAtContainerWrite.put(rcv, stmts);
    }
    stmts.add(s);
  }
  // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<


  // hailong: google analytics
  private NOpNode createGoogleAnalyticsSetScreenNameOpNode(Stmt s) {
    if (!gaUtil.isGoogleAnalyticsSetScreenNameCall(s)) {
      return null;
    }
    Integer namePos = gaUtil.getGoogleAnlayticsSetScreenNamePos(s);
    if (namePos == null) {
      Logger.err("name pos is null for " + s);
    }
    InvokeExpr ie = s.getInvokeExpr();
    Value arg = ie.getArg(namePos - 1);
    NNode nameNode = simpleNode(arg);
    NNode trackerNode = simpleNode(jimpleUtil.receiver(s));
    Pair<Stmt, SootMethod> callSite = new Pair<>(s, jimpleUtil.lookup(s));
    return new NGoogleAnalyticsSetScreenNameOpNode(trackerNode, nameNode, callSite);
  }

  // hailong: google analytics
  private NOpNode createGoogleAnalyticsTrackerSendOpNode(Stmt s) {
    if (!gaUtil.isGoogleAnalyticsTrackerSendCall(s)) {
      return null;
    }
    Integer bundlePos = gaUtil.getGoogleAnlayticsTrackerSendMapPos(s);
    if (bundlePos == null) {
      Logger.err("bundle pos is null for " + s);
    }
    InvokeExpr ie = s.getInvokeExpr();
    Value arg = ie.getArg(bundlePos - 1);
    NNode bundleNode = simpleNode(arg);
    NNode trackerNode = simpleNode(jimpleUtil.receiver(s));
    Pair<Stmt, SootMethod> callSite = new Pair<>(s, jimpleUtil.lookup(s));
    return new NGoogleAnalyticsTrackerSendOpNode(trackerNode, bundleNode, callSite);
  }

  // hailong: google analytics
  private NOpNode createGoogleAnalyticsNewTrackerOpNode(Stmt s) {
    if (!gaUtil.isGoogleAnalyticsNewTrackerCall(s)) {
      return null;
    }
    Integer paramPos = gaUtil.getGoogleAnlayticsNewTrackerParamPos(s);
    if (paramPos == null) {
      Logger.err("param pos is null for " + s);
    }
    InvokeExpr ie = s.getInvokeExpr();
    Value arg = ie.getArg(paramPos - 1);
    NNode paramNode = simpleNode(arg);
    NNode analyticsInstanceNode = simpleNode(jimpleUtil.receiver(s));
    NNode trackerNode = simpleNode(jimpleUtil.lhs(s));
    Pair<Stmt, SootMethod> callSite = new Pair<>(s, jimpleUtil.lookup(s));
    return new NGoogleAnalyticsNewTrackerOpNode(trackerNode, analyticsInstanceNode, paramNode, callSite);
  }

  // hailong: google analytics
  private NOpNode createGoogleAnalyticsAutoActivityTrackingOpNode(Stmt s) {
    if (!gaUtil.isGoogleAnalyticsAutoActivityTrackingCall(s)) {
      return null;
    }
    Integer paramPos = gaUtil.getGoogleAnlayticsAutoActivityTrackingParamPos(s);
    if (paramPos == null) {
      Logger.err("param pos is null for " + s);
    }
    InvokeExpr ie = s.getInvokeExpr();

    Value arg = ie.getArg(paramPos - 1);
    NNode paramNode = simpleNode(arg);
    NNode trackerNode = simpleNode(jimpleUtil.receiver(s));
    Pair<Stmt, SootMethod> callSite = new Pair<>(s, jimpleUtil.lookup(s));
    return new NGoogleAnalyticsAutoActivityTrackingOpNode(trackerNode, paramNode, callSite);
  }

  // hailong: google analytics
  private NOpNode createGoogleAnalyticsScreenViewBuilderBuildOpNode(Stmt s) {
    if (!gaUtil.isGoogleAnalyticsScreenViewBuilderBuildCall(s)) {
      return null;
    }
    NNode screenViewBuilderNode = simpleNode(jimpleUtil.receiver(s));
    NNode bundleNode = simpleNode(jimpleUtil.lhs(s));
    Pair<Stmt, SootMethod> callSite = new Pair<>(s, jimpleUtil.lookup(s));
    return new NGoogleAnalyticsScreenViewBuilderBuildOpNode(screenViewBuilderNode, bundleNode, callSite);
  }

  // hailong: google analytics
  private NOpNode createGoogleAnalyticsEventBuilderBuildOpNode(Stmt s) {
    if (!gaUtil.isGoogleAnalyticsEventBuilderBuildCall(s)) {
      return null;
    }
    NNode builderNode = simpleNode(jimpleUtil.receiver(s));
    NNode bundleNode = simpleNode(jimpleUtil.lhs(s));
    Pair<Stmt, SootMethod> callSite = new Pair<>(s, jimpleUtil.lookup(s));
    return new NGoogleAnalyticsEventBuilderBuildOpNode(builderNode, bundleNode, callSite);
  }

  // hailong: google analytics
  private NOpNode createGoogleAnalyticsExceptionBuilderBuildOpNode(Stmt s) {
    if (!gaUtil.isGoogleAnalyticsExceptionBuilderBuildCall(s)) {
      return null;
    }
    NNode builderNode = simpleNode(jimpleUtil.receiver(s));
    NNode bundleNode = simpleNode(jimpleUtil.lhs(s));
    Pair<Stmt, SootMethod> callSite = new Pair<>(s, jimpleUtil.lookup(s));
    return new NGoogleAnalyticExceptionBuilderBuildOpNode(builderNode, bundleNode, callSite);
  }

  // hailong: google analytics
  private NOpNode createGoogleAnalyticsSocialBuilderBuildOpNode(Stmt s) {
    if (!gaUtil.isGoogleAnalyticsSocialBuilderBuildCall(s)) {
      return null;
    }
    NNode builderNode = simpleNode(jimpleUtil.receiver(s));
    NNode bundleNode = simpleNode(jimpleUtil.lhs(s));
    Pair<Stmt, SootMethod> callSite = new Pair<>(s, jimpleUtil.lookup(s));
    return new NGoogleAnalyticsSocialBuilderBuildOpNode(builderNode, bundleNode, callSite);
  }

  // hailong: google analytics
  private NOpNode createGoogleAnalyticsTimingBuilderBuildOpNode(Stmt s) {
    if (!gaUtil.isGoogleAnalyticsTimingBuilderBuildCall(s)) {
      return null;
    }
    NNode builderNode = simpleNode(jimpleUtil.receiver(s));
    NNode bundleNode = simpleNode(jimpleUtil.lhs(s));
    Pair<Stmt, SootMethod> callSite = new Pair<>(s, jimpleUtil.lookup(s));
    return new NGoogleAnalyticsTimingBuilderBuildOpNode(builderNode, bundleNode, callSite);
  }

  // hailong: google analytics
  private NOpNode createGoogleAnalyticsGetInstanceOpNode(Stmt s) {
    if (!gaUtil.isGoogleAnalyticsGetInstanceCall(s)) {
      return null;
    }
    Integer paramPos = gaUtil.getGoogleAnlayticsGetInstanceContextPos(s);
    if (paramPos == null) {
      Logger.err("param pos is null for " + s);
    }
    InvokeExpr ie = s.getInvokeExpr();
    Value arg = ie.getArg(paramPos - 1);
    NNode paramNode = simpleNode(arg);
    NNode lhsNode = simpleNode(jimpleUtil.lhs(s));
    Pair<Stmt, SootMethod> callSite = new Pair<>(s, jimpleUtil.lookup(s));
    return new NGoogleAnalyticsGetInstanceOpNode(paramNode, lhsNode, callSite);
  }


  /***********************
   * debug purpose
   ***********************/
  private void writeNodes(BufferedWriter writer, Set<NNode> insterestingNode) throws IOException {
    for (NNode reach : insterestingNode) {
      Integer label = reach.id;
      String tag = reach.toString();
      writer.write("\n n" + label + " [label=\"");
      writer.write(tag.replace('"', '\'') + "\"];");
    }
  }

  private void writeNode(BufferedWriter writer, NNode reach) throws IOException {
    Integer label = reach.id;
    String tag = reach.toString();
    writer.write("\n n" + label + " [label=\"");
    writer.write(tag.replace('"', '\'') + "\"];");
  }

  private void writeSucc(BufferedWriter writer, NNode root, Set<Pair<NNode, NNode>> edges) throws IOException {
    for (NNode succ : root.getSuccessors()) {
      Pair<NNode, NNode> e = new Pair<>(root, succ);
      if (edges.contains(e))
        continue;
      writer.write("\n n" + root.id + " -> n" + succ.id + ";");
      edges.add(e);
      if (succ instanceof NOpNode)
        continue;
      writeSucc(writer, succ, edges);
    }
  }

  private void writePred(BufferedWriter writer, NNode root, Set<Pair<NNode, NNode>> edges) throws IOException {
    for (NNode pred : root.getPredecessors()) {
      Pair<NNode, NNode> e = new Pair<>(pred, root);
      if (edges.contains(e))
        continue;
      writer.write("\n n" + pred.id + " -> n" + root.id + ";");
      edges.add(e);
//      if (pred instanceof NOpNode)
//        continue;
      writePred(writer, pred, edges);
    }
  }

  public void dump(String dotFile) {
    try {
      FileWriter output = new FileWriter(dotFile);
      BufferedWriter writer = new BufferedWriter(output);

      writer.write("digraph G {");
      writer.write("\n rankdir=LR;");
      writer.write("\n node[shape=box];");
      // draw window nodes
      Set<NNode> interestingNodes = Sets.newHashSet();
//      for (NNode node : NOpNode.getNodes()) {
//        writeNodes(writer, GraphUtil.v().backwardReachableNodes(node));
//        interestingNodes.add(node);
//      }
      for (NNode node : NOpNode.getNodes(NGoogleAnalyticsSetScreenNameOpNode.class)) {
        writeNodes(writer, GraphUtil.v().allBackwardReachableNodes(node));
        interestingNodes.add(node);
      }
//      for (NNode node : NOpNode.getNodes(NWriteContainerOpNode.class)) {
//        writeNodes(writer, GraphUtil.v().allBackwardReachableNodes(node));
//        interestingNodes.add(node);
//      }
//      for (NNode node : NOpNode.getNodes(NReadContainerOpNode.class)) {
//        writeNodes(writer, GraphUtil.v().allBackwardReachableNodes(node));
//        interestingNodes.add(node);
//      }
//      for (NNode node : NOpNode.getNodes(NGoogleAnalyticsNewTrackerOpNode.class)) {
//        writeNodes(writer, GraphUtil.v().backwardReachableNodes(node));
//        interestingNodes.add(node);
//      }
//      for (NNode node : NOpNode.getNodes(NGoogleAnalyticsAutoActivityTrackingOpNode.class)) {
//        writeNodes(writer, GraphUtil.v().backwardReachableNodes(node));
//        interestingNodes.add(node);
//      }
//      for (NNode node : NOpNode.getNodes(NGoogleAnalyticsScreenViewBuilderBuildOpNode.class)) {
//        writeNodes(writer, GraphUtil.v().backwardReachableNodes(node));
//        interestingNodes.add(node);
//      }
//      for (NNode node : NOpNode.getNodes(NGetClassOpNode.class)) {
//        writeNodes(writer, GraphUtil.v().backwardReachableNodes(node));
//        interestingNodes.add(node);
//      }
//      for (NNode node : allNActivityNodes.values()) {
//        writeNode(writer, node);
//        interestingNodes.add(node);
//      }
      Set<Pair<NNode, NNode>> edges = Sets.newHashSet();
      for (NNode node : interestingNodes) {
        writePred(writer, node, edges);
      }
      Logger.verb(TAG, "---- total edges: " + edges.size());
      // end of .dot file
      writer.write("\n}");
      writer.close();
      Logger.verb(TAG, "flow graph dump to file: " + dotFile);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
