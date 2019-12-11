package com.bmxgates.logger;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;
import android.util.Log;

public class TrackLocator {

	private static final long FIX_EXPIRATION = 300000;
	
	public static final Track UNKNOWN_TRACK = new Track(-1, "N/A", -1, -1, 76200);
	
	public static final List<Track> TRACKS = new ArrayList<Track>(5);
	
	static {
		TRACKS.add(new Track(1, "Whittier Narrows BMX", 34.047482, -118.069431, 76200));
		TRACKS.add(new Track(2, "Bellflower BMX", 33.893653, -118.138924, 76200));
		TRACKS.add(new Track(3, "Orange Y BMX", 33.7848255,-117.8295102, 76200));
		TRACKS.add(new Track(4, "Long Beach BMX", 33.804886,-118.127932, 54864));
		TRACKS.add(new Track(5, "Black Mountain BMX", 33.705374, -112.056824, 76200));
	};
	
	public static Track byTrackId(int trackId){
		for(Track track : TRACKS){
			if (track.trackId==trackId)
				return track;
		}
		
		return null;
	}

	public static void obtainLocation(Context context, LocationListener locationListener){
		
		LocationManager locationManager = (LocationManager) 
				context.getSystemService(Context.LOCATION_SERVICE);

		if (locationManager == null)
			return;
		
		
		Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (location != null) {
			long fixTime = location.getTime();
			Log.i("BMXTrackLocator", "Using last know location fixed at " + (System.currentTimeMillis() - fixTime));
			if (System.currentTimeMillis() - fixTime < FIX_EXPIRATION){
				Log.i("BMXTrackLocator", "Using last know location");
				locationListener.onLocationChanged(location);
				return;
			}
		}
		
		// if GPS Enabled get lat/long using GPS Services
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        	Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
        	locationManager.requestSingleUpdate(criteria, locationListener, Looper.myLooper());
			Log.i("BMXTrackLocator", "Scheduled single GPS update");
        }
        
        return;
	}

	public static Track locateTrack(Location location) {
		
		if (location == null)
			return null;
		
		float[] results = new float[1];
		for (int i=0;i<TRACKS.size(); i++){
			Track track = TRACKS.get(i);
			Location.distanceBetween(location.getLatitude(), location.getLongitude(), track.lat, track.log, results);

			Log.d("BMXLogger","Distances: " + results[0]);
			if (results[0]<500)
				return track;
		}
		
		return UNKNOWN_TRACK;
	}
	
	public static class Track {

		int trackId; 
		
		String name;
		
		double lat;
		
		double log;
		
		long autoStop;
		
		public Track(int trackId, String name, double lat, double log, long autoStop) {
			this.trackId=trackId;
			this.name=name;
			this.lat=lat;
			this.log=log;
			this.autoStop=autoStop;
		}
		
		public String toString() {
			return name;
		}
	}
}
