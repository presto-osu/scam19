/*
 * TestMain.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.instrument;

import org.junit.Test;

public class TestMain {

  @Test
  public void test() {
    String apk = "bible.study.fiftyday.challenge.king.james.bible.christian.religion.apk";
    String[] args = new String[] {
            "/home/zhanhail/workspace/ga/apk/" + apk,
            "true",
            "/home/zhanhail/Android/Sdk/platforms",
            "/home/zhanhail/workspace/ga/android-ga/android-ga-instrument/build/classes/java/main",
            "UA-22467386-22",
            "/home/zhanhail/workspace/ga/xml/" + apk + ".xml",
            "true"
    };
    Main.main(args);
  }
}
