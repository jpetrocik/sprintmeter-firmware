package com.bmxgates.logger;

import android.app.Application;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.bmxgates.logger.data.SprintDatabaseHelper;

public class BMXSprintApplication extends Application {
	public static final String BMX_SPRINT_APPLICATION ="BMXSprintApplication";

	private static final int TOTAL_MESSAGE_SIZE = 9;
	private static final int SPLIT_START_BYTE = 3;
	private static final int CHECKSUM_START_BYTE = 7;

	BluetoothSerial bluetoothSerial;

	SQLiteDatabase database;

	Handler serialHandler;

	@Override
	public void onCreate() {
		super.onCreate();

		SQLiteOpenHelper sprintDatabaseHelper = new SprintDatabaseHelper(this);
		openDatabase(sprintDatabaseHelper);

		bluetoothSerial = new BluetoothSerial(this, new BluetoothSerial.MessageHandler() {

			@Override
			public int read(int bufferSize, byte[] buffer) {
				return readBluetoothMessage(bufferSize, buffer);
			}
		}, "SprintMeter");
		bluetoothSerial.onResume();
	}

	protected int readBluetoothMessage(int bufferSize, byte[] buffer) {
		int i = 0;

		// ignore everything until ready
		if (serialHandler == null)
			return bufferSize;

		// loop bytes look for start of message
		for (; i < bufferSize;) {

			//do we have enough for a complete message
			if ((bufferSize - i) < TOTAL_MESSAGE_SIZE)
				break;

			// look for start of message, skipping over bytes
			if ( buffer[i] == 'B' && buffer[i+1] == 'M' && buffer[i+2] == 'X') {

				try {
					int split = (ui(buffer[i+SPLIT_START_BYTE]) << 24) | (ui(buffer[i+SPLIT_START_BYTE+1]) << 16) | (ui(buffer[i+SPLIT_START_BYTE+2]) << 8) | (ui(buffer[i+SPLIT_START_BYTE+3]));
					int checksum = (ui(buffer[i+CHECKSUM_START_BYTE]) << 8) | (ui(buffer[i+CHECKSUM_START_BYTE+1]));

//					Log.v(BMXSprintApplication.class.getName(), "Split: " + split);

					Message message = serialHandler.obtainMessage();
					message.arg1 = split;
					message.arg2 = checksum;

					serialHandler.sendMessage(message);

					return i + TOTAL_MESSAGE_SIZE;
				} catch (Throwable t) {
					Log.i(BMXSprintApplication.class.getName(), "Failed processing message: " + t.getMessage());
				}

			}

			i++;
		}

		//return number of bytes read
		return i;

	}

	public void reconnect() {
		bluetoothSerial.close();
		bluetoothSerial.connect();
	}

	public Handler getSerialHandler() {
		return serialHandler;
	}

	public void setSerialHandler(Handler serialHandler) {
		this.serialHandler = serialHandler;
	}

	public boolean isConnected() {
		return bluetoothSerial.connected;
	}

	public AsyncTask<SQLiteOpenHelper, Void, SQLiteDatabase> openDatabase(SQLiteOpenHelper sqliteOpenHelper){
		return new AsyncTask<SQLiteOpenHelper, Void, SQLiteDatabase>() {
			@Override
			protected SQLiteDatabase doInBackground(SQLiteOpenHelper... params) {
				SQLiteDatabase database = params[0].getWritableDatabase();

				return database;
			}

			@Override
			protected void onPostExecute(SQLiteDatabase sqlitedatabase) {
				super.onPostExecute(sqlitedatabase);

				database = sqlitedatabase;

				Intent intent = new Intent(SprintDatabaseHelper.DATABASE_OPENED_ACTION);
				LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
			}


		}.execute(sqliteOpenHelper);
	}

	public SQLiteDatabase getDatabase() {
		return database;
	}


	private static long ul(byte b){
		return (long) (b & 0xff);
	}

	private static int ui(byte b){
		return (int) (b & 0xff);
	}


}
