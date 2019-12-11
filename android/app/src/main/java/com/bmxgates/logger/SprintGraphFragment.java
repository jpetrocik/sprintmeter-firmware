package com.bmxgates.logger;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.ActionBar.LayoutParams;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.bmxgates.logger.data.Sprint.Split;

public class SprintGraphFragment extends Fragment {

	XYSeries splits;

	double minSpeed, maxSpeed;

	GraphicalView graphicalView;

	XYMultipleSeriesRenderer mSeriesRender;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_sprint_graph, container, false);

		splits = new XYSeries("Splits");

		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		dataset.addSeries(splits);

		XYSeriesRenderer splitRender = new XYSeriesRenderer();
		splitRender.setShowLegendItem(false);
		splitRender.setDisplayChartValues(true);
		splitRender.setChartValuesTextSize(16);

		mSeriesRender = new XYMultipleSeriesRenderer();
		mSeriesRender.addSeriesRenderer(splitRender);
		// mSeriesRender.setShowAxes(false);
		// mSeriesRender.setShowGrid(false);
		mSeriesRender.setShowLabels(false);
		mSeriesRender.setZoomEnabled(false, false);
		mSeriesRender.setPanEnabled(false, false);
		mSeriesRender.setYAxisMin(15);
		mSeriesRender.setYAxisMax(35);

		graphicalView = ChartFactory.getLineChartView(getActivity(), dataset, mSeriesRender);
		graphicalView.setBackgroundColor(Color.BLACK);
		LinearLayout chartContainer = (LinearLayout) rootView.findViewById(R.id.chartContainer);
		chartContainer.addView(graphicalView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		return rootView;
	}

	public void addSplit(Split split) {
		addSplit(split.distance, split.speed);
	}

	public void addSplit(long distance, double speed) {

//		if (speed < minSpeed) {
//			minSpeed = speed;
//			mSeriesRender.setYAxisMin(minSpeed * Formater.SPEED_CONVERSION - 5);
//		}
//
//		if (speed > maxSpeed) {
//			maxSpeed = speed;
//			mSeriesRender.setYAxisMax(maxSpeed * Formater.SPEED_CONVERSION + 5);
//		}

		double userSpeed = speed * Formater.SPEED_CONVERSION;
		splits.add(distance, (double) (Math.round(userSpeed * 100.0) / 100.0));
		graphicalView.repaint();
	}

	public void reset() {
		splits.clear();
		graphicalView.repaint();
		// index=0;
	}

}
