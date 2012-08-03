package com.teamboid.twitter.views;

import java.util.ArrayList;

import com.teamboid.twitter.R;
import com.teamboid.twitter.listadapters.FeedListAdapter;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitterapi.status.Status;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
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
			Log.d("WIDGET VIEW FACTORY", "Constructor");
		}
		
		private Context _context;
		private ArrayList<Status> _items;
		private int mAppWidgetId;
		
		@Override
		public int getCount() {
			Log.d("WIDGET VIEW FACTORY", "getCount() - " + _items.size());
			return _items.size();
		}

		@Override
		public long getItemId(int position) { return _items.get(position).getId(); }

		@Override
		public RemoteViews getLoadingView() {
			Log.d("WIDGET VIEW FACTORY", "getLoadingView()");
			return null;
		}

		@Override
		public RemoteViews getViewAt(int position) {
			Log.d("WIDGET VIEW FACTORY", "getViewAt(" + position + ")");
			Status status = _items.get(position);
			RemoteViews rv = new RemoteViews(_context.getPackageName(), R.layout.widget_feed_item);
            rv.setTextViewText(R.id.feedItemUserName, status.getUser().getScreenName());
            rv.setTextViewText(R.id.feedItemText, status.getText());

//            Bundle extras = new Bundle();
//            extras.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
//            extras.putInt("numberToToast", position);
//            Intent fillInIntent = new Intent();
//            fillInIntent.putExtras(extras);
            //rv.setOnClickFillInIntent(R.id.widget_item, fillInIntent);
            
            return rv;
		}

		@Override
		public int getViewTypeCount() {
			Log.d("WIDGET VIEW FACTORY", "getViewTypeCount()");
			return 1;
		}

		@Override
		public boolean hasStableIds() { return true; }

		@Override
		public void onCreate() {
			Log.d("WIDGET VIEW FACTORY", "onCreate");
			if(AccountService.getCurrentAccount() == null) return;
			FeedListAdapter adapt = AccountService.getTimelineFeedAdapter(AccountService.getCurrentAccount().getId());
			if(adapt == null) return;
			Status[] feed = adapt.toArray();
			for(Status item : feed) _items.add(item);
		}

		@Override
		public void onDataSetChanged() {
			Log.d("WIDGET VIEW FACTORY", "onDataSetChanged()");
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
