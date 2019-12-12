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
		long[] marks = SettingsActivity.getSplits(this);
		boolean autoStop = SettingsActivity.getAutoStop(this);

		Sprint sprint = sprintManager.get(index);
		int sprintNum = sprintManager.totalSprints() - index;

		speedometerView.set(-1.0, sprint.getDistance(), sprint.getTime());
		speedometerView.setMaxSpeed(sprint.getMaxSpeed());
		sprintCountView.setText("Sprint #" + sprintNum);
		displayLocation(TrackLocator.byTrackId(sprint.getTrackId()));

		sprintArrayAdatper.clear();
		int nextMark = 0;
		while (nextMark < marks.length) {
			Split split = sprint.calculateApproximateSplit(marks[nextMark]);
			Split bestSplit = sprintManager.bestSplit(marks[nextMark]);
			if (split == null)
				break;

			sprintArrayAdatper.add(split, bestSplit);

			nextMark++;
		}

		if (autoStop) {
			Split split = sprint.calculateApproximateSplit(track.autoStop);
			Split bestSplit = sprintManager.bestSplit(track.autoStop);
			if (split != null)
				sprintArrayAdatper.add(split, bestSplit);
		}

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
			
		//ignore first split, since first split is actually starting point
		if (sprintManager.getDistance() == 0) {
			goButton.setBackgroundColor(getResources().getColor(R.color.RED_LIGHT));
			goButton.setText("Stop");
			return true;
		}

		int splitTime = msg.arg1;
		sprintManager.addSplitTime(splitTime, wheelSize);


		// auto stop timer is enabled
		if (autoStop && sprintManager.getDistance() >= track.autoStop) {


			Split Split = sprintManager.calculateApproximateSplit(track.autoStop);
			Split bestSplit = sprintManager.bestSplit(track.autoStop);
			sprintArrayAdatper.add(Split, bestSplit);

			speedometerView.set(-1, Split.distance, Split.time);
			speedometerView.setMaxSpeed(sprintManager.getMaxSpeed());

			stopSprint();

			// record splits at marked distances
		} else if (nextMark < marks.length && sprintManager.getDistance() >= marks[nextMark]) {
			Split split = sprintManager.calculateApproximateSplit(marks[nextMark]);
			Split bestSplit = sprintManager.bestSplit(marks[nextMark]);
			sprintArrayAdatper.add(split, bestSplit);

			nextMark++;
		}

		speedometerView.set(sprintManager.getSpeed(), sprintManager.getDistance(), sprintManager.getTime());

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
