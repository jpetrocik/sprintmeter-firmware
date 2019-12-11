package com.bmxgates.logger;

import android.location.Location;
import android.os.Bundle;
import android.view.View;

import com.bmxgates.logger.TrackLocator.Track;
import com.bmxgates.logger.data.SprintManager;
import com.bmxgates.logger.data.SprintManager.Type;

public class BoxSprintAcivity  extends TrackPracticeActivity {

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		findViewById(R.id.track_location).setVisibility(View.GONE);
	}

	protected void createSprintManager() {
		sprintManager = new SprintManager(Type.BOX);
		if (application.getDatabase() != null) {
			onDatabaseOpened();
		}

	}

	@Override
	public void onLocationChanged(Location location) {
		Track track = new Track(-1, "Box Sprints", location.getLatitude(), location.getLongitude(), SettingsActivity.getBoxSprintDistance(this));
		updateLocation(track);
	}

}
