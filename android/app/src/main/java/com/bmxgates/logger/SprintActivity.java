package com.bmxgates.logger;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.bmxgates.logger.data.Sprint;
import com.bmxgates.logger.data.Sprint.Split;
import com.bmxgates.logger.data.SprintManager;
import com.bmxgates.ui.SwipeListener;

public class SprintActivity extends AbstractSprintActivity {

	long runup;

	long sprintDistance;

	int wheelSize;

	Button goButton;

	Button connectButton;

	TextView diffTimeView;

	TextView diffSpeedView;

	TextView sprintCountView;

	SprintGraphFragment sprintGraph;

	SpeedometerFragment speedometerView;

	int sprintIndex;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sprint);

		createSprintManager(SprintManager.Type.SPRINT);

		speedometerView = (SpeedometerFragment) getSupportFragmentManager().findFragmentById(R.id.sprint_speedometer);
		speedometerView.show20Time(false);

		sprintGraph = (SprintGraphFragment) getSupportFragmentManager().findFragmentById(R.id.sprint_speed_graph);

		diffTimeView = (TextView) findViewById(R.id.diff_view);
		diffSpeedView = (TextView) findViewById(R.id.diff_spd_view);
		sprintCountView = (TextView) findViewById(R.id.sprint_sprint_count);

		// hide goButton until connection is established
		goButton = (Button) findViewById(R.id.sprint_go_button);

		// disable button until connected or connection fails
		connectButton = (Button) findViewById(R.id.sprint_connect_button);
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
		} else {
			goButton.setVisibility(View.GONE);
			connectButton.setEnabled(false);
		}

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

		//load settings
		runup = SettingsActivity.getRunupDistance(this);
		wheelSize = SettingsActivity.getWheelSize(this);
		sprintDistance = SettingsActivity.getSprintDistance(this);

		//display last sprint
		if (sprintManager.totalSprints() > 0){
			loadSprint(sprintManager.totalSprints() - 1);
		}
	}

	@Override
	protected void readySprint() {
		super.readySprint();

		//load settings
		runup = SettingsActivity.getRunupDistance(this);
		wheelSize = SettingsActivity.getWheelSize(this);
		sprintDistance = SettingsActivity.getSprintDistance(this);

		goButton.setText("Waiting....");
		goButton.setBackgroundColor(getResources().getColor(R.color.YELLOW_LIGHT));

		sprintIndex = sprintManager.ready();
		sprintCountView.setText("Sprint #" + sprintIndex);

		reset();
	}

	protected void reset() {
		diffTimeView.setText("0.00");
		diffSpeedView.setText("00.0");
		sprintCountView.setText("Sprint #" + sprintManager.totalSprints());

		speedometerView.reset();
		speedometerView.setDistance(runup);

		sprintGraph.reset();
	}

	protected void loadSprint(int index) {
		if (sprintManager.totalSprints() == 0)
			return;

		sprintIndex = index;

		if (sprintIndex < 0) {
			sprintIndex = 0;
		}

		// 9 = 9 OR 9 < 10, set sprintIndex to last index (i.e. 8)
		if (sprintManager.totalSprints() <= sprintIndex) {
			sprintIndex = sprintManager.totalSprints() - 1;
		}

		Sprint sprint = sprintManager.get(sprintIndex);
		sprintCountView.setText("Sprint #" + (sprintIndex+1));

		speedometerView.set(-1.0, sprint.getDistance(), sprint.getTime());
		speedometerView.setSpeed(sprint.getAverageSpeed(), false);
		speedometerView.setMaxSpeed(sprint.getMaxSpeed());

		long diffTime = sprint.getTime() - sprintManager.bestTime();
		diffTimeView.setText(Formater.time(diffTime, false));
		speedometerView.setBestTime(diffTime == 0);
		speedometerView.setBestSpeed(diffTime == 0);

		double diffSpeed = sprintManager.bestSpeed() - sprint.getMaxSpeed();
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
	protected boolean processSplit(Message msg) {

		if (checkSumError)
			speedometerView.setError();

		//ignore all splits until runup is exhausted
		if (runup > 0) {
			runup -= wheelSize;
			speedometerView.setDistance(runup);
			return true;
		}

		//update speedometer view
		if (sprintManager.getDistance() == 0) {
			goButton.setBackgroundColor(getResources().getColor(R.color.RED_LIGHT));
			goButton.setText("Stop");

			sprintManager.addSplitTime(0, 1);

			return true;
		}

		int splitTime = msg.arg1;
		Split split = sprintManager.addSplitTime(splitTime, wheelSize);

		//stop once sprint distance is reached and recalculate stop point
		if (sprintManager.getDistance() >= sprintDistance) {
			Split adjustedSplit = sprintManager.calculateApproximateSplit(sprintDistance);

			sprintManager.replaceLast(adjustedSplit);

			stopSprint();

			return true;
		}

		speedometerView.set(sprintManager.getSpeed(), sprintManager.getDistance(), sprintManager.getTime());

		return true;
	}

	protected void stopSprint() {
		Log.i(TrackPracticeActivity.class.getName(), "Sprint mode: STOP");

		goButton.setBackgroundColor(getResources().getColor(R.color.GREEN_LIGHT));
		goButton.setText("Start");

		validateCurrentSprint();

		sprintManager.stop();

		//displays the last sprint, if not validate displays the last valid
		//sprint
		loadSprint(sprintManager.totalSprints()-1);
	}

	private void validateCurrentSprint() {
		if (sprintManager.getDistance() < sprintDistance) {
			sprintManager.setValid(false);
			reset();
			return;
		}
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
