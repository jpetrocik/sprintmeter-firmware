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

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

public class SprintGraphFragment extends Fragment {

	XYSeries splits;

	double minSpeed = Integer.MAX_VALUE, maxSpeed = Integer.MIN_VALUE;

	View rootView;

	GraphicalView graphicalView;

	LinearLayout chartContainer;

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

	public void renderChart() {

		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		dataset.addSeries(splits);

		XYSeriesRenderer splitRender = new XYSeriesRenderer();
		splitRender.setShowLegendItem(false);
		splitRender.setDisplayChartValues(true);
		splitRender.setChartValuesTextSize(24);

		XYMultipleSeriesRenderer mSeriesRender = new XYMultipleSeriesRenderer();
		mSeriesRender.addSeriesRenderer(splitRender);
		// mSeriesRender.setShowAxes(false);
		// mSeriesRender.setShowGrid(false);
		mSeriesRender.setShowLabels(false);
		mSeriesRender.setZoomEnabled(true, false);
		mSeriesRender.setPanEnabled(true, false);
		mSeriesRender.setYAxisMin(minSpeed-5);
		mSeriesRender.setYAxisMax(maxSpeed+5);

		graphicalView = ChartFactory.getLineChartView(getActivity(), dataset, mSeriesRender);
		graphicalView.setBackgroundColor(Color.BLACK);
		chartContainer.addView(graphicalView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		graphicalView.repaint();
	}

	public void addSplit(Split split) {
		addSplit(split.distance, split.speed);
	}

	public void addSplit(long distance, double speed) {

		double userSpeed = speed * Formater.SPEED_CONVERSION;
		userSpeed = (double) (Math.round(userSpeed * 100.0) / 100.0);
		if (userSpeed < minSpeed) {
			minSpeed = userSpeed;
		}

		if (userSpeed > maxSpeed) {
			maxSpeed = userSpeed;
		}

		splits.add(distance, userSpeed);

	}

	public void reset() {
		splits = new XYSeries("Splits");
		chartContainer.removeView(graphicalView);
	}

}
