package com.bmxgates.logger;

import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.bmxgates.commons.MathUtils;
import com.bmxgates.logger.data.Sprint;
import com.bmxgates.logger.data.SprintManager;

import java.util.ArrayList;
import java.util.List;

public class TrackPracticeService extends AbstractSprintService {

	boolean autoStop;

	int wheelSize;

	long[] marks;

	TrackLocator.Track track;

	Handler handler;

	Runnable autoReadyChecker;

	LocalBinder<TrackPracticeService> binder = new LocalBinder(this);

	public TrackPracticeService() {
		handler = new Handler(Looper.getMainLooper());
	}

	public int readySprint(int trackId) {
		int index = super.readySprint(trackId);

		//load settings
		autoStop = SettingsActivity.getAutoStop(this);
		wheelSize = SettingsActivity.getWheelSize(this);

		return index;
	}

	@Override
	boolean processSplit(Message msg) {

		//first split starts sprint
		if (sprintManager.getDistance() == 0) {
			sprintManager.addSplitTime(0, 1);

			startSprint();
			return true;
		}

		int splitTime = msg.arg1;
		sprintManager.addSplitTime(splitTime, wheelSize);


		// auto stop timer is enabled
		if (autoStop && sprintManager.getDistance() >= track.autoStop) {
			stopSprint();

			return true;
		}

		Intent updateIntent = new Intent(UPDATE_ACTION);
		LocalBroadcastManager.getInstance(this).sendBroadcast(updateIntent);

		return true;
	}

	@Override
	SprintManager createSprintManager() {
		return new SprintManager(SprintManager.Type.TRACK);
	}

	@Override
	protected void validateCurrentSprint(){

		if (sprintManager.getDistance() < 18288) {
			sprintManager.setValid(false);
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
					Sprint.Split split = sprint.calculateApproximateSplit(marks[nextMark]);
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
					Sprint.Split split = sprint.calculateApproximateSplit(track.autoStop);
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
			Sprint.Split split = sprintManager.calculateApproximateSplit(marks[nextMark]);

			//this sprint is incomplete
			if (split == null) {
				sprintManager.setValid(false);
				return;
			}

			splitTimes.add(split.time);
			nextMark++;
		}

		if (autoStop) {
			Sprint.Split split = sprintManager.calculateApproximateSplit(track.autoStop);

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

	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	public void enableAutoReady() {
		autoReadyChecker = createAutoReadyChecker();
		handler.postDelayed(autoReadyChecker, 500);
	}

	Runnable createAutoReadyChecker() {
		return new Runnable() {
			public void run() {
				if (System.currentTimeMillis() > (TrackPracticeService.this.lastMessageTime + 2000)) {

					//stop current sprint
					if (sprintManager.mode() == SprintManager.Mode.SPRINTING) {
						Log.i(TrackPracticeService.class.getName(), "Auto stopping sprint");
						stopSprint();
					}

					//auto ready for next sprint
					if (sprintManager.mode() == SprintManager.Mode.STOPPED && autoReadyChecker != null) {
						readySprint(track.trackId);
					}
				}

				enableAutoReady();
			}
		};
	}

	public void disableAutoReady() {
		handler.removeCallbacks(autoReadyChecker);
		autoReadyChecker = null;
	}

	public void setTrack(TrackLocator.Track track) {
		this.track = track;
	}


}
