package com.teamboid.twitter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.SSLCertificateSocketFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import com.teamboid.twitter.TabsAdapter.MentionsFragment;
import com.teamboid.twitter.compat.Api11;

public class PushReceiver extends BroadcastReceiver {
	public static final String SENDER_EMAIL = "107821281305";
	
	// public static final String SERVER = "http://192.168.0.9:1337";
	public static final String SERVER = "http://boid.nodester.com";
	
	public static class PushWorker extends Service{
		
		@Override
		public IBinder onBind(Intent arg0) {
			return null;
		}
		
		private static final String ENCRYPTION_KEY = "boidisalovelyappandijustlovehavingencrpytiontoworkwithnodester...../.khnihi";
		
		@Override
		public int onStartCommand(final Intent intent, int flags, int startId) {
			if(intent.hasExtra("reg")){
			new Thread(new Runnable(){

				@Override
				public void run() {
					try{
						Account acc = AccountService.getCurrentAccount();
						
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
						
						DefaultHttpClient dhc = new DefaultHttpClient();
						HttpPost p = new HttpPost(SERVER + "/register");
						p.setEntity( new StringEntity(query) );
						HttpResponse r = dhc.execute(p);
						
						if(r.getStatusLine().getStatusCode() == 200){
							Log.d("push", "REGISTERED");
						} else{ throw new Exception("NON 200 RESPONSE"); }
						
					}catch(Exception e){ e.printStackTrace(); }
					settingUp = false;
				}
				
			}).start();
			} else if(intent.hasExtra("hm")){
				Bundle b = intent.getBundleExtra("hm");
				try {
					JSONObject status = new JSONObject(b.getString("tweet"));
					final twitter4j.Status s = new twitter4j.internal.json.StatusJSONImpl(status);
					//TODO The account the mention is for should be passed from the server too
					// We have this now in "account" as a string
					//Also, we need a way of combining multiple mentions/messages into one notification.
					Api11.displayNotification(PushWorker.this, s);
					AccountService.activity.runOnUiThread(new Runnable(){

						@Override
						public void run() {
							 AccountService.getFeedAdapter(AccountService.activity, MentionsFragment.ID, AccountService.getCurrentAccount().getId()).add(new twitter4j.Status[]{ s });
						}
						
					});
				} catch(Exception e) {
					e.printStackTrace();
				}
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
