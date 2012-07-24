package com.teamboid.twitter.services;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.teamboid.twitter.R;
import com.teamboid.twitter.SendTweetTask;
import com.teamboid.twitter.SendTweetTask.Result;
import com.teamboid.twitter.columns.TimelineFragment;
import com.teamboid.twitter.utilities.NetworkUtils;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.IBinder;
import android.widget.Toast;

/**
 * Sends Tweets for us in the background
 * @author kennydude
 */
public class SendTweetService extends Service {
	
	public static final String NETWORK_AVAIL = "com.teamboid.twitter.NETWORK_AVAIL";
	public static final String UPDATE_STATUS = "com.teamboid.twitter.UPDATE_SENDTWEET_STATUS";
	public static final String LOAD_TWEETS = "com.teamboid.twitter.LOAD_TWEET";
	public List<SendTweetTask> tweets = new ArrayList<SendTweetTask>();
	
	public class SendTweetAsyncTask extends AsyncTask<Object,Object,Object> {
		@Override
		protected Object doInBackground(Object... arg0) {
			if(!NetworkUtils.haveNetworkConnection(SendTweetService.this)) return null;
			loadTweets();
			for(int i = 0; i < tweets.size(); i++) {
				final SendTweetTask stt = tweets.get(i);
				stt.result.errorCode = Result.WAITING;
				Intent update = new Intent(UPDATE_STATUS);
				sendBroadcast(update);
				if(stt.sendTweet(SendTweetService.this).sent == true) {
					try { 
						((Activity)AccountService.activity).runOnUiThread(new Runnable() {
							@Override
							public void run() {
								AccountService.getFeedAdapter((Activity) AccountService.activity, TimelineFragment.ID,
										AccountService.getCurrentAccount().getId()).add(new twitter4j.Status[]{stt.tweet});
							}						
						});
					} catch(Exception e) { e.printStackTrace(); }
					try {
						if(((Activity)AccountService.activity).hasWindowFocus()) {
							throw new Exception("Activity does not have focus");
						}
					} catch(Exception e) {
						Toast.makeText(SendTweetService.this, R.string.sent_tweet, Toast.LENGTH_SHORT).show();
					}
					tweets.remove(i);
				} else tweets.set(i, stt);
				sendBroadcast(update);
			}
			saveTweets();
			return null;
		}
	}
	
	@Override
	public IBinder onBind(Intent arg0) { return null; }
	
	private static final String TWEETS = "sendtweetservice-queue";
	
	private SharedPreferences getPrefs() {
		return getSharedPreferences(TWEETS, Context.MODE_PRIVATE);
	}
	private static boolean loaded = false;
	
	private void loadTweets() {
		if(loaded) return;
		SharedPreferences sp = getPrefs();
		for(String key : sp.getAll().keySet()) {
			try { tweets.add(SendTweetTask.fromJSONObject(new JSONObject(sp.getString(key, "{}")))); }
			catch(Exception e) { e.printStackTrace(); }
		}
		loaded = true;
		sp.edit().clear().commit();
	}
	
	private void saveTweets(){
		Editor ed = getPrefs().edit();
		for(int i = 0; i < tweets.size(); i++) {
			SendTweetTask stt = tweets.get(i);
			try { ed.putString(i + "", stt.toJSONObject().toString()); }
			catch(Exception e) { e.printStackTrace(); }
		}
		ed.commit();
	}
	
	private void startBackground() {
		try { new SendTweetAsyncTask().execute(); }
		catch(Exception e) { e.printStackTrace(); }
	}
	
	private static SendTweetService scs;
	public static SendTweetService getInstance() {
		if(scs == null) {
			synchronized(SendTweetService.class) { new SendTweetService(); }
		}
		return scs;
	}
	
	public SendTweetService() { scs = this; }
	
	public static void addTweet(SendTweetTask stt){
		getInstance().tweets.add(stt);
		getInstance().startBackground();
	}
	public static void initialize(){
		getInstance().loadTweets();
    	getInstance().sendBroadcast(new Intent(UPDATE_STATUS));
	}
	
	public static void removeTweet(SendTweetTask tweet){
		getInstance().loadTweets();
		getInstance().tweets.remove(tweet);
		getInstance().saveTweets();
    	Intent update = new Intent(UPDATE_STATUS);
    	update.putExtra("delete", true);
    	getInstance().sendBroadcast(update);
	}
		
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent == null) return Service.START_STICKY;
	    if(intent.getAction().equals(NETWORK_AVAIL)) {
	    	startBackground();
	    } else if(intent.getAction().equals(LOAD_TWEETS)) {
	    	loadTweets();
	    	Intent update = new Intent(UPDATE_STATUS);
	    	update.putExtra("dontupdate", true);
	    	sendBroadcast(update);
	    	startBackground();
	    }	
	    return Service.START_STICKY;
	}	
}
