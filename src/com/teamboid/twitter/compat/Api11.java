package com.teamboid.twitter.compat;

import com.handlerexploit.prime.utils.ImageManager;
import com.teamboid.twitter.R;
import com.teamboid.twitter.TweetViewer;
import com.teamboid.twitter.utilities.Utilities;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;

/**
 * Methods for API Level 11 up
 * @author kennydude
 *
 */
@TargetApi(11)
public class Api11 {
	
	public static int SINGLE_NOTIFCATION = 100;
	
	/**
	 * Display single notification
	 * @param context
	 * @param s
	 */
	public static void displayNotification(final Context context, final twitter4j.Status s){
		final String imageURL = Utilities.getUserImage(s.getUser().getScreenName(), context);
		ImageManager.getInstance(context).get(imageURL, new ImageManager.OnImageReceivedListener() {
			
			@SuppressWarnings("deprecation")
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
					Api16.displayNotification(context, profileImg, s, nb, nm);
				} else{
					nm.notify(s.getId() + "", 100, nb.getNotification());
				}
				
			}
		});
	}
	
}
