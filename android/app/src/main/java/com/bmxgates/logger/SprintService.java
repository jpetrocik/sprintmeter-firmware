package com.bmxgates.logger;

import android.content.Intent;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.bmxgates.logger.data.Sprint;
import com.bmxgates.logger.data.SprintManager;

public class SprintService extends AbstractSprintService {

	long runUp;

	long sprintDistance;

	LocalBinder<SprintService> binder = new LocalBinder(this);

	public int readySprint(int trackId) {
		int index = super.readySprint(trackId);

		//load settings
		runUp = SettingsActivity.getRunupDistance(this);
		wheelSize = SettingsActivity.getWheelSize(this);
		sprintDistance = SettingsActivity.getSprintDistance(this);

		//seding a second time to send runUp distance
		Intent readyIntent = new Intent(READY_ACTION);
		readyIntent.putExtra("runUp", runUp);
		readyIntent.putExtra("initial", false);
		LocalBroadcastManager.getInstance(this).sendBroadcast(readyIntent);

		return index;
	}

	boolean processSplit(Message msg) {

		//ignore all splits until run up is exhausted
		if (runUp > 0) {
			runUp -= wheelSize;

			if (runUp < 0)
				runUp = 0;

			Intent runUpIntent = new Intent(READY_ACTION);
			runUpIntent.putExtra("runUp", runUp);
			runUpIntent.putExtra("initial", false);
			LocalBroadcastManager.getInstance(this).sendBroadcast(runUpIntent);
			return true;
		}

		//first tick, starts sprint
		if (sprintManager.getDistance() == 0) {
			sprintManager.addSplitTime(0, 1);

			startSprint();
			return true;
		}

		int splitTime = msg.arg1;
		Sprint.Split split = sprintManager.addSplitTime(splitTime, wheelSize);

		//stop once sprint distance is reached and recalculate stop point
		if (sprintManager.getDistance() >= sprintDistance) {

			Sprint.Split adjustedSplit = sprintManager.calculateApproximateSplit(sprintDistance);
			sprintManager.replaceLast(adjustedSplit);

			stopSprint();

			return true;
		}

		Intent updateIntent = new Intent(UPDATE_ACTION);
		LocalBroadcastManager.getInstance(this).sendBroadcast(updateIntent);
		return true;

	}

	SprintManager createSprintManager() {
		return new SprintManager(SprintManager.Type.SPRINT);
	}


	protected void validateCurrentSprint() {
		if (sprintManager.getDistance() < sprintDistance) {
			sprintManager.setValid(false);
			return;
		}
	}


	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}


}
