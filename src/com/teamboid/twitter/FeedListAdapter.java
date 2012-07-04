package com.teamboid.twitter;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import twitter4j.GeoLocation;
import twitter4j.Place;
import twitter4j.Status;

import java.util.ArrayList;

import com.handlerexploit.prime.utils.ImageManager;
import com.handlerexploit.prime.widgets.RemoteImageView;

/**
 * The list adapter used for the lists that contain tweets, such as the timeline column.
 * @author Aidan Follestad
 */
public class FeedListAdapter extends BaseAdapter {

	public FeedListAdapter(Activity context, String id) {
		mContext = context;
		tweets = new ArrayList<Status>();
		ID = id;
	}

	private ArrayList<Status> tweets;

	private Activity mContext;
	public String ID;
	public int selectedItem = -1;
	private long lastViewedTweet;
	private int lastViewedTopMargin;
	public long account;
	public String user;

	public void setLastViewed(ListView list) {
		if(list == null) return;
		else if(getCount() == 0) return;
		Status t = (Status)getItem(list.getFirstVisiblePosition());
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

	private boolean add(Status tweet, String[] filter) {
		boolean added = false;
		int index = findAppropIndex(tweet, false);
		if(!update(tweet)) {
			if(filter != null) {
				boolean found = false;
				for(String mute : filter) {
					if(tweet.getText().toString().contains(mute.toLowerCase())) {
						found = true;
						break;
					}
				}
				if(found) return false;
			}
			tweets.add(index, tweet);
			added = true;
		}
		return added;
	}
	public boolean addInverted(Status tweet) {
		boolean added = false;
		int index = findAppropIndex(tweet, true);
		if(!update(tweet)) {
			tweets.add(index, tweet);
			added = true;
		}

		return added;
	}
	public int add(Status[] toAdd) { return add(toAdd, false); }
	public int add(Status[] toAdd, boolean filter) {
		int toReturn = 0;
		String[] fi = null;
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		String prefName = Long.toString(account) + "_muting";
		fi = Utilities.jsonToArray(mContext, prefs.getString(prefName, "")).toArray(new String[0]);
		for(Status tweet : toAdd) {
			if(add(tweet, fi)) toReturn++;
		}
		notifyDataSetChanged();
		return toReturn;
	}
	public int addInverted(Status[] toAdd) {
		int toReturn = 0;
		for(Status tweet : toAdd) {
			if(addInverted(tweet)) toReturn++;
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
				if(tweets.get(i).getText().toString().contains(mute.toLowerCase())) {
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
		else toReturn = (RelativeLayout)LayoutInflater.from(mContext).inflate(R.layout.feed_item, null);
		Status tweet = tweets.get(position);
		TextView indicatorTxt = (TextView)toReturn.findViewById(R.id.feedItemRetweetIndicatorTxt);
		TextView userNameTxt = (TextView)toReturn.findViewById(R.id.feedItemUserName);
		if(tweet.isRetweet()) {
			Spannable rtSpan = new SpannableString("RT by @" + tweet.getUser().getScreenName());
			rtSpan.setSpan(new NoUnderlineClickableSpan() {
				@Override
				public void onClick(View arg0) { }
			}, 0, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			tweet = tweet.getRetweetedStatus();			
			((ImageView)toReturn.findViewById(R.id.feedItemRetweetIndicatorImg)).setVisibility(View.VISIBLE);
			RelativeLayout.LayoutParams userNameParams = (RelativeLayout.LayoutParams)userNameTxt.getLayoutParams();
			userNameParams.addRule(RelativeLayout.BELOW, R.id.feedItemRetweetIndicatorTxt);
			userNameTxt.setLayoutParams(userNameParams);
			indicatorTxt.setText(rtSpan);
			indicatorTxt.setVisibility(View.VISIBLE);
		} else {
			((ImageView)toReturn.findViewById(R.id.feedItemRetweetIndicatorImg)).setVisibility(View.GONE);
			RelativeLayout.LayoutParams userNameParams = (RelativeLayout.LayoutParams)userNameTxt.getLayoutParams();
			userNameParams.addRule(RelativeLayout.BELOW, 0);
			userNameTxt.setLayoutParams(userNameParams);
			indicatorTxt.setVisibility(View.GONE);
		}
		
		final RemoteImageView profilePic = (RemoteImageView)toReturn.findViewById(R.id.feedItemProfilePic);
		profilePic.setImageResource( R.drawable.silouette );
		String url = Utilities.getUserImage(tweet.getUser(), mContext);
		profilePic.setImageURL(url);
		if(PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("show_real_names", false)) {
			userNameTxt.setText(tweet.getUser().getName());
		} else userNameTxt.setText(tweet.getUser().getScreenName());
		final Status fTweet = tweet;
		profilePic.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mContext.startActivity(new Intent(mContext.getApplicationContext(), ProfileScreen.class)
					.putExtra("screen_name", fTweet.getUser().getScreenName()).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
			}
		});
		TextView itemTxt = (TextView)toReturn.findViewById(R.id.feedItemText); 
		itemTxt.setText(Utilities.twitterifyText(mContext, tweet.getText(), tweet.getURLEntities(), tweet.getMediaEntities(), false));
		itemTxt.setLinksClickable(false);
		((TextView)toReturn.findViewById(R.id.feedItemTimerTxt)).setText(Utilities.friendlyTimeShort(tweet.getCreatedAt()));
		final ImageView mediaPreview = (ImageView)toReturn.findViewById(R.id.feedItemMediaPreview);
		String media = Utilities.getTweetYFrogTwitpicMedia(tweet);
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
		if(Utilities.tweetContainsVideo(tweet)) {
			toReturn.findViewById(R.id.feedItemVideoIndicator).setVisibility(View.VISIBLE);
		} else toReturn.findViewById(R.id.feedItemVideoIndicator).setVisibility(View.GONE);
		if(tweet.getGeoLocation() != null || tweet.getPlace() != null) {
			toReturn.findViewById(R.id.locationFrame).setVisibility(View.VISIBLE);
			if(tweet.getPlace() != null) {
				Place p = tweet.getPlace();
				((TextView)toReturn.findViewById(R.id.locationIndicTxt)).setText(p.getFullName());
			} else {
				GeoLocation g = tweet.getGeoLocation();
				((TextView)toReturn.findViewById(R.id.locationIndicTxt)).setText(Double.toString(g.getLatitude()) + ", " + Double.toString(g.getLongitude()));
			}
		} else toReturn.findViewById(R.id.locationFrame).setVisibility(View.GONE);
		if(tweet.isFavorited()) ((ImageView)toReturn.findViewById(R.id.feedItemFavoritedIndicator)).setVisibility(View.VISIBLE);
		else ((ImageView)toReturn.findViewById(R.id.feedItemFavoritedIndicator)).setVisibility(View.GONE);
		RelativeLayout replyFrame = (RelativeLayout)toReturn.findViewById(R.id.inReplyToFrame);
		if(tweet.getInReplyToStatusId() > 0) {
			replyFrame.setVisibility(View.VISIBLE);
			((TextView)toReturn.findViewById(R.id.inReplyIndicTxt)).setText(mContext.getString(R.string.in_reply_to) + " @" + tweet.getInReplyToScreenName());
			if(tweet.getGeoLocation() != null || tweet.getPlace() != null) {
				RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)replyFrame.getLayoutParams();
				params.addRule(RelativeLayout.BELOW, R.id.locationFrame);
				replyFrame.setLayoutParams(params);
			}
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)toReturn.findViewById(R.id.feedItemMediaFrame).getLayoutParams();
			params.addRule(RelativeLayout.BELOW, R.id.inReplyToFrame);
			toReturn.findViewById(R.id.feedItemMediaFrame).setLayoutParams(params);
		} else replyFrame.setVisibility(View.GONE);
		return toReturn;
	}
}