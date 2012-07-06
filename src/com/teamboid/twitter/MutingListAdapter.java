package com.teamboid.twitter;

import java.util.ArrayList;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

/**
 * The list adapter used in the muting manager, displays the all current muting rules.
 * @author Aidan Follestad
 */
public class MutingListAdapter extends BaseAdapter {

	public MutingListAdapter(Activity context) {
		mContext = context;
	}

	private Activity mContext;
	
	@Override
	public int getCount() { return loadKeywords().length; }
	@Override
	public Object getItem(int position) { return loadKeywords()[position]; }
	@Override
	public long getItemId(int position) { return position; }
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		RelativeLayout toReturn = null;
		if(convertView != null) toReturn = (RelativeLayout)convertView;
		else toReturn = (RelativeLayout)LayoutInflater.from(mContext).inflate(R.layout.muting_manager_item, null);
		final String[] types = mContext.getResources().getStringArray(R.array.muting_types);
		final String curRule = loadKeywords()[position];
		if(curRule.contains("@")) {
			((TextView)toReturn.findViewById(android.R.id.text1)).setText(curRule.substring(0, curRule.indexOf("@")).replace("%40", "@"));
			if(curRule.endsWith("@" + types[1])) {
				((TextView)toReturn.findViewById(android.R.id.text2)).setText(types[1].toLowerCase());
			} else ((TextView)toReturn.findViewById(android.R.id.text2)).setText(types[2].toLowerCase());
		} else {
			((TextView)toReturn.findViewById(android.R.id.text1)).setText(curRule.replace("%40", "@"));
			((TextView)toReturn.findViewById(android.R.id.text2)).setText(types[0].toLowerCase());
		}
		return toReturn;
	}
	
	private String[] loadKeywords() {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		String prefName = Long.toString(AccountService.getCurrentAccount().getId()) + "_muting";
		return Utilities.jsonToArray(mContext, prefs.getString(prefName, "")).toArray(new String[0]);
	}
	
	public boolean add(String term) {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		String prefName = Long.toString(AccountService.getCurrentAccount().getId()) + "_muting";
		ArrayList<String> cols = Utilities.jsonToArray(mContext, prefs.getString(prefName, ""));
		if(!cols.contains(term)) {
			cols.add(term);
			prefs.edit().putString(prefName, Utilities.arrayToJson(mContext, cols)).commit();
			notifyDataSetChanged();
			return true;
		}
		return false;
	}
	
	public void remove(int[] indicies) {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		String prefName = Long.toString(AccountService.getCurrentAccount().getId()) + "_muting";
		ArrayList<String> cols = Utilities.jsonToArray(mContext, prefs.getString(prefName, ""));
		for (int i : indicies) cols.remove(i);
		prefs.edit().putString(prefName, Utilities.arrayToJson(mContext, cols)).commit();
	}
}