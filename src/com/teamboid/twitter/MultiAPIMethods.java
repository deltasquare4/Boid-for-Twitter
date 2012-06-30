package com.teamboid.twitter;

import twitter4j.Status;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;

import com.handlerexploit.prime.utils.ImageManager;

public class MultiAPIMethods {

	public final static int MENTION_NOTIFICATION_ID = 100;
	public final static int MESSAGE_NOTIFICATION_ID = 200;
	
	private static Notification mentionNotify;
	private static Notification messageNotify;
	
	@SuppressLint("NewApi")
	public static void ShowNotification(Status s, Context context) {
		NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		String imageURL = s.getUser().getProfileImageURL().toString();
		Bitmap profileImg = ImageManager.getInstance(context).get(imageURL);
		
		if(Build.VERSION.SDK_INT < 16) {
			//TODO ICE CREAM SANDWICH old style notification
			Notification.Builder nb = 
					new Notification.Builder(context)
					.setContentTitle(s.getUser().getScreenName())
					.setContentText(s.getText())
					.setLargeIcon(profileImg)
					.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, TweetViewer.class).putExtra("sr_tweet", Utilities.serializeObject(s)), 0))
					.setAutoCancel(true)
					.setSmallIcon(R.drawable.statusbar_icon)
					.setTicker(s.getText());
			nm.notify(MENTION_NOTIFICATION_ID, nb.getNotification());
		} else {
			//TODO JELLYBEAN expandable notification
			Notification.Builder nb = new Notification.Builder(context)
				.setContentTitle(s.getUser().getScreenName())
				.setContentText(s.getText())
				.setLargeIcon(profileImg)
				.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, TweetViewer.class).putExtra("sr_tweet", Utilities.serializeObject(s)), 0))
				.setAutoCancel(true)
				.setSmallIcon(R.drawable.statusbar_icon)
				.setTicker(s.getText());
			String media = Utilities.getTweetYFrogTwitpicMedia(s);
			Notification noti = null;
			if(media != null && !media.isEmpty()) {
				noti = new Notification.BigPictureStyle(nb)
					.bigPicture(ImageManager.getInstance(context).get(media))
					.bigLargeIcon(profileImg)
					.build(); 
			} else {
				noti = new Notification.BigTextStyle(nb)
					.bigText(s.getText())
					.build();
			}
			nm.notify(MENTION_NOTIFICATION_ID, noti);
		}
	}
}
