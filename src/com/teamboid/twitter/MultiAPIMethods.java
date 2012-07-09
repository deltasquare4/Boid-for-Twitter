package com.teamboid.twitter;

import twitter4j.Status;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import com.handlerexploit.prime.utils.ImageManager;

@SuppressLint("NewApi")
public class MultiAPIMethods {

	public final static int MENTION_NOTIFICATION_ID = 100;
	public final static int MESSAGE_NOTIFICATION_ID = 200;

	public static void showSingleNotification(final String accScreenName, final Status s, final Context context) {
		String imageURL = s.getUser().getProfileImageURL().toString();
		ImageManager.getInstance(context).get(imageURL, new ImageManager.OnImageReceivedListener() {
			@Override
			public void onImageReceived(String arg0, Bitmap profileImg) {
				displayNotificationICS(context, accScreenName, new Status[] { s }, profileImg);
				//				TODO if(Build.VERSION.SDK_INT < 16) {
				//					displayNotificationICS(context, accScreenName, new Status[] { s }, profileImg);
				//				} else displayNotificationJB(context, new Status[] { s }, profileImg); 
			}
		});
	}
	public static void showMultiNotification(final String accScreenName, final twitter4j.Status[] statuses, final Context context) {
		displayNotificationICS(context, accScreenName, statuses, null);
		//				TODO if(Build.VERSION.SDK_INT < 16) {
		//					displayNotificationICS(context, accScreenName, new Status[] { s }, profileImg);
		//				} else displayNotificationJB(context, new Status[] { s }, profileImg); 
	}

	private static void displayNotificationICS(final Context context, final String accScreenName, final twitter4j.Status[] statuses, final Bitmap profileImg) {
		final NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification.Builder nb = new Notification.Builder(context);
		if(statuses.length == 1) {
			Status s = statuses[0];
			nb.setContentTitle(s.getUser().getScreenName())
			.setContentText(s.getText())
			.setLargeIcon(profileImg)
			.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, TweetViewer.class).putExtra("sr_tweet", Utilities.serializeObject(s)), 0))
			.setAutoCancel(true)
			.setSmallIcon(R.drawable.statusbar_icon)
			.setTicker(accScreenName + " - " + context.getString(R.string.new_mention));
		} else {
			//TODO pending intent should send signal to open certain column type and refresh it
			String text = context.getString(R.string.new_mentions).replace("{x}", Integer.toString(statuses.length));
			nb.setContentTitle(accScreenName)
			.setContentText(text)
			.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, TweetViewer.class), 0))
			.setAutoCancel(true)
			.setSmallIcon(R.drawable.statusbar_icon)
			.setTicker(accScreenName + " - " + text);
		}
		nm.notify(MENTION_NOTIFICATION_ID, nb.build());
	}

//	private static void displayNotificationJB(final Context context, final Status[] statuses, final Bitmap profileImg) {
//				final Notification.Builder nb = new Notification.Builder(context)
//				.setContentTitle(s.getUser().getScreenName())
//				.setContentText(s.getText())
//				.setLargeIcon(profileImg)
//				.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, TweetViewer.class).putExtra("sr_tweet", Utilities.serializeObject(s)), 0))
//				.setAutoCancel(true)
//				.setSmallIcon(R.drawable.statusbar_icon)
//				.setTicker(s.getText());
//				String media = Utilities.getTweetYFrogTwitpicMedia(s);
//				if(media != null && !media.isEmpty()) {
//					ImageManager.getInstance(context).get(media, new ImageManager.OnImageReceivedListener() {
//						@Override
//						public void onImageReceived(String arg0, Bitmap media) {
//							Notification noti = new Notification.BigPictureStyle(nb)
//							.bigPicture(media)
//							.bigLargeIcon(profileImg)
//							.build();
//							nm.notify(MENTION_NOTIFICATION_ID, noti);
//						}
//					});
//				} else {
//					Notification noti = new Notification.BigTextStyle(nb)
//					.bigText(s.getText())
//					.build();
//					nm.notify(MENTION_NOTIFICATION_ID, noti);
//				}
//	}
}
