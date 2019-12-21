package com.bmxgates.logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;

import com.bmxgates.logger.data.SprintDatabaseHelper;
import com.bmxgates.logger.data.SprintManager;

public abstract class AbstractSprintActivity extends FragmentActivity  {

	/**
	 * Checksum is sent with each split from SprintLogger.  Used to ensure
	 * no data was missed
	 */
	int prevCheckSum = -1;

	/**
	 * The last time a message was received
	 */
	long lastMessageTime;

	/**
	 * Indicates a checksum error occurred
	 */
	boolean checkSumError = false;
	
	Handler serialHandler;
	
	BMXSprintApplication application;
	
	SprintManager sprintManager;

	private BroadcastReceiver bluetoothStatusReceiver = new BroadcastReceiver() {
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


	private BroadcastReceiver databaseReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();
			if (BluetoothSerial.BLUETOOTH_FAILED.equals(action))
				onDatabaseOpened();
		}
	};

	private void onDatabaseOpened() {
		if (sprintManager != null)
			sprintManager.setDatabase(application.getDatabase());
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		application = (BMXSprintApplication) getApplication();

		serialHandler = new Handler(new Handler.Callback() {
	
			@Override
			public boolean handleMessage(Message msg) {
				return doHandleMessage(msg);
			}
		});
		
	}

	final boolean doHandleMessage(Message msg){
		lastMessageTime = System.currentTimeMillis();

		/**
		 * Due to the queuing of message, the stop button may be pressed before
		 * all the messages have been processed. In that case sprintManager
		 * isn't expecting any additional splits so ignore them
		 */
		if (!sprintManager.isReady()) {
			return true;
		}

		validateChecksum(msg);

		return processSplit(msg);
	}

	protected abstract boolean processSplit(Message msg);

	final protected void createSprintManager(SprintManager.Type type){
		sprintManager = new SprintManager(type);
		sprintManager.setDatabase(application.getDatabase());

	}
	
	protected void readySprint(){
		prevCheckSum = -1;
		checkSumError = false;
	}
	
	@Override
	protected void onPause() {
		super.onPause();

		application.setSerialHandler(null);

		LocalBroadcastManager.getInstance(this).unregisterReceiver(bluetoothStatusReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(databaseReceiver);

	}

	@Override
	protected void onResume() {
		super.onResume();

		application.setSerialHandler(serialHandler);

		IntentFilter bluetoothFilter = new IntentFilter(BluetoothSerial.BLUETOOTH_CONNECTED);
		bluetoothFilter.addAction(BluetoothSerial.BLUETOOTH_DISCONNECTED);
		bluetoothFilter.addAction(BluetoothSerial.BLUETOOTH_FAILED);
		LocalBroadcastManager.getInstance(this).registerReceiver(bluetoothStatusReceiver, bluetoothFilter);

		IntentFilter databaseFilter = new IntentFilter(SprintDatabaseHelper.DATABASE_OPENED_ACTION);
		LocalBroadcastManager.getInstance(this).registerReceiver(databaseReceiver, databaseFilter);

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


	/*
	 * validateChecksum ensures we didn't miss a message.
	 * The validateChecksum + split = checksum
	 */
	void validateChecksum(Message msg){

		int checkSum = msg.arg2;

		//checkSum == 0 to prevent devide by errror when rolling over
		if (prevCheckSum  == -1 || checkSum == 0) {
			prevCheckSum = checkSum;
			checkSumError = false;
			return;
		}

		checkSumError = ((checkSum % prevCheckSum ) != 1);

		prevCheckSum = checkSum;
	}

	protected abstract void connectionFailed();

	protected abstract void connectionLost();

	protected abstract void connectionRestored();


}
