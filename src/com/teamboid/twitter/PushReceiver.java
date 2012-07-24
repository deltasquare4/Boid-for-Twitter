package com.teamboid.twitter;

import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import com.teamboid.twitter.columns.MentionsFragment;
import com.teamboid.twitter.compat.Api11;
import com.teamboid.twitter.listadapters.FeedListAdapter;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.NightModeUtils;

public class PushReceiver extends BroadcastReceiver {
	
	public static final String SENDER_EMAIL = "107821281305";
	public static int pushForId = 0;
	// In release builds this should be nodester, but I might change it back to my local IP for 
	// changes ;)
	// public static final String SERVER = "http://192.168.0.9:1337";
	public static final String SERVER = "http://boid.nodester.com";
	
	public static class PushWorker extends Service{
		
		@Override
		public IBinder onBind(Intent arg0) { return null; }
		
		private static final String ENCRYPTION_KEY = "boidisalovelyappandijustlovehavingencrpytiontoworkwithnodester...../.khnihi";
		
		@Override
		public int onStartCommand(final Intent intent, int flags, int startId) {
			if(intent.hasExtra("reg")){
				final Intent i = new Intent("com.teamboid.twitter.PUSH_PROGRESS");
				i.putExtra("progress", 500);
				sendBroadcast(i);
				new Thread(new Runnable(){
					@Override
					public void run() {
						try {
							Account acc = AccountService.getAccount(pushForId);
							
							// Build
							JSONObject jo = new JSONObject();
							jo.put("userid", acc.getId());
							jo.put("accesstoken", acc.getToken());
							jo.put("token", intent.getStringExtra("reg"));
							jo.put("accesssecret", acc.getSecret());
							
							// Encrypt
							byte[] input = jo.toString().getBytes("utf-8");
							MessageDigest md = MessageDigest.getInstance("MD5");
							byte[] thedigest = md.digest(ENCRYPTION_KEY.getBytes("UTF-8"));
							SecretKeySpec skc = new SecretKeySpec(thedigest, "AES/ECB/PKCS5Padding");
							Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
							cipher.init(Cipher.ENCRYPT_MODE, skc);
							
							byte[] cipherText = new byte[cipher.getOutputSize(input.length)];
						    int ctLength = cipher.update(input, 0, input.length, cipherText, 0);
						    ctLength += cipher.doFinal(cipherText, ctLength);
						    String query = Base64.encodeToString(cipherText, Base64.DEFAULT);
						    i.putExtra("progress", 700);
							sendBroadcast(i);
							
							DefaultHttpClient dhc = new DefaultHttpClient();
							HttpPost p = new HttpPost(SERVER + "/register");
							p.setEntity( new StringEntity(query) );
							HttpResponse r = dhc.execute(p);
							
							if(r.getStatusLine().getStatusCode() == 200){
								Log.d("push", "REGISTERED");
								i.putExtra("progress", 1000);
								sendBroadcast(i);
							} else{ throw new Exception("NON 200 RESPONSE"); }
							
						} catch(Exception e) { 
							e.printStackTrace();
							i.putExtra("progress", 1000);
							i.putExtra("error", true);
							sendBroadcast(i);
						}
						settingUp = false;
					}
				}).start();
			} else if(intent.hasExtra("hm")) {
				final Bundle b = intent.getBundleExtra("hm");
				
				if(NightModeUtils.isNightMode(this)){
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
					if(prefs.getBoolean("night_mode_pause_notifications", false) == true){
						stopSelf();
						return Service.START_NOT_STICKY;
					}
				}
				
				try {
					String type = b.getString("type");
					Integer accId = 0;
					try { accId = Integer.parseInt(b.getString("account")); }
					catch(Exception e) { }
					if(type.equals("reply")) {
						JSONObject status = new JSONObject(b.getString("tweet"));
						status.put("id", Long.parseLong(status.getString("id_str")));
						final twitter4j.Status s = new twitter4j.internal.json.StatusJSONImpl(status);
						//TODO The account the mention is for should be passed from the server too
						// We have this now in "account" as a string
						//Also, we need a way of combining multiple mentions/messages into one notification.
						Api11.displayReplyNotification(accId, PushWorker.this, s);
						if(AccountService.activity != null){
							((Activity)AccountService.activity).runOnUiThread(new Runnable() {
								@Override
								public void run() {
									// You MUST insert the mention into the adapter associated with the 
									// account the mention is for, not the adapter for the current account.
									FeedListAdapter adapt = AccountService.getFeedAdapter((Activity) AccountService.activity, 
											MentionsFragment.ID, Long.parseLong(b.getString("account")), false);
									if(adapt != null) adapt.add(new twitter4j.Status[] { s });
								}
							});
						}
					} else if(type.equals("dm")){
						//Yes I know it's "tweet". Deal with it
						JSONObject json = new JSONObject(b.getString("tweet"));
						final twitter4j.DirectMessage dm = new twitter4j.internal.json.DirectMessageJSONImpl(json);
						Api11.displayDirectMessageNotification(accId, PushWorker.this, dm);
					}
				} catch(Exception e) { e.printStackTrace(); }
			}
			return Service.START_NOT_STICKY;
		}
	}
	
	public static boolean settingUp = false;

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
		if(settingUp == true) return; // Don't double-register
		settingUp = true;		
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