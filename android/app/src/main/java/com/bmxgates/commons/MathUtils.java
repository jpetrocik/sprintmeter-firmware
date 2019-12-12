package com.bmxgates.commons;

import java.util.Arrays;
import java.util.List;

public class MathUtils {

	public static long mean(long[] times) {
		long sum = 0L;
		long[] arr$ = times;
		int len$ = times.length;

		for(int i$ = 0; i$ < len$; ++i$) {
			long t = arr$[i$];
			sum += t;
		}

		return sum / (long)times.length;
	}

	public static long trimmedMean(long[] times, double percentage) {
		double n = (double)times.length;
		double k = n * percentage;
		double R = n - 2.0D * k;
		int startIndex = (int)Math.floor(k);
		int endIndex = (int)Math.ceil(R);
		double ki = (double)(startIndex + 1) - k;
		long sum = 0L;
		if (R == (double)endIndex) {
			++endIndex;
		}

		Arrays.sort(times);

		for(int i = startIndex; i < endIndex; ++i) {
			long time = times[i];
			if (i != startIndex && i + 1 != endIndex) {
				sum += time;
			} else {
				sum += (long)((double)time * ki);
			}
		}

		return Math.round((double)sum / R);
	}

	public static long[] trimmedRange(long[] times, double percentage) {
		double n = (double)times.length;
		double k = n * percentage;
		double R = n - 2.0D * k;
		long[] range = new long[2];
		Arrays.sort(times);
		int startIndex = (int)Math.floor(k);
		int endIndex = (int)Math.ceil(k);
		range[0] = Math.round((double)(times[startIndex] + times[endIndex]) / 2.0D);
		if (Math.floor(R) == R) {
			startIndex = (int)Math.floor(R);
		} else {
			startIndex = (int)Math.floor(R) - 1;
		}

		if (Math.ceil(R) == R) {
			endIndex = (int)Math.ceil(R);
		} else {
			endIndex = (int)Math.ceil(R) - 1;
		}

		range[1] = Math.round((double)(times[startIndex] + times[endIndex]) / 2.0D);
		return range;
	}

	public static long[] range(long[] times) {
		int n = times.length;
		long[] range = new long[2];
		Arrays.sort(times);
		range[0] = times[0];
		range[1] = times[n - 1];
		return range;
	}

	public static long boundryThreshold(List<Long> t) {
		int size = t.size();
		long[] times = new long[size];

		for(int i=0;i<size;i++){
			times[i]=t.get(i);
		}

		return boundryThreshold(times);
	}

	public static long boundryThreshold(long[] times) {
		Arrays.sort(times);
		if (times.length == 1) {
			return times[0] + 1L;
		} else {
			long q2 = median(times);
			long[][] bisected = split(times, q2);
			long q1 = median(bisected[0]);
			long q3 = median(bisected[1]);
			long interquartileRange = q3 - q1;
			return q3 + interquartileRange * 3L;
		}
	}

	public static long[][] split(long[] sortedArray, long q) {
		long[] upper = new long[sortedArray.length];
		long[] lower = new long[sortedArray.length];
		int lIndex = 0;
		int hIndex = 0;

		for(int i = 0; i < sortedArray.length; ++i) {
			long value = sortedArray[i];
			if (value < q) {
				lower[lIndex++] = value;
			} else {
				upper[hIndex++] = value;
			}
		}

		long[][] split = new long[][]{Arrays.copyOf(lower, lIndex), Arrays.copyOf(upper, hIndex)};
		return split;
	}

	public static long median(long[] sortedArray) {
		int maxIndex = sortedArray.length - 1;
		if (maxIndex == 0) {
			return sortedArray[0];
		} else {
			int midLow = (int)Math.floor((double)maxIndex / 2.0D);
			int midHigh = (int)Math.ceil((double)maxIndex / 2.0D);
			return Math.round((double)(sortedArray[midLow] + sortedArray[midHigh]) / 2.0D);
		}
	}
}
