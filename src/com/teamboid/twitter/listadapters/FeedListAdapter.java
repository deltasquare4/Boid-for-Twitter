package com.teamboid.twitter.listadapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;

import com.handlerexploit.prime.ImageManager;
import com.handlerexploit.prime.RemoteImageView;
import com.teamboid.twitter.ProfileScreen;
import com.teamboid.twitter.R;
import com.teamboid.twitter.columns.MentionsFragment;
import com.teamboid.twitter.columns.TimelineFragment;
import com.teamboid.twitter.utilities.Utilities;
import com.teamboid.twitterapi.status.GeoLocation;
import com.teamboid.twitterapi.status.Place;
import com.teamboid.twitterapi.status.Status;

/**
 * The list adapter used for the lists that contain tweets, such as the timeline
 * column.
 * 
 * @author Aidan Follestad
 */
public class FeedListAdapter extends BaseAdapter {

	public static void ApplyFontSize(TextView in, Context c) {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(c);
		in.setTextSize(Float.parseFloat(prefs.getString("font_size", "14")));
	}

	public static void ApplyFontSize(TextView in, Context c, boolean scaleUp) {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(c);
		if (scaleUp)
			in.setTextSize(Float.parseFloat(prefs.getString("font_size", "15")) + 2);
		else
			in.setTextSize(Float.parseFloat(prefs.getString("font_size", "15")));
	}

	public static void addRule(View target, int relativeToId, int rule) {
		RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) target
				.getLayoutParams();
		p.addRule(rule, relativeToId);
		target.setLayoutParams(p);
	}

	public FeedListAdapter(Activity context, String id, long _account) {
		mContext = context;
		tweets = new ArrayList<Status>();
		ID = id;
		account = _account;
	}

	private ArrayList<Status> tweets;

	private Activity mContext;
	public String ID;
	private long lastViewedTweet;
	private int lastViewedTopMargin;
	public String user;
	public long account;
	
	public void setLastViewed(ListView list) {
		if (list == null)
			return;
		else if (getCount() == 0)
			return;
		Status t = (Status) getItem(list.getFirstVisiblePosition());
		if (t == null)
			return;
		lastViewedTweet = t.getId();
		View v = list.getChildAt(0);
		lastViewedTopMargin = (v == null) ? 0 : v.getTop();
		System.out.println("SetLastViewed(lastViewedID=" + lastViewedTweet + ", topMargin=" + lastViewedTopMargin + ") [" + this.ID + "]"); 
	}

	public void restoreLastViewed(ListView list) {
		if (list == null) {
			return;
		} else if (getCount() == 0) {
			return;
		}
		int index = find(lastViewedTweet);
		if (index > -1) {
			list.setSelectionFromTop(index, lastViewedTopMargin);
		}
		System.out.println("RestoreLastViewed(lastViewedID=" + lastViewedTweet + ", index=" + index + ", topMargin=" + lastViewedTopMargin + ") [" + this.ID + "]");
	}

	private boolean shouldFilter(Context context, Status tweet) {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		if (prefs.getBoolean("enable_muting", false)) {
			if(this.ID != null) {
				boolean filteringDisabled = false;
				if(this.ID.equals(TimelineFragment.ID)) {
					filteringDisabled = (prefs.getBoolean("mute_timeline_enabled", true) == false);
				} else if(this.ID.equals(MentionsFragment.ID)) {
					filteringDisabled = (prefs.getBoolean("mute_mentions_enabled", false) == false);
				}
				if(filteringDisabled) return false;
			}
		} else return false;
		
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
				if (tweet.getUser().getScreenName().toLowerCase()
						.equals(query.substring(1).toLowerCase())) {
					return true;
				}
				if (tweet.isRetweet()) {
					tweet = tweet.getRetweetedStatus();
					if (tweet.getUser().getScreenName().toLowerCase()
							.equals(query.substring(1).toLowerCase())) {
						return true;
					}
				}
			} else if (types[2].equals(type)) {
				if (tweet.getSourcePlain().toLowerCase().equals(query.toLowerCase())) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean add(Status tweet, boolean inverted) {
		if (shouldFilter(mContext, tweet)) {
			return false;
		}
		if (!update(tweet)) {
			tweets.add(findAppropIndex(tweet, inverted), tweet);
			return true;
		} else {
			return false;
		}
	}

	public boolean add(Status tweet) {
		return add(tweet, false);
	}

	public int add(Status[] toAdd) {
		int toReturn = 0;
		for (Status tweet : toAdd) {
			if (add(tweet))
				toReturn++;
		}
		notifyDataSetChanged();
		return toReturn;
	}

	public void remove(int index) {
		tweets.remove(index);
		notifyDataSetChanged();
	}

	public void remove(long tweetId) {
		int index = 0;
		for (Status t : tweets) {
			if (t.getId() == tweetId) {
				tweets.remove(index);
				notifyDataSetChanged();
				break;
			}
			index++;
		}
	}

	public void clear() {
		tweets.clear();
		notifyDataSetChanged();
	}

	public void filter() {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		if (prefs.getBoolean("enable_muting", false)) {
			if(this.ID != null) {
				boolean filteringDisabled = false;
				if(this.ID.equals(TimelineFragment.ID)) {
					filteringDisabled = (prefs.getBoolean("mute_timeline_enabled", true) == false);
				} else if(this.ID.equals(MentionsFragment.ID)) {
					filteringDisabled = (prefs.getBoolean("mute_mentions_enabled", false) == false);
				}
				if(filteringDisabled) return;
			}
		} else return;
		
		for (int i = 0; i < tweets.size(); i++) {
			if (shouldFilter(mContext, tweets.get(i))) {
				tweets.remove(i);
			}
		}
		notifyDataSetChanged();
	}

	public int find(long statusId) {
		int index = -1;
		ArrayList<Status> temp = tweets;
		for (int i = 0; i < temp.size(); i++) {
			if (temp.get(i).getId() == statusId) {
				index = i;
				break;
			}
		}
		return index;
	}

	public Status[] toArray() {
		ArrayList<Status> toReturn = new ArrayList<Status>();
		for (Status t : tweets)
			toReturn.add(t);
		return toReturn.toArray(new Status[0]);
	}

	public boolean update(Status toFind) {
		boolean found = false;
		for (int i = 0; i < tweets.size(); i++) {
			if (tweets.get(i).getId() == toFind.getId()) {
				found = true;
				tweets.set(i, toFind);
				notifyDataSetChanged();
				break;
			}
		}
		return found;
	}

	private int findAppropIndex(Status tweet, boolean invert) {
		int toReturn = 0;
		for (Status t : tweets) {
			if (invert && t.getCreatedAt().after(tweet.getCreatedAt()))
				break;
			else if (!invert && t.getCreatedAt().before(tweet.getCreatedAt()))
				break;
			toReturn++;
		}
		return toReturn;
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
	public long getItemId(int position) {
		if ((position == 0 && tweets.size() == 0) || position > tweets.size())
			return 0;
		else if (position == -1 && tweets.size() == 1)
			return tweets.get(0).getId();
		return tweets.get(position).getId();
	}

	public static RelativeLayout createStatusView(Status tweet,
			final Context mContext, View convertView) {
		RelativeLayout toReturn = null;
		if (convertView != null)
			toReturn = (RelativeLayout) convertView;
		else
			toReturn = (RelativeLayout) LayoutInflater.from(mContext).inflate(
					R.layout.feed_item, null);

		TextView indicatorTxt = (TextView) toReturn
				.findViewById(R.id.feedItemRetweetIndicatorTxt);
		TextView userNameTxt = (TextView) toReturn
				.findViewById(R.id.feedItemUserName);
		TextView timerTxt = (TextView) toReturn
				.findViewById(R.id.feedItemTimerTxt);
		TextView itemTxt = (TextView) toReturn.findViewById(R.id.feedItemText);
		TextView locIndicator = (TextView) toReturn
				.findViewById(R.id.locationIndicTxt);
		TextView replyIndic = (TextView) toReturn
				.findViewById(R.id.inReplyIndicTxt);
		final ImageView mediaPreview = (ImageView) toReturn
				.findViewById(R.id.feedItemMediaPreview);
		ImageView rtIndic = (ImageView) toReturn
				.findViewById(R.id.feedItemRetweetIndicatorImg);
		ImageView mediaIndic = (ImageView) toReturn
				.findViewById(R.id.feedItemMediaIndicator);
		ImageView favoritedIndic = (ImageView) toReturn
				.findViewById(R.id.feedItemFavoritedIndicator);
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
		ApplyFontSize(itemTxt, mContext);
		ApplyFontSize(userNameTxt, mContext);

		if (tweet.isRetweet()) {
			indicatorTxt.setText("@" + tweet.getUser().getScreenName());
			tweet = tweet.getRetweetedStatus();
			rtIndic.setVisibility(View.VISIBLE);
			addRule(userNameTxt, R.id.feedItemRetweetIndicatorTxt,
					RelativeLayout.BELOW);
			indicatorTxt.setVisibility(View.VISIBLE);
		} else {
			rtIndic.setVisibility(View.GONE);
			addRule(userNameTxt, 0, RelativeLayout.BELOW);
			indicatorTxt.setVisibility(View.GONE);
		}
		if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(
				"show_real_names", false)) {
			userNameTxt.setText(tweet.getUser().getName());
		} else
			userNameTxt.setText("@" + tweet.getUser().getScreenName());
		if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(
				"enable_profileimg_download", true)) {
			profilePic.setImageResource(R.drawable.sillouette);
			profilePic.setImageURL(Utilities.getUserImage(tweet.getUser()
					.getScreenName(), mContext));
			final Status fTweet = tweet;
			profilePic.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					mContext.startActivity(new Intent(mContext
							.getApplicationContext(), ProfileScreen.class)
							.putExtra("screen_name",
									fTweet.getUser().getScreenName()).addFlags(
									Intent.FLAG_ACTIVITY_CLEAR_TOP));
				}
			});
		} else
			profilePic.setVisibility(View.GONE);
		itemTxt.setText(Utilities.twitterifyText(mContext, tweet));
		itemTxt.setLinksClickable(false);
		timerTxt.setText(Utilities.friendlyTimeShort(tweet.getCreatedAt()));
		boolean hasMedia = false;
		if (prefs.getBoolean("enable_media_download", true)) {
			final String media = Utilities.getTweetYFrogTwitpicMedia(tweet);
			if (media != null && !media.isEmpty()) {
				hasMedia = true;
				addRule(locFrame, R.id.feedItemMediaFrame, RelativeLayout.BELOW);
				addRule(replyFrame, R.id.feedItemMediaFrame,
						RelativeLayout.BELOW);
				mediaFrame.setVisibility(View.VISIBLE);
				mediaPreview.setVisibility(View.GONE);
				mediaIndic.setVisibility(View.VISIBLE);
				if (prefs.getBoolean("enable_inline_previewing", true)) {
					itemTxt.setMinHeight(Utilities.convertDpToPx(mContext, 35)
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
				addRule(locFrame, R.id.feedItemText, RelativeLayout.BELOW);
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
		if (tweet.isFavorited())
			favoritedIndic.setVisibility(View.VISIBLE);
		else
			favoritedIndic.setVisibility(View.GONE);
		if (tweet.getInReplyToStatusId() > 0) {
			replyFrame.setVisibility(View.VISIBLE);
			replyIndic.setText(mContext.getString(R.string.in_reply_to)
					.replace("{user}", tweet.getInReplyToScreenName()));
			if (tweet.getGeoLocation() != null || tweet.getPlace() != null) {
				addRule(replyFrame, R.id.locationFrame, RelativeLayout.BELOW);
			} else if (!hasMedia)
				addRule(replyFrame, R.id.feedItemText, RelativeLayout.BELOW);
		} else
			replyFrame.setVisibility(View.GONE);
		return toReturn;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		return createStatusView(tweets.get(position), mContext, convertView);
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
}