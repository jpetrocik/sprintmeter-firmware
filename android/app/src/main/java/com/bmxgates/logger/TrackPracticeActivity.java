package com.bmxgates.logger;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.bmxgates.logger.TrackLocator.Track;
import com.bmxgates.logger.data.Sprint;
import com.bmxgates.logger.data.Sprint.Split;
import com.bmxgates.logger.data.SprintManager;
import com.bmxgates.logger.data.SprintManager.Type;
import com.bmxgates.ui.SwipeListener;

import java.util.Date;

public class TrackPracticeActivity extends AbstractSprintActivity implements LocationListener {

	int wheelSize;

	boolean autoStop;

	long[] marks;

	SpeedometerFragment speedometerView;

	TextView sprintCountView;

	ListView sprintView;

	SplitAdapter sprintArrayAdatper;

	int nextMark = 0;

	Track track;

	int sprintIndex = 0;

	public TrackPracticeActivity() {
		goButtonId = R.id.track_go_button;
		connectButtonId = R.id.track_connect_button;
		layoutId = R.layout.activity_track_practice;
		
		sprintManager = new SprintManager(Type.TRACK);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		speedometerView = (SpeedometerFragment) getSupportFragmentManager().findFragmentById(R.id.track_speedometer);

		sprintCountView = (TextView) findViewById(R.id.track_sprint_count);

		sprintArrayAdatper = new SplitAdapter(this);
		
		sprintView = (ListView) findViewById(R.id.splitsListView);
		sprintView.setAdapter(sprintArrayAdatper);
		sprintView.setOnTouchListener(new SwipeListener(this, new SwipeListener.Callback() {

			@Override
			public boolean swipeLeft() {
				sprintIndex++;
				if (sprintManager.totalSprints() > sprintIndex) {
					loadSprint(sprintIndex);
				} else {
					sprintIndex--;
				}

				return true;
			}

			@Override
			public boolean swipeRight() {
				if (sprintIndex > 0) {
					sprintIndex--;
					loadSprint(sprintIndex);
				}

				return true;
			}

		}));

		goButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!sprintManager.isReady()) {
					readySprint();
				} else {
					stopSprint();
				}
			}
		});

		createSprintManager();
		
		TrackLocator.obtainLocation(this, this);
	}

	protected void createSprintManager() {
		if (application.getDatabase() != null) {
			onDatabaseOpened();
		}

	}

	@Override
	protected void onDatabaseOpened() {
		super.onDatabaseOpened();
		sprintCountView.setText("Sprint #" + (sprintManager.totalSprints() + 1));
	}

	protected void loadSprint(int index) {

		//TODO The wheel size could have changed
		int wheelSize = SettingsActivity.getWheelSize(this);
		long[] marks = SettingsActivity.getSplits(this);

		Sprint sprint = sprintManager.get(index);
		int sprintNum = sprintManager.totalSprints() - index;

		speedometerView.set(-1.0, sprint.distance(), sprint.time());
		speedometerView.setMaxSpeed(sprint.maxSpeed());
		sprintCountView.setText("Sprint #" + sprintNum);
		displayLocation(TrackLocator.byTrackId(sprint.getTrackId()));

		sprintArrayAdatper.clear();
		int nextMark = 0;
		while (nextMark < marks.length) {
			Split adjSplit = sprint.split(marks[nextMark], wheelSize);
			Split bestSplit = sprintManager.bestSplit(marks[nextMark], wheelSize);
			if (adjSplit == null)
				break;

			sprintArrayAdatper.add(adjSplit, bestSplit);

			nextMark++;
		}

//		sprintArrayAdatper.add(sprint.allSplits().get(sprint.allSplits().size() - 1));

	}

	protected void updateLocation(Track track) {

		if (track == null)
			return;

		this.track = track;

		displayLocation(track);
	}

	private void displayLocation(Track track) {
		TextView textView = (TextView) findViewById(R.id.track_location);
		textView.setText(Formater.date(new Date()) + " @ " + track.name);
	}

	@Override
	protected void readySprint() {
		super.readySprint();
		
		// reload settings
		autoStop = SettingsActivity.getAutoStop(this);
		marks = SettingsActivity.getSplits(this);
		wheelSize = SettingsActivity.getWheelSize(this);

		if (track == null)
			return;
		
		//start sprint manager
		int sprint = sprintManager.start(track.trackId);

		//update ui
		displayLocation(track);
		goButton.setText("Waiting....");
		goButton.setBackgroundColor(getResources().getColor(R.color.YELLOW_LIGHT));
		sprintArrayAdatper.clear();
		sprintCountView.setText("Sprint #" + sprint);
		speedometerView.reset();

		sprintIndex = 0;
		nextMark = 0;
	}

	@Override
	protected void stopSprint() {
		goButton.setBackgroundColor(getResources().getColor(R.color.GREEN_LIGHT));
		goButton.setText("Start");

		speedometerView.setSpeed(-1);

		sprintManager.stop();
	}

	/**
	 * Implements of Handler.Classback
	 * 
	 * @param msg
	 * @return
	 */
	protected boolean doHandleMessage(Message msg) {

		if (super.doHandleMessage(msg))
			return true;

		if (checksumError)
			speedometerView.setError(true);
			
		int splitTime = msg.arg1;

		//first split
		if (speedometerView.getDistance() == 0) {
			goButton.setBackgroundColor(getResources().getColor(R.color.RED_LIGHT));
			goButton.setText("Stop");

			speedometerView.setDistance(wheelSize/2);
			return true;

		//all other splits
		} else {
			speedometerView.add(wheelSize, splitTime);
		}
		
		// save split time
		sprintManager.addSplit(speedometerView.getDistance(), speedometerView.getTime(), speedometerView.getSpeed());

		// auto stop timer is enabled
		if (autoStop && speedometerView.getDistance() >= track.autoStop) {

			// remove over distance and update ui
//			if (speedometerView.getDistance() > track.autoStop) {
//				sprintManager.removeSplit(speedometerView.getDistance());
//				sprintManager.addSplit(calculatedSplit);

//			}

			Split Split = sprintManager.split(track.autoStop, wheelSize);
			Split bestSplit = sprintManager.bestSplit(track.autoStop, wheelSize);
			sprintArrayAdatper.add(Split, bestSplit);

			speedometerView.set(-1, Split.distance, Split.time);
			speedometerView.setMaxSpeed(sprintManager.maxSpeed());

			stopSprint();

			return true;
		}

		// record splits at marked distances
		if (nextMark < marks.length && speedometerView.getDistance() >= marks[nextMark]) {
			Split split = sprintManager.split(marks[nextMark], wheelSize);
			Split bestSplit = sprintManager.bestSplit(marks[nextMark], wheelSize);
			sprintArrayAdatper.add(split, bestSplit);

			nextMark++;
		}

		return true;

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onLocationChanged(Location location) {
		Track track = TrackLocator.locateTrack(location);
		updateLocation(track);
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

}
