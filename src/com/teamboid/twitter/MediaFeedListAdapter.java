package com.teamboid.twitter;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import twitter4j.Status;

import java.util.ArrayList;

import com.handlerexploit.prime.widgets.RemoteImageView;

/**
 * The list adapter used for the media timeline tab, displays tiles of pictures.
 * @author Aidan Follestad
 */
public class MediaFeedListAdapter extends BaseAdapter {

	public MediaFeedListAdapter(Activity context, String id) {
		mContext = context;
		tweets = new ArrayList<Status>();
		ID = id;
	}

	public ArrayList<Status> tweets;

	private Activity mContext;
	public String ID;
	public int selectedItem = -1;
	public long account;
	private long lastViewedTweet;

	public void setLastViewed(GridView list) {
		if(list == null) return;
		else if(getCount() == 0) return;
		Status t = (Status)getItem(list.getFirstVisiblePosition());
		if(t == null) return;
		lastViewedTweet = t.getId();
	}
	public void restoreLastViewed(GridView list) {
		if(lastViewedTweet == 0 || list == null) return;
		else if(getCount() == 0) return;
		list.setSelection(find(lastViewedTweet));
	}

	private boolean add(Status tweet, String[] filter) {
		boolean added = false;
		int index = findAppropIndex(tweet, false);
		if(!update(tweet)) {
			if(filter != null) {
				boolean found = false;
				for(String mute : filter) {
					if(tweet.getText().toLowerCase().contains(mute.toLowerCase())) {
						found = true;
						break;
					}
				}
				if(found) return false;
			}
			tweets.add(index, tweet);
			added = true;
			notifyDataSetChanged();
		}
		return added;
	}
	public int add(Status[] toAdd) { return add(toAdd, false); }
	public int add(Status[] toAdd, boolean filter) {
		int toReturn = 0;
		String[] fi = null;
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		String prefName = Long.toString(AccountService.getCurrentAccount().getId()) + "_muting";
		fi = Utilities.jsonToArray(mContext, prefs.getString(prefName, "")).toArray(new String[0]);
		for(Status tweet : toAdd) {
			if(Utilities.getTweetYFrogTwitpicMedia(tweet) != null) {
				if(add(tweet, fi)) toReturn++;
			}
		}
		return toReturn;
	}
	public void remove(int index) {
		tweets.remove(index);
		notifyDataSetChanged();
	}
	public void remove(long tweetId) {
		int index = 0;
		for(Status t : tweets) {
			if(t.getId() == tweetId) {
				tweets.remove(index);
				break;
			}
			index++;
		}
		notifyDataSetChanged();
	}
	public void filter(String[] keywords) {
		for(int i = 0; i < tweets.size(); i++) {
			for(String mute : keywords) {
				if(tweets.get(i).getText().toLowerCase().contains(mute.toLowerCase())) {
					tweets.remove(i);
					if(i > 0) i--;
				}
			}
		}
		notifyDataSetChanged();
	}
	public void clear() {
		tweets.clear();
		notifyDataSetChanged();
	}
	public int find(long statusId) {
		int index = 0;
		ArrayList<Status> temp = tweets;
		for(int i = 0; i < temp.size(); i++) {
			if(temp.get(i).getId() == statusId) {
				index = i;
				break;
			}
		}
		return index;
	}

	public Status[] toArray() {
		ArrayList<Status> toReturn = new ArrayList<Status>();
		for(Status t : tweets) toReturn.add(t);
		return toReturn.toArray(new Status[0]);
	}

	private boolean update(Status toFind) {
		boolean found = false;
		for(int i = 0; i < tweets.size(); i++) {
			if(tweets.get(i).getId() == toFind.getId()) {
				found = true;
				tweets.set(i, toFind);
				break;
			}
		}
		return found;
	}

	private int findAppropIndex(Status tweet, boolean invert) {
		int toReturn = 0;
		for(Status t : tweets) {
			if(invert && t.getCreatedAt().after(tweet.getCreatedAt())) break;
			else if(!invert && t.getCreatedAt().before(tweet.getCreatedAt())) break;
			toReturn++;
		}
		return toReturn;
	}

	@Override
	public int getCount() { return tweets.size(); }
	@Override
	public Object getItem(int position) { return tweets.get(position); }
	@Override
	public long getItemId(int position) {
		if((position == 0 && tweets.size() == 0) || position > tweets.size()) return 0;
		else if(position == -1 && tweets.size() == 1) return tweets.get(0).getId();
		return tweets.get(position).getId();
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) { 
		RelativeLayout toReturn = null;
		if(convertView != null) toReturn = (RelativeLayout)convertView;
		else toReturn = (RelativeLayout)LayoutInflater.from(mContext).inflate(R.layout.media_list_item, null);
		RemoteImageView img = (RemoteImageView)toReturn.findViewById(R.id.mediaItemImage);
		img.setImageURL(Utilities.getTweetYFrogTwitpicMedia(tweets.get(position)));
		return toReturn;
	}
}