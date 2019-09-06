/*
 * Transformation.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.transformation;

import com.google.common.collect.Sets;
import edu.osu.cse.presto.android.gator.Configs;
import edu.osu.cse.presto.android.gator.Logger;
import org.atteo.classindex.ClassIndex;
import org.atteo.classindex.IndexSubclasses;

import java.util.Date;
import java.util.Set;

/**
 * Simple Soot transformation, without GUI analysis
 */
public class Transformation {
  final String TAG = Transformation.class.getSimpleName();
  private static Transformation instance;

  public static synchronized Transformation v() {
    if (instance == null) {
      instance = new Transformation();
    }
    return instance;
  }

  public void run() {
    Logger.info(TAG, "Start @ " + new Date());
    long startTime = System.nanoTime();

    executeClientAnalyses();

    long estimatedTime = System.nanoTime() - startTime;
    Logger.info(TAG, "End: " + (estimatedTime * 1.0e-09) + " sec");
  }


  void executeClientAnalyses() {
    Set<Client> clients = readClients();

    for (Client client : clients) {
      String clientName = client.getClass().getSimpleName();

      Logger.info(clientName, "Start @ " + new Date());
      long startTime = System.nanoTime();

      client.run();

      long estimatedTime = System.nanoTime() - startTime;
      Logger.info(clientName, "End: " + (estimatedTime * 1.0e-09) + " sec");
    }
  }

  Set<Client> readClients() {
    Set<Client> clients = Sets.newHashSet();
    for (Class<?> klass : ClassIndex.getSubclasses(Client.class)) {
      String klassName = klass.getSimpleName();
      if (Configs.clients.contains(klassName)) {
        Object newInstance = null;
        try {
          newInstance = klass.newInstance();
        } catch (Exception e) {
          Logger.warn("Cannot create an instance for `" + klassName + "'");
        }
        if (newInstance != null) {
          if (newInstance instanceof Client) {
            clients.add((Client) newInstance);
          } else {
            Logger.warn("`" + klassName + "' does not implement Transformation.Client");
          }
        }
      }
    }
    return clients;
  }


  @IndexSubclasses
  public interface Client {
    void run();
  }
}
