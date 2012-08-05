package com.teamboid.twitter.widgets;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import com.handlerexploit.prime.ImageManager;
import com.teamboid.twitter.R;
import com.teamboid.twitter.listadapters.FeedListAdapter;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.NetworkUtils;
import com.teamboid.twitter.utilities.Utilities;
import com.teamboid.twitterapi.status.GeoLocation;
import com.teamboid.twitterapi.status.Place;
import com.teamboid.twitterapi.status.Status;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RelativeLayout;
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
			mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		}

		private Context _context;
		private ArrayList<Status> _items;
		private int mAppWidgetId;

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
			}

			rv.setImageViewBitmap(R.id.feedItemProfilePic, downloadImage(status.getUser().getProfileImageUrl()));
			if(PreferenceManager.getDefaultSharedPreferences(_context).getBoolean("show_real_names", false)) {
				rv.setTextViewText(R.id.feedItemUserName, status.getUser().getName());
			} else rv.setTextViewText(R.id.feedItemUserName, status.getUser().getScreenName());
			rv.setTextViewText(R.id.feedItemText, status.getText());
			rv.setTextViewText(R.id.feedItemTimerTxt, Utilities.friendlyTimeHourMinute(status.getCreatedAt()));

//			boolean hasMedia = false;
//			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//			if(prefs.getBoolean("enable_media_download", true)) {
//				final String media = Utilities.getTweetYFrogTwitpicMedia(status);
//				if(media != null && !media.isEmpty()) {
//					hasMedia = true;
//					addRule(locFrame, R.id.feedItemMediaFrame, RelativeLayout.BELOW);
//					addRule(replyFrame, R.id.feedItemMediaFrame, RelativeLayout.BELOW);
//					mediaFrame.setVisibility(View.VISIBLE);
//					mediaPreview.setVisibility(View.GONE);
//					mediaIndic.setVisibility(View.VISIBLE);
//					if(prefs.getBoolean("enable_inline_previewing", true)) {
//						itemTxt.setMinHeight(Utilities.convertDpToPx(mContext, 35) +
//								Integer.parseInt(prefs.getString("font_size", "16")));
//						mediaProg.setVisibility(View.VISIBLE);
//						ImageManager download = ImageManager.getInstance(mContext);
//						download.get(media, new ImageManager.OnImageReceivedListener() {
//							@Override
//							public void onImageReceived(String source, Bitmap bitmap) {
//								mediaProg.setVisibility(View.GONE);
//								mediaPreview.setVisibility(View.VISIBLE);
//								mediaPreview.setImageBitmap(bitmap);
//							}
//						});
//					} else hideInlineMedia(toReturn);
//				} else hideInlineMedia(toReturn);
//			} else hideInlineMedia(toReturn);
//			if(Utilities.tweetContainsVideo(tweet)) {
//				videoIndic.setVisibility(View.VISIBLE);
//			} else videoIndic.setVisibility(View.GONE);
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
//			} else toReturn.findViewById(R.id.locationFrame).setVisibility(View.GONE);
//			if(tweet.isFavorited()) favoritedIndic.setVisibility(View.VISIBLE);
//			else favoritedIndic.setVisibility(View.GONE);
//			if(tweet.getInReplyToStatusId() > 0) {
//				replyFrame.setVisibility(View.VISIBLE);
//				replyIndic.setText(mContext.getString(R.string.in_reply_to) + " @" + tweet.getInReplyToScreenName());
//				if(tweet.getGeoLocation() != null || tweet.getPlace() != null) {
//					addRule(replyFrame, R.id.locationFrame, RelativeLayout.BELOW);
//				} else if(!hasMedia) addRule(replyFrame, R.id.feedItemText, RelativeLayout.BELOW);
//			} else replyFrame.setVisibility(View.GONE);
			
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
