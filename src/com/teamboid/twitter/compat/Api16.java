package com.teamboid.twitter.compat;

import com.handlerexploit.prime.ImageManager;
import com.teamboid.twitter.ComposerScreen;
import com.teamboid.twitter.R;
import com.teamboid.twitter.utilities.Utilities;

import com.teamboid.twitterapi.dm.DirectMessage;
import com.teamboid.twitterapi.status.Status;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

/**
 * Api level 16 (Jellybean) only methods!
 *
 * @author kennydude
 */
@TargetApi(16)
public class Api16 {
	
	private static PendingIntent getReplyActionIntent(Context context, Status s) {
		Intent replyIntent = new Intent(context, ComposerScreen.class)
    		.putExtra("reply_to", s.getId())
    		.putExtra("reply_to_name", s.getUser().getScreenName())
    		.putExtra("append", Utilities.getAllMentions(s))
    		.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent replyPi = PendingIntent.getActivity(context, 0, replyIntent, PendingIntent.FLAG_ONE_SHOT);
		return replyPi; 
	}
	
	/**
	 * Please call Api11.displayNotification instead. This gets called by it if we're on JB
	 */
	public static void displayReplyNotification(final int accId, final Context context, final Bitmap profileImg, final Status s, final Builder nb, final NotificationManager nm) {
		nb.addAction(R.drawable.reply_light, context.getString(R.string.reply_str),
				getReplyActionIntent(context, s));
		nb.addAction(R.drawable.retweet_light, context.getString(R.string.retweet_str), null); //TODO
		
		String media = Utilities.getTweetYFrogTwitpicMedia(s);
		if(media != null && !media.isEmpty()) {
			ImageManager.getInstance(context).get(media, new ImageManager.OnImageReceivedListener() {
				@Override
				public void onImageReceived(String arg0, Bitmap media) {
					Notification noti = Api11.setupNotification(accId, 
							new Notification.BigPictureStyle(nb)
					.bigPicture(media)
					.bigLargeIcon(profileImg)
					.setSummaryText(s.getText())
					.build(),
					context);
					nm.notify(s.getId() + "", Api11.SINGLE_NOTIFCATION, noti);
				}
			});
		} else {
			Notification noti = Api11.setupNotification(accId,
					new Notification.BigTextStyle(nb)
			.bigText(s.getText())
			.build(),
			context);
			nm.notify(s.getId() + "", Api11.SINGLE_NOTIFCATION, noti);
		}
	}

	public static void displayDMNotification(final int accId, final Context context, final Bitmap profileImg, final DirectMessage msg, final Builder nb, final NotificationManager nm, final String text) {
		nb.addAction(R.drawable.reply_light, context.getString(R.string.reply_str), null); //TODO
		nb.addAction(R.drawable.delete_light, context.getString(R.string.delete_str), null); //TODO
		
		Notification noti = Api11.setupNotification(accId,
				new Notification.BigTextStyle(nb)
		.bigText(text)
		.build(),
		context);
		nm.notify(msg.getId() + "", Api11.SINGLE_NOTIFCATION, noti);
	}
}
