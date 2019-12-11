package com.bmxgates.logger.data;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SprintDatabaseHelper extends SQLiteOpenHelper {

  public static final String TABLE_SPRINT_TIMES = "sprint_times";
  public static final String COLUMN_SPRINT_ID = "sprint_id";
  public static final String COLUMN_DISTANCE = "distance";
  public static final String COLUMN_TIME = "time";
  public static final String COLUMN_SPEED = "speed";
  public static final String COLUMN_TRACK_ID = "track_id";
  public static final String COLUMN_SPRINT_TYPE = "sprint_type";
  
  public static final String[] SPRINT_TIMES_COLUMNS = new String[] {COLUMN_SPRINT_ID, COLUMN_DISTANCE, COLUMN_TIME, COLUMN_SPEED, COLUMN_TRACK_ID, COLUMN_SPRINT_TYPE};
  
  private static final String DATABASE_NAME = "bmxsprints.db";
  private static final int DATABASE_VERSION = 4;

  // Database creation sql statement
  private static final String DATABASE_CREATE = "create table " + TABLE_SPRINT_TIMES + "(" + 
		  COLUMN_SPRINT_ID + " integer, " + 
		  COLUMN_DISTANCE + " integer not null, " + 
		  COLUMN_TIME + " integer not null, " +
		  COLUMN_SPEED + " double not null, " + 
		  COLUMN_TRACK_ID + " integer not null, " + 
		  COLUMN_SPRINT_TYPE + " string " + 
		  ")";
  
  public static final String DATABASE_OPENED_ACTION = "database-opened";

  public SprintDatabaseHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase database) {
    database.execSQL(DATABASE_CREATE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    Log.w(SprintDatabaseHelper.class.getName(),
        "Upgrading database from version " + oldVersion + " to "
            + newVersion + ", which will destroy all old data");
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_SPRINT_TIMES);
    onCreate(db);
  }
  
} 