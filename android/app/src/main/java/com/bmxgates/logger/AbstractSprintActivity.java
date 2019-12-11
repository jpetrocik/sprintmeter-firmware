package com.bmxgates.logger;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.bmxgates.logger.data.SprintDatabaseHelper;
import com.bmxgates.logger.data.SprintManager;

public abstract class AbstractSprintActivity extends FragmentActivity  {
	protected final static String BMX_MAIN_ACTIVITY = "BMXMainActivity";

	/**
	 * Checksum is sent with each split from SprintLogger.  Uused to ensure
	 * no data was missed
	 */
	long validateChecksum;

	/**
	 * Indicates a checksum error occured
	 */
	boolean checksumError= false;
	
	int goButtonId, connectButtonId, layoutId;
	
	Dialog dialog;

	Handler serialHandler;
	
	BMXSprintApplication application;
	
	Button goButton, connectButton;

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(layoutId);

		application = (BMXSprintApplication) getApplication();

		serialHandler = new Handler(new Handler.Callback() {
	
			@Override
			public boolean handleMessage(Message msg) {
				return doHandleMessage(msg);
			}
		});
		
		// hide goButton until connection is established
		goButton = (Button) findViewById(goButtonId);

		// disable button until connected or connection fails
		connectButton = (Button) findViewById(connectButtonId);
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
	}


	protected boolean doHandleMessage(Message msg){
		
		/**
		 * Due to the queuing of message, the stop button may be pressed before
		 * all the messages have been processed. In that case sprintManager
		 * isn't expecting any additional splits so ignore them
		 */
		if (!sprintManager.isReady()) {
			return true;
		}

//		validateChecksum(msg);
		
		return false;
	}
	
	protected void onDatabaseOpened(){
		sprintManager.setDatabase(application.getDatabase());
	}
	
	protected void readySprint(){
		validateChecksum = -1;
		checksumError = false;
	}
	
	protected abstract void stopSprint();
	
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


	/**
	 * Perform all activity required when connection restored
	 */
	protected void connectionRestored() {
		Log.i(BMX_MAIN_ACTIVITY, "Connection restored");

		// toggle buttons, this saves existing state of goButton
		// so when restored button is in last know state
		goButton.setVisibility(View.VISIBLE);
		connectButton.setVisibility(View.GONE);

		if (dialog != null) {
			dialog.dismiss();
			dialog = null;
		}
	}

	protected void connectionLost() {
		Log.i(BMX_MAIN_ACTIVITY, "Connection lost");

		goButton.setVisibility(View.GONE);
		connectButton.setVisibility(View.VISIBLE);
		connectButton.setText("Connecting...");
		connectButton.setEnabled(false);
	}

	protected void connectionFailed() {
		Log.i(BMX_MAIN_ACTIVITY, "Connection failed");

		goButton.setVisibility(View.GONE);
		connectButton.setText("Reconnect");
		connectButton.setEnabled(true);

		if (dialog != null) {
			dialog.dismiss();
			dialog = null;
		}
	}
	
	/*
	 * validateChecksum ensures we didn't miss a message.
	 * The validateChecksum + split = checksum
	 */
	private void validateChecksum(Message msg){
		
		int split = msg.arg1;

		Log.d("BMXLogger", String.valueOf(split));
		
		long checksum = msg.getData().getLong("checksum");
		if (validateChecksum  == -1) {
			validateChecksum = checksum;
		} else {
			validateChecksum += split;
		}
		
		checksumError = validateChecksum-checksum != 0;
	}

}
