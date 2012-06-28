package com.teamboid.twitter;

import com.handlerexploit.prime.utils.ImageManager;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import twitter4j.Status;

/**
 * API Level 11 Methods to avoid errors on lower
 * @author kennydude
 *
 */
public class Api11Methods {
	/**
	 * Shows a Notification in the notification area about "s"
	 * @param s
	 */
	public static void ShowNotification(Status s, Context context){
		Notification.Builder nb = new Notification.Builder(context);
		nb.setContentTitle(s.getUser().getScreenName());
		nb.setContentText(s.getText());
		ImageManager imageManager = ImageManager.getInstance(context);
		String imageURL = s.getUser().getProfileImageURL().toString();
		nb.setLargeIcon(imageManager.get(imageURL));
		nb.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, TweetViewer.class).putExtra("sr_tweet", Utilities.serializeObject(s)), 0));
		nb.setAutoCancel(true);
		nb.setSmallIcon(R.drawable.messages_tab);
		nb.setTicker(s.getText());
		NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify("pushnotify", 0, nb.getNotification());
	}
}
