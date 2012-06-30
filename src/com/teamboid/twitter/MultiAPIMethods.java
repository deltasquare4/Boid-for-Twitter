package com.teamboid.twitter;

import twitter4j.Status;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.handlerexploit.prime.utils.ImageManager;

public class MultiAPIMethods {

	@SuppressLint("NewApi")
	public static void ShowNotification(Status s, Context context){
		NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		if(Build.VERSION.SDK_INT < 16) {
			//TODO ICE CREAM SANDWICH notification
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
		} else {
			//TODO JELLYBEAN expandable notification
			Notification noti = new Notification.BigTextStyle(
					new Notification.Builder(context)
					.setContentTitle("New mentions")
					.setContentText("New mentions for @afollestad")
					.setSmallIcon(R.drawable.statusbar_icon))
			.bigText("Hello, my name is Aidan Follestad. This is a very large expandable notification that should be able to open and close when you use the expand gesture on it. These notifications will eventually be used for push notifications that tell Boid users when they have new mentions or direct messages that can be viewed. Push notifications will come right as the mention or message is received on Twitter because push notifications are instant.")
			.build();
			mNotificationManager.notify(1, noti);
		}
	}
}
