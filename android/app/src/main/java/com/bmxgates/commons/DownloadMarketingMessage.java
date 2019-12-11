package com.bmxgates.commons;

import java.io.InputStream;
import java.net.URL;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.common.io.ByteStreams;

public class DownloadMarketingMessage extends AsyncTask<Void, Void, String> {
	
	protected static final String MESSAGE_RECEIVED = "message-recieved";

	protected static final String MESSAGE_EXTRA_NAME = DownloadMarketingMessage.class.getPackage().toString() + ".Message";

	public String marketingMessage;

	private Context context;
	
	public DownloadMarketingMessage(Context context){
		this.context=context;
	}
	
	@Override
	protected String doInBackground(Void... params) {
		try {
			URL url = new URL("http://www.bmxgates.com/m.html");

			InputStream inStream = url.openStream();
			byte[] data = ByteStreams.toByteArray(inStream);

			return new String(data);

		} catch (Exception e) {
			Log.e("DownloadMarketingMessage", "Unable to download message");
		}
		return null;
	}

	@Override
	protected void onPostExecute(String result) {
		if (result != null) {

			marketingMessage = result;

			Intent intent = new Intent(DownloadMarketingMessage.MESSAGE_RECEIVED);
			intent.putExtra(DownloadMarketingMessage.MESSAGE_EXTRA_NAME, result);
			LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
		}
	}
}
