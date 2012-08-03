package com.teamboid.twitter.views;

import java.util.ArrayList;

import com.teamboid.twitter.R;
import com.teamboid.twitter.listadapters.FeedListAdapter;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;
import com.teamboid.twitterapi.status.Status;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

public class WidgetRemoteViewService extends RemoteViewsService {

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
            
            if(PreferenceManager.getDefaultSharedPreferences(_context).getBoolean("show_real_names", false)) {
				rv.setTextViewText(R.id.feedItemUserName, status.getUser().getName());
			} else rv.setTextViewText(R.id.feedItemUserName, status.getUser().getScreenName());
            rv.setTextViewText(R.id.feedItemText, status.getText());
            rv.setTextViewText(R.id.feedItemTimerTxt, Utilities.friendlyTimeShort(status.getCreatedAt()));
            
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
		public void onDestroy() { _items.clear(); }
	}
}
