package com.teamboid.twitter.tweetwidgets;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * A widget for Tweet Views
 * But it's very generic and is used to embed things anywhere really :D
 * @author kennydude
 */
public abstract class TweetWidget extends Service {
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	public void sendErrorView(Context c, Intent ix){
		Intent i = new Intent(ix.getStringExtra("callbackApp"));	
		i.putExtra("error", "true");
		i.putExtra("forUrl", ix.getData().toString());
		i.putExtra("widgetId",ix.getLongExtra("widgetId", 0));
		c.startService(i);
		Log.d("twrs", "Starting relay...");
	}
	
	public void sendRemoteViews(RemoteViews rv, Context c, Intent ix){
		Intent i = new Intent(ix.getStringExtra("callbackApp"));
		i.putExtra("rv", rv);
		i.putExtra("forUrl", ix.getData().toString());
		i.putExtra("widgetId",ix.getLongExtra("widgetId", 0));
		c.startService(i);
		Log.d("twrs", "Starting relay...");
	}
	
	public abstract void onReceive(Context cntxt, Intent intent);
	
	public void handleCommand(final Intent i){
		new Thread(new Runnable(){
			@Override
			public void run() {
				onReceive(TweetWidget.this, i);
				stopSelf();
			}
		}).start();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    handleCommand(intent);
	    return Service.START_STICKY;
	}
}
