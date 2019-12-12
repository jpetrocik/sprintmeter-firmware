package com.bmxgates.logger.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import com.bmxgates.logger.data.Sprint.Split;

import java.util.ArrayList;
import java.util.List;
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

			sprint.addSplit(new Split(currentDistance, currentTime, currentSpeed));
		}

	}

	public int start(int trackId){
		currentSprint = new Sprint(System.currentTimeMillis(), trackId);
		sprintHistory.add(0, currentSprint);
		
		return sprintHistory.size();
	}

	public int start(){
		if (type == Type.TRACK)
			throw new IllegalArgumentException("Track practice must have associated track");
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
						
						for (Split split : sprint.getSplits()){
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

	public boolean isReady() {
		return currentSprint != null;
	}

	public Sprint get(int index){
		return sprintHistory.get(index);
	}

	public Split addSplitTime(long splitTime, int splitDistance) {
		if (currentSprint == null){
			Log.i("BMXSprintManager", "No current sprint");
			return null;
		}
		return currentSprint.addSplitTime(splitTime, splitDistance);
	}
	
	public void addSplit(Split split) {
		if (currentSprint == null){
			Log.i("BMXSprintManager", "No current sprint");
			return;
		}
		currentSprint.addSplit(split);
	}

	public long getDistance() {
		if (currentSprint != null)
			return currentSprint.getDistance();
		return 0;
	}

	public double getMaxSpeed() {
		if (currentSprint != null)
			return currentSprint.getMaxSpeed();
		return 0;
	}

	public double getSpeed() {
		if (currentSprint != null)
			return currentSprint.getSpeed();
		return 0;
	}

	public long getTime() {
		if (currentSprint != null)
			return currentSprint.getTime();
		return 0;
	}


	public Split calculateApproximateSplit(long distance){
		if (currentSprint != null)
			return currentSprint.calculateApproximateSplit(distance);

		return null;
	}

	public Split bestSplit(long distance){
		Split best = null;
		for (Sprint sprint : sprintHistory) {
			Split split = sprint.calculateApproximateSplit(distance);
			if (split != null && (best == null || best.time > split.time))
				best = split;
		}

		return best;
	}

	public int totalSprints() {
		return sprintHistory.size();
	}
}
