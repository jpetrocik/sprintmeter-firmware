package com.bmxgates.logger;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Formater {

	private final static double MS_PER_CM_TO_MPH_CONVERSION = 2.23691410468716;

	private final static double uS_PER_MM_TO_MPH_CONVERSION = 2236.91410468716;

	private final static double MM_TO_FOOT_CONVERSION = 0.00328084;

	public final static double SPEED_CONVERSION = uS_PER_MM_TO_MPH_CONVERSION;

	public final static double DISTANCE_CONVERSION = MM_TO_FOOT_CONVERSION;

	public final static double MPH_20 = 20/uS_PER_MM_TO_MPH_CONVERSION;

	private final static DecimalFormat speedFormat = new DecimalFormat("00.0");
	
	private final static DecimalFormat distanceFormat = new DecimalFormat("0000.0");

	private final static DateFormat dateFormat = DateFormat.getDateInstance(SimpleDateFormat.MEDIUM);
	

	public static String speed(double value){
		return speedFormat.format(value * SPEED_CONVERSION);
	}

	public static String distance(long value){
		return distanceFormat.format(value * DISTANCE_CONVERSION);
	}

	public static CharSequence time(long time, boolean fixed) {
		long min = Math.abs(time/60000000);
		time = time % 60000000;
		long sec = Math.abs(time/1000000);
		time = time % 1000000;
		long millis = Math.abs(Math.round(time/1000.0));


		//add sign to time, where split diff is negative
		String sign = time<0?"-":"";

		if (fixed)
			return sign + String.format("%02d:", min) + String.format("%02d.", sec) + String.format("%03d", millis);
		else 
			return sign + (min>0?String.format("%02d:", min):"") + (sec>0?String.format("%02d.", sec):"0.") + String.format("%03d", millis);
			
	}
	
	public static String date(Date date){
		return dateFormat.format(date);
	}
}
