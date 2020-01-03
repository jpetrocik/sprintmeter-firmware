package com.bmxgates.logger;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.bmxgates.logger.data.Sprint;
import com.bmxgates.logger.data.SprintDatabaseHelper;
import com.bmxgates.logger.data.SprintManager;

import static com.bmxgates.logger.BMXSprintApplication.SPRINT_NOTIFICATION_CHANNEL;

public class SprintService extends Service {

	SprintManager sprintManager;

	BMXSprintApplication application;

	private Handler serialHandler;

	long runUp;

	long sprintDistance;

	int wheelSize;

	private long lastMessageTime;

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
	public void onCreate() {
		super.onCreate();

		application = (BMXSprintApplication)this.getApplication();

		IntentFilter databaseFilter = new IntentFilter(SprintDatabaseHelper.DATABASE_OPENED_ACTION);
		LocalBroadcastManager.getInstance(this).registerReceiver(databaseReceiver, databaseFilter);

		sprintManager = new SprintManager(SprintManager.Type.TRACK);
		sprintManager.setDatabase(application.getDatabase());

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

		if (checkSumError)
			speedometerView.setError();

		//ignore all splits until runup is exhausted
		if (runUp > 0) {
			runUp -= wheelSize;
			speedometerView.setDistance(runUp);
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
		Sprint.Split split = sprintManager.addSplitTime(splitTime, wheelSize);

		//stop once sprint distance is reached and recalculate stop point
		if (sprintManager.getDistance() >= sprintDistance) {
			Sprint.Split adjustedSplit = sprintManager.calculateApproximateSplit(sprintDistance);

			sprintManager.replaceLast(adjustedSplit);

			stopSprint();

			return true;
		}

		speedometerView.set(sprintManager.getSpeed(), sprintManager.getDistance(), sprintManager.getTime());

		return true;

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

	public int ready() {
		int index = sprintManager.ready();

		Notification notification = new NotificationCompat.Builder(this, SPRINT_NOTIFICATION_CHANNEL)
				.setContentTitle("Sprints")
				.setContentText("Ready...")
				.setSmallIcon(R.drawable.ic_launcher)
				.build();

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(1, notification);

		//load settings
		runUp = SettingsActivity.getRunupDistance(this);
		wheelSize = SettingsActivity.getWheelSize(this);
		sprintDistance = SettingsActivity.getSprintDistance(this);

		return index;
	}

	public void stop() {
		Notification notification = new NotificationCompat.Builder(this, SPRINT_NOTIFICATION_CHANNEL)
				.setContentTitle("Sprints")
				.setContentText("Stopped...")
				.setSmallIcon(R.drawable.ic_launcher)
				.build();

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(1, notification);
	}

	public int totalSprints() {
		return sprintManager.totalSprints();
	}

	public long bestTime() {
		return  sprintManager.bestTime();
	}

	public double bestSpeed() {
		return sprintManager.bestSpeed();
	}

	public long getDistance() {
		return sprintManager.getDistance();
	}

	public void setValid(boolean b) {
		sprintManager.setValid(false);
	}

	public boolean isReady() {
		return sprintManager.isReady();
	}

	public class LocalBinder extends Binder {
		SprintService getService() {
			// Return this instance of LocalService so clients can call public methods
			return SprintService.this;
		}
	}

	LocalBinder binder = new LocalBinder();
	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
}
