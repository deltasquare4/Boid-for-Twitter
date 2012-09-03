package com.teamboid.twitter.compat;

import org.json.JSONArray;
import org.json.JSONObject;

import com.handlerexploit.prime.ImageManager;
import com.teamboid.twitter.ConversationScreen;
import com.teamboid.twitter.R;
import com.teamboid.twitter.TimelineScreen;
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

import com.teamboid.twitterapi.dm.DirectMessage;
import com.teamboid.twitterapi.status.Status;
import com.teamboid.twitterapi.utilities.Utils;

/**
 * Methods for API Level 14 up
 * 
 * @author kennydude
 */
@TargetApi(14)
public class Api11 { // We don't support API 11, we only support API 14-16

	public static int MENTIONS = 100;
	public static int DM = 200;

	/**
	 * Applies settings
	 */
	public static Notification setupNotification(int accId, Notification nb,
			Context c) {
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(c);
		nb.defaults = Notification.DEFAULT_LIGHTS;
		if (p.getBoolean(accId + "_c2dm_vibrate", false) == true) {
			nb.defaults |= Notification.DEFAULT_VIBRATE;
		}
		try {
			if (!p.getString(accId + "_c2dm_ringtone", "").equals("")) {
				nb.sound = Uri.parse(p.getString(accId + "_c2dm_ringtone", ""));
			}
		} catch (Exception e) {
		}
		return nb;
	}

	/**
	 * Display single notification
	 */
	public static void displayReplyNotification(final int accId,
			final Context context, final Status s) {
		final String imageURL = Utilities.getUserImage(s.getUser()
				.getScreenName(), context, s.getUser());
		ImageManager.getInstance(context).get(imageURL,
				new ImageManager.OnImageReceivedListener() {
					@SuppressLint("NewApi")
					@Override
					public void onImageReceived(String arg0, Bitmap profileImg) {
						Intent content = new Intent(context, TweetViewer.class)
								.putExtra("sr_tweet", Utils.serializeObject(s))
								.putExtra("account", accId)
								.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						PendingIntent pi = PendingIntent.getActivity(context,
								0, content, PendingIntent.FLAG_ONE_SHOT);
						final NotificationManager nm = (NotificationManager) context
								.getSystemService(Context.NOTIFICATION_SERVICE);
						final Notification.Builder nb = new Notification.Builder(
								context)
								.setContentTitle(s.getUser().getScreenName())
								.setContentText(s.getText())
								.setLargeIcon(profileImg)
								.setContentIntent(pi)
								.setAutoCancel(true)
								.setSmallIcon(R.drawable.statusbar_icon)
								.setTicker(
										context.getString(R.string.mentioned_by)
												.replace(
														"{user}",
														s.getUser()
																.getScreenName())
												+ " - " + s.getText());

						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
							Api16.displayReplyNotification(accId, context,
									profileImg, s, nb, nm);
						} else {
							@SuppressWarnings("deprecation")
							Notification n = setupNotification(accId,
									nb.getNotification(), context);
							nm.notify(accId + "", MENTIONS, n);
						}
					}
				});
	}

	public static void displayDirectMessageNotification(final int accId,
			final Context c, final DirectMessage dm) {
		final String imageURL = Utilities.getUserImage(
				dm.getSenderScreenName(), c, dm.getSender());
		ImageManager.getInstance(c).get(imageURL,
				new ImageManager.OnImageReceivedListener() {
					@SuppressWarnings("deprecation")
					@Override
					public void onImageReceived(String arg0, Bitmap profileImg) {
						Intent content = new Intent(c, ConversationScreen.class)
								.putExtra("screen_name",
										dm.getSenderScreenName())
								.putExtra("account", accId)
								.putExtra("notification", true)
								.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						PendingIntent pi = PendingIntent.getActivity(c, 0,
								content, PendingIntent.FLAG_ONE_SHOT);
						final NotificationManager nm = (NotificationManager) c
								.getSystemService(Context.NOTIFICATION_SERVICE);
						final Notification.Builder nb = new Notification.Builder(
								c)
								.setContentTitle(dm.getSender().getScreenName())
								.setContentText(dm.getText())
								.setLargeIcon(profileImg)
								.setContentIntent(pi)
								.setAutoCancel(true)
								.setSmallIcon(R.drawable.statusbar_icon)
								.setTicker(
										c.getString(R.string.messaged_by)
												.replace(
														"{user}",
														dm.getSenderScreenName()));
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
							Api16.displayDMNotification(accId, c, profileImg,
									dm, nb, nm, dm.getText());
						} else {
							Notification n = setupNotification(accId,
									nb.getNotification(), c);
							nm.notify(accId + "", DM, n);
						}
					}
				});
	}

	public static void displayMany(final long accId, final int queue,
			final Context c, final JSONArray ja) {
		try {
			if (ja.length() == 0) {
				return;
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				Api16.displayMany(accId, queue, c, ja);
				return;
			}
			final JSONObject first = ja.getJSONObject(ja.length() - 1);
			final String imageURL = Utilities.getUserImage(
					first.getString("user"), c);
			ImageManager.getInstance(c).get(imageURL,
					new ImageManager.OnImageReceivedListener() {
						@SuppressWarnings("deprecation")
						@Override
						public void onImageReceived(String source, Bitmap bitmap) {
							try {
								Intent content = new Intent(c,
										TimelineScreen.class)
										.putExtra("switch", queue)
										.putExtra("account", accId)
										.addFlags(
												Intent.FLAG_ACTIVITY_CLEAR_TOP);
								PendingIntent pi = PendingIntent
										.getActivity(c, 0, content,
												PendingIntent.FLAG_ONE_SHOT);
								NotificationManager nm = (NotificationManager) c
										.getSystemService(Context.NOTIFICATION_SERVICE);
								Notification.Builder nb = new Notification.Builder(
										c)
										.setContentTitle(
												first.getString("user"))
										.setContentText(
												first.getString("content"))
										.setAutoCancel(true)
										.setContentIntent(pi)
										.setSmallIcon(R.drawable.statusbar_icon)
										.setLargeIcon(bitmap)
										.setNumber(ja.length());
								Notification n = setupNotification((int) accId,
										nb.getNotification(), c);
								nm.notify(accId + "", queue, n);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}

					});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
