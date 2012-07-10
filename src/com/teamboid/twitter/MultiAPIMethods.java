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

@SuppressLint("NewApi")
public class MultiAPIMethods {

	public final static int MENTION_NOTIFICATION_ID = 100;
	public final static int MESSAGE_NOTIFICATION_ID = 200;
	
	public static void showNotification(final Status s, final Context context) {
		String imageURL = s.getUser().getProfileImageURL().toString();
		ImageManager.getInstance(context).get(imageURL, new ImageManager.OnImageReceivedListener() {
			@Override
			public void onImageReceived(String arg0, Bitmap profileImg) {
				displayNotification(context, s, profileImg);
			}
		});
	}
	
	private static void displayNotification(final Context context, final Status s, final Bitmap profileImg) {
		final NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		final Notification.Builder nb = 
				new Notification.Builder(context)
				.setContentTitle(s.getUser().getScreenName())
				.setContentText(s.getText())
				.setLargeIcon(profileImg)
				.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, TweetViewer.class).putExtra("sr_tweet", Utilities.serializeObject(s)), 0))
				.setAutoCancel(true)
				.setSmallIcon(R.drawable.statusbar_icon)
				.setTicker(s.getText());
		
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			//TODO JELLYBEAN expandable notification
			String media = Utilities.getTweetYFrogTwitpicMedia(s);
			if(media != null && !media.isEmpty()) {
				ImageManager.getInstance(context).get(media, new ImageManager.OnImageReceivedListener() {
					@Override
					public void onImageReceived(String arg0, Bitmap media) {
						Notification noti = new Notification.BigPictureStyle(nb)
							.bigPicture(media)
							.bigLargeIcon(profileImg)
							.build();
						nm.notify(MENTION_NOTIFICATION_ID, noti);
					}
				});
			} else {
				Notification noti = new Notification.BigTextStyle(nb)
					.bigText(s.getText())
					.build();
				nm.notify(MENTION_NOTIFICATION_ID, noti);
			}
		}
	}
}
