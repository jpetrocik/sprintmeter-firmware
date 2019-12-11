package com.bmxgates.logger;

import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;

import com.bmxgates.logger.data.SprintManager;
import com.bmxgates.logger.data.Sprint.Split;
import com.bmxgates.logger.data.SprintManager.Type;

public class SprintActivity extends AbstractSprintActivity {

	/*
	 * Distance to ignore before spring
	 */
	long runup;

	/*
	 * Distance of sprint, read from settings
	 */
	long sprintDistance;
	
	int wheelSize;

	SpeedometerFragment speedometerView;

	SprintGraphFragment sprintGraph;

	public SprintActivity() {
		goButtonId = R.id.sprint_go_button;
		connectButtonId = R.id.sprint_connect_button;
		layoutId = R.layout.activity_sprint;
		
		sprintManager = new SprintManager(Type.SPRINT);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		speedometerView = (SpeedometerFragment) getSupportFragmentManager().findFragmentById(R.id.sprint_speedometer);
		speedometerView.show20Time(false);
		
		sprintGraph = (SprintGraphFragment) getSupportFragmentManager().findFragmentById(R.id.sprint_speed_graph);

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

		if (application.getDatabase() != null) {
			onDatabaseOpened();
		}
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

		sprintManager.start();
		
		speedometerView.reset();
		speedometerView.setDistance(runup);
	}

	@Override
	protected boolean doHandleMessage(Message msg){

		if (super.doHandleMessage(msg))
			return true;

		if (checksumError)
			speedometerView.setError(true);

		//ignore all splits until runup is exhausted
		if (runup > 0){
			runup -= wheelSize;
			speedometerView.setDistance(runup);
			return true;
		}
		
		//update speedometer view
		if (speedometerView.getDistance() == 0) {
			goButton.setBackgroundColor(getResources().getColor(R.color.RED_LIGHT));
			goButton.setText("Stop");

			speedometerView.setDistance(1);
			return true;
		} else {
			speedometerView.add(wheelSize, msg.arg1);
			sprintGraph.addSplit(speedometerView.getDistance(), speedometerView.getSpeed());
			sprintManager.addSplit(speedometerView.getDistance(), speedometerView.getTime(), speedometerView.getSpeed());
		}


		//stop once sprint distance is reached
		if (speedometerView.getDistance() >= sprintDistance) {
			Split split = sprintManager.split(sprintDistance, wheelSize);
			speedometerView.set(-1, split.distance, split.time);

			stopSprint();

			return true;
		}

		return true;
	}

	
	@Override
	protected void stopSprint() {
		goButton.setBackgroundColor(getResources().getColor(R.color.GREEN_LIGHT));
		goButton.setText("Start");

		speedometerView.setSpeed(-1);
		
		sprintManager.stop();
	}

}
