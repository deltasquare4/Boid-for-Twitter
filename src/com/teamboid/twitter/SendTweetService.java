package com.teamboid.twitter;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.teamboid.twitter.SendTweetTask.Result;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

/**
 * Sends Tweets for us in the background
 * @author kennydude
 *
 */
public class SendTweetService extends Service {
	public static final String NETWORK_AVAIL = "com.teamboid.twitter.NETWORK_AVAIL";
	// public static final String ADD_TWEET = "com.teamboid.twitter.ADD_TWEET";
	public static final String UPDATE_STATUS = "com.teamboid.twitter.UPDATE_SENDTWEET_STATUS";
	public static final String REMOVE_TWEET = "com.teamboid.twitter.REMOVE_TWEET";
	public static final String LOAD_TWEETS = "com.teamboid.twitter.LOAD_TWEET";
	
	public List<SendTweetTask> tweets = new ArrayList<SendTweetTask>();
	
	public class SendTweetAsyncTask extends AsyncTask<Object,Object,Object>{

		@Override
		protected Object doInBackground(Object... arg0) {
			// Check if we can actually send data
			if(!NetworkUtils.haveNetworkConnection(SendTweetService.this)) return null;
			
			loadTweets();
			
			for(int i = 0; i < tweets.size(); i++) { //this prevents concurrent thread modification exceptions
				SendTweetTask stt = tweets.get(i);
				Log.d("sts", "Sending Tweet...");
				stt.result.errorCode = Result.WAITING;
				Intent update = new Intent(UPDATE_STATUS);
				sendBroadcast(update);
				
				if(stt.sendTweet(SendTweetService.this).sent == true) {
					tweets.remove(i);
				} else{
					tweets.set(i, stt);
				}
				
				Log.d("sts", "Updating Timeline screen....");
				// Intent update = new Intent(UPDATE_STATUS);
				//update.putExtra("tweet", stt.toBundle());
				sendBroadcast(update);
			}
			
			saveTweets();
			
			return null;
		}
		
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	private static final String TWEETS = "sendtweetservice-queue";
	
	SharedPreferences getPrefs(){
		return getSharedPreferences(TWEETS, Context.MODE_PRIVATE);
	}
	private static boolean loaded = false;
	
	void loadTweets(){
		if(loaded) return;
		SharedPreferences sp = getPrefs();
		for(String key : sp.getAll().keySet()){
			try{
				tweets.add(SendTweetTask.fromJSONObject(new JSONObject(sp.getString(key, "{}"))));
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		loaded = true;
		sp.edit().clear().commit();
	}
	
	void saveTweets(){
		Editor ed = getPrefs().edit();
	
		for(int i = 0; i < tweets.size(); i++) { //this prevents concurrent thread modification exceptions
			SendTweetTask stt = tweets.get(i);
			try{
				ed.putString(i + "", stt.toJSONObject().toString());
			} catch(Exception e){
				e.printStackTrace();
			}
		}
		ed.commit();
	}
	
	void startBackground(){
		try{
			new SendTweetAsyncTask().execute();
		} catch(Exception e){ e.printStackTrace(); }
	}
	
	static SendTweetService scs;
	static SendTweetService getInstance(){
		if(scs == null) synchronized(SendTweetService.class) { new SendTweetService(); }
		return scs;
	}
	
	public SendTweetService(){
		scs = this;
		Log.d("sts", "New instance.");
	}
	
	public static void addTweet(SendTweetTask stt){
		getInstance().tweets.add(stt);
		
		
    	
		getInstance().startBackground();
	}
	public static void initialize(){
		getInstance().loadTweets();
		Intent update = new Intent(UPDATE_STATUS);
    	getInstance().sendBroadcast(update);
	}
		
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent == null){
			return Service.START_STICKY;
		}
		
	    if(intent.getAction().equals(NETWORK_AVAIL)){
	    	// If there was no network at the time of send, we get notified here :)
	    	startBackground();
	    } else if(intent.getAction().equals(REMOVE_TWEET)){
	    	loadTweets();
	    	tweets.remove(intent.getIntExtra("tweet", 0));
	    	saveTweets();
	    	
	    	Intent update = new Intent(UPDATE_STATUS);
	    	update.putExtra("delete", true);
	    	sendBroadcast(update);
	    } else if(intent.getAction().equals(LOAD_TWEETS)){
	    	loadTweets();
	    	Intent update = new Intent(UPDATE_STATUS);
	    	update.putExtra("dontupdate", true);
	    	sendBroadcast(update);
	    	startBackground();
	    }
		
	    return Service.START_STICKY;
	}
	
}
