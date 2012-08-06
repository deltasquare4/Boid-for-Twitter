package com.teamboid.twitter.widgets;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import com.teamboid.twitter.R;
import com.teamboid.twitter.TweetViewer;
import com.teamboid.twitter.listadapters.FeedListAdapter;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;
import com.teamboid.twitterapi.status.Status;
import com.teamboid.twitterapi.utilities.Utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

public class TimelineWidgetViewService extends RemoteViewsService {

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {		
		return new WidgetRemoteViewFactory(getApplicationContext(), intent);
	}

	public class WidgetRemoteViewFactory implements RemoteViewsService.RemoteViewsFactory {

		public WidgetRemoteViewFactory(Context context, Intent intent) {
			_context = context;
			_items = new ArrayList<Status>();
			//mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		}

		private Context _context;
		private ArrayList<Status> _items;
		//private int mAppWidgetId;

		@Override
		public int getCount() { return _items.size(); }

		@Override
		public long getItemId(int position) { return position; }

		@Override
		public RemoteViews getLoadingView() { return null; }

		private Bitmap downloadImage(String imageUrl) {
			try {
				HttpURLConnection conn= (HttpURLConnection)new URL(imageUrl).openConnection();
				conn.setDoInput(true);
				conn.connect();
				Bitmap toReturn = BitmapFactory.decodeStream(conn.getInputStream());
				conn.disconnect();
				return toReturn;
			} catch (Exception e) { e.printStackTrace(); }
			return null;
		}

		@Override
		public RemoteViews getViewAt(int position) {
			Status status = _items.get(position);
			final RemoteViews rv = new RemoteViews(_context.getPackageName(), R.layout.widget_feed_item);
			if(status.isRetweet()) {
				rv.setViewVisibility(R.id.feedItemRetweetIndicatorImg, View.VISIBLE);
				rv.setViewVisibility(R.id.feedItemRetweetIndicatorTxt, View.VISIBLE);
				rv.setTextViewText(R.id.feedItemRetweetIndicatorTxt, "@" + status.getUser().getScreenName());
				status = status.getRetweetedStatus();
			} else {
				rv.setViewVisibility(R.id.feedItemRetweetIndicatorImg, View.GONE);
				rv.setViewVisibility(R.id.feedItemRetweetIndicatorTxt, View.GONE);
			}
			
			Intent itemClickIntent = new Intent(getApplicationContext(), TweetViewer.class)
				.putExtra("sr_tweet", Utils.serializeObject(status))
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			rv.setOnClickFillInIntent(R.id.feedItemRelativeLayout, itemClickIntent);

			rv.setImageViewBitmap(R.id.feedItemProfilePic, downloadImage(status.getUser().getProfileImageUrl()));
			if(PreferenceManager.getDefaultSharedPreferences(_context).getBoolean("show_real_names", false)) {
				rv.setTextViewText(R.id.feedItemUserName, status.getUser().getName());
			} else rv.setTextViewText(R.id.feedItemUserName, status.getUser().getScreenName());
			rv.setTextViewText(R.id.feedItemTimerTxt, Utilities.friendlyTimeHourMinute(status.getCreatedAt()));
			rv.setTextViewText(R.id.feedItemText, Utilities.twitterifyText(_context, status));
			
			final String media = Utilities.getTweetYFrogTwitpicMedia(status);
			if(media != null && !media.isEmpty()) { 
				rv.setViewVisibility(R.id.feedItemMediaIndicator, View.VISIBLE);
			} else rv.setViewVisibility(R.id.feedItemMediaIndicator, View.GONE);
			if(Utilities.tweetContainsVideo(status)) {
				rv.setViewVisibility(R.id.feedItemVideoIndicator, View.VISIBLE);
			} else rv.setViewVisibility(R.id.feedItemVideoIndicator, View.GONE);
			
//			if(tweet.getGeoLocation() != null || tweet.getPlace() != null) {
//				if(!hasMedia) addRule(locFrame, R.id.feedItemText, RelativeLayout.BELOW);
//				locFrame.setVisibility(View.VISIBLE);
//				if(tweet.getPlace() != null) {
//					Place p = tweet.getPlace();
//					locIndicator.setText(p.getFullName());
//				} else {
//					GeoLocation g = tweet.getGeoLocation();
//					locIndicator.setText(g.toString());
//				}
//			}
			if(status.isFavorited()) {
				rv.setViewVisibility(R.id.feedItemFavoritedIndicator, View.VISIBLE);
			} else rv.setViewVisibility(R.id.feedItemFavoritedIndicator, View.GONE);
			if(status.getInReplyToStatusId() > 0) {
				rv.setViewVisibility(R.id.inReplyToFrame, View.VISIBLE);
				rv.setTextViewText(R.id.inReplyIndicTxt, status.getInReplyToScreenName());
//				if(tweet.getGeoLocation() != null || tweet.getPlace() != null) {
//					addRule(replyFrame, R.id.locationFrame, RelativeLayout.BELOW);
//				} else if(!hasMedia) addRule(replyFrame, R.id.feedItemText, RelativeLayout.BELOW);
			} else rv.setViewVisibility(R.id.inReplyToFrame, View.GONE);
			
			return rv;
		}

		@Override
		public int getViewTypeCount() { return 1; }

		@Override
		public boolean hasStableIds() { return true; }

		@Override
		public void onCreate() { }

		@Override
		public void onDataSetChanged() {
			if(AccountService.getCurrentAccount() == null) return;
			FeedListAdapter adapt = AccountService.getTimelineFeedAdapter(AccountService.getCurrentAccount().getId());
			if(adapt == null) return;
			Status[] feed = adapt.toArray();
			for(Status item : feed) _items.add(item);
		}

		@Override
		public void onDestroy() { }
	}
}
