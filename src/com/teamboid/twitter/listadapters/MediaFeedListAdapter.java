package com.teamboid.twitter.listadapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.ImageView.ScaleType;

import java.util.ArrayList;

import com.handlerexploit.prime.RemoteImageView;
import com.teamboid.twitter.R;
import com.teamboid.twitter.TabsAdapter.BaseGridFragment;

/**
 * The list adapter used for the media timeline tab, displays tiles of pictures.
 * 
 * @author Aidan Follestad
 */
public class MediaFeedListAdapter extends BaseAdapter {

	public static class MediaFeedItem {
		public String imgurl = "";
		public long tweet_id = -1;
		@Deprecated
		public String twicsy_id = "";
		public String content;
	}

	public MediaFeedListAdapter(Activity context, String id, long _account) {
		mContext = context;
		tweets = new ArrayList<MediaFeedItem>();
		ID = id;
		account = _account;
	}

	public ArrayList<MediaFeedItem> tweets;

	public MediaFeedItem get(int pos) {
		return tweets.get(pos);
	}

	private Activity mContext;
	public String ID;
	public int selectedItem = -1;
	public long account;
	private int lastViewedTweet;

	public void setLastViewed(GridView list) {
		if (list == null)
			return;
		else if (getCount() == 0)
			return;
		lastViewedTweet = list.getFirstVisiblePosition();
	}

	public void restoreLastViewed(GridView list) {
		if (lastViewedTweet == 0 || list == null)
			return;
		else if (getCount() == 0)
			return;
		list.setSelection(lastViewedTweet);
	}

	/*
	 * private boolean add(Status tweet, String[] filter) { boolean added =
	 * false; int index = findAppropIndex(tweet, false); if(!update(tweet)) {
	 * if(filter != null) { boolean found = false; for(String mute : filter) {
	 * if(tweet.getText().toLowerCase().contains(mute.toLowerCase())) { found =
	 * true; break; } } if(found) return false; } tweets.add(index, tweet);
	 * added = true; notifyDataSetChanged(); } return added; }
	 */

	public int add(MediaFeedItem[] toAdd, BaseGridFragment frag) {
		int toReturn = 0;
		for (MediaFeedItem tweet : toAdd) {
			tweets.add(tweet);
			if (frag != null)
				frag.setListShown(true);
			toReturn++;
		}
		return toReturn;
	}

	public void remove(int index) {
		tweets.remove(index);
		notifyDataSetChanged();
	}

	public void clear() {
		tweets.clear();
		notifyDataSetChanged();
	}

	public MediaFeedItem[] toArray() {
		return tweets.toArray(new MediaFeedItem[0]);
	}

	@Override
	public int getCount() {
		return tweets.size();
	}

	@Override
	public Object getItem(int position) {
		return tweets.get(position);
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		View toReturn = null;
		if (convertView != null)
			toReturn = convertView;
		else
			toReturn = LayoutInflater.from(mContext).inflate(
					R.layout.media_list_item, null);
		final RemoteImageView img = (RemoteImageView) toReturn
				.findViewById(R.id.mediaItemImage);
		final View prog = toReturn.findViewById(R.id.mediaItemProgress);
		
		img.setScaleType(ScaleType.CENTER_CROP);
		img.setImageURL(tweets.get(position).imgurl);
		return toReturn;
	}

	@Override
	public long getItemId(int arg0) {
		return arg0;
	}
}