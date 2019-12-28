package com.bmxgates.logger;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.bmxgates.commons.MathUtils;
import com.bmxgates.logger.TrackLocator.Track;
import com.bmxgates.logger.data.Sprint;
import com.bmxgates.logger.data.Sprint.Split;
import com.bmxgates.logger.data.SprintManager;
import com.bmxgates.ui.SwipeListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TrackPracticeActivity extends AbstractSprintActivity implements LocationListener {

	boolean autoStop;

	long[] marks;

	int wheelSize;

	int nextMark = 0;

	Track track;

	TextView sprintCountView;

	ListView sprintView;

	SplitAdapter sprintArrayAdatper;

	Handler handler;

	Runnable autoReadyChecker;

	Button goButton;

	Button connectButton;

	SpeedometerFragment speedometerView;

	public TrackPracticeActivity() {
		handler = new Handler(Looper.getMainLooper());
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_track_practice);

		createSprintManager(SprintManager.Type.TRACK);

		speedometerView = (SpeedometerFragment) getSupportFragmentManager().findFragmentById(R.id.track_speedometer);

		// hide goButton until connection is established
		goButton = (Button) findViewById(R.id.track_go_button);

		// disable button until connected or connection fails
		connectButton = (Button) findViewById(R.id.track_connect_button);
		connectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				connectionLost();
				application.reconnect();
			}
		});

		//decide which button to show initially
		if (application.isConnected()) {
			connectButton.setVisibility(View.GONE);
			goButton.setVisibility(View.VISIBLE);
		} else {
			goButton.setVisibility(View.GONE);
			connectButton.setVisibility(View.VISIBLE);
			connectButton.setEnabled(false);
		}

		sprintCountView = (TextView) findViewById(R.id.track_sprint_count);

		sprintArrayAdatper = new SplitAdapter(this);
		
		sprintView = (ListView) findViewById(R.id.splitsListView);
		sprintView.setAdapter(sprintArrayAdatper);
		sprintView.setOnTouchListener(new SwipeListener(this, new SwipeListener.Callback() {

			@Override
			public boolean swipeLeft() {
				loadSprint(++sprintIndex);
				return true;
			}

			@Override
			public boolean swipeRight() {
				loadSprint(--sprintIndex);
				return true;
			}

		}));

		goButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!sprintManager.isReady()) {
					readySprint();
					enableAutoReady();
				} else {
					stopSprint();
					handler.removeCallbacks(autoReadyChecker);
					autoReadyChecker = null;
				}
			}
		});

		TrackLocator.obtainLocation(this, this);

		//load settings
		autoStop = SettingsActivity.getAutoStop(this);
		marks = SettingsActivity.getSplits(this);
		wheelSize = SettingsActivity.getWheelSize(this);

		//display last sprint
		if (sprintManager.totalSprints() > 0)
			loadSprint(sprintManager.totalSprints()-1);

	}

	protected void renderSprint(Sprint sprint) {
		sprintCountView.setText("Sprint #" + sprintIndex+1);

		displayLocation(TrackLocator.byTrackId(sprint.getTrackId()));

		speedometerView.set(-1.0, sprint.getDistance(), sprint.getTime());
		speedometerView.setMaxSpeed(sprint.getMaxSpeed());

		double bestTime = sprintManager.bestTime();
		speedometerView.setBestTime(bestTime >= sprint.getTime());

		double maxSpeed = sprintManager.bestSpeed();
		speedometerView.setBestMaxSpeed(maxSpeed <= sprint.getMaxSpeed());

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

		Log.i(TrackPracticeActivity.class.getName(), "Sprint mode: READY");

		if (track == null)
			return;

		// reload settings
		autoStop = SettingsActivity.getAutoStop(this);
		marks = SettingsActivity.getSplits(this);
		wheelSize = SettingsActivity.getWheelSize(this);

		//start sprint manager
		sprintIndex = sprintManager.ready(track.trackId);

		//update ui
		displayLocation(track);
		goButton.setText("Waiting....");
		goButton.setBackgroundColor(getResources().getColor(R.color.YELLOW_LIGHT));
		sprintArrayAdatper.clear();
		sprintCountView.setText("Sprint #" + sprintIndex);

		reset();

		nextMark = 0;
	}

	protected void reset() {
		speedometerView.reset();
	}

	protected void stopSprint() {
		Log.i(TrackPracticeActivity.class.getName(), "Sprint mode: STOP");

		goButton.setBackgroundColor(getResources().getColor(R.color.GREEN_LIGHT));
		goButton.setText("Start");
		speedometerView.setSpeed(-1);

		validateCurrentSprint();

		sprintManager.stop();

		loadSprint(sprintManager.totalSprints()-1);
	}

	protected void validateCurrentSprint(){

		if (sprintManager.getDistance() < 18288) {
			sprintManager.setValid(false);
			reset();
			return;
		}

		if (sprintManager.totalSprints() < 6) {
			return;
		}

		List<Long> maxTimes = new ArrayList<Long>();
		int nextMark = 0;
		while (nextMark < marks.length) {
			List<Long> rawMarkTimes = new ArrayList<Long>();
			for (int i = 0 ; i < sprintManager.totalSprints(); i++) {

				Sprint sprint = sprintManager.get(i);
				if (sprint.isValid()) {
					Split split = sprint.calculateApproximateSplit(marks[nextMark]);
					if (split == null)
						break;

					rawMarkTimes.add(split.time);
				}
			}

			//ensure we have enough samples
			if (rawMarkTimes.size() < 6) {
				return;
			}

			long maxTime = MathUtils.boundryThreshold(rawMarkTimes);
			maxTimes.add(maxTime);

			nextMark++;
		}

		if (autoStop) {
			List<Long> rawMarkTimes = new ArrayList<Long>();
			for (int i = 0 ; i < sprintManager.totalSprints(); i++) {

				Sprint sprint = sprintManager.get(i);
				if (sprint.isValid()) {
					Split split = sprint.calculateApproximateSplit(track.autoStop);
					if (split == null)
						break;

					rawMarkTimes.add(split.time);
				}
			}

			//ensure we have enough samples
			if (rawMarkTimes.size() < 6) {
				return;
			}

			long maxTime = MathUtils.boundryThreshold(rawMarkTimes);
			maxTimes.add(maxTime);
		}


		List<Long> splitTimes = new ArrayList<Long>();
		nextMark = 0;
		while (nextMark < marks.length) {
			Split split = sprintManager.calculateApproximateSplit(marks[nextMark]);

			//this sprint is incomplete
			if (split == null) {
				sprintManager.setValid(false);
				return;
			}

			splitTimes.add(split.time);
			nextMark++;
		}

		if (autoStop) {
			Split split = sprintManager.calculateApproximateSplit(track.autoStop);

			//this sprint is incomplete
			if (split == null) {
				sprintManager.setValid(false);
				return;
			}

			splitTimes.add(split.time);
		}

		//calculate boundary threshold on all times and filter
		boolean valid = true;
		for (int i = 0 ; i < splitTimes.size() ; i++) {

			Log.i(TrackPracticeActivity.class.getName(), "Split: " + splitTimes.get(i) + ", Max: " + maxTimes.get(i));
			if (splitTimes.get(i)> maxTimes.get(i))
				valid = false;
		}
		sprintManager.setValid(valid);

		if (!valid) {
			reset();
		}
	}

	/**
	 * Implements of Handler.Classback
	 * 
	 * @param msg
	 * @return
	 */
	protected boolean processSplit(Message msg) {

		if (checkSumError)
			speedometerView.setError();

//		Log.v(TrackPracticeActivity.class.getName(), "Split: " + msg.arg1);

		//ignore first split, since first split is actually starting point
		if (sprintManager.getDistance() == 0) {
			goButton.setBackgroundColor(getResources().getColor(R.color.RED_LIGHT));
			goButton.setText("Stop");

			sprintManager.addSplitTime(0, 1);

			return true;
		}

		int splitTime = msg.arg1;
		sprintManager.addSplitTime(splitTime, wheelSize);


		// auto stop timer is enabled
		if (autoStop && sprintManager.getDistance() >= track.autoStop) {


			Split split = sprintManager.calculateApproximateSplit(track.autoStop);
			Split bestSplit = sprintManager.bestSplit(track.autoStop);
			sprintArrayAdatper.add(split, bestSplit);

			stopSprint();

			return true;
		}

		if (nextMark < marks.length && sprintManager.getDistance() >= marks[nextMark]) {
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
	}

	@Override
	public void onLocationChanged(Location location) {
		Track track = TrackLocator.locateTrack(location);
		updateLocation(track);
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	public void enableAutoReady() {
		autoReadyChecker = createAutoReadyChecker();
		handler.postDelayed(autoReadyChecker, 500);
	}

	Runnable createAutoReadyChecker() {
		return new Runnable() {
			public void run() {
//				Log.v(TrackPracticeActivity.class.getName(), "Checking for auto stopping condition");
				if (System.currentTimeMillis() > (TrackPracticeActivity.this.lastMessageTime + 2000)) {
					if (sprintManager.mode() == SprintManager.Mode.SPRINTING) {
						Log.i(TrackPracticeActivity.class.getName(), "Auto stopping sprint");
						TrackPracticeActivity.this.stopSprint();
					}

					if (sprintManager.mode() == SprintManager.Mode.STOPPED && autoReadyChecker != null) {
						TrackPracticeActivity.this.readySprint();
					}
				}

				enableAutoReady();
			}
		};
	}

	/**
	 * Perform all activity required when connection restored
	 */
	protected void connectionRestored() {
		Log.i(AbstractSprintActivity.class.getName(), "Connection restored");

		goButton.setVisibility(View.VISIBLE);
		connectButton.setVisibility(View.GONE);

	}

	protected void connectionLost() {
		Log.i(AbstractSprintActivity.class.getName(), "Connection lost");

		goButton.setVisibility(View.GONE);
		connectButton.setVisibility(View.VISIBLE);
		connectButton.setText("Connecting...");
		connectButton.setEnabled(false);
	}

	protected void connectionFailed() {
		Log.i(AbstractSprintActivity.class.getName(), "Connection failed");

		goButton.setVisibility(View.GONE);
		connectButton.setVisibility(View.VISIBLE);
		connectButton.setText("Reconnect");
		connectButton.setEnabled(true);

	}


}
