package com.bmxgates.logger;

import android.os.Bundle;
import android.view.View;

public class BoxSprintAcivity  extends TrackPracticeActivity {

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		findViewById(R.id.track_location).setVisibility(View.GONE);

//		createSprintManager(SprintManager.Type.BOX);
	}

}
