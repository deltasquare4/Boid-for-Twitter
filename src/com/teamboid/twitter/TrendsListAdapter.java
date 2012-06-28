package com.teamboid.twitter;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;

import android.content.Context;
import twitter4j.Trend;
import twitter4j.Trends;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * The list adapter used for the trends column.
 * @author Aidan Follestad
 */

public class TrendsListAdapter extends BaseAdapter {

	public TrendsListAdapter(Context timeline) {
		mActivity = timeline;
		trends = new ArrayList<Trend>();
	}
	
	private Context mActivity;
	private ArrayList<Trend> trends;
	public String id;
	
	public void add(Trend[] trs) {
		for(Trend tr : trs) trends.add(tr);
		notifyDataSetChanged();
	}
	public void add(Trends toAdd) {
		for(Trend trend : toAdd.getTrends()) {
			if(!contains(trend)) trends.add(trend);
			notifyDataSetChanged();
		}
	}
	public void add(Trends[] toAdd) {
		for(Trends trend : toAdd) {
			for(Trend t : trend.getTrends()) {
				if(!contains(t)) trends.add(t);
			}
			notifyDataSetChanged();
		}
	}
	public void clear() {
		trends.clear();
		notifyDataSetChanged();
	}

	public Trend[] toArray() { return trends.toArray(new Trend[0]); }
	
	private Boolean contains(Trend toFind) {
		Boolean found = false;
		ArrayList<Trend> itemCache = trends;
		for(Trend trend : itemCache) {
			if(trend.getQuery().equals(toFind.getQuery())) {
				found = true;
				break;
			}
		}
		return found;
	}
	
	@Override
	public int getCount() { return trends.size(); }
	@Override
	public Object getItem(int position) {
		try { return URLDecoder.decode(trends.get(position).getQuery(), "UTF-8"); }
		catch (UnsupportedEncodingException e) { 
			e.printStackTrace();
			return null;
		}
	}
	@Override
	public long getItemId(int position) { return position; }
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView toReturn = null;
		if(convertView != null) toReturn = (TextView)convertView;
		else toReturn = (TextView)LayoutInflater.from(mActivity).inflate(R.layout.trends_list_item, null); 
		Trend curItem = trends.get(position);
		toReturn.setText(curItem.getName());
		return toReturn;
	}
}

