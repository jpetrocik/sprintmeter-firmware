package com.bmxgates.logger;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.bmxgates.logger.data.SprintDatabaseHelper;
import com.bmxgates.logger.data.SprintManager;

import static com.bmxgates.logger.BMXSprintApplication.SPRINT_NOTIFICATION_CHANNEL;

public abstract class AbstractSprintService extends Service {

	static final String START_ACTION = "sprint_start";

	static final String UPDATE_ACTION = "sprint_update";

	static final String STOP_ACTION = "sprint_stop";

	static final String READY_ACTION = "sprint_ready";

	static final String ERROR_ACTION = "sprint_error";

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

	SprintManager sprintManager;

	BMXSprintApplication application;

	Handler serialHandler;

	int wheelSize;

	private BroadcastReceiver databaseReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();
			onDatabaseOpened();
		}
	};

	private void onDatabaseOpened() {
		if (sprintManager != null)
			sprintManager.setDatabase(application.getDatabase());
	}

	@Override
	public void onCreate() {
		super.onCreate();

		application = (BMXSprintApplication)this.getApplication();

		IntentFilter databaseFilter = new IntentFilter(SprintDatabaseHelper.DATABASE_OPENED_ACTION);
		LocalBroadcastManager.getInstance(this).registerReceiver(databaseReceiver, databaseFilter);

		sprintManager = createSprintManager();
		sprintManager.setDatabase(application.getDatabase());

		serialHandler = new Handler(new Handler.Callback() {

			@Override
			public boolean handleMessage(Message msg) {
				return doHandleMessage(msg);
			}
		});

		application.setSerialHandler(serialHandler);

	}

	final boolean doHandleMessage(Message msg) {
		lastMessageTime = System.currentTimeMillis();

		if (!sprintManager.isReady()) {
			return true;
		}

		validateChecksum(msg);

		return processSplit(msg);
	}

	/*
	 * validateChecksum ensures we didn't miss a message.
	 * The validateChecksum + split = checksum
	 */
	final void validateChecksum(Message msg){

		int checkSum = msg.arg2;

		//checkSum == 0 to prevent devide by errror when rolling over
		if (prevCheckSum  == -1 || checkSum == 0) {
			prevCheckSum = checkSum;
			checkSumError = false;
			return;
		}

		checkSumError = ((checkSum % prevCheckSum ) != 1);

		if (checkSumError) {
			Intent startIntent = new Intent(ERROR_ACTION);
			LocalBroadcastManager.getInstance(this).sendBroadcast(startIntent);
		}

		prevCheckSum = checkSum;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		Notification notification = new NotificationCompat.Builder(this, SPRINT_NOTIFICATION_CHANNEL)
				.setContentTitle("Sprints")
				.setContentText("Stopped...")
				.setSmallIcon(R.drawable.ic_launcher)
				.build();

		startForeground(1, notification);

		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	public int readySprint(int trackId) {
		prevCheckSum = -1;
		checkSumError = false;

		int index = sprintManager.ready(trackId);


		Notification notification = new NotificationCompat.Builder(this, SPRINT_NOTIFICATION_CHANNEL)
				.setContentTitle("Sprints")
				.setContentText("Ready...")
				.setSmallIcon(R.drawable.ic_launcher)
				.build();

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(1, notification);

		return index;
	}

	final public void startSprint() {
		Intent startIntent = new Intent(START_ACTION);
		LocalBroadcastManager.getInstance(this).sendBroadcast(startIntent);

		Notification notification = new NotificationCompat.Builder(this, SPRINT_NOTIFICATION_CHANNEL)
				.setContentTitle("Sprints")
				.setContentText("Sprinting...")
				.setSmallIcon(R.drawable.ic_launcher)
				.build();

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(1, notification);
	}

	final public void stopSprint() {

		validateCurrentSprint();

		sprintManager.stop();

		Intent stopIntent = new Intent(STOP_ACTION);
		LocalBroadcastManager.getInstance(application).sendBroadcast(stopIntent);

		Notification notification = new NotificationCompat.Builder(this, SPRINT_NOTIFICATION_CHANNEL)
				.setContentTitle("Sprints")
				.setContentText("Stopped...")
				.setSmallIcon(R.drawable.ic_launcher)
				.build();

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(1, notification);
	}

	final public SprintManager getSprintManager() {
		return sprintManager;
	}

	final public boolean isReady() {
		return sprintManager.isReady();
	}

	abstract void validateCurrentSprint();

	abstract boolean processSplit(Message msg);

	abstract SprintManager createSprintManager();


}
