package com.teamboid.twitter.compat;

import com.handlerexploit.prime.ImageManager;
import com.teamboid.twitter.utilities.Utilities;

import twitter4j.Status;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;

/**
 * Api level 16 (Jellybean) only methods!
 * @author kennydude
 *
 */
@TargetApi(16)
public class Api16 {

	/**
	 * Please call Api11.displayNotification instead. This gets called by it if we're on JB
	 */
	public static void displayReplyNotification(final int accId, final Context context, final Bitmap profileImg, final Status s, final Builder nb, final NotificationManager nm) {
		String media = Utilities.getTweetYFrogTwitpicMedia(s);
		if(media != null && !media.isEmpty()) {
			ImageManager.getInstance(context).get(media, new ImageManager.OnImageReceivedListener() {
				@Override
				public void onImageReceived(String arg0, Bitmap media) {
					Notification noti = Api11.setupNotification(accId, 
						new Notification.BigPictureStyle(nb)
						.bigPicture(media)
						.bigLargeIcon(profileImg)
						.build()
					,context);
					nm.notify(s.getId() + "", Api11.SINGLE_NOTIFCATION, noti);
				}
			});
		} else {
			Notification noti = Api11.setupNotification(accId,
				new Notification.BigTextStyle(nb)
				.bigText(s.getText())
				.build()
			,context);
			nm.notify(s.getId() + "", Api11.SINGLE_NOTIFCATION, noti);
		}
	}
}
