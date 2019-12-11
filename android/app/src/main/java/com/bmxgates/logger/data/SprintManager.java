package com.bmxgates.logger.data;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import com.bmxgates.logger.data.Sprint.Split;
public class SprintManager {

	public enum Type {TRACK, BOX, SPRINT};
	
	public static final String DATABASE_CONNECTED = "SPRINT_MANAGER_READY";

	List<Sprint> sprintHistory = new ArrayList<Sprint>();
	
	Sprint currentSprint;
	
	SQLiteDatabase database;
	
	Type type;
	
	public SprintManager(Type type) {
		this.type = type;
	}
	
	public void setDatabase(SQLiteDatabase database){
		this.database = database;
		
		loadHistory();
	}
	
	protected void loadHistory(){
		Cursor results = database.query(SprintDatabaseHelper.TABLE_SPRINT_TIMES, 
				SprintDatabaseHelper.SPRINT_TIMES_COLUMNS, 
				SprintDatabaseHelper.COLUMN_SPRINT_ID + " > ? and " + SprintDatabaseHelper.COLUMN_SPRINT_TYPE + " = ?", 
				new String[] {String.valueOf(System.currentTimeMillis()/86400000), type.toString()}, 
				null, 
				null, 
				SprintDatabaseHelper.COLUMN_SPRINT_ID + " desc, " + SprintDatabaseHelper.COLUMN_DISTANCE + " asc");
		
		int sprintIdIndex = results.getColumnIndex(SprintDatabaseHelper.COLUMN_SPRINT_ID);
		int timeIndex = results.getColumnIndex(SprintDatabaseHelper.COLUMN_TIME);
		int distanceIndex = results.getColumnIndex(SprintDatabaseHelper.COLUMN_DISTANCE);
		int speedIndex = results.getColumnIndex(SprintDatabaseHelper.COLUMN_SPEED);
		int trackIdIndex = results.getColumnIndex(SprintDatabaseHelper.COLUMN_TRACK_ID);

		
		long currentSprintId = -1;
		long currentTime = -1;
		int currentSpeed = -1;
		int currentDistance = -1;
		
		Sprint sprint = null;
		while(results.moveToNext()){
			long sprintId = results.getLong(sprintIdIndex);
			
			if (currentSprintId != sprintId){
				int trackId = results.getInt(trackIdIndex);
				sprint = new Sprint(sprintId, trackId);
				sprintHistory.add(sprint);
				currentSprintId=sprintId;
			} 

			currentTime = results.getLong(timeIndex);
			currentDistance = results.getInt(distanceIndex);
			currentSpeed = results.getInt(speedIndex);

			sprint.addSplit(currentDistance, currentTime, currentSpeed);
		}

	}
	
	
	public int start(int trackId){
		currentSprint = new Sprint(System.currentTimeMillis(), trackId);
		sprintHistory.add(0, currentSprint);
		
		return sprintHistory.size();
	}

	public int start(){
		
		if (type == Type.TRACK)
			throw new IllegalArgumentException("Track pratice must have associated track");
		currentSprint = new Sprint(System.currentTimeMillis());
		sprintHistory.add(0, currentSprint);
		
		return sprintHistory.size();
	}

	public void stop(){
		
		if (database != null){
			new AsyncTask<Sprint, Void, Void>() {

				@Override
				protected Void doInBackground(Sprint... allSprints) {
					
					//actually is always only a single sprint
					for (Sprint sprint : allSprints){
						long sprintId  = System.currentTimeMillis();
						
						for (Split split : sprint.allSplits()){
							ContentValues values = new ContentValues();
							values.put(SprintDatabaseHelper.COLUMN_SPRINT_ID, sprintId);
							
							values.put(SprintDatabaseHelper.COLUMN_DISTANCE, split.distance);
							values.put(SprintDatabaseHelper.COLUMN_TIME, split.time);
							values.put(SprintDatabaseHelper.COLUMN_SPEED, split.speed);
							values.put(SprintDatabaseHelper.COLUMN_TRACK_ID, sprint.trackId);
							values.put(SprintDatabaseHelper.COLUMN_SPRINT_TYPE, SprintManager.this.type.toString());
							database.insert(SprintDatabaseHelper.TABLE_SPRINT_TIMES, null, values);
						}
					}
					return null;
				}

			}.execute(currentSprint);
		}
		currentSprint = null;
	}
	
	public Sprint get(int index){
		return sprintHistory.get(index);
	}

	public Split addSplit(long distance, long time, double speed) {
		if (currentSprint == null){
			Log.i("BMXSprintManager", "No current spring");
			return null;
		}
		return currentSprint.addSplit(distance, time, speed);
	}
	
	public void addSplit(Split split) {
		if (currentSprint == null){
			Log.i("BMXSprintManager", "No current spring");
			return;
		}
		currentSprint.addSplit(split);
	}

	public void removeSplit(long distance) {
		if (currentSprint == null){
			Log.i("BMXSprintManager", "No current spring");
			return;
		}
		currentSprint.removeSplit(distance);
	}
	
	
	public Split split(long distance, int wheel){
		if (currentSprint != null)
			return currentSprint.split(distance, wheel);
		
		return null;
	}

	public boolean isReady() {
		return currentSprint != null;
	}

	public double maxSpeed() {
		if (currentSprint != null)
			return currentSprint.maxSpeed();
		
		return 0;
	}

	public int totalSprints() {
		return sprintHistory.size();
	}
}
