/*
 * ApkUtil.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.instrument.util;

import edu.osu.cse.presto.android.gator.Logger;
import edu.osu.cse.presto.android.gator.instrument.xml.MyAxmlWriter;
import org.apache.commons.io.IOUtils;
import pxb.android.axml.AxmlReader;
import pxb.android.axml.AxmlVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static soot.toDex.DexPrinter.SIGNATURE_FILE_PATTERN;

public class ApkUtil {
  private final String TAG = ApkUtil.class.getSimpleName();

  private static ApkUtil instance;

  public static synchronized ApkUtil v() {
    if (instance == null) {
      instance = new ApkUtil();
    }
    return instance;
  }

  public void repack(Path originalApkPath, Path newApkPath) {
    // modify manifest
    InputStream manifestIS = null;
    ZipFile archive = null;
    try {
      try {
        archive = new ZipFile(originalApkPath.toFile());
        for (Enumeration<? extends ZipEntry> entries = archive.entries(); entries.hasMoreElements(); ) {
          ZipEntry entry = entries.nextElement();
          String entryName = entry.getName();
          // We are dealing with the Android manifest
          if (entryName.equals("AndroidManifest.xml")) {
            manifestIS = archive.getInputStream(entry);
            break;
          }
        }
      } catch (Exception e) {
        throw new RuntimeException("Error when looking for manifest in apk: " + e);
      }

      if (manifestIS == null) {
        throw new RuntimeException("Cannot find AndroidManifest.xml in " + originalApkPath);
      }

      MyAxmlWriter axmlWriter = new MyAxmlWriter();
      try {
        AxmlReader axmlReader = new AxmlReader(IOUtils.toByteArray(manifestIS));
        manifestIS.close();
        axmlReader.accept(new AxmlVisitor(axmlWriter));
//        axmlReader.accept(new DumpAdapter(axmlWriter));
      } catch (Exception e) {
        Logger.err(TAG, e.getMessage());
        e.printStackTrace();
      }

      try {
        final ZipOutputStream zos = new ZipOutputStream(
                Files.newOutputStream(newApkPath,
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE));
        copyAllButManifestAndSigFiles(archive, zos);
        zos.putNextEntry(new ZipEntry("AndroidManifest.xml"));
        zos.write(axmlWriter.toByteArray());
        zos.closeEntry();
        zos.close();
      } catch (Exception e) {
        throw new RuntimeException("Error when writing manifest: " + e);
      }
    } finally {
      if (archive != null) {
        try {
          archive.close();
        } catch (IOException e) {
          throw new RuntimeException("Error when repacking apk: " + e);
        }
      }
    }
  }

  private void copyAllButManifestAndSigFiles(ZipFile source, ZipOutputStream destination) throws IOException {
    Enumeration<? extends ZipEntry> sourceEntries = source.entries();
    while (sourceEntries.hasMoreElements()) {
      ZipEntry sourceEntry = sourceEntries.nextElement();
      String sourceEntryName = sourceEntry.getName();
      if (sourceEntryName.equals("AndroidManifest.xml") || isSignatureFile(sourceEntryName)) {
        continue;
      }
      // separate ZipEntry avoids compression problems due to encodings
      ZipEntry destinationEntry = new ZipEntry(sourceEntryName);
      // use the same compression method as the original (certain files
      // are stored, not compressed)
      destinationEntry.setMethod(sourceEntry.getMethod());
      // copy other necessary fields for STORE method
      destinationEntry.setSize(sourceEntry.getSize());
      destinationEntry.setCrc(sourceEntry.getCrc());
      // finally craft new entry
      destination.putNextEntry(destinationEntry);
      InputStream zipEntryInput = source.getInputStream(sourceEntry);
      byte[] buffer = new byte[2048];
      int bytesRead = zipEntryInput.read(buffer);
      while (bytesRead > 0) {
        destination.write(buffer, 0, bytesRead);
        bytesRead = zipEntryInput.read(buffer);
      }
      destination.closeEntry();
      zipEntryInput.close();
    }
  }

  private boolean isSignatureFile(String fileName) {
    return SIGNATURE_FILE_PATTERN.matcher(fileName).matches();
  }
}
