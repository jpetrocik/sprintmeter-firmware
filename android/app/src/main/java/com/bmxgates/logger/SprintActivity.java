package com.bmxgates.logger;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.bmxgates.logger.data.Sprint.Split;
import com.bmxgates.logger.data.SprintManager;

public class SprintActivity extends AbstractSprintActivity {

	long runup;

	long sprintDistance;

	int wheelSize;

	Button goButton;

	Button connectButton;

	SprintGraphFragment sprintGraph;

	SpeedometerFragment speedometerView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sprint);

		createSprintManager(SprintManager.Type.SPRINT);

		speedometerView = (SpeedometerFragment) getSupportFragmentManager().findFragmentById(R.id.sprint_speedometer);
		speedometerView.show20Time(false);
		
		sprintGraph = (SprintGraphFragment) getSupportFragmentManager().findFragmentById(R.id.sprint_speed_graph);

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

	}

	@Override
	protected void readySprint(){
		super.readySprint();

		sprintGraph.reset();
		
		//load settings
		runup = SettingsActivity.getRunupDistance(this);
		wheelSize = SettingsActivity.getWheelSize(this);
		sprintDistance = SettingsActivity.getSprintDistance(this);
		
		goButton.setText("Waiting....");
		goButton.setBackgroundColor(getResources().getColor(R.color.YELLOW_LIGHT));

		sprintManager.ready();
		
		speedometerView.reset();
		speedometerView.setDistance(runup);
	}

	@Override
	protected boolean processSplit(Message msg){

		if (checkSumError)
			speedometerView.setError();

		//ignore all splits until runup is exhausted
		if (runup > 0){
			runup -= wheelSize;
			speedometerView.setDistance(runup);
			return true;
		}

		//update speedometer view
		if (sprintManager.getDistance() == 0) {
			goButton.setBackgroundColor(getResources().getColor(R.color.RED_LIGHT));
			goButton.setText("Stop");

			//the bike has moved an unknown distance, so start the sprint with a half the
			// distance of the wheel movement between splits
			sprintManager.addSplitTime(0, wheelSize/2);

			return true;
		}


		int splitTime = msg.arg1;
		sprintManager.addSplitTime(splitTime, wheelSize);

		speedometerView.set(sprintManager.getSpeed(), sprintManager.getDistance(), sprintManager.getTime());
		sprintGraph.addSplit(sprintManager.getDistance(), sprintManager.getSpeed());

		//stop once sprint distance is reached
		if (sprintManager.getDistance() >= sprintDistance) {
			Split split = sprintManager.calculateApproximateSplit(sprintDistance);
			speedometerView.set(-1, split.distance, split.time);

			sprintGraph.renderChart();

			stopSprint();
		}

		return true;
	}

	protected void stopSprint() {
		Log.i(TrackPracticeActivity.class.getName(), "Sprint mode: STOP");

		if(sprintManager.isBestTime()) {
			speedometerView.setBestTime(true);
		}

		if(sprintManager.isMaxSpeed()) {
			speedometerView.setBestMaxSpeed(true);
		}

		goButton.setBackgroundColor(getResources().getColor(R.color.GREEN_LIGHT));
		goButton.setText("Start");
		speedometerView.setSpeed(-1);

		sprintManager.stop();
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
