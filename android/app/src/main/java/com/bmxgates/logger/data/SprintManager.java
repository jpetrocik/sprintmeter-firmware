package com.bmxgates.logger.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import com.bmxgates.logger.data.Sprint.Split;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalLong;

public class SprintManager {

	public enum Mode {STOPPED, READY, SPRINTING}

	public enum Type {TRACK, BOX, SPRINT};
	
	public static final String DATABASE_CONNECTED = "SPRINT_MANAGER_READY";

	List<Sprint> sprintHistory = new ArrayList<Sprint>();
	
	Sprint currentSprint;
	
	SQLiteDatabase database;
	
	Type type;

	long sessionWindow = 86400000l * 4l;

	public SprintManager(Type type) {
		this.type = type;
	}
	
	public void setDatabase(SQLiteDatabase database){
		this.database = database;

		if (this.database != null)
			loadHistory();
	}
	
	protected void loadHistory(){
		Cursor results = database.query(SprintDatabaseHelper.TABLE_SPRINT_TIMES, 
				SprintDatabaseHelper.SPRINT_TIMES_COLUMNS, 
				SprintDatabaseHelper.COLUMN_SPRINT_ID + " > ? and " + SprintDatabaseHelper.COLUMN_SPRINT_TYPE + " = ?", 
				new String[] {String.valueOf(System.currentTimeMillis()-sessionWindow), type.toString()},
				null, 
				null, 
				SprintDatabaseHelper.COLUMN_SPRINT_ID + " asc, " + SprintDatabaseHelper.COLUMN_DISTANCE + " asc");
		
		int sprintIdIndex = results.getColumnIndex(SprintDatabaseHelper.COLUMN_SPRINT_ID);
		int timeIndex = results.getColumnIndex(SprintDatabaseHelper.COLUMN_TIME);
		int distanceIndex = results.getColumnIndex(SprintDatabaseHelper.COLUMN_DISTANCE);
		int speedIndex = results.getColumnIndex(SprintDatabaseHelper.COLUMN_SPEED);
		int trackIdIndex = results.getColumnIndex(SprintDatabaseHelper.COLUMN_TRACK_ID);

		
		long currentSprintId = -1;
		long currentTime = -1;
		double currentSpeed = -1;
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
			currentSpeed = results.getDouble(speedIndex);

			sprint.addSplit(new Split(currentDistance, currentTime, currentSpeed));
		}

	}

	public int ready(int trackId){
		currentSprint = new Sprint(System.currentTimeMillis(), trackId);
		sprintHistory.add( currentSprint);

		return sprintHistory.size();
	}

	public int ready(){
		if (type == Type.TRACK)
			throw new IllegalArgumentException("Track practice must have associated track");
		currentSprint = new Sprint(System.currentTimeMillis());
		sprintHistory.add( currentSprint);
		
		return sprintHistory.size();
	}

	public void stop(){
		if (currentSprint.valid) {
			if (database != null) {
				new AsyncTask<Sprint, Void, Void>() {

					@Override
					protected Void doInBackground(Sprint... allSprints) {

						//actually is always only a single sprint
						for (Sprint sprint : allSprints) {
							long sprintId = System.currentTimeMillis();

							for (Split split : sprint.getSplits()) {
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
		} else {
			sprintHistory.remove(currentSprint);
		}
		currentSprint = null;
	}

	public boolean isReady() {
		return currentSprint != null;
	}

	public Mode mode() {
		if (currentSprint == null) {
			return Mode.STOPPED;
		} else if ( currentSprint.getDistance() == 0) {
			return Mode.READY;
		} else if ( currentSprint.getDistance() > 0) {
			return Mode.SPRINTING;
		} else {
			Log.w(SprintManager.class.getName(), "Unknown sprint condition");
			return Mode.SPRINTING;
		}
	}
	public Sprint get(int index){
		return sprintHistory.get(index);
	}

	public Split addSplitTime(long splitTime, int splitDistance) {
		if (currentSprint == null){
			Log.i(SprintManager.class.getName(), "No current sprint");
			return null;
		}
		return currentSprint.addSplitTime(splitTime, splitDistance);
	}
	
	public void addSplit(Split split) {
		if (currentSprint == null){
			Log.i(SprintManager.class.getName(), "No current sprint");
			return;
		}
		currentSprint.addSplit(split);
	}

	public void removeSplit(long distance) {
		currentSprint.removeSplit(distance);
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

	public void setValid(boolean valid) {
		if (currentSprint != null)
			currentSprint.setValid(valid);
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

	public long bestTime() {
		OptionalLong bestTime = sprintHistory.stream().mapToLong(s -> s.getTime()).min();
		if (bestTime.isPresent())
			return bestTime.getAsLong();

		return 0L;
	}

	public double bestSpeed() {
		OptionalDouble maxSpeed = sprintHistory.stream().mapToDouble(s -> s.getMaxSpeed()).max();
		if (maxSpeed.isPresent())
			return maxSpeed.getAsDouble();

		return 0.0;
	}



	public int totalSprints() {
		return sprintHistory.size();
	}
}
