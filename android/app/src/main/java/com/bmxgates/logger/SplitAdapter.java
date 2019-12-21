package com.bmxgates.logger;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bmxgates.logger.data.Sprint.Split;

import java.util.ArrayList;
import java.util.List;

public class SplitAdapter extends BaseAdapter {

	private List<Split> splits = new ArrayList<Split>();
	private List<Split> bestSplits = new ArrayList<Split>();

	private Context context;
	
	public SplitAdapter(Context context) {
        this.context = context;
    }
	@Override
	public int getCount() {
		return splits.size();
	}

	public void add(Split split, Split bestSplit){
		splits.add(split);
		bestSplits.add(bestSplit);
		notifyDataSetChanged();
	}
	
	public void clear(){
		splits.clear();
		notifyDataSetChanged();
	}
	
	@Override
	public Split getItem(int position) {
		return splits.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		ViewHolder viewHolder;
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View newView = inflater.inflate(R.layout.sprint_view_item, parent, false);

			viewHolder = new ViewHolder();
			newView.setTag(viewHolder);
			viewHolder.diff = (TextView) newView.findViewById(R.id.diffItemView);
			viewHolder.distance = (TextView) newView.findViewById(R.id.dstItemView);
			viewHolder.time = (TextView) newView.findViewById(R.id.splitItemView);
			viewHolder.speed = (TextView) newView.findViewById(R.id.speedItemView);

			convertView = newView;
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}

		Split split = getItem(position);
		Split bestSplit = bestSplits.get(position);
		viewHolder.diff.setText(Formater.time(  split.time - bestSplit.time, false));
		viewHolder.distance.setText(Formater.distance(split.distance));
		viewHolder.time.setText(Formater.time(split.time, true));
		viewHolder.speed.setText(Formater.speed(split.speed));

		return convertView;
	}

	public static class ViewHolder {
		TextView distance, time, diff, speed;
	}
	

}
