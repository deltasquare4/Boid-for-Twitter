package com.teamboid.twitter.tweetwidgets;

import java.util.HashMap;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * Simplifies Host Code
 * @author kennydude
 *
 */
public class TweetWidgetHostHelper {
	
	private Boolean sLock = false;
	private ITweetWidgetService sService;
	public HashMap<String, IFoundWidget> ifw = new HashMap<String, IFoundWidget>();
	
	private ServiceConnection sc = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			sService = ITweetWidgetService.Stub.asInterface(service);
			try {
				sService.updateCallback(c);
				sLock.notify();
			} catch (Exception e) { }
			Log.d("twhh", "Set callbacks");
		}
		@Override
		public void onServiceDisconnected(ComponentName name) { }
	};
	
	private Callbacks c = new Callbacks();
	
	class Callbacks extends ITweetWidgetCallback.Stub {
		@Override
		public void updateWidget(long forTweetId, String forUrl, RemoteViews remoteView) throws RemoteException {
			Log.d("twhh", "Got a widget. Now showing");
			ifw.get(forTweetId + "-" + forUrl).displayWidget(remoteView);
		}
		@Override
		public void errorMessage(long forTweetId, String forUrl) throws RemoteException {
			ifw.get(forTweetId + "-" + forUrl).displayError();
		}
	}
	
	public void load(Context c){
		//synchronized (sServiceLock) {
			if (sService == null) {
				c.bindService(new Intent(c, TweetWidgetService.class), sc, Context.BIND_AUTO_CREATE);
				try {
					//synchronized (sLock) {
						sLock.wait();
					//}
				} catch (Exception e) { }
				Log.d("twhh", "Returning load()");
			}
        //}
	}
	
	public void stop(Context c){
		try{
			c.unbindService(sc);
		}catch(Exception e) { }
	}
	
	public interface IFoundWidget{
		void displayWidget(RemoteViews rv);
		void displayError();
	}
	
	// Note: If you're using your own Host, make sure to change this! :D
	public String CALLBACK_SERVICE_ACTION = "com.teamboid.twitter.ACTION_TWEET_WIDGET";
	
	Intent getIntent(String forUrl){
		Intent i = new Intent("com.android.ACTION_EMBED");
		i.setData(Uri.parse(forUrl));
		i.putExtra("callbackApp", CALLBACK_SERVICE_ACTION);
		return i;
	}
	
	public void findWidget(long forTweetId, String forUrl, Context c, IFoundWidget callback){
		Intent i = getIntent(forUrl);
		i.putExtra("widgetId", forTweetId);
		
		PackageManager pm = c.getPackageManager();
		List<ResolveInfo> ac = pm.queryIntentServices(i, 0);
		if(ac.size() == 0) return;
		try{
			i.setPackage(ac.get(0).serviceInfo.applicationInfo.packageName);
		}catch(NullPointerException e){}
		
		ifw.put(forTweetId + "-" + forUrl, callback);
		c.startService(i);
	}
	
	public String getWidgetName(String forUrl, Context c){
		Intent i = getIntent(forUrl);
		PackageManager pm = c.getPackageManager();
		List<ResolveInfo> ac = pm.queryIntentServices(i, 0);
		if(ac.size() == 0) return null;
		else return ac.get(0).loadLabel(pm).toString();
	}
}
