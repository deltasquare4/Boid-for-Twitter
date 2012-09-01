package com.teamboid.twitter.services;

import java.util.Calendar;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.teamboid.twitter.Account;
import com.teamboid.twitter.compat.Api11;
import com.teamboid.twitter.utilities.NightModeUtils;
import com.teamboid.twitterapi.client.Authorizer;
import com.teamboid.twitterapi.client.Paging;
import com.teamboid.twitterapi.client.Twitter;
import com.teamboid.twitterapi.dm.DirectMessage;
import com.teamboid.twitterapi.status.Status;
import com.teamboid.twitterapi.utilities.Utils;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Processes notifications
 * 
 * Note: You cannot use the AccountService. We are in a completely different process
 * so that we are not chugging it's weight around at boot-time
 * @author kennydude
 *
 */
public class NotificationService extends BroadcastReceiver {
	public static void setupAlarm(Context c, long accId, boolean on){
		Intent intent = new Intent();
		intent.setData(Uri.parse("boid:notify/"+accId));
		intent.setPackage("com.teamboid.twitter");
		//intent.putExtra("account", accId);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(c, 0, intent, Intent.FILL_IN_DATA);
		
		AlarmManager alm = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
		
		if(on == true){
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(System.currentTimeMillis());
			calendar.add(Calendar.SECOND, 10);
			int period = Integer.parseInt((PreferenceManager.getDefaultSharedPreferences(c)).getString(accId + "_c2dm_period", "15"));
			alm.setInexactRepeating(AlarmManager.RTC, calendar.getTimeInMillis(),
					period * (1000 * 60), pendingIntent);
		} else
			alm.cancel(pendingIntent);
	}

	@Override
	public void onReceive(Context cntxt, Intent intent) {
		Log.d("nf", "NotificationService");
		if(intent.getData() != null){
			Log.d("nf", intent.getDataString());
			if(!intent.getDataString().startsWith("boid:notify")) return;
			Intent service = new Intent(cntxt, ActualService.class);
			service.putExtra("account", Long.parseLong(intent.getData().getPath()));
			cntxt.startService(service);
		} else if(intent.hasExtra("account")){ 
			Log.d("nf", "acc");
			Intent service = new Intent(cntxt, ActualService.class);
			service.putExtra("account", intent.getLongExtra("account", -1));
			cntxt.startService(service);
		} else if(intent.getAction() != null){
			if(!intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))return;
			Log.d("nf", "setup");
			Map<String, ?> sp = cntxt.getSharedPreferences("profiles-v2",
					Context.MODE_PRIVATE).getAll();
			SharedPreferences def = PreferenceManager.getDefaultSharedPreferences(cntxt);
			for(final String token : sp.keySet()) {
				 
				 final Account toAdd = (Account)Utils.deserializeObject((String)sp.get(token));
				 setupAlarm(cntxt, toAdd.getId(), def.getBoolean(toAdd.getId() + "_c2dm", true));
				 
			}
		}
	}
	
	/**
	 * Set all mentions to be read and remove all existing notifications for mentions
	 * 
	 * How this works: We have a queue "c2dm_mention_queue_{{ACID}}" which is added to, and removed from
	 * @param id
	 * @param c
	 */
	public static void setReadMentions(long id, Context c) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
		sp.edit().remove("c2dm_mention_queue_" + id).commit();
		NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(id + "", Api11.MENTIONS);
	}
	public static void setReadDMs(long id, Context c) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
		sp.edit().remove("c2dm_dm_queue_" + id).commit();
		NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(id + "", Api11.MENTIONS);
	}
	
	public static long getRegisteringFor(Context c){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
		return sp.getLong("c2dm_for", -1);
	}
	public static void setRegisteringFor(Context c, long id){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
		sp.edit().putLong("c2dm_for", id).commit();
	}
	
	public static class ActualService extends Service{
		public ActualService(){ super(); }
		
		public Handler handler = new Handler();
		
		public void addMessageToQueue(String queue, long accId, String user, String value){
			try{
				SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
				JSONArray ja = new JSONArray(sp.getString("c2dm_" + queue + "_queue_" + accId, "[]"));
				JSONObject jo = new JSONObject();
				jo.put("user", user);
				jo.put("content", value);
				ja.put(jo);
				sp.edit().putString("c2dm_" + queue + "_queue_" + accId, ja.toString()).commit();
			} catch(Exception e){ // Should never happen
				e.printStackTrace();
			}
		}
		
		public void showQueue(final String queue, final long accId,final Object single){
			try{
				handler.post(new Runnable(){

					@Override
					public void run() {
						try{
							if(NightModeUtils.isNightMode(ActualService.this)){
								SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ActualService.this);
								if(prefs.getBoolean("night_mode_pause_notifications", false) == true){
									return;
								}
							}
							NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
							nm.cancel(accId + "", queue.equals("mention") ? Api11.MENTIONS : Api11.DM);
							
							SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ActualService.this);
							JSONArray ja = new JSONArray(sp.getString("c2dm_" + queue + "_queue_" + accId, "[]"));
							Log.d("boid", ja.length() + "");
							if(ja.length() == 1 && single != null){
								if(queue.equals("mention")){
									Api11.displayReplyNotification((int) accId, ActualService.this, (Status)single);
								} else if(queue.equals("dm")){
									Api11.displayDirectMessageNotification((int) accId, ActualService.this, (DirectMessage)single);
								}
							} else{
								if(queue.equals("mention")){
									Api11.displayMany(accId, Api11.MENTIONS, ActualService.this, ja);
								} else if(queue.equals("dm")){
									Api11.displayMany(accId, Api11.DM, ActualService.this, ja);
								}
							}
						} catch(Exception e){
							e.printStackTrace();
						}
					}
					
				});
			} catch(Exception e){
				e.printStackTrace();
			}
		}
		
		Twitter getTwitter(long accId){
			SharedPreferences sp = getSharedPreferences("profiles-v2",
					Context.MODE_PRIVATE);
			String s = sp.getString(accId + "", null);
			if (s == null)
				return null;

			com.teamboid.twitter.Account toAdd = (com.teamboid.twitter.Account) Utils
					.deserializeObject(s);
			Log.d("contactsync", "Hello " + toAdd.getId());
			return Authorizer.create(AccountService.CONSUMER_KEY,
					AccountService.CONSUMER_SECRET, AccountService.CALLBACK_URL)
					.getAuthorizedInstance(toAdd.getToken(), toAdd.getSecret());
		}
		
		long getSinceId(long accId, String column){
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			return prefs.getLong("c2dm_"+accId + "_since_column_" + column, -1);
		}
		
		boolean columnEnabled(long accId, String column){
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			return prefs.getBoolean(accId + "_c2dm_" + column, true);
		}
		
		void setSinceId(long accId, String column, long since_id){
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			prefs.edit().putLong("c2dm_"+accId + "_since_column_" + column, since_id).commit();
		}
		
		@Override
		public int onStartCommand(Intent intent, int flags, int startId) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			if(prefs.getBoolean("notifications_global", true) == false){
				Log.d("nf", "Globally off");
				return Service.START_NOT_STICKY;
			}
			
			final long accId = intent.getLongExtra("account", -1);
			new Thread(new Runnable(){

				@Override
				public void run() {
					Twitter client = getTwitter(accId);
					
					if(columnEnabled(accId, "mention")){
						Log.d("boid", "mention check");
						try{
							long since_id = getSinceId(accId, "mention");
							Paging paging = new Paging(5);
							if(since_id != -1)
								paging.setSinceId(since_id);
							
							Status[] m = client.getMentions(paging);
							Log.d("boid", m.length + " m");
							if(m.length > 0){
								setSinceId(accId, "mention", m[0].getId());
								for(Status status : m){
									addMessageToQueue("mention", accId, status.getUser().getScreenName(), status.getText());
								}
								showQueue("mention", accId, m[0]);
							}
						} catch(Exception e){
							e.printStackTrace();
						}
					}
					
					if(columnEnabled(accId, "dm")){
						Log.d("boid", "dm check");
						try{
							long since_id = getSinceId(accId, "dm");
							Paging paging = new Paging(5);
							if(since_id != -1)
								paging.setSinceId(since_id);
							
							DirectMessage[] m = client.getDirectMessages(paging);
							Log.d("boid", m.length + " m");
							if(m.length > 0){
								setSinceId(accId, "dm", m[0].getId());
								for(DirectMessage message : m){
									addMessageToQueue("dm", accId, message.getSenderScreenName(), message.getText());
								}
								showQueue("dm", accId, m[0]);
							}
						} catch(Exception e){
							e.printStackTrace();
						}
					}
				}
				
			}).start();
			return Service.START_NOT_STICKY;
		}

		@Override
		public IBinder onBind(Intent arg0) {
			return null;
		}
		
	}

}
