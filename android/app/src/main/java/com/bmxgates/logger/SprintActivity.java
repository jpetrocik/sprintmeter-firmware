package com.bmxgates.logger;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.bmxgates.logger.data.Sprint;
import com.bmxgates.logger.data.Sprint.Split;
import com.bmxgates.logger.data.SprintManager;
import com.bmxgates.ui.SwipeListener;

public class SprintActivity extends AbstractSprintActivity<SprintService> {

	private static final int SPRINT_TRACK_ID = 453342;

	Button goButton;

	Button connectButton;

	TextView diffTimeView;

	TextView diffSpeedView;

	TextView sprintCountView;

	SprintGraphFragment sprintGraph;

	SpeedometerFragment speedometerView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sprint);

		speedometerView = (SpeedometerFragment) getSupportFragmentManager().findFragmentById(R.id.sprint_speedometer);
		speedometerView.show20Time(false);

		sprintGraph = (SprintGraphFragment) getSupportFragmentManager().findFragmentById(R.id.sprint_speed_graph);

		diffTimeView = (TextView) findViewById(R.id.diff_view);
		diffSpeedView = (TextView) findViewById(R.id.diff_spd_view);
		sprintCountView = (TextView) findViewById(R.id.sprint_sprint_count);

		goButton = (Button) findViewById(R.id.sprint_go_button);
		goButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!sprintService.isReady()) {
					newSprint();
				} else {
 					sprintService.stopSprint();
				}
			}
		});

		connectButton = (Button) findViewById(R.id.sprint_connect_button);
		connectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				connectionLost();
				application.reconnect();
			}
		});

		sprintGraph.setOnTouchListener(new SwipeListener(this, new SwipeListener.Callback() {

			@Override
			public boolean swipeLeft() {
				loadSprint(--sprintIndex);
				return true;
			}

			@Override
			public boolean swipeRight() {
				loadSprint(++sprintIndex);
				return true;
			}

		}));

		startForgroundService();

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

		//bind to foreground service
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

	private void startForgroundService() {
		Intent serviceIntent = new Intent(this, SprintService.class);
		ContextCompat.startForegroundService(this, serviceIntent);
	}

	@Override
	protected void newSprint() {
		sprintIndex = sprintService.readySprint(SPRINT_TRACK_ID);
	}

	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection connection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			LocalBinder<SprintService> binder = (LocalBinder) service;
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

	protected void renderSprint(Sprint sprint) {
		sprintCountView.setText("Sprint #" + (sprintIndex+1));

		speedometerView.set(-1.0, sprint.getDistance(), sprint.getTime());
		speedometerView.setSpeed(sprint.getAverageSpeed(), false);
		speedometerView.setMaxSpeed(sprint.getMaxSpeed());

		long diffTime = sprint.getTime() - sprintService.getSprintManager().bestTime();
		diffTimeView.setText(Formater.time(diffTime, false));
		speedometerView.setBestTime(diffTime == 0);
		speedometerView.setBestSpeed(diffTime == 0);

		double diffSpeed = sprintService.getSprintManager().bestSpeed() - sprint.getMaxSpeed();
		diffSpeedView.setText(Formater.speed(diffSpeed));
		speedometerView.setBestMaxSpeed(diffSpeed == 0);

		sprintGraph.reset();
		boolean skipFirst = true;
		for (Split s : sprint.getSplits()) {
			if (skipFirst) {
				skipFirst = false;
				continue;
			}
			sprintGraph.addSplit(s);
		}
		sprintGraph.renderChart(sprint.getMaxSpeed());
	}


	@Override
	protected void onSprintReady(Intent intent) {
		boolean initial = intent.getBooleanExtra("initial", true);

		if (initial) {
			goButton.setText("Waiting....");
			goButton.setBackgroundColor(getResources().getColor(R.color.YELLOW_LIGHT));
			sprintCountView.setText("Sprint #" + sprintIndex);

			diffTimeView.setText("0.00");
			diffSpeedView.setText("00.0");
			sprintCountView.setText("Sprint #" + sprintService.getSprintManager().totalSprints());

			speedometerView.reset();

			sprintGraph.reset();
		}

		speedometerView.setDistance(intent.getLongExtra("runUp", 0l));
	}

	@Override
	protected void onSprintStarted(Intent intent) {
		goButton.setBackgroundColor(getResources().getColor(R.color.RED_LIGHT));
		goButton.setText("Stop");
	}

	@Override
	protected void onSprintUpdate(Intent intent) {
		speedometerView.set(sprintService.getSprintManager().getSpeed(), sprintService.getSprintManager().getDistance(), sprintService.getSprintManager().getTime());
	}

	protected void onSprintEnded(Intent intent) {
		Log.i(TrackPracticeActivity.class.getName(), "Sprint mode: STOP");

		goButton.setBackgroundColor(getResources().getColor(R.color.GREEN_LIGHT));
		goButton.setText("Start");


		//displays the last sprint, if not validate displays the last valid
		//sprint
		loadSprint(sprintService.getSprintManager().totalSprints()-1);
	}

	@Override
	protected void onSprintError(Intent intent) {
		speedometerView.setError();
	}

	/**
	 * Perform all activity required when connection restored
	 */
	protected void connectionRestored() {
		Log.i(AbstractSprintActivity.class.getName(), "Connection restored");

		// toggle buttons, this saves existing state of goButton
		// so when restored button is in last know state
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
		connectButton.setText("Reconnect");
		connectButton.setEnabled(true);

	}


}
