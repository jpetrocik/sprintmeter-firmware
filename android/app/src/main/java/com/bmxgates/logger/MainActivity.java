package com.bmxgates.logger;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

	BMXSprintApplication application;
	
	TextView messageView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		getActionBar().setTitle("BMXGates.com");

		application = (BMXSprintApplication)getApplication();
		
		Button button = (Button) findViewById(R.id.trackPracticeButton);
		button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				Intent myIntent = new Intent(MainActivity.this, TrackPracticeActivity.class);
				startActivity(myIntent);
			}
		});

		button = (Button) findViewById(R.id.sprintPracticeButton);
		button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				Intent myIntent = new Intent(MainActivity.this, SprintActivity.class);
				startActivity(myIntent);
				
			}
		});

		button = (Button) findViewById(R.id.boxSprintButton);
		button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				Intent myIntent = new Intent(MainActivity.this, BoxSprintAcivity.class);
				startActivity(myIntent);
				
			}
		});


		button = (Button) findViewById(R.id.stopwatchButton);
		button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				Intent myIntent = new Intent(MainActivity.this, StopWatchActivity.class);
				startActivity(myIntent);
				
			}
		});

		messageView = (TextView) findViewById(R.id.textView1);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.settingsMenuItem:
			Intent myIntent = new Intent(MainActivity.this, SettingsActivity.class);
			startActivity(myIntent);

			return true;
		case R.id.reconnectMenuItem:
			application.reconnect();
		default:
			return super.onOptionsItemSelected(item);
		}
	}	

}
