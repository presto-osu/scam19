/*
 * DatabaseController.java - part of the GATOR project
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
import android.util.Log;
import com.google.android.gms.analytics.Tracker;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Closeable;
import java.util.*;

public class DatabaseController implements Closeable {
  private final String TAG = "presto.ga.rt." + DatabaseController.class.getSimpleName();
  private final SQLiteOpenHelperWrapper sqLiteOpenHelperWrapper;
  static final String DB_NAME = "presto_ga_stat.db";
  private final Context mAppContext;
  private Proxy proxy;

  DatabaseController(Proxy proxy, Context context) {
    this.sqLiteOpenHelperWrapper = new SQLiteOpenHelperWrapper(this, context, DB_NAME);
    this.mAppContext = context;
    this.proxy = proxy;
  }

  Context getContext() {
    return mAppContext;
  }

  @Override
  public void close() {
    try {
      this.sqLiteOpenHelperWrapper.close();
    } catch (SQLiteException sQLiteException) {
      Log.e(TAG, "Sql error closing database: " + sQLiteException);
    } catch (IllegalStateException illegalStateException) {
      Log.e(TAG, "Error closing database: " + illegalStateException);
    }
  }

  private final SQLiteDatabase getWritableDatabase() {
    try {
      return this.sqLiteOpenHelperWrapper.getWritableDatabase();
    } catch (SQLiteException sQLiteException) {
      Log.e(TAG, "Error opening database: " + sQLiteException);
      throw sQLiteException;
    }
  }

  public final void beginTransaction() {
    this.getWritableDatabase().beginTransaction();
  }

  public final void setTransactionSuccessful() {
    this.getWritableDatabase().setTransactionSuccessful();
  }

  public final void endTransaction() {
    this.getWritableDatabase().endTransaction();
  }

  public long numberOfStoredHits() {
    Dispatcher.checkIfInWorkerThread();
    return this.executeRawQuery("SELECT COUNT(*) FROM hits", null);
  }

  public long numberOfStoredRandomizedHits() {
    Dispatcher.checkIfInWorkerThread();
    return this.executeRawQuery("SELECT COUNT(*) FROM random_hits", null);
  }

  private long executeRawQuery(String query, String[] args) {
    SQLiteDatabase sQLiteDatabase = this.getWritableDatabase();
    try (Cursor cursor = sQLiteDatabase.rawQuery(query, args)) {
      if (cursor.moveToFirst()) {
        return cursor.getLong(0);
      }
      return 0;
    } catch (SQLiteException sQLiteException) {
      Log.e(TAG, "Database error " + query + " " + sQLiteException);
      throw sQLiteException;
    }
  }

  public void incrementRandomViews(String screenName, double epsilon, int user) {
    SQLiteDatabase sQLiteDatabase = this.getWritableDatabase();
    String histogramText = null, totalViewsText = null;
    try (Cursor cursor = sQLiteDatabase.query("stats", new String[]{"random_histogram", "total_random_views"},
            null, null, null, null, null)) {
      if (cursor.moveToFirst()) {
        histogramText = cursor.getString(0);
        totalViewsText = cursor.getString(1);
      }
      String epsilonName = "e-" + epsilon;
      String userName = "u-" + user;
      try {
        JSONObject jsonObject = new JSONObject(totalViewsText);
        JSONObject user2views = jsonObject.has(userName) ? jsonObject.getJSONObject(userName) : new JSONObject();
        long totalViews = user2views.has(epsilonName) ? user2views.getLong(epsilonName) : 0;
        user2views.put(epsilonName, totalViews + 1);
        jsonObject.put(userName, user2views);

        ContentValues contentValues = new ContentValues();
        contentValues.put("total_random_views", jsonObject.toString());

        jsonObject = new JSONObject(histogramText);
        JSONObject user2hist = jsonObject.has(userName) ? jsonObject.getJSONObject(userName) : new JSONObject();
        JSONObject histogram = user2hist.has(epsilonName) ? user2hist.getJSONObject(epsilonName) : new JSONObject();
        long num = histogram.has(screenName) ? histogram.getLong(screenName) : 0;
        histogram.put(screenName, num + 1);
        user2hist.put(epsilonName, histogram);
        jsonObject.put(userName, user2hist);

        contentValues.put("random_histogram", jsonObject.toString());
        int i = sQLiteDatabase.update("stats", contentValues, null, null);
        if (i < 1)
          Log.e(TAG, "Failed to update, return " + i);
      } catch (JSONException e) {
        Log.e(TAG, "Failed to read from JSON: " + e);
      }
    } catch (SQLiteException sQLiteException) {
      Log.e(TAG, "Error updating " + sQLiteException);
      throw sQLiteException;
    }
  }

  public void incrementActualViews(String screenName) {
    executeRawQuery("UPDATE stats SET total_actual_views = total_actual_views + 1", null);
    SQLiteDatabase sQLiteDatabase = this.getWritableDatabase();
    String text = null;
    try (Cursor cursor = sQLiteDatabase.query("stats", new String[]{"actual_histogram"},
            null, null, null, null, null)) {
      if (cursor.moveToFirst()) {
        text = cursor.getString(0);
      }
      try {
        JSONObject jsonObject = new JSONObject(text);
        long num = jsonObject.has(screenName) ? jsonObject.getLong(screenName) : 0;
        jsonObject.put(screenName, num + 1);

        ContentValues contentValues = new ContentValues();
        contentValues.put("actual_histogram", jsonObject.toString());
        int i = sQLiteDatabase.update("stats", contentValues, null, null);
        if (i < 1)
          Log.e(TAG, "Failed to update histogram, return " + i);
      } catch (JSONException e) {
        Log.e(TAG, "Failed to read histogram from JSON: " + e);
      }
    } catch (SQLiteException sQLiteException) {
      Log.e(TAG, "Error updating histogram " + sQLiteException);
      throw sQLiteException;
    }
  }

  public long actualViewsSoFar() {
    return this.executeRawQuery("SELECT total_actual_views FROM stats", null);
  }

  public final List<String> readScreenNames() {
    SQLiteDatabase sQLiteDatabase = this.getWritableDatabase();
    try (Cursor cursor = sQLiteDatabase.query("screen_names", new String[]{"name"},
            null, null, null, null, null)) {
      List<String> names = new ArrayList<>();
      if (cursor.moveToFirst()) {
        do {
          names.add(cursor.getString(0));
        } while (cursor.moveToNext());
      }
      return names;
    } catch (SQLiteException sQLiteException) {
      Log.e(TAG, "Error loading screen names from the database " + sQLiteException);
      throw sQLiteException;
    }
  }

  public final long storeNewScreenName(String name) {
    SQLiteDatabase sQLiteDatabase = this.getWritableDatabase();
    ContentValues contentValues = new ContentValues();
    contentValues.put("name", name);
    try {
      long id = sQLiteDatabase.insert("screen_names", null, contentValues);
      if (id == -1L) {
        Log.e(TAG, "Failed to insert new screen name (got -1)");
        return -1;
      }
      Log.d(TAG, "New screen name saved to database: " + name);
      return id;
    } catch (SQLiteException sQLiteException) {
      Log.e(TAG, "Error storing a hit " + sQLiteException);
      throw sQLiteException;
    }
  }

  public final void deleteHit(long id) {
    deleteHit(id, "hits");
  }

  public final void deleteRandomizedHit(long id) {
    deleteHit(id, "random_hits");
  }

  private void deleteHit(long id, String tbl) {
    Dispatcher.checkIfInWorkerThread();
    List<Long> arrayList = new ArrayList<>(1);
    arrayList.add(id);
    Log.i(TAG, "Deleting hit in " + tbl + ", id=" + id);
    this.deleteHits(arrayList, tbl);
  }

  private void deleteHits(List<Long> idList, String tbl) {
    Dispatcher.checkIfInWorkerThread();
    if (idList.isEmpty()) {
      return;
    }
    StringBuilder stringBuilder = new StringBuilder("hit_id");
    stringBuilder.append(" in (");
    Long l;
    for (int i = 0; i < idList.size(); ++i) {
      l = idList.get(i);
      if (l == null || l == 0L) {
        Log.w(TAG, "Invalid hit in " + tbl + " id=" + l);
      }
      if (i > 0) {
        stringBuilder.append(",");
      }
      stringBuilder.append(l);
    }
    stringBuilder.append(")");
    String string = stringBuilder.toString();
    try {
      int n = this.getWritableDatabase().delete(tbl, string, null);
      if (n != idList.size()) {
        Log.d(TAG, "Deleted fewer hits then expected from " + tbl + " " + idList.size() + " " + n + " " + string);
      }
    } catch (SQLiteException sQLiteException) {
      Log.e(TAG, "Error deleting hits in " + tbl + " " + sQLiteException);
      throw sQLiteException;
    }
  }

  private List<HitInfo> readHits(long limit, String tbl) {
    Dispatcher.checkIfInWorkerThread();
    SQLiteDatabase sQLiteDatabase = this.getWritableDatabase();
    try (Cursor cursor = sQLiteDatabase.query(tbl, new String[]{"hit_id", "hit_map"},
            null, null, null, null,
            String.format("%s ASC", "hit_id"), Long.toString(limit))) {
      List<HitInfo> hits = new ArrayList<>();
      if (cursor.moveToFirst()) {
        do {
          long id = cursor.getLong(0);
          String map = cursor.getString(1);
          hits.add(decodeHit(id, map));
        } while (cursor.moveToNext());
      }
      Log.d(TAG, "Hits read from " + tbl + ": " + hits.size());
      return hits;
    } catch (SQLiteException sQLiteException) {
      Log.e(TAG, "Error loading hits from " + tbl + " " + sQLiteException);
      throw sQLiteException;
    }
  }

  public final List<HitInfo> readHits(long limit) {
    return readHits(limit, "hits");
  }

  public final List<HitInfo> readRandomizedHits(long limit) {
    return readHits(limit, "random_hits");
  }

  private long storeHit(Tracker tracker, Map<String, String> map, String tbl) {
    Dispatcher.checkIfInWorkerThread();
    SQLiteDatabase sQLiteDatabase = this.getWritableDatabase();
    ContentValues contentValues = new ContentValues();
    String hitInfo = encodeHit(tracker, map);
    contentValues.put("hit_map", hitInfo);
    try {
      long id = sQLiteDatabase.insert(tbl, null, contentValues);
      if (id == -1L) {
        Log.e(TAG, "Failed to insert a hit (got -1) to " + tbl);
        return -1;
      }
      Log.d(TAG, "Hit saved to " + tbl + ". db-id=" + id + ", hit=" + hitInfo);
      return id;
    } catch (SQLiteException sQLiteException) {
      Log.e(TAG, "Error storing a hit to " + tbl + " " + sQLiteException);
      throw sQLiteException;
    }

  }

  public final long storeHit(Tracker tracker, Map<String, String> map) {
    return storeHit(tracker, map, "hits");
  }

  public final long storeRandomizedHit(Tracker tracker, Map<String, String> map) {
    return storeHit(tracker, map, "random_hits");
  }

  private String encodeHit(Tracker tracker, Map<String, String> map) {
    JSONObject jsonObject = new JSONObject();
    try {
      for (String key : map.keySet()) {
        String value = map.get(key);
        jsonObject.put(key, value);
      }
      jsonObject.put("tracking_id", proxy.getTrackingId(tracker));
    } catch (JSONException e) {
      Log.e(TAG, "Failed to convert hit to JSON: " + e);
    }
    return jsonObject.toString();
  }

  private HitInfo decodeHit(long id, String s) {
    Tracker tracker = null;
    Map<String, String> map = new HashMap<>();
    try {
      JSONObject jsonObject = new JSONObject(s);
      for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
        String k = it.next();
        if (k.equals("tracking_id")) {
          tracker = proxy.getTracker(jsonObject.getString(k));
          if (tracker == null) {
            Log.e(TAG, "Tracker is set to null");
          }
        } else {
          map.put(k, jsonObject.getString(k));
        }
      }
    } catch (JSONException e) {
      Log.e(TAG, "Failed to read hit from JSON: " + e);
    }
    return new HitInfo(id, tracker, map);
  }
}
