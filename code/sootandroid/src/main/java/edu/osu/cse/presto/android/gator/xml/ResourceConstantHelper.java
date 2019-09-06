/*
 * ResourceConstantHelper.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.xml;

import edu.osu.cse.presto.android.gator.Configs;
import edu.osu.cse.presto.android.gator.Logger;

import java.io.*;

public class ResourceConstantHelper {
  private static void loadConstFromStream(NameValueFunction nvf, String tag, InputStream inputStream) {
    if (tag == null || nvf == null || inputStream == null)
      return;

    BufferedReader br = null;
    try {
      br = new BufferedReader(new InputStreamReader(inputStream));
      String curLine;
      boolean isTagMatched = false;
      while ((curLine = br.readLine()) != null){
        curLine = curLine.trim();
        if (curLine.equals("}") && isTagMatched){
          break;
        }
        String[] prefixs = curLine.split(" ");
        if (prefixs.length < 4)
          continue;
        if (prefixs[3].equals("class") && prefixs[4].equals(tag)) {
          //Class declearation
          isTagMatched = true;
          Logger.verb("RCONST", "Tag : "+ tag + " matched " + curLine);
          continue;
        }

        if (isTagMatched && curLine.equals("{"))
          continue;

        if (isTagMatched) {
          if (prefixs.length < 6)
            Logger.verb("RCONST", curLine + "Length lt 6");
          if (!prefixs[5].equals("="))
            continue;
          String name = prefixs[4];
          if (prefixs[6].endsWith(";"))
            prefixs[6] = prefixs[6].substring(0, prefixs.length - 1);
          Integer val = Integer.parseInt(prefixs[6]);
          nvf.feed(name, val);
        }
      }
    } catch (IOException e){
      e.printStackTrace();
    } finally {
      if (br != null)
        try {
          br.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
    }
  }

  public static void loadConstFromResFile(NameValueFunction nvf, String tag, String fileName) {
    if (tag == null || nvf == null || fileName == null)
      return;
    Logger.verb("XMLParser", "Reading from " + fileName);
    loadConstFromStream(nvf, tag, ResourceConstantHelper.class.getClassLoader().getResourceAsStream(fileName));
  }
}
