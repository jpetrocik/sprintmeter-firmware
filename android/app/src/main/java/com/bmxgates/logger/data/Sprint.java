package com.bmxgates.logger.data;

import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Sprint {

	int trackId;
	
	long sprintId; 
	
	double maxSpeed;

	long distance;

	long time;

	double speed;

	List<Split> splits = new ArrayList<Split>();

	boolean valid = true;

	protected Sprint(long sprintId, int trackId){
		this.sprintId = sprintId;
		this.trackId = trackId;
	}

	protected Sprint(long sprintId){
		this.sprintId = sprintId;
	}

	public Split addSplitTime(long splitTime, int splitDistance) {
		distance += splitDistance;
		time += splitTime;

		//recalculate only if there is a split
		if (splitTime > 0)
			speed = splitDistance / (double)splitTime;

		Split split = new Split(distance, time, speed);
		addSplit(split);
		
		return split;
	}

	public void addSplit(Split split) {
		splits.add(split);

		if (maxSpeed < split.speed)
			maxSpeed = split.speed;

		distance = split.distance;
		time = split.time;
		speed = split.speed;
	}

	public void setValid(boolean valid) {
		Log.i(Sprint.class.getName(), "Sprint determined to be " + (valid?"valid":"invalid"));
		this.valid = valid;
	}

	public boolean isValid() {
		return valid;
	}

	public void removeSplit(long distance) {
		Iterator<Split> allSplits = splits.iterator();

		while (allSplits.hasNext()){
			Split split = allSplits.next();
			if(split.distance==distance){
				allSplits.remove();
				break;
			}
		}
	}

	/**
	 * Finds the split for a given distance, using linear function aX+b=Y, to
	 * find slope to approximate the requested distance
	 * 
	 * @param distance
	 */
	public Split calculateApproximateSplit(long distance) {
		Split low = null, high = null;

		for (Split s :splits) {
			if (s.distance < distance) {
				low = s;
			}
			if (s.distance >= distance) {
				high = s;
				break;
			}
		}

		if (high == null)
			return null;

		if (high.distance == distance)
			return high;
			
		//r=d/t
		double speed =  (high.distance - low.distance) / (double)(high.time - low.time);
		
		//estimated time
		long  additionalDistance = distance - low.distance;
		long time = low.time +(long)(additionalDistance/speed);

		return new Split(distance, time, speed);
	}

	public List<Split> getSplits() {
		return splits;
	}

	public double getMaxSpeed() {
		return maxSpeed;
	}

	public long getTime() {
		return time;
	}

	public long getDistance() {
		return distance;
	}

	public double getSpeed() {
		return speed;
	}

	public int getTrackId() {
		return trackId;
	}

	public void setTrackId(int trackId) {
		this.trackId = trackId;
	}

	public static class Split {
		public long distance;

		public long time;

		public double speed;

		public Split(long distance, long time, double speed) {
			this.distance = distance;
			this.time = time;
			this.speed = speed;
		}
	}


}
