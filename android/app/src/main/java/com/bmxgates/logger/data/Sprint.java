package com.bmxgates.logger.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Sprint {

	int trackId;
	
	long sprintId; 
	
	double maxSpeed;

	long distance;

	long time;

	List<Split> splits = new ArrayList<Split>();

	protected Sprint(long sprintId, int trackId){
		this.sprintId = sprintId;
		this.trackId = trackId;
	}

	protected Sprint(long sprintId){
		this.sprintId = sprintId;
	}

	public Split addSplit(long distance, long time, double speed) {
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
	public Split split(long distance, int wheel) {
		Split low = null, high = null;

		//done for fast look up, looping is slow
		int index = (int) (distance/wheel);

		high = splits.get(index-1);
		if (high.distance == distance)
			return high;
			
		low = splits.get(index-2);

		//r=d/t
		double speed =  (high.distance - low.distance) / (double)(high.time - low.time);
		
		//t=d/r
		long time = (long) (distance/speed);

		return new Split(distance, time, speed);
	}

	public List<Split> allSplits() {
		return splits;
	}

	public double maxSpeed() {
		return maxSpeed;
	}

	public long time() {
		return time;
	}

	public long distance() {
		return distance;
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

	public int getTrackId() {
		return trackId;
	}

	public void setTrackId(int trackId) {
		this.trackId = trackId;
	}

}
