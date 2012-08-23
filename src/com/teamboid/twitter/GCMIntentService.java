package com.teamboid.twitter;

import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import com.google.android.gcm.GCMBaseIntentService;
import com.teamboid.twitter.cab.TimelineCAB;
import com.teamboid.twitter.columns.MentionsFragment;
import com.teamboid.twitter.compat.Api11;
import com.teamboid.twitter.listadapters.FeedListAdapter;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitterapi.dm.DirectMessage;
import com.teamboid.twitterapi.dm.DirectMessageJSON;
import com.teamboid.twitterapi.status.Status;
import com.teamboid.twitterapi.status.StatusJSON;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

/**
 * Push
 * DO NOT MOVE. GCM REQUIRES IT HERE
 * @author kennydude
 *
 */
public class GCMIntentService extends GCMBaseIntentService {
	protected GCMIntentService(String senderId) {
		super(senderId);
	}
	public Handler handler = new Handler();
	
	public static long getRegisteringFor(Context c){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
		return sp.getLong("c2dm_for", -1);
	}
	public static void setRegisteringFor(Context c, long id){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
		sp.edit().putLong("c2dm_for", id).commit();
	}
	
	public static final String BROADCAST = "com.teamboid.twitter.PUSH_PROGRESS";
	public static final String SENDER_ID = "107821281305";
	public static final String SERVER = "http://boid.nodester.com";
	private static final String ENCRYPTION_KEY = "efjiowewefbhjdbfhjedbfhjdfhbfberjgbisdbhebfuiehfudbvhjdnbfjwqhvfhjiou9fywe8ftyw87rtwfueiofhwekfh";
	
	/**
     * Called on registration or unregistration error.
     *
     * @param context application's context.
     * @param errorId error id returned by the GCM service.
     */
	@Override
    protected void onError(Context context, String errorId){
    	Intent intent = new Intent(BROADCAST);
    	intent.putExtra("error", true);
    	intent.putExtra("progress", 1000);
    	context.sendBroadcast(intent);
    }
	
	/**
	 * Called when the GCM server tells pending messages have been deleted
	 * because the device was idle.
	 *
	 * @param context application's context.
	 * @param total total number of collapsed messages
	 */
	protected void onDeletedMessages(Context context, int total) {
	}

	/**
	 * Called when a cloud message has been received.
	 *
	 * @param context application's context.
	 * @param intent intent containing the message payload as extras.
	 */
	@Override
	protected void onMessage(Context context, final Intent intent){
		try{
			String type = intent.getStringExtra("type");
			Integer accId = 0;
			try { accId = Integer.parseInt(intent.getStringExtra("account")); }
			catch(Exception e) { }
			
			if(type.equals("reply")) {
				JSONObject status = new JSONObject(intent.getStringExtra("tweet"));
				status.put("id", Long.parseLong(status.getString("id_str")));
				final Status s = new StatusJSON(status);
				Api11.displayReplyNotification(accId, context, s);
				TimelineCAB.context.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						FeedListAdapter adapt = AccountService.getFeedAdapter(TimelineCAB.context, 
								MentionsFragment.ID, Long.parseLong(intent.getStringExtra("account")), false);
						if(adapt != null) adapt.add(new Status[] { s });
					}
				});
			} else if(type.equals("dm")) {
				JSONObject json = new JSONObject(intent.getStringExtra("tweet"));
				final DirectMessage dm = new DirectMessageJSON(json);
				Api11.displayDirectMessageNotification(accId, context, dm);
			} else if(type.endsWith("multiReply")){
				// TODO: This
			}
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	@Override
	protected void onRegistered(final Context context, final String registrationId) {
		new Thread(new Runnable(){

			@Override
			public void run() {
				try{
					final Intent i = new Intent(BROADCAST);
					Account acc = AccountService.getAccount(getRegisteringFor(context));
					
					// Build
					JSONObject jo = new JSONObject();
					jo.put("userid", acc.getId());
					jo.put("accesstoken", acc.getToken());
					jo.put("token", registrationId);
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
					
					if(r.getStatusLine().getStatusCode() == 200) {
						Log.d("push", "REGISTERED");
						i.putExtra("progress", 1000);
						handler.post(new Runnable(){

							@Override
							public void run() {
								sendBroadcast(i);
							}
							
						});
					} else throw new Exception("NON 200 RESPONSE");
				} catch(Exception e){
					e.printStackTrace();
					final Intent i = new Intent(BROADCAST);
					i.putExtra("progress", 1000);
					i.putExtra("error", true);
					handler.post(new Runnable(){

						@Override
						public void run() {
							sendBroadcast(i);
						}
						
					});
				}
			}
			
		});	
	}

	@Override
	protected void onUnregistered(final Context context, final String registrationId) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					DefaultHttpClient dhc = new DefaultHttpClient();
					HttpGet get = new HttpGet(SERVER + "/remove/" + getRegisteringFor(context));
					org.apache.http.HttpResponse r = dhc.execute(get);
					if (r.getStatusLine().getStatusCode() == 200) {
						final Intent i = new Intent(BROADCAST);
						i.putExtra("progress", 1000);
						handler.post(new Runnable(){

							@Override
							public void run() {
								sendBroadcast(i);
							}
							
						});
					} else
						throw new Exception("NON 200 RESPONSE ;__;");
				} catch (Exception e) {
					e.printStackTrace();
					final Intent i = new Intent(BROADCAST);
					i.putExtra("error", true);
					i.putExtra("progress", 1000);
					handler.post(new Runnable(){

						@Override
						public void run() {
							sendBroadcast(i);
						}
						
					});
				}
			}
		});
	}

}
