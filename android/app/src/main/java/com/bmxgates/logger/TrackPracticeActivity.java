package com.bmxgates.logger;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.bmxgates.logger.TrackLocator.Track;
import com.bmxgates.logger.data.Sprint;
import com.bmxgates.logger.data.Sprint.Split;
import com.bmxgates.logger.data.SprintManager;
import com.bmxgates.ui.SwipeListener;

import java.util.Date;

public class TrackPracticeActivity extends AbstractSprintActivity<TrackPracticeService> {

	Track track;

	TextView sprintCountView;

	ListView sprintView;

	SplitAdapter sprintArrayAdatper;

	Button goButton;

	Button connectButton;

	SpeedometerFragment speedometerView;

	long[] marks;

	int nextMark = 0;

	boolean autoStop;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_track_practice);

		speedometerView = (SpeedometerFragment) getSupportFragmentManager().findFragmentById(R.id.track_speedometer);

		// hide goButton until connection is established
		goButton = (Button) findViewById(R.id.track_go_button);
		goButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!sprintService.isReady()) {
					newSprint();
					sprintService.enableAutoReady();
				} else {
					sprintService.stopSprint();
					sprintService.disableAutoReady();
				}
			}
		});

		// disable button until connected or connection fails
		connectButton = (Button) findViewById(R.id.track_connect_button);
		connectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				connectionLost();
				application.reconnect();
			}
		});

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


		TrackLocator.obtainLocation(this, new LocationListener() {
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
		});

	}

	@Override
	protected void onResume() {
		super.onResume();

		//decide which button to show initially
		if (application.isConnected()) {
			connectButton.setVisibility(View.GONE);
			goButton.setVisibility(View.VISIBLE);
		} else {
			goButton.setVisibility(View.GONE);
			connectButton.setVisibility(View.VISIBLE);
			connectButton.setEnabled(false);
		}

		//start foreground service
		Intent intent = new Intent(this, SprintService.class);
		bindService(intent, connection, Context.BIND_AUTO_CREATE);

		/**
		 * When activity resumes update the screen according to the current state of the
		 * sprint
		 */
		if (sprintService != null) {
			SprintManager.Mode currentState = sprintService.getSprintManager().mode();

			if (currentState == SprintManager.Mode.SPRINTING) {
				goButton.setBackgroundColor(getResources().getColor(R.color.RED_LIGHT));
				goButton.setText("Stop");

			} else {
				if (currentState == SprintManager.Mode.STOPPED) {
					goButton.setBackgroundColor(getResources().getColor(R.color.GREEN_LIGHT));
					goButton.setText("Start");
				}

				if (currentState == SprintManager.Mode.READY) {
					goButton.setBackgroundColor(getResources().getColor(R.color.YELLOW_LIGHT));
					goButton.setText("Waiting...");
				}

				if (sprintService.getSprintManager().totalSprints() > 0) {
					loadSprint(sprintService.getSprintManager().totalSprints() - 1);
				}
			}
		}


	}

	protected void renderSprint(Sprint sprint) {
		sprintCountView.setText("Sprint #" + sprintIndex+1);

		displayLocation(TrackLocator.byTrackId(sprint.getTrackId()));

		speedometerView.set(-1.0, sprint.getDistance(), sprint.getTime());
		speedometerView.setMaxSpeed(sprint.getMaxSpeed());

		double bestTime = sprintService.getSprintManager().bestTime();
		speedometerView.setBestTime(bestTime >= sprint.getTime());

		double maxSpeed = sprintService.getSprintManager().bestSpeed();
		speedometerView.setBestMaxSpeed(maxSpeed <= sprint.getMaxSpeed());

		sprintArrayAdatper.clear();
		int nextMark = 0;
		while (nextMark < marks.length) {
			Split split = sprint.calculateApproximateSplit(marks[nextMark]);
			Split bestSplit = sprintService.getSprintManager().bestSplit(marks[nextMark]);
			if (split == null)
				break;

			sprintArrayAdatper.add(split, bestSplit);

			nextMark++;
		}

		if (autoStop) {
			Split split = sprint.calculateApproximateSplit(track.autoStop);
			Split bestSplit = sprintService.getSprintManager().bestSplit(track.autoStop);
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
	protected void newSprint() {
		Log.i(TrackPracticeActivity.class.getName(), "Sprint mode: READY");

		if (track == null)
			return;

		marks = SettingsActivity.getSplits(this);
		autoStop = SettingsActivity.getAutoStop(this);

		//start sprint manager
		sprintIndex = sprintService.readySprint(track.trackId);

		//update ui
		displayLocation(track);
		goButton.setText("Waiting....");
		goButton.setBackgroundColor(getResources().getColor(R.color.YELLOW_LIGHT));
		sprintArrayAdatper.clear();
		sprintCountView.setText("Sprint #" + sprintIndex);

		reset();

		nextMark = 0;
	}

	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection connection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className,
									   IBinder service) {
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			LocalBinder<TrackPracticeService> binder = (LocalBinder) service;
			sprintService = binder.getService();

			Log.i(SprintActivity.class.getName(), "Binded to SprintService");
			if (sprintService.getSprintManager().totalSprints() > 0){
				loadSprint(sprintService.getSprintManager().totalSprints() - 1);
			}

		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
		}
	};

	protected void reset() {
		speedometerView.reset();
	}

	@Override
	protected void sprintReady(Intent intent) {
		goButton.setBackgroundColor(getResources().getColor(R.color.YELLOW_LIGHT));
		goButton.setText("Waiting...");
	}

	@Override
	protected void sprintStarted(Intent intent) {
		goButton.setBackgroundColor(getResources().getColor(R.color.RED_LIGHT));
		goButton.setText("Stop");
	}

	@Override
	protected void sprintUpdate(Intent intent) {
		SprintManager sprintManager = sprintService.getSprintManager();

		if (nextMark < marks.length) {
			if (sprintManager.getDistance() >= marks[nextMark]) {
				Split split = sprintManager.calculateApproximateSplit(marks[nextMark]);
				Split bestSplit = sprintManager.bestSplit(marks[nextMark]);
				sprintArrayAdatper.add(split, bestSplit);

				nextMark++;
			}
		}

		speedometerView.set(sprintManager.getSpeed(), sprintManager.getDistance(), sprintService.getSprintManager().getTime());
	}

	@Override
	protected void sprintEnded(Intent intent) {
		Log.i(TrackPracticeActivity.class.getName(), "Sprint mode: STOP");

		Sprint.Split split = sprintService.getSprintManager().calculateApproximateSplit(track.autoStop);
		Sprint.Split bestSplit = sprintService.getSprintManager().bestSplit(track.autoStop);
		sprintArrayAdatper.add(split, bestSplit);

		goButton.setBackgroundColor(getResources().getColor(R.color.GREEN_LIGHT));
		goButton.setText("Start");
		speedometerView.setSpeed(-1);

		loadSprint(sprintService.getSprintManager().totalSprints()-1);
	}

	@Override
	protected void sprintError(Intent intent) {
		speedometerView.setError();
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
