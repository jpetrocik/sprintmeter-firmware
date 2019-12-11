package com.bmxgates.ui;

import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class SwipeListener implements View.OnTouchListener {

	GestureDetector gestureDetector;
	
	public SwipeListener(Context context, Callback callback){
		gestureDetector = new SwipeGestureDetector(context, callback);
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
	}

	  // Gesture detection
    public class SwipeGestureDetector extends GestureDetector {

    	private static final int SWIPE_MIN_DISTANCE = 120;
        private static final int SWIPE_MAX_OFF_PATH = 250;
        private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    	public SwipeGestureDetector(Context context, final Callback callback){
    		super(context, new SimpleOnGestureListener(){
    	
		        
		        @Override
		        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		            try {
		                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
		                    return false;
		                // right to left swipe
		                if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
		                    return callback.swipeLeft();
		                }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
		                    return callback.swipeRight();
		                }
		            } catch (Exception e) {
		               Log.e("SwipeListener", e.getMessage());
		            }
		            return false;
		        }
		
		            @Override
		        public boolean onDown(MotionEvent e) {
		              return true;
		        }
    		});
    	}
    }
    
    public interface Callback {
    	public boolean swipeLeft();
    	
    	public boolean swipeRight();
    }
}
