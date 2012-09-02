package com.teamboid.twitter.compat;

import org.json.JSONArray;
import org.json.JSONObject;

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
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;

/**
 * Api level 16 (Jellybean) only methods!
 * 
 * @author kennydude and Aidan Follestad
 */
@TargetApi(16)
public class Api16 {

	private static PendingIntent getReplyActionIntent(Context context,
			Status s, int accId) {
		Intent replyIntent = new Intent(context, ComposerScreen.class)
				.putExtra("account", accId).putExtra("reply_to", s.getId())
				.putExtra("reply_to_name", s.getUser().getScreenName())
				.putExtra("append", Utilities.getAllMentions(s, accId))
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent replyPi = PendingIntent.getActivity(context, 0,
				replyIntent, PendingIntent.FLAG_ONE_SHOT);
		return replyPi;
	}

	/**
	 * Please call Api11.displayNotification instead. This gets called by it if
	 * we're on JB
	 */
	public static void displayReplyNotification(final int accId,
			final Context context, final Bitmap profileImg, final Status s,
			final Builder nb, final NotificationManager nm) {
		nb.addAction(R.drawable.reply_light,
				context.getString(R.string.reply_str),
				getReplyActionIntent(context, s, accId));
		nb.addAction(R.drawable.retweet_light,
				context.getString(R.string.retweet_str), null); // TODO

		String media = Utilities.getTweetYFrogTwitpicMedia(s);
		if (media != null && !media.isEmpty()) {
			ImageManager.getInstance(context).get(media,
					new ImageManager.OnImageReceivedListener() {
						@Override
						public void onImageReceived(String arg0, Bitmap media) {
							Notification noti = Api11.setupNotification(
									accId,
									new Notification.BigPictureStyle(nb)
											.bigPicture(media)
											.bigLargeIcon(profileImg)
											.setSummaryText(s.getText())
											.build(), context);
							nm.notify(s.getId() + "", Api11.MENTIONS, noti);
						}
					});
		} else {
			Notification noti = Api11.setupNotification(accId,
					new Notification.BigTextStyle(nb).bigText(s.getText())
							.build(), context);
			nm.notify(s.getId() + "", Api11.MENTIONS, noti);
		}
	}

	public static void displayDMNotification(final int accId,
			final Context context, final Bitmap profileImg,
			final DirectMessage msg, final Builder nb,
			final NotificationManager nm, final String text) {
		nb.addAction(R.drawable.reply_light,
				context.getString(R.string.reply_str), null); // TODO
		nb.addAction(R.drawable.delete_light,
				context.getString(R.string.delete_str), null); // TODO

		Notification noti = Api11.setupNotification(accId,
				new Notification.BigTextStyle(nb).bigText(text).build(),
				context);
		nm.notify(msg.getId() + "", Api11.DM, noti);
	}

	public static void setLowPirority(Builder nb) {
		nb.setPriority(Notification.PRIORITY_LOW);
	}

	static int getQueueMessage(int queue) {
		if (queue == Api11.MENTIONS)
			return R.string.mention_str;
		else if (queue == Api11.DM)
			return R.string.directmessage_str;
		return 0;
	}

	static String getQueueContent(Context c, int queue, int length) {
		if (queue == Api11.MENTIONS)
			return c.getString(R.string.x_new_mentions).replace("{X}", "" + length);
		else if (queue == Api11.DM)
			return c.getString(R.string.x_new_messages).replace("{X}", "" + length);
		return null;
	}
	
	public static void displayMany(long accId, int queue, Context c,
			JSONArray ja) {
		try {
			Notification.Builder nb = new Notification.Builder(c)
					.setContentTitle(c.getString(getQueueMessage(queue)))
					.setContentText(getQueueContent(c, queue, ja.length()))
					.setSmallIcon(R.drawable.statusbar_icon);
			Notification.InboxStyle inbox = new Notification.InboxStyle(nb);
			int m = 5;
			if (ja.length() < 5) {
				m = ja.length();
			}
			for (int i = 0; i <= m; i++) {
				JSONObject jo = ja.getJSONObject(i);
				String user = jo.getString("user") + ": ";
				SpannableStringBuilder sp = new SpannableStringBuilder(user
						+ jo.getString("content"));
				sp.setSpan(new StyleSpan(Typeface.BOLD), 0, user.length(),
						Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
				inbox.addLine(sp);
			}

			NotificationManager nm = (NotificationManager) c
					.getSystemService(Context.NOTIFICATION_SERVICE);
			nm.notify(accId + "", queue, inbox.build());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
