package com.teamboid.twitter.notifications;

import org.json.JSONArray;
import org.json.JSONObject;

import com.teamboid.twitter.R;
import com.teamboid.twitter.compat.Api11;
import com.teamboid.twitter.contactsync.BaseTwitterSync;
import com.teamboid.twitter.utilities.NightModeUtils;
import com.teamboid.twitterapi.client.Paging;
import com.teamboid.twitterapi.client.Twitter;
import com.teamboid.twitterapi.dm.DirectMessage;
import com.teamboid.twitterapi.status.Status;

import android.accounts.Account;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Does notifications
 * 
 * @author kennydude
 * 
 */
public class NotificationService extends Service {
	
	public static final String AUTHORITY = "com.teamboid.twitter.notifications";

	/**
	 * Set all mentions to be read and remove all existing notifications for
	 * mentions
	 * 
	 * How this works: We have a queue "c2dm_mention_queue_{{ACID}}" which is
	 * added to, and removed from
	 * 
	 * @param id
	 * @param c
	 */
	public static void setReadMentions(long id, Context c) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
		sp.edit().putString("c2dm_mention_queue_" + id, "[]").commit();
		NotificationManager nm = (NotificationManager) c
				.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(id + "", Api11.MENTIONS);
	}

	public static void setReadDMs(long id, Context c) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
		sp.edit().putString("c2dm_dm_queue_" + id, "[]").commit();
		NotificationManager nm = (NotificationManager) c
				.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(id + "", Api11.DM);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return getSyncAdapter().getSyncAdapterBinder();
	}

	SyncAdapterImpl instance;

	SyncAdapterImpl getSyncAdapter() {
		if (instance == null)
			instance = new SyncAdapterImpl(this);
		return instance;
	}

	private static class SyncAdapterImpl extends BaseTwitterSync {

		public Handler handler = new Handler();

		public void addMessageToQueue(String queue, long accId, String user,
				String value) {
			try {
				SharedPreferences sp = PreferenceManager
						.getDefaultSharedPreferences(mContext);
				JSONArray ja = new JSONArray(sp.getString("c2dm_" + queue
						+ "_queue_" + accId, "[]"));
				JSONObject jo = new JSONObject();
				jo.put("user", user);
				jo.put("content", value);
				ja.put(jo);
				sp.edit()
						.putString("c2dm_" + queue + "_queue_" + accId,
								ja.toString()).commit();
			} catch (Exception e) { // Should never happen
				e.printStackTrace();
			}
		}

		public void showQueue(final String queue, final long accId,
				final Object single) {
			try {
				handler.post(new Runnable() {
					@Override
					public void run() {
						try {
							if (NightModeUtils.isNightMode(mContext)) {
								SharedPreferences prefs = PreferenceManager
										.getDefaultSharedPreferences(mContext);
								if (prefs
										.getBoolean(
												"night_mode_pause_notifications",
												false) == true) {
									return;
								}
							}

							SharedPreferences sp = PreferenceManager
									.getDefaultSharedPreferences(mContext);
							JSONArray ja = new JSONArray(sp.getString("c2dm_"
									+ queue + "_queue_" + accId, "[]"));
							Log.d("boid", ja.length() + "");
							if (ja.length() == 1 && single != null) {
								if (queue.equals("mention")) {
									Api11.displayReplyNotification((int) accId,
											mContext, (Status) single);
								} else if (queue.equals("dm")) {
									Api11.displayDirectMessageNotification(
											(int) accId, mContext,
											(DirectMessage) single);
								}
							} else {
								if (queue.equals("mention")) {
									Api11.displayMany(accId, Api11.MENTIONS,
											mContext, ja);
								} else if (queue.equals("dm")) {
									Api11.displayMany(accId, Api11.DM,
											mContext, ja);
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public SyncAdapterImpl(Context context) {
			super(context);
		}

		long getSinceId(long accId, String column) {
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(mContext);
			return prefs.getLong("c2dm_" + accId + "_since_column_" + column,
					-1);
		}

		boolean columnEnabled(long accId, String column) {
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(mContext);
			return prefs.getBoolean(accId + "_c2dm_" + column, true);
		}

		void setSinceId(long accId, String column, long since_id) {
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(mContext);
			prefs.edit()
					.putLong("c2dm_" + accId + "_since_column_" + column,
							since_id).commit();
		}

		@Override
		public void onPerformSync(Account account, Bundle extras,
				String authority, ContentProviderClient provider,
				SyncResult syncResult) {
			setupSync(account);

			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(mContext);
			if (prefs.getBoolean("notifications_global", true) == false) {
				Log.d("nf", "Globally off");
				return;
			}
			Log.d("nf", "Notifications are syncing now.");

			long accId = getId();
			Twitter client = getTwitter();

			if (columnEnabled(accId, "mention")) {
				Log.d("boid", "mention check");
				try {
					long since_id = getSinceId(accId, "mention");
					Paging paging = new Paging(5);
					if (since_id != -1)
						paging.setSinceId(since_id);

					Status[] m = client.getMentions(paging);
					Log.d("boid", m.length + " m");
					if (m.length > 0) {
						Log.d("boid", m[0].getText());
						setSinceId(accId, "mention", m[0].getId());
						for (Status status : m) {
							addMessageToQueue("mention", accId, status
									.getUser().getScreenName(),
									status.getText());
						}
						showQueue("mention", accId, m[0]);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			if (columnEnabled(accId, "dm")) {
				Log.d("boid", "dm check");
				try {
					SharedPreferences p = PreferenceManager
							.getDefaultSharedPreferences(mContext);
					boolean dm_priv = p.getBoolean(accId
							+ "_c2dm_messages_priv", false);

					long since_id = getSinceId(accId, "dm");
					Paging paging = new Paging(5);
					if (since_id != -1)
						paging.setSinceId(since_id);

					DirectMessage[] m = client.getDirectMessages(paging);
					Log.d("boid", m.length + " m");
					if (m.length > 0) {
						setSinceId(accId, "dm", m[0].getId());
						for (DirectMessage message : m) {
							String t = message.getText();
							if (dm_priv) {
								t = mContext.getString(R.string.message_recv)
										.replace("{user}", "");
							}
							addMessageToQueue("dm", accId,
									message.getSenderScreenName(), t);
						}
						showQueue("dm", accId, m[0]);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			int period = Integer.parseInt((PreferenceManager
					.getDefaultSharedPreferences(mContext)).getString(accId
					+ "_c2dm_period", "15"));
			syncResult.delayUntil = period * 60;
		}
	}
}