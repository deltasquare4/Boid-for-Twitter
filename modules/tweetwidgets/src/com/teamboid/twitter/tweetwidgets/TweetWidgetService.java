package com.teamboid.twitter.tweetwidgets;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.RemoteViews;

/**
 * This is a service which manages interaction between
 * other apps using Tweet Widgets and our TweetViewer :D
 * @author kennydude
 */
public class TweetWidgetService extends Service {
	
public static final ITweetWidgetService.Stub binder = new ITweetWidgetService.Stub(){
		ITweetWidgetCallback itwc;
		
		@Override
		public void updateWidget(long forTweetId, String forUrl,
				RemoteViews remoteView) throws RemoteException {
			itwc.updateWidget(forTweetId, forUrl, remoteView);
		}

		@Override
		public void updateCallback(ITweetWidgetCallback callback)
				throws RemoteException{
			itwc = callback;
		}

		@Override
		public void sendError(long forTweetId, String forUrl)
				throws RemoteException {
			itwc.errorMessage(forTweetId, forUrl);
		}
		
	};
	
	@Override
	public IBinder onBind(Intent arg0) {
		return binder;
	}
	
	public void handleCommand(Intent i){
		if(i != null && i.hasExtra("widgetId")) {
			try {
				if(i.hasExtra("rv")){
					binder.updateWidget(i.getLongExtra("widgetId", 0), i.getStringExtra("forUrl"), (RemoteViews)i.getParcelableExtra("rv"));
				} else binder.sendError(i.getLongExtra("widgetId", 0), i.getStringExtra("forUrl"));
			} catch (RemoteException e) { e.printStackTrace(); }
		}
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    handleCommand(intent);
	    return Service.START_STICKY;
	}
}
