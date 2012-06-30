package com.teamboid.twitter;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class PushReceiver extends BroadcastReceiver {
	public static final String SENDER_EMAIL = "push@boidapp.com";
	public static final String SERVER = "http://192.168.0.9:1337";
	
	public static class PushWorker extends Service{
		
		@Override
		public IBinder onBind(Intent arg0) {
			return null;
		}
		
		@Override
		public int onStartCommand(final Intent intent, int flags, int startId) {
			if(intent.hasExtra("reg")){
			new Thread(new Runnable(){

				@Override
				public void run() {
					try{
						DefaultHttpClient dhc = new DefaultHttpClient();
						HttpGet g = new HttpGet(SERVER + "/register?userid="+AccountService.getCurrentAccount().getId()+
								"&token=" + intent.getStringExtra("reg"));
						HttpResponse r = dhc.execute(g);
						if(r.getStatusLine().getStatusCode() == 200){
							Log.d("push", "REGISTERED");
						}
					}catch(Exception e){ e.printStackTrace(); }
				}
				
			}).start();
			} else if(intent.hasExtra("hm")){
				new Thread(new Runnable(){
					@Override
					public void run() {
						Bundle b = intent.getBundleExtra("hm");
						try{
							JSONObject status = new JSONObject(b.getString("tweet"));
							/* note: Twitter4J hack again ;) */
							twitter4j.Status s = new twitter4j.internal.json.StatusJSONImpl(status);
							MultiAPIMethods.ShowNotification(s, PushWorker.this);
						} catch(Exception e) {
							e.printStackTrace();
						}
					}				
				}).start();
			}
			return Service.START_STICKY;
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("boidpush", "Got a push message");
		if (intent.getAction().equals("com.google.android.c2dm.intent.REGISTRATION")) {
			handleRegistration(context, intent);
		} else if (intent.getAction().equals("com.google.android.c2dm.intent.RECEIVE")) {
			handleMessage(context, intent);
		}
	}

	private void handleMessage(Context context, Intent intent) {
		Log.d("boidpush", "Got a message :D");
		context.startService(new Intent(context, PushWorker.class).putExtra("hm", intent.getExtras()));
	}

	private void handleRegistration(Context context, Intent intent) {
		Log.d("boidpush", "REGISTER");
		String registration = intent.getStringExtra("registration_id"); 
	    if (intent.getStringExtra("error") != null) {
	        // Registration failed, should try again later.
	    } else if (intent.getStringExtra("unregistered") != null) {
	        // unregistration done, new messages from the authorized sender will be rejected
	    } else if (registration != null) {
	    	// Send the registration ID to the 3rd party site that is sending the messages.
	    	// This should be done in a separate thread.
	    	// When done, remember that all registration is done. 
	    	Log.d("boidpush", "c2dm worked! :D");
	    	context.startService(new Intent(context, PushWorker.class).putExtra("reg", registration));
	    }
	}

}
