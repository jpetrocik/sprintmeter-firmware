package com.bmxgates.logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;

import com.bmxgates.logger.data.Sprint;

public abstract class AbstractSprintActivity<T extends AbstractSprintService> extends FragmentActivity  {

	int sprintIndex = 0;

	BMXSprintApplication application;
	
	T sprintService;

	BroadcastReceiver bluetoothStatusReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();
			if (BluetoothSerial.BLUETOOTH_CONNECTED.equals(action))
				connectionRestored();
			else if (BluetoothSerial.BLUETOOTH_DISCONNECTED.equals(action))
				connectionLost();
			else if (BluetoothSerial.BLUETOOTH_FAILED.equals(action))
				connectionFailed();
		}
	};

	BroadcastReceiver sprintStatusReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();
			if (SprintService.READY_ACTION.equals(action)) {
				onSprintReady(intent);
			} else if (SprintService.START_ACTION.equals(action)) {
				onSprintStarted(intent);
			} else if (SprintService.UPDATE_ACTION.equals(action)) {
				onSprintUpdate(intent);
			} else if (SprintService.STOP_ACTION.equals(action)) {
				onSprintEnded(intent);
			} else if (SprintService.ERROR_ACTION.equals(action)) {
				onSprintError(intent);
			}

		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		application = (BMXSprintApplication) getApplication();
	}


	@Override
	protected void onPause() {
		super.onPause();

		LocalBroadcastManager.getInstance(this).unregisterReceiver(bluetoothStatusReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(sprintStatusReceiver);
	}


	@Override
	protected void onResume() {
		super.onResume();

		IntentFilter bluetoothFilter = new IntentFilter(BluetoothSerial.BLUETOOTH_CONNECTED);
		bluetoothFilter.addAction(BluetoothSerial.BLUETOOTH_DISCONNECTED);
		bluetoothFilter.addAction(BluetoothSerial.BLUETOOTH_FAILED);
		LocalBroadcastManager.getInstance(this).registerReceiver(bluetoothStatusReceiver, bluetoothFilter);

		IntentFilter sprintFilter = new IntentFilter(SprintService.START_ACTION);
		sprintFilter.addAction(SprintService.UPDATE_ACTION);
		sprintFilter.addAction(SprintService.STOP_ACTION);
		sprintFilter.addAction(SprintService.READY_ACTION);
		LocalBroadcastManager.getInstance(this).registerReceiver(sprintStatusReceiver, sprintFilter);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}


	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Intent myIntent;

		switch (item.getItemId()) {
		case R.id.reconnectMenuItem:
			connectionLost();
			application.reconnect();
			return true;

		case R.id.settingsMenuItem:
			myIntent = new Intent(this, SettingsActivity.class);
			startActivity(myIntent);
			return true;

		case R.id.trackMenuItem:
			myIntent = new Intent(this, TrackListActivity.class);
			startActivity(myIntent);
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}


	final protected void loadSprint(int index){
		if (sprintService.getSprintManager().totalSprints() == 0)
			return;

		sprintIndex = index;

		if (sprintIndex < 0) {
			sprintIndex = 0;
		}

		// 9 = 9 OR 9 < 10, set sprintIndex to last index (i.e. 8)
		if (sprintService.getSprintManager().totalSprints() <= sprintIndex) {
			sprintIndex = sprintService.getSprintManager().totalSprints() - 1;
		}

		Sprint sprint = sprintService.getSprintManager().get(sprintIndex);
		renderSprint(sprint);
	}

	protected abstract void renderSprint(Sprint sprint);

	protected abstract void connectionFailed();

	protected abstract void connectionLost();

	protected abstract void connectionRestored();

	protected abstract void onSprintReady(Intent intent);

	protected abstract void onSprintStarted(Intent intent);

	protected abstract void onSprintUpdate(Intent intent);

	protected abstract void onSprintEnded(Intent intent);

	protected abstract void onSprintError(Intent intent);

	protected abstract  void newSprint();

}
