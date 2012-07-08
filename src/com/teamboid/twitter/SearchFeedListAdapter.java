package com.teamboid.twitter;

import java.util.ArrayList;

import com.handlerexploit.prime.utils.ImageManager;
import com.handlerexploit.prime.widgets.RemoteImageView;

import twitter4j.GeoLocation;
import twitter4j.Place;
import twitter4j.Tweet;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * The list adapter used in activites that search for tweets.
 * @author Aidan Follestad
 */
public class SearchFeedListAdapter extends BaseAdapter {

	public SearchFeedListAdapter(Context context, long _account) {
		mContext = context;
		tweets = new ArrayList<Tweet>();
		account = _account;
	}
	public SearchFeedListAdapter(Context context, String _id, long _account) {
		mContext = context;
		tweets = new ArrayList<Tweet>();
		ID = _id;
		account = _account;
	}

	private Context mContext;
	private ArrayList<Tweet> tweets;
	public String ID;
	public ListView list;
	private long lastViewedTweet;
	private int lastViewedTopMargin;
	public long account;

	public void setLastViewed(ListView list) {
		if(list == null) return;
		else if(getCount() == 0) return;
		Tweet t = (Tweet)getItem(list.getFirstVisiblePosition());
		if(t == null) return;
		lastViewedTweet = t.getId();
		View v = list.getChildAt(0);
		lastViewedTopMargin = (v == null) ? 0 : v.getTop();
	}
	public void restoreLastViewed(ListView list) {
		if(lastViewedTweet == 0 || list == null) return;
		else if(getCount() == 0) return;
		list.setSelectionFromTop(find(lastViewedTweet), lastViewedTopMargin);
	}

	private boolean add(Tweet tweet) {
		boolean added = false;
		if(!update(tweet)) {
			tweets.add(findAppropIndex(tweet), tweet);
			added = true;
		}
		notifyDataSetChanged();
		return added;
	}
	public int add(Tweet[] toAdd) {
		int before = tweets.size();
		int added = 0;
		for(Tweet tweet : toAdd) {
			if(add(tweet)) added++;
		}
		if(before == 0) return 0;
		else if(added == before) return 0;
		else return (tweets.size() - before);
	}
	public void remove(int index) {
		tweets.remove(index);
		notifyDataSetChanged();
	}
	public void clear() {
		tweets.clear();
		notifyDataSetChanged();
	}

	public Tweet[] toArray() { return tweets.toArray(new Tweet[0]); }

	public Boolean update(Tweet toFind) {
		Boolean found = false;
		for(int i = 0; i < tweets.size(); i++) {
			if(tweets.get(i).getId() == toFind.getId()) {
				found = true;
				tweets.set(i, toFind);
				break;
			}
		}
		return found;
	}

	private int findAppropIndex(Tweet tweet) {
		int toReturn = 0;
		ArrayList<Tweet> itemCache = tweets;
		for(Tweet t : itemCache) {
			if(t.getCreatedAt().before(tweet.getCreatedAt())) break;
			toReturn++;
		}
		return toReturn;
	}

	public int find(long statusId) {
		int index = 0;
		ArrayList<Tweet> temp = tweets;
		for(int i = 0; i < temp.size(); i++) {
			if(temp.get(i).getId() == statusId) {
				index = i;
				break;
			}
		}
		return index;
	}

	
	@Override
	public int getCount() { return tweets.size(); }
	@Override
	public Object getItem(int position) {
		return tweets.get(position);
	}
	@Override
	public long getItemId(int position) {
		if((position == 0 && tweets.size() == 0) || position > tweets.size()) return 0;
		else if(position == -1 && tweets.size() == 1) return tweets.get(0).getId();
		return tweets.get(position).getId();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		RelativeLayout toReturn = null;
		if(convertView != null) toReturn = (RelativeLayout)convertView;
		else toReturn = (RelativeLayout)LayoutInflater.from(mContext).inflate(R.layout.feed_item, null);
		toReturn.findViewById(R.id.feedItemMediaFrame).setVisibility(View.GONE);
		final Tweet tweet = tweets.get(position);
		RemoteImageView profilePic = (RemoteImageView)toReturn.findViewById(R.id.feedItemProfilePic);
		if(PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("enable_profileimg_download", true)) {
			profilePic.setImageResource(R.drawable.sillouette);
			profilePic.setImageURL(Utilities.getUserImage(tweet.getFromUser(), mContext));
			profilePic.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) { 
					mContext.startActivity(new Intent(mContext, ProfileScreen.class).putExtra("screen_name", tweet.getFromUser()).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				}
			}); 
		} else{
			profilePic.setVisibility(View.GONE);
		}
		if(PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("show_real_names", false)) {
			((TextView)toReturn.findViewById(R.id.feedItemUserName)).setText(tweet.getFromUserName());
		} else ((TextView)toReturn.findViewById(R.id.feedItemUserName)).setText(tweet.getFromUser());
		TextView itemTxt = (TextView)toReturn.findViewById(R.id.feedItemText); 
		itemTxt.setText(Utilities.twitterifyText(mContext, tweet.getText(), tweet.getURLEntities(), tweet.getMediaEntities(), false));
		itemTxt.setLinksClickable(false);
		((TextView)toReturn.findViewById(R.id.feedItemTimerTxt)).setText(Utilities.friendlyTimeShort(tweet.getCreatedAt()));
		if(tweet.getPlace() != null || tweet.getGeoLocation() != null) {
			toReturn.findViewById(R.id.locationFrame).setVisibility(View.VISIBLE);
			if(tweet.getPlace() != null) {
				Place p = tweet.getPlace();
				((TextView)toReturn.findViewById(R.id.locationIndicTxt)).setText(p.getFullName());
			} else {
				GeoLocation g = tweet.getGeoLocation();
				((TextView)toReturn.findViewById(R.id.locationIndicTxt)).setText(Double.toString(g.getLatitude()) + ", " + Double.toString(g.getLongitude()));
			}
		} else toReturn.findViewById(R.id.locationFrame).setVisibility(View.GONE);
		final ImageView mediaPreview = (ImageView)toReturn.findViewById(R.id.feedItemMediaPreview);
		if(PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("enable_media_download", true)) {
			final String media = Utilities.getTweetYFrogTwitpicMedia(tweet);
			if(media != null && !media.isEmpty()) {
				toReturn.findViewById(R.id.feedItemMediaFrame).setVisibility(View.VISIBLE);
				final ProgressBar progress = (ProgressBar)toReturn.findViewById(R.id.feedItemMediaProgress);
				mediaPreview.setVisibility(View.GONE);
				toReturn.findViewById(R.id.feedItemMediaIndicator).setVisibility(View.VISIBLE);
				if(PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("enable_inline_previewing", true)) {
					progress.setVisibility(View.VISIBLE);
					ImageManager download = ImageManager.getInstance(mContext);
					download.get(media, new ImageManager.OnImageReceivedListener() {
						@Override
						public void onImageReceived(String source, Bitmap bitmap) {
							progress.setVisibility(View.GONE);
							mediaPreview.setVisibility(View.VISIBLE);
							mediaPreview.setImageBitmap(bitmap);
						}
					});
				} else {
					toReturn.findViewById(R.id.feedItemMediaFrame).setVisibility(View.GONE);
					toReturn.findViewById(R.id.feedItemMediaProgress).setVisibility(View.GONE);
					mediaPreview.setVisibility(View.GONE);
					mediaPreview.setImageBitmap(null);
				}
			} else {
				toReturn.findViewById(R.id.feedItemMediaIndicator).setVisibility(View.GONE);
				toReturn.findViewById(R.id.feedItemMediaFrame).setVisibility(View.GONE);
				toReturn.findViewById(R.id.feedItemMediaProgress).setVisibility(View.GONE);
				mediaPreview.setVisibility(View.GONE);
				mediaPreview.setImageBitmap(null);
			}
		} else {
			toReturn.findViewById(R.id.feedItemMediaIndicator).setVisibility(View.GONE);
			toReturn.findViewById(R.id.feedItemMediaFrame).setVisibility(View.GONE);
			toReturn.findViewById(R.id.feedItemMediaProgress).setVisibility(View.GONE);
			mediaPreview.setVisibility(View.GONE);
			mediaPreview.setImageBitmap(null);
		}
		if(Utilities.tweetContainsVideo(tweet)) {
			toReturn.findViewById(R.id.feedItemVideoIndicator).setVisibility(View.VISIBLE);
		} else toReturn.findViewById(R.id.feedItemVideoIndicator).setVisibility(View.GONE);
		return toReturn;
	}
}