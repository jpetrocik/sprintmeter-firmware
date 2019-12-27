package com.bmxgates.logger;

import android.app.ActionBar.LayoutParams;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.bmxgates.logger.data.Sprint.Split;
import com.google.common.collect.EvictingQueue;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

public class SprintGraphFragment extends Fragment {

	XYSeries splits;

	View rootView;

	GraphicalView graphicalView;

	LinearLayout chartContainer;

	EvictingQueue<Double> smoothAvg;

	View.OnTouchListener onTouchListener;

	public SprintGraphFragment() {
		smoothAvg = EvictingQueue.create(6);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_sprint_graph, container, false);

		splits = new XYSeries("Splits");

		chartContainer = (LinearLayout) rootView.findViewById(R.id.chartContainer);

		return rootView;
	}

	public void renderChart(double maxSpeed) {

		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		dataset.addSeries(splits);

		XYSeriesRenderer splitRender = new XYSeriesRenderer();
		splitRender.setShowLegendItem(false);
		splitRender.setDisplayChartValues(false);
		splitRender.setChartValuesTextSize(24);

		XYMultipleSeriesRenderer mSeriesRender = new XYMultipleSeriesRenderer();
		mSeriesRender.addSeriesRenderer(splitRender);
		mSeriesRender.setShowAxes(false);
		mSeriesRender.setShowGrid(false);
		mSeriesRender.setShowLabels(false);
		mSeriesRender.setZoomEnabled(false, false);
		mSeriesRender.setPanEnabled(false, false);
		mSeriesRender.setYAxisMin(maxSpeed-0.002235222170365);
		mSeriesRender.setYAxisMax(maxSpeed+0.002235222170365);

		graphicalView = ChartFactory.getLineChartView(getActivity(), dataset, mSeriesRender);
		graphicalView.setBackgroundColor(Color.BLACK);
		if (onTouchListener != null)
			graphicalView.setOnTouchListener(onTouchListener);
		chartContainer.addView(graphicalView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		graphicalView.repaint();

	}

	public void setOnTouchListener(View.OnTouchListener onTouchListner) {
		this.onTouchListener = onTouchListner;
	}

	public void addSplit(Split split) {
		addSplit(split.distance, split.speed);
	}

	public void addSplit(long distance, double speed) {

//		double userSpeed = speed * Formater.SPEED_CONVERSION;

		double smoothedSpeed = smooth(speed);
//		smoothedSpeed = (double) (Math.round(smoothedSpeed * 100.0) / 100.0);
		splits.add(distance, smoothedSpeed);

	}

	public double smooth(double instantSpeed) {
		smoothAvg.add(instantSpeed);

		return smoothAvg.stream().mapToDouble(Double::doubleValue).average().getAsDouble();
	}

	public void reset() {
		splits = new XYSeries("Splits");
		chartContainer.removeView(graphicalView);
	}

}
