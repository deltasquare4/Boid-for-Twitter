package com.teamboid.twitter.compat;

import com.handlerexploit.prime.ImageManager;
import com.teamboid.twitter.ConversationScreen;
import com.teamboid.twitter.R;
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

/**
 * Methods for API Level 14 up
 *
 * @author kennydude
 */
@TargetApi(14)
public class Api11 { //We don't support API 11, we only support API 14-16

    public static int SINGLE_NOTIFCATION = 100;

    /**
     * Applies settings
     */
    public static Notification setupNotification(int accId, Notification nb, Context c) {
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
    public static void displayReplyNotification(final int accId, final Context context, final Status s) {
        final String imageURL = Utilities.getUserImage(s.getUser().getScreenName(), context, s.getUser());
        ImageManager.getInstance(context).get(imageURL, new ImageManager.OnImageReceivedListener() {
            @SuppressLint("NewApi")
            @Override
            public void onImageReceived(String arg0, Bitmap profileImg) {
                Intent content = new Intent(context, TweetViewer.class)
                        .putExtra("sr_tweet", Utilities.serializeObject(s))
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent pi = PendingIntent.getActivity(context, 0, content, PendingIntent.FLAG_ONE_SHOT);
                final NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                final Notification.Builder nb =
                        new Notification.Builder(context)
                                .setContentTitle(s.getUser().getScreenName())
                                .setContentText(s.getText())
                                .setLargeIcon(profileImg)
                                .setContentIntent(pi)
                                .setAutoCancel(true)
                                .setSmallIcon(R.drawable.statusbar_icon)
                                .setTicker(context.getString(R.string.mentioned_by).replace("{user}",
                                        s.getUser().getScreenName()) + " - " + s.getText());

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    Api16.displayReplyNotification(accId, context, profileImg, s, nb, nm);
                } else {
                    Notification n = setupNotification(accId, nb.getNotification(), context);
                    nm.notify(accId + "_MENTIONS", 100, n);
                }
            }
        });
    }

    public static void displayDirectMessageNotification(final int accId, final Context c, final DirectMessage dm) {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(c);
        String x = dm.getText();
        if (!p.getBoolean(accId + "_c2dm_messages_priv", false)) {
            x = c.getString(R.string.message_recv).replace("{user}", dm.getSender().getName());
        }
        final String text = x;
        final String imageURL = Utilities.getUserImage(dm.getSenderScreenName(), c, dm.getSender());
        ImageManager.getInstance(c).get(imageURL, new ImageManager.OnImageReceivedListener() {
            @SuppressWarnings("deprecation")
            @Override
            public void onImageReceived(String arg0, Bitmap profileImg) {
                Intent content = new Intent(c, ConversationScreen.class)
                        .putExtra("screen_name", dm.getSenderScreenName())
                        .putExtra("notification", true)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent pi = PendingIntent.getActivity(c, 0, content, PendingIntent.FLAG_ONE_SHOT);
                final NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
                final Notification.Builder nb =
                        new Notification.Builder(c)
                                .setContentTitle(dm.getSender().getScreenName())
                                .setContentText(text)
                                .setLargeIcon(profileImg)
                                .setContentIntent(pi)
                                .setAutoCancel(true)
                                .setSmallIcon(R.drawable.statusbar_icon)
                                .setTicker(c.getString(R.string.messaged_by).replace("{user}", dm.getSenderScreenName()));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    Api16.displayDMNotification(accId, c, profileImg, dm, nb, nm, text);
                } else {
                    Notification n = setupNotification(accId, nb.getNotification(), c);
                    nm.notify(accId + "_MESSAGES", 100, n);
                }
            }
        });
    }
}
