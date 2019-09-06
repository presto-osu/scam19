/*
 * SQLiteOpenHelperWrapper.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE
 * in the root directory.
 */

package edu.osu.cse.presto.android.gator.instrument.runtime;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SQLiteOpenHelperWrapper extends SQLiteOpenHelper {
  private final String TAG = "presto.ga.rt." + SQLiteOpenHelperWrapper.class.getSimpleName();
  private DatabaseController dbController;
  private final Timer timer = new Timer();
  private static final String CREATE_HITS_TABLE_SQL = String.format(
          "CREATE TABLE IF NOT EXISTS %s ( " +
                  "'%s' INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                  "'%s' TEXT NOT NULL);", "hits", "hit_id", "hit_map");
  private static final String CREATE_RANDOM_HITS_TABLE_SQL = String.format(
          "CREATE TABLE IF NOT EXISTS %s ( " +
                  "'%s' INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                  "'%s' TEXT NOT NULL);", "random_hits", "hit_id", "hit_map");
  private static final String CREATE_STATS_TABLE_SQL = String.format(
          "CREATE TABLE IF NOT EXISTS %s ( " +
                  "'%s' TEXT NOT NULL," +
                  "'%s' TEXT NOT NULL," +
                  "'%s' INTEGER NOT NULL," +
                  "'%s' TEXT NOT NULL);", "stats", "actual_histogram", "random_histogram", "total_actual_views", "total_random_views");
  private static final String CREATE_SCREEN_NAMES_TABLE_SQL = String.format(
          "CREATE TABLE IF NOT EXISTS %s ( " +
                  "'%s' TEXT NOT NULL);", "screen_names", "name");

  public SQLiteOpenHelperWrapper(DatabaseController dbController, Context context, String name) {
    super(context, name, null, 1);
    this.dbController = dbController;
  }

  @Override
  public final SQLiteDatabase getWritableDatabase() {
    if (!timer.checkTimeLimit(60000)) { // wait for 1 min
      throw new SQLiteException("Database open failed");
    }
    try {
      return super.getWritableDatabase();
    } catch (SQLiteException e) {
      timer.start();
      Log.w(TAG, "Opening the database failed, dropping the table and recreating it " + e);
      this.dbController.getContext().getDatabasePath(DatabaseController.DB_NAME).delete();
      try {
        SQLiteDatabase sQLiteDatabase = super.getWritableDatabase();
        timer.clear();
        return sQLiteDatabase;
      } catch (SQLiteException e2) {
        Log.e(TAG, "Failed to open freshly created database " + e2);
        throw e2;
      }
    }
  }

  private boolean checkIfExistTable(SQLiteDatabase sQLiteDatabase, String tbl) {
    try (Cursor cursor = sQLiteDatabase.query("SQLITE_MASTER", new String[]{"name"}, "name=?", new String[]{tbl}, null, null, null)) {
      return cursor.moveToFirst();
    } catch (SQLiteException sQLiteException) {
      Log.e(TAG, "Error querying for table " + tbl + " " + sQLiteException);
      return false;
    }
  }

  private static Set<String> getColumnNamesOfTable(SQLiteDatabase sQLiteDatabase, String tbl) {
    HashSet<String> hashSet = new HashSet<>();
    String sql = String.format("SELECT * FROM %s LIMIT 0", tbl);
    try (Cursor cursor = sQLiteDatabase.rawQuery(sql, null)) {
      String[] arrstring = cursor.getColumnNames();
      hashSet.addAll(Arrays.asList(arrstring));
    }
    return hashSet;
  }

  @Override
  public void onOpen(SQLiteDatabase sQLiteDatabase) {
    Log.i(TAG, "Opening database: " + sQLiteDatabase.getPath());
    if (Build.VERSION.SDK_INT < 15) {
      try (Cursor cursor = sQLiteDatabase.rawQuery("PRAGMA journal_mode=memory", null)) {
        cursor.moveToFirst();
      }
    }
    if (!this.checkIfExistTable(sQLiteDatabase, "stats")) {
      sQLiteDatabase.execSQL(CREATE_STATS_TABLE_SQL);
      ContentValues cv = new ContentValues();
      cv.put("actual_histogram", "{}");
      cv.put("random_histogram", "{}");
      cv.put("total_actual_views", 0);
      cv.put("total_random_views", "{}");
      sQLiteDatabase.insert("stats", null, cv);
    } else {
      Set<String> columnNames = SQLiteOpenHelperWrapper.getColumnNamesOfTable(sQLiteDatabase, "stats");
      String[] COLUMN_NAMES = new String[]{"actual_histogram", "random_histogram", "total_actual_views", "total_random_views"};
      for (int i = 0; i < 1; ++i) {
        String name = COLUMN_NAMES[i];
        if (columnNames.remove(name)) continue;
        throw new SQLiteException("Database stats is missing required column: " + name);
      }
    }

    if (!this.checkIfExistTable(sQLiteDatabase, "screen_names")) {
      sQLiteDatabase.execSQL(CREATE_SCREEN_NAMES_TABLE_SQL);
    } else {
      Set<String> columnNames = SQLiteOpenHelperWrapper.getColumnNamesOfTable(sQLiteDatabase, "screen_names");
      String[] COLUMN_NAMES = new String[]{"name"};
      for (int i = 0; i < 1; ++i) {
        String name = COLUMN_NAMES[i];
        if (columnNames.remove(name)) continue;
        throw new SQLiteException("Database screen_names is missing required column: " + name);
      }
    }

    if (!this.checkIfExistTable(sQLiteDatabase, "hits")) {
      sQLiteDatabase.execSQL(CREATE_HITS_TABLE_SQL);
    } else {
      Set<String> columnNames = SQLiteOpenHelperWrapper.getColumnNamesOfTable(sQLiteDatabase, "hits");
      String[] COLUMN_NAMES = new String[]{"hit_id", "hit_map"};
      for (int i = 0; i < 2; ++i) {
        String name = COLUMN_NAMES[i];
        if (columnNames.remove(name)) continue;
        throw new SQLiteException("Database hits is missing required column: " + name);
      }
    }

    if (!this.checkIfExistTable(sQLiteDatabase, "random_hits")) {
      sQLiteDatabase.execSQL(CREATE_RANDOM_HITS_TABLE_SQL);
    } else {
      Set<String> columnNames = SQLiteOpenHelperWrapper.getColumnNamesOfTable(sQLiteDatabase, "random_hits");
      String[] COLUMN_NAMES = new String[]{"hit_id", "hit_map"};
      for (int i = 0; i < 2; ++i) {
        String name = COLUMN_NAMES[i];
        if (columnNames.remove(name)) continue;
        throw new SQLiteException("Database random_hits is missing required column: " + name);
      }
    }
  }

  @Override
  public void onCreate(SQLiteDatabase sqLiteDatabase) {
    File file = new File(sqLiteDatabase.getPath());
    file.setReadable(false, false);
    file.setWritable(false, false);
    file.setReadable(true, true);
    file.setWritable(true, true);
  }

  @Override
  public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

  }
}
