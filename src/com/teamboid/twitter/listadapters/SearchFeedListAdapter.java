package com.teamboid.twitter.listadapters;

import java.util.ArrayList;

import com.handlerexploit.prime.ImageManager;
import com.handlerexploit.prime.RemoteImageView;
import com.teamboid.twitter.ProfileScreen;
import com.teamboid.twitter.R;
import com.teamboid.twitter.TabsAdapter.BoidAdapter;
import com.teamboid.twitter.utilities.Utilities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.teamboid.twitterapi.search.Tweet;
import com.teamboid.twitterapi.status.GeoLocation;
import com.teamboid.twitterapi.status.Place;

/**
 * The list adapter used in activites that search for tweets.
 * 
 * @author Aidan Follestad
 */
public class SearchFeedListAdapter extends BoidAdapter<Tweet> {

	public SearchFeedListAdapter(Context context, long _account, String query ) {
		super(context, null, null);
		mContext = context;
		tweets = new ArrayList<Tweet>();
		account = _account;
		_query = query;
	}

	public SearchFeedListAdapter(Context context, String _id, long _account, String query) {
		super(context, null, null);
		mContext = context;
		tweets = new ArrayList<Tweet>();
		ID = _id;
		account = _account;
		_query = query;
	}

	private Context mContext;
	private ArrayList<Tweet> tweets;
	public String ID;
	public ListView list;
	private long lastViewedTweet;
	private int lastViewedTopMargin;
	public long account;
	public String _query;
	

	public void setLastViewed(ListView list) {
		if (list == null)
			return;
		else if (getCount() == 0)
			return;
		Tweet t = (Tweet) getItem(list.getFirstVisiblePosition());
		if (t == null)
			return;
		lastViewedTweet = t.getId();
		View v = list.getChildAt(0);
		lastViewedTopMargin = (v == null) ? 0 : v.getTop();
	}

	public void restoreLastViewed(ListView list) {
		if (list == null)
			return;
		else if (getCount() == 0)
			return;
		list.setSelectionFromTop(find(lastViewedTweet), lastViewedTopMargin);
	}

	private boolean shouldFilter(Context context, Tweet tweet) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		if ((prefs.getBoolean("enable_muting", false) == false) || (prefs.getBoolean("mute_search_enabled", false) == false)) {
			return false;
		}
		
		final String[] filter = Utilities.getMuteFilters(context);
		if (filter == null || filter.length == 0) {
			return false;
		}
		final String[] types = context.getResources().getStringArray(
				R.array.muting_types);
		for (String rule : filter) {

			String query = null;
			String type = null;
			if (rule.contains("@")) {
				if (rule.endsWith("@" + types[1])) {
					query = rule.substring(0, rule.indexOf("@"));
					type = types[1];
				} else {
					query = rule.substring(0, rule.indexOf("@"));
					type = types[2];
				}
			} else {
				query = rule;
				type = types[0];
			}
			query = query.replace("%40", "@");

			if (types[0].equals(type)) {
				if (tweet.getText().toString().toLowerCase()
						.contains(query.toLowerCase())) {
					return true;
				}
			} else if (types[1].equals(type)) {
				if (tweet.getFromUser().toLowerCase()
						.equals(query.substring(1).toLowerCase())) {
					return true;
				}
				/*
				 * Equivelent to use of isRetweet in FeedListAdapter, filter if
				 * it's a retweet of a muted user.
				 */
				if (tweet.getText().startsWith(
						"RT " + tweet.getFromUser() + ":")) {
					return true;
				}
			} else if (types[2].equals(type)) {
				if (tweet.getSourcePlain().toLowerCase().equals(query.toLowerCase())) {
					return true;
				}
			}
		}
		return false;
	}

	public void add(Tweet tweet) {
		if (shouldFilter(mContext, tweet)) {
			return;
		}
		boolean added = false;
		if (!update(tweet)) {
			tweets.add(findAppropIndex(tweet), tweet);
			added = true;
		}
		notifyDataSetChanged();
		return;
	}

	public int add(Tweet[] toAdd) {
		int before = tweets.size();
		int added = 0;
		for (Tweet tweet : toAdd) {
			add(tweet);
			added++;
		}
		if (before == 0)
			return added;
		else if (added == before)
			return 0;
		else
			return (tweets.size() - before);
	}

	public void remove(int index) {
		tweets.remove(index);
		notifyDataSetChanged();
	}

	public void clear() {
		tweets.clear();
		notifyDataSetChanged();
	}

	public Tweet[] toArray() {
		return tweets.toArray(new Tweet[0]);
	}

	public Boolean update(Tweet toFind) {
		Boolean found = false;
		for (int i = 0; i < tweets.size(); i++) {
			if (tweets.get(i).getId() == toFind.getId()) {
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
		for (Tweet t : itemCache) {
			if (t.getCreatedAt().before(tweet.getCreatedAt()))
				break;
			toReturn++;
		}
		return toReturn;
	}

	public void filter() {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		if ((prefs.getBoolean("enable_muting", false) == false) || (prefs.getBoolean("mute_search_enabled", false) == false)) {
			return;
		}

		for (int i = 0; i < tweets.size(); i++) {
			if (shouldFilter(mContext, tweets.get(i))) {
				tweets.remove(i);
			}
		}
		notifyDataSetChanged();
	}

	public int find(long statusId) {
		int index = -1;
		ArrayList<Tweet> temp = tweets;
		for (int i = 0; i < temp.size(); i++) {
			if (temp.get(i).getId() == statusId) {
				index = i;
				break;
			}
		}
		return index;
	}

	@Override
	public int getCount() {
		return tweets.size();
	}

	@Override
	public Tweet getItem(int position) {
		return tweets.get(position);
	}

	@Override
	public long getItemId(int position) {
		if ((position == 0 && tweets.size() == 0) || position > tweets.size())
			return 0;
		else if (position == -1 && tweets.size() == 1)
			return tweets.get(0).getId();
		return tweets.get(position).getId();
	}

	public static RelativeLayout createTweetView(final Tweet tweet,
			final Context mContext, View convertView, String query) {
		RelativeLayout toReturn = null;
		if (convertView != null)
			toReturn = (RelativeLayout) convertView;
		else
			toReturn = (RelativeLayout) LayoutInflater.from(mContext).inflate(
					R.layout.feed_item, null);

		TextView userNameTxt = (TextView) toReturn
				.findViewById(R.id.feedItemUserName);
		TextView timerTxt = (TextView) toReturn
				.findViewById(R.id.feedItemTimerTxt);
		TextView itemTxt = (TextView) toReturn.findViewById(R.id.feedItemText);
		TextView locIndicator = (TextView) toReturn
				.findViewById(R.id.locationIndicTxt);
		final ImageView mediaPreview = (ImageView) toReturn
				.findViewById(R.id.feedItemMediaPreview);
		ImageView mediaIndic = (ImageView) toReturn
				.findViewById(R.id.feedItemMediaIndicator);
		ImageView videoIndic = (ImageView) toReturn
				.findViewById(R.id.feedItemVideoIndicator);
		RemoteImageView profilePic = (RemoteImageView) toReturn
				.findViewById(R.id.feedItemProfilePic);
		final ProgressBar mediaProg = (ProgressBar) toReturn
				.findViewById(R.id.feedItemMediaProgress);
		View replyFrame = toReturn.findViewById(R.id.inReplyToFrame);
		View mediaFrame = toReturn.findViewById(R.id.feedItemMediaFrame);
		View locFrame = toReturn.findViewById(R.id.locationFrame);
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		FeedListAdapter.ApplyFontSize(itemTxt, mContext);
		FeedListAdapter.ApplyFontSize(userNameTxt, mContext);
		userNameTxt.setText("@" + tweet.getFromUser());
		if (prefs.getBoolean("enable_profileimg_download", true)) {
			profilePic.setImageResource(R.drawable.sillouette);
			profilePic.setImageURL(Utilities.getUserImage(tweet.getFromUser(),
					mContext));
			final Tweet fTweet = tweet;
			profilePic.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					mContext.startActivity(new Intent(mContext
							.getApplicationContext(), ProfileScreen.class)
							.putExtra("screen_name", fTweet.getFromUser())
							.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				}
			});
		} else
			profilePic.setVisibility(View.GONE);
		itemTxt.setText(Utilities.twitterifyText(mContext, tweet.getText(),
				tweet.getUrlEntities(), tweet.getMediaEntities(), false, query));
		itemTxt.setLinksClickable(false);
		timerTxt.setText(Utilities.friendlyTimeShort(tweet.getCreatedAt()));
		boolean hasMedia = false;
		if (prefs.getBoolean("enable_media_download", true)) {
			final String media = Utilities.getTweetYFrogTwitpicMedia(tweet);
			if (media != null && !media.isEmpty()) {
				hasMedia = true;
				FeedListAdapter.addRule(locFrame, R.id.feedItemMediaFrame,
						RelativeLayout.BELOW);
				FeedListAdapter.addRule(replyFrame, R.id.feedItemMediaFrame,
						RelativeLayout.BELOW);
				mediaFrame.setVisibility(View.VISIBLE);
				mediaPreview.setVisibility(View.GONE);
				mediaIndic.setVisibility(View.VISIBLE);
				if (prefs.getBoolean("enable_inline_previewing", true)) {
					itemTxt.setMinHeight(Utilities.convertDpToPx(mContext, 30)
							+ Integer.parseInt(prefs.getString("font_size",
									"16")));
					mediaProg.setVisibility(View.VISIBLE);
					ImageManager download = ImageManager.getInstance(mContext);
					download.get(media,
							new ImageManager.OnImageReceivedListener() {
								@Override
								public void onImageReceived(String source,
										Bitmap bitmap) {
									mediaProg.setVisibility(View.GONE);
									mediaPreview.setVisibility(View.VISIBLE);
									mediaPreview.setImageBitmap(bitmap);
								}
							});
				} else
					hideInlineMedia(toReturn);
			} else
				hideInlineMedia(toReturn);
		} else
			hideInlineMedia(toReturn);
		if (Utilities.tweetContainsVideo(tweet)) {
			videoIndic.setVisibility(View.VISIBLE);
		} else
			videoIndic.setVisibility(View.GONE);
		if (tweet.getGeoLocation() != null || tweet.getPlace() != null) {
			if (!hasMedia)
				FeedListAdapter.addRule(locFrame, R.id.feedItemText,
						RelativeLayout.BELOW);
			locFrame.setVisibility(View.VISIBLE);
			if (tweet.getPlace() != null) {
				Place p = tweet.getPlace();
				locIndicator.setText(p.getFullName());
			} else {
				GeoLocation g = tweet.getGeoLocation();
				locIndicator.setText(g.toString());
			}
		} else
			toReturn.findViewById(R.id.locationFrame).setVisibility(View.GONE);
		return toReturn;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		return createTweetView(tweets.get(position), mContext, convertView, _query);
	}

	private static void hideInlineMedia(View toReturn) {
		ProgressBar mediaProg = (ProgressBar) toReturn
				.findViewById(R.id.feedItemMediaProgress);
		View mediaFrame = toReturn.findViewById(R.id.feedItemMediaFrame);
		ImageView mediaPreview = (ImageView) toReturn
				.findViewById(R.id.feedItemMediaPreview);
		ImageView mediaIndic = (ImageView) toReturn
				.findViewById(R.id.feedItemMediaIndicator);
		TextView itemTxt = (TextView) toReturn.findViewById(R.id.feedItemText);
		itemTxt.setMinHeight(0);
		mediaIndic.setVisibility(View.GONE);
		mediaFrame.setVisibility(View.GONE);
		mediaProg.setVisibility(View.GONE);
		mediaPreview.setVisibility(View.GONE);
		mediaPreview.setImageBitmap(null);
	}

	@Override
	public int getPosition(long id) {
		for(int i = 0; i <= this.getCount()-1; i++){
			if(getItem(i).getId() == id){
				return i;
			}
		}
		return -1;
	}
}