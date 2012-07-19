package com.teamboid.twitter.compat;

import com.handlerexploit.prime.ImageManager;
import com.teamboid.twitter.ConversationScreen;
import com.teamboid.twitter.R;
import com.teamboid.twitter.TweetViewer;
import com.teamboid.twitter.utilities.Utilities;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;

/**
 * Methods for API Level 11 up
 * @author kennydude
 */
@TargetApi(11)
public class Api11 {
	
	public static int SINGLE_NOTIFCATION = 100;
	
	/**
	 * Applies settings
	 * @param nb
	 */
	public static void setupNotification(int accId, Notification nb, Context c) {
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(c);
		if(p.getBoolean("c2dm_vibrate", false) == true) {
			nb.vibrate = new long[]{ 100, 100, 100 };
		}
		try {
			if(!p.getString("c2dm_ringtone", "").equals("")) {
				nb.sound = Uri.parse(p.getString("c2dm_ringtone", ""));
			}
		} catch(Exception e) { }
	}
	
	/**
	 * Display single notification
	 * @param context
	 * @param s
	 */
	public static void displayReplyNotification(final int accId, final Context context, final twitter4j.Status s){
		final String imageURL = Utilities.getUserImage(s.getUser().getScreenName(), context, s.getUser());
		ImageManager.getInstance(context).get(imageURL, new ImageManager.OnImageReceivedListener() {
			
			@SuppressLint("NewApi")
			@Override
			public void onImageReceived(String arg0, Bitmap profileImg) {
				
				final NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
				
				final Notification.Builder nb = 
						new Notification.Builder(context)
						.setContentTitle(s.getUser().getScreenName())
						.setContentText(s.getText())
						.setLargeIcon(profileImg)
						.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, TweetViewer.class).putExtra("sr_tweet", Utilities.serializeObject(s)), PendingIntent.FLAG_ONE_SHOT))
						.setAutoCancel(true)
						.setSmallIcon(R.drawable.statusbar_icon)
						.setTicker(s.getText());
				
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
					// Pass up to class. We do this, otherwise Dalvik complains. I've done this before
					Api16.displayReplyNotification(accId, context, profileImg, s, nb, nm);
				} else{
					Notification n = nb.build();
					setupNotification(accId, n, context);
					nm.notify(s.getId() + "", 100, n);
				}
				
			}
		});
	}
	
	public static void displayDirectMessageNotification(final int accId, final Context c, final twitter4j.DirectMessage dm){
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(c);
		
		String x = dm.getText();
		if(p.getBoolean("c2dm_messages_priv", false) == true) {
			x = c.getString(R.string.message_recv).replace("{user}", dm.getSender().getName());
		}
		final String text = x;
		
		final String imageURL = Utilities.getUserImage(dm.getSender().getScreenName(), c, dm.getSender());
		ImageManager.getInstance(c).get(imageURL, new ImageManager.OnImageReceivedListener() {
			
			@SuppressWarnings("deprecation")
			@Override
			public void onImageReceived(String arg0, Bitmap profileImg) {
				
				final NotificationManager nm = (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
				
				final Notification.Builder nb = 
						new Notification.Builder(c)
						.setContentTitle(dm.getSender().getScreenName())
						.setContentText(text)
						.setLargeIcon(profileImg)
						.setContentIntent(PendingIntent.getActivity(c, 0, new Intent(c, ConversationScreen.class).putExtra("username", dm.getSenderScreenName()), PendingIntent.FLAG_ONE_SHOT))
						.setAutoCancel(true)
						.setSmallIcon(R.drawable.statusbar_icon)
						.setTicker(text);
				
				
				
				//if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
					// Pass up to class. We do this, otherwise Dalvik complains. I've done this before
				//	Api16.displayDMNotification(c, profileImg, dm, nb, nm);
				//} else{
					Notification n = nb.getNotification();
					setupNotification(accId, n, c);
					nm.notify(dm.getId() + "", 100, n);
				// }
				
			}
		});
	}
	
}
