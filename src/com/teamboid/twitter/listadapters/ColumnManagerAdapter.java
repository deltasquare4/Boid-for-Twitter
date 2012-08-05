package com.teamboid.twitter.listadapters;

import java.util.ArrayList;

import com.teamboid.twitter.R;
import com.teamboid.twitter.columns.FavoritesFragment;
import com.teamboid.twitter.columns.MediaTimelineFragment;
import com.teamboid.twitter.columns.MentionsFragment;
import com.teamboid.twitter.columns.MessagesFragment;
import com.teamboid.twitter.columns.MyListsFragment;
import com.teamboid.twitter.columns.NearbyFragment;
import com.teamboid.twitter.columns.ProfileTimelineFragment;
import com.teamboid.twitter.columns.SavedSearchFragment;
import com.teamboid.twitter.columns.TimelineFragment;
import com.teamboid.twitter.columns.TrendsFragment;
import com.teamboid.twitter.columns.UserListFragment;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ColumnManagerAdapter extends BaseAdapter {

	public ColumnManagerAdapter(Activity context) {
		_context = context;
	}
	
	private Activity _context;
	
	public ArrayList<String> getColumns() {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_context);
		return Utilities.jsonToArray(prefs.getString(Long.toString(
                AccountService.getCurrentAccount().getId()) + "_columns", ""));
	}
	public void setColumns(ArrayList<String> cols) {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_context);
		prefs.edit().putString(Long.toString(AccountService.getCurrentAccount().getId()) +
                "_columns", Utilities.arrayToJson(cols)).commit();
		notifyDataSetChanged();
	}
	
	public void addColumn(String id, int index) {
		ArrayList<String> cols = getColumns();
		if(index > -1) cols.add(index, id);
		else cols.add(id);
		setColumns(cols);
	}
	
	public void removeColumn(int index) {
		//final String prefName = Long.toString(AccountService.getCurrentAccount().getId()) + "_default_column";
		ArrayList<String> toSet = getColumns();
		toSet.remove(index);
		setColumns(toSet);
	}
	
	public void resetColumns() {
		ArrayList<String> cols = new ArrayList<String>();
		cols.add(TimelineFragment.ID);
        cols.add(MentionsFragment.ID);
        cols.add(MessagesFragment.ID);
        cols.add(TrendsFragment.ID);
		setColumns(cols);
	}
	
	public void moveColumn(int from, int to) {
		String toMove = getColumns().get(from);
		removeColumn(from);
		addColumn(toMove, to);
	}
	
	@Override
	public int getCount() {
		return getColumns().size();
	}

	@Override
	public Object getItem(int position) {
		return getColumns().get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		RelativeLayout toReturn = null;
		if(convertView == null) {
			toReturn = (RelativeLayout)LayoutInflater.from(_context).inflate(R.layout.drag_list_item, null);
		} else toReturn = (RelativeLayout)convertView;
		
		String c = getColumns().get(position);
		Drawable image = null;
		if(c.equals(TimelineFragment.ID)) {
			c = _context.getString(R.string.timeline_str);
			image = _context.getTheme().obtainStyledAttributes(new int[] { R.attr.timelineTab }).getDrawable(0);
		} else if(c.equals(MentionsFragment.ID)) {
			c = _context.getString(R.string.mentions_str);
			image = _context.getTheme().obtainStyledAttributes(new int[] { R.attr.mentionsTab }).getDrawable(0);
		} else if(c.equals(MessagesFragment.ID)) {
			c = _context.getString(R.string.messages_str);
			image = _context.getTheme().obtainStyledAttributes(new int[] { R.attr.messagesTab }).getDrawable(0);
		} else if(c.equals(TrendsFragment.ID)) {
			c = _context.getString(R.string.trends_str);
			image = _context.getTheme().obtainStyledAttributes(new int[] { R.attr.trendsTab }).getDrawable(0);
		} else if(c.equals(FavoritesFragment.ID)) {
			c = _context.getString(R.string.favorites_str);
			image = _context.getTheme().obtainStyledAttributes(new int[] { R.attr.favoritesTab }).getDrawable(0);
		} else if(c.startsWith(SavedSearchFragment.ID + "@")) {
			c = c.substring(SavedSearchFragment.ID.length() + 1).replace("%40", "@");
			image = _context.getTheme().obtainStyledAttributes(new int[] { R.attr.savedSearchTab }).getDrawable(0);
		} else if(c.startsWith(UserListFragment.ID + "@")) {
			c = c.substring(UserListFragment.ID.length() + 1);
			c = c.substring(0, c.indexOf("@")).replace("%40", "@");
			image = _context.getTheme().obtainStyledAttributes(new int[] { R.attr.userListTab }).getDrawable(0);
		} else if(c.equals(NearbyFragment.ID)) {
			c = _context.getString(R.string.nearby_str);
			image = _context.getTheme().obtainStyledAttributes(new int[] { R.attr.nearbyTab }).getDrawable(0);
		} else if(c.equals(MediaTimelineFragment.ID)) {
			c = _context.getString(R.string.media_timeline_str);
			image = _context.getTheme().obtainStyledAttributes(new int[] { R.attr.mediaTab }).getDrawable(0);
		} else if(c.equals(MyListsFragment.ID)) {
			c = _context.getString(R.string.my_lists_str);
			image = _context.getTheme().obtainStyledAttributes(new int[] { R.attr.userListTab }).getDrawable(0);
		} else if(c.startsWith(ProfileTimelineFragment.ID + "@")) {
			c = "@" + c.substring(ProfileTimelineFragment.ID.length() + 1);
			image = _context.getTheme().obtainStyledAttributes(new int[] { R.attr.userFeedTab }).getDrawable(0);
		} else c = "Invalid Column";
		
		if(image != null) {
			((ImageView)toReturn.findViewById(R.id.columnImage)).setImageDrawable(image);
		}
		((TextView)toReturn.findViewById(R.id.text)).setText(c);
		return toReturn;
	}
}
