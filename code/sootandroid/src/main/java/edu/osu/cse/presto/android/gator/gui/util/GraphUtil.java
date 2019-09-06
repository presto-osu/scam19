/*
 * GraphUtil.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */
package edu.osu.cse.presto.android.gator.gui.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NNode;
import edu.osu.cse.presto.android.gator.gui.flowgraph.nodes.NOpNode;
import soot.toolkits.scalar.Pair;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class GraphUtil {
  public static boolean verbose;
  private static GraphUtil instance;

  private GraphUtil() {
  }

  public static synchronized GraphUtil v() {
    if (instance == null) {
      instance = new GraphUtil();
    }
    return instance;
  }

  public Set<NNode> reachableNodes(NNode n) {
    return reachableNodes(n, true);
  }

  public Set<NNode> reachableNodes(NNode n, boolean includeSelf) {
    Set<NNode> res = Sets.newHashSet();
    findReachableNodes(n, res, includeSelf);
    return res;
  }

  public void findReachableNodes(NNode start, Set<NNode> reachableNodes, boolean includeSelf) {
    LinkedList<NNode> worklist = Lists.newLinkedList();
    worklist.add(start);
    if (includeSelf)
      reachableNodes.add(start);
    while (!worklist.isEmpty()) {
      NNode n = worklist.remove();
      for (NNode s : n.getSuccessors()) {
        if (reachableNodes.contains(s)) {
          continue;
        }
        if (!includeSelf && s.equals(start)) {
          continue;
        }
        if (!(s instanceof NOpNode)) {
          worklist.add(s);
        }
        reachableNodes.add(s);
        if (verbose) {
          System.out.println("[findReachableNodes] Edge: " + n + " --> " + s);
        }
      }
    }
  }

  // ///
  public Set<NNode> backwardReachableNodes(NNode n) {
    // p("[BackwardReachable] " + n);
    Set<NNode> res = Sets.newHashSet();
    findBackwardReachableNodes(n, res, true);
    return res;
  }

  public Set<NNode> allBackwardReachableNodes(NNode n) {
    // p("[BackwardReachable] " + n);
    Set<NNode> res = Sets.newHashSet();
    findBackwardReachableNodes(n, res, false);
    return res;
  }

  private void findBackwardReachableNodes(NNode start, Set<NNode> reachableNodes, boolean stopAtOpNode) {
    LinkedList<NNode> worklist = Lists.newLinkedList();
    worklist.add(start);
    reachableNodes.add(start);
    while (!worklist.isEmpty()) {
      NNode n = worklist.remove();
      for (NNode s : n.getPredecessors()) {
        if (reachableNodes.contains(s)) {
          continue;
        }
        if (verbose) {
          System.out.println("[findReachableNodes] Edge: " + n + " --> " + s);
        }
        if (s instanceof NOpNode && stopAtOpNode) {
//          if (!(start instanceof NOpNode)) {
          reachableNodes.add(s);
//          }
        } else {
          worklist.add(s);
          reachableNodes.add(s);
        }
      }
    }
  }

  public Set<NNode> forwardReachableNodes(NNode n) {
    Set<NNode> res = Sets.newHashSet();
    findForwardReachableNodes(n, res);
    return res;
  }

  private void findForwardReachableNodes(NNode start, Set<NNode> reachableNodes) {
    LinkedList<NNode> worklist = Lists.newLinkedList();
    worklist.add(start);
    reachableNodes.add(start);
    while (!worklist.isEmpty()) {
      NNode n = worklist.remove();
      for (NNode s : n.getSuccessors()) {
        if (reachableNodes.contains(s)) {
          continue;
        }
        if (s instanceof NOpNode) {
          reachableNodes.add(s);
        } else {
          worklist.add(s);
          reachableNodes.add(s);
        }
      }
    }
  }

  // ///
  public Set<NNode> descendantNodes(NNode n) {
    Set<NNode> res = Sets.newHashSet();
    findDescendantNodes(n, res);
    return res;
  }

  public void findDescendantNodes(NNode start, Set<NNode> descendantNodes) {
    LinkedList<NNode> worklist = Lists.newLinkedList();
    worklist.add(start);
    descendantNodes.add(start);
    while (!worklist.isEmpty()) {
      NNode n = worklist.remove();
      for (NNode s : n.getChildren()) {
        if (descendantNodes.contains(s)) {
          continue;
        }
        worklist.add(s);
        descendantNodes.add(s);
      }
    }
  }

  // ///
  public Set<NNode> ancestorNodes(NNode n) {
    Set<NNode> res = Sets.newHashSet();
    findAncestorNodes(n, res);
    return res;
  }

  public void findAncestorNodes(NNode start, Set<NNode> ancestorNodes) {
    LinkedList<NNode> worklist = Lists.newLinkedList();
    worklist.add(start);
    ancestorNodes.add(start);
    while (!worklist.isEmpty()) {
      NNode n = worklist.remove();
      for (Iterator<NNode> iter = n.getParents(); iter.hasNext(); ) {
        NNode s = iter.next();
        if (ancestorNodes.contains(s)) {
          continue;
        }
        worklist.add(s);
        ancestorNodes.add(s);
      }
    }
  }

  public void dumpParentChildTree(NNode root) {
    LinkedList<Pair<NNode, String>> stack = Lists.newLinkedList();
    stack.addFirst(new Pair<NNode, String>(root, ""));
    while (!stack.isEmpty()) {
      Pair<NNode, String> p = stack.removeFirst();
      NNode node = p.getO1();
      String indent = p.getO2();
      System.out.println(indent + node);
      String newIndent = indent + "  ";
      for (NNode child : node.getChildren()) {
        stack.addFirst(new Pair<NNode, String>(child, newIndent));
      }
    }
  }

  public Set<List<NNode>> dfs(NNode from, NNode to) {
    Set<List<NNode>> ret = Sets.newHashSet();
    if (from.equals(to)) {
      ret.add(Lists.newArrayList(to));
      return ret;
    }
    for (NNode succ : from.getSuccessors()) {
      Set<List<NNode>> paths = dfs(succ, to);
      for (List<NNode> path : paths) {
        path.add(0, from);
        ret.add(path);
      }
    }
    return ret;
  }
}
