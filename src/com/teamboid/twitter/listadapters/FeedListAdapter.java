package com.teamboid.twitter.listadapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.ImageView.ScaleType;

import java.util.ArrayList;

import com.handlerexploit.prime.RemoteImageView;
import com.teamboid.twitter.Account;
import com.teamboid.twitter.ProfileScreen;
import com.teamboid.twitter.R;
import com.teamboid.twitter.TabsAdapter.BoidAdapter;
import com.teamboid.twitter.columns.MentionsFragment;
import com.teamboid.twitter.columns.TimelineFragment;
import com.teamboid.twitter.utilities.BoidActivity;
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
public class FeedListAdapter extends BoidAdapter<Status> implements Filterable {
	public FeedListAdapter(Context context, String id, Account acc) {
		super(context, id, acc);
		this.ID = id;
	}

	public String ID;

	@Override
	public long getItemId(int position) {
		return getItem(position).getId();
	}

	@Override
	public int getPosition(long id) {
		for(int i = 0; i <= this.getCount(); i++){
			if(getItem(i).getId() == id){
				return i;
			}
		}
		return -1;
	}
	
	Filter f;
	public Filter getFilter(){
		if(f == null) f = new TweetFilter();
		return f;
	}
	
	/**
	 * Filters Tweets (Apply muting etc)
	 * @author kennydude
	 *
	 */
	private class TweetFilter extends Filter{

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();
			results.count = 0;
			
			// Check if we are going to filter
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
			if (prefs.getBoolean("enable_muting", false)) {
				if(ID != null) {
					boolean filteringDisabled = false;
					if(ID.equals(TimelineFragment.ID)) {
						filteringDisabled = (prefs.getBoolean("mute_timeline_enabled", true) == false);
					} else if(ID.equals(MentionsFragment.ID)) {
						filteringDisabled = (prefs.getBoolean("mute_mentions_enabled", false) == false);
					}
					if(filteringDisabled) return results;
				}
			} else return results;
			
			// If we haven't returned then we must filter ourselves
			final String[] filter = Utilities.getMuteFilters(getContext());
			if (filter == null || filter.length == 0) {
				return results;
			}
			
			// This will contain the IDs of the tweets we need to filter out
			ArrayList<Integer> stripResults = new ArrayList<Integer>();
			
			final String[] types = getContext().getResources().getStringArray(
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
				
				// Now we go through each tweet
				for(int i = 0; i <= getCount(); i++){
					Status tweet = getItem(i);

					if (types[0].equals(type)) {
						if (tweet.getText().toString().toLowerCase()
								.contains(query.toLowerCase())) {
							stripResults.add(i);
						}
					} else if (types[1].equals(type)) {
						if (tweet.getUser().getScreenName().toLowerCase()
								.equals(query.substring(1).toLowerCase())) {
							stripResults.add(i);
						}
						if (tweet.isRetweet()) {
							tweet = tweet.getRetweetedStatus();
							if (tweet.getUser().getScreenName().toLowerCase()
									.equals(query.substring(1).toLowerCase())) {
								stripResults.add(i);
							}
						}
					} else if (types[2].equals(type)) {
						if (tweet.getSourcePlain().toLowerCase().equals(query.toLowerCase())) {
							stripResults.add(i);
						}
					}
					
				}
			}
			
			return results;
		}

		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			if(results.count != 0){
				// Now delete
				
				@SuppressWarnings("unchecked")
				ArrayList<Integer> r = (ArrayList<Integer>) results.values;
				for(Integer i : r){
					remove(getItem(i));
				}
			}
		}
		
	}
	
	// View stuff here {
	
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
		final RemoteImageView mediaPreview = (RemoteImageView) toReturn
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
			profilePic.setScaleType(ScaleType.FIT_XY);
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
				
				if (prefs.getBoolean("enable_inline_previewing", true)) {
					
					addRule(locFrame, R.id.feedItemMediaFrame, RelativeLayout.BELOW);
					addRule(replyFrame, R.id.feedItemMediaFrame,
							RelativeLayout.BELOW);
					mediaFrame.setVisibility(View.VISIBLE);
					mediaPreview.setVisibility(View.VISIBLE);
					mediaIndic.setVisibility(View.VISIBLE);
				
					itemTxt.setMinHeight(Utilities.convertDpToPx(mContext, 35)
							+ Integer.parseInt(prefs.getString("font_size",
									"16")));
					mediaProg.setVisibility(View.VISIBLE);
					mediaPreview.setScaleType(ScaleType.CENTER_CROP);
					// ImageManager download = ImageManager.getInstance(mContext);
					mediaPreview.onImageFinished = new BoidActivity.OnAction() {
						
						@Override
						public void done() {
							mediaProg.setVisibility(View.GONE);
						}
					};
					mediaPreview.setImageURL(media);
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
		return createStatusView( getItem(position), getContext(), convertView);
	}

	private static void hideInlineMedia(View toReturn) {
		ProgressBar mediaProg = (ProgressBar) toReturn
				.findViewById(R.id.feedItemMediaProgress);
		View mediaFrame = toReturn.findViewById(R.id.feedItemMediaFrame);
		RemoteImageView mediaPreview = (RemoteImageView) toReturn
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
	
	// } end of views
	
	public boolean update(Status toFind) {
		boolean found = false;
		for (int i = 0; i < getCount(); i++) {
			if (getItem(i).getId() == toFind.getId()) {
				found = true;
				insert(toFind, i);
				notifyDataSetChanged();
				break;
			}
		}
		return found;
	}
}