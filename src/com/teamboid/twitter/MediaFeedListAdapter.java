package com.teamboid.twitter;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;

import com.handlerexploit.prime.utils.ImageManager;
import com.teamboid.twitter.TabsAdapter.BaseGridFragment;

/**
 * The list adapter used for the media timeline tab, displays tiles of pictures.
 * @author Aidan Follestad
 */
public class MediaFeedListAdapter extends BaseAdapter {
	
	public static class MediaFeedItem{
		public String imgurl = "";
		public long tweet_id = -1;
		public String twicsy_id = "";
	}

	public MediaFeedListAdapter(Activity context, String id, long _account) {
		mContext = context;
		tweets = new ArrayList<MediaFeedItem>();
		ID = id;
		account = _account;
	}

	public ArrayList<MediaFeedItem> tweets;
	public MediaFeedItem get(int pos){ return tweets.get(pos); }

	private Activity mContext;
	public String ID;
	public int selectedItem = -1;
	public long account;
	private int lastViewedTweet;

	public void setLastViewed(GridView list) {
		if(list == null) return;
		else if(getCount() == 0) return;
		lastViewedTweet = list.getFirstVisiblePosition();
	}
	
	public void restoreLastViewed(GridView list) {
		if(lastViewedTweet == 0 || list == null) return;
		else if(getCount() == 0) return;
		list.setSelection(lastViewedTweet);
	}

	/*
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
	*/
	
	public int add(MediaFeedItem[] toAdd, BaseGridFragment frag) {
		int toReturn = 0;
		for(MediaFeedItem tweet : toAdd) {
			tweets.add(tweet);
			if(frag != null) frag.setListShown(true);
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
	public int getCount() { return tweets.size(); }
	@Override
	public Object getItem(int position) { return tweets.get(position); }

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) { 
		RelativeLayout toReturn = null;
		if(convertView != null) toReturn = (RelativeLayout)convertView;
		else toReturn = (RelativeLayout)LayoutInflater.from(mContext).inflate(R.layout.media_list_item, null);
		final ImageView img = (ImageView)toReturn.findViewById(R.id.mediaItemImage);
		final ProgressBar prog = (ProgressBar)toReturn.findViewById(R.id.mediaItemProgress);
		img.setVisibility(View.GONE);
		prog.setVisibility(View.VISIBLE);
		ImageManager downloader = ImageManager.getInstance(mContext);
		downloader.get(tweets.get(position).imgurl, new ImageManager.OnImageReceivedListener() {
			@Override
			public void onImageReceived(String arg0, Bitmap image) {
				prog.setVisibility(View.GONE);
				img.setImageBitmap(image);
				img.setVisibility(View.VISIBLE);
			}
		});
		return toReturn;
	}

	@Override
	public long getItemId(int arg0) {
		return arg0;
	}
}