package com.teamboid.twitter.contactsync;

import java.util.ArrayList;
import java.util.HashMap;

import com.teamboid.twitterapi.user.User;
import android.accounts.Account;
import android.app.Service;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

public class ContactSyncAdapterService extends Service {
	public static final String CONTACT_VERSION = "1";

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
		public SyncAdapterImpl(
				ContactSyncAdapterService contactSyncAdapterService) {
			super(contactSyncAdapterService);
		}

		public void addContact(User user) {
			ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();

			ContentProviderOperation.Builder builder = ContentProviderOperation
					.newInsert(RawContacts.CONTENT_URI);
			builder.withValue(RawContacts.ACCOUNT_NAME, account.name);
			builder.withValue(RawContacts.ACCOUNT_TYPE, account.type);
			builder.withValue(RawContacts.SYNC1, user.getScreenName());
			builder.withValue(RawContacts.SYNC4, CONTACT_VERSION);
			operationList.add(builder.build());

			builder = ContentProviderOperation
					.newInsert(ContactsContract.Data.CONTENT_URI);
			builder.withValueBackReference(
					ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID,
					0);
			builder.withValue(
					ContactsContract.Data.MIMETYPE,
					ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
			builder.withValue(
					ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
					user.getName());
			operationList.add(builder.build());

			builder = ContentProviderOperation
					.newInsert(ContactsContract.Data.CONTENT_URI);
			builder.withValueBackReference(
					ContactsContract.Data.RAW_CONTACT_ID, 0);
			builder.withValue(ContactsContract.Data.MIMETYPE,
					ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE);
			builder.withValue(ContactsContract.CommonDataKinds.Nickname.NAME,
					user.getScreenName());
			builder.withValue(ContactsContract.CommonDataKinds.Nickname.TYPE,
					ContactsContract.CommonDataKinds.Nickname.TYPE_CUSTOM);
			builder.withValue(ContactsContract.CommonDataKinds.Nickname.LABEL,
					"Twitter Screen Name");
			operationList.add(builder.build());

			builder = ContentProviderOperation
					.newInsert(ContactsContract.Data.CONTENT_URI);
			builder.withValueBackReference(
					ContactsContract.Data.RAW_CONTACT_ID, 0);
			builder.withValue(ContactsContract.Data.MIMETYPE,
					ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE);
			builder.withValue(ContactsContract.CommonDataKinds.Note.NOTE,
					user.getDescription());
			operationList.add(builder.build());

			builder = ContentProviderOperation
					.newInsert(ContactsContract.Data.CONTENT_URI);
			builder.withValueBackReference(
					ContactsContract.Data.RAW_CONTACT_ID, 0);
			builder.withValue(ContactsContract.Data.MIMETYPE,
					ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE);
			builder.withValue(ContactsContract.CommonDataKinds.Website.URL,
					user.getUrl());
			builder.withValue(ContactsContract.CommonDataKinds.Website.TYPE,
					ContactsContract.CommonDataKinds.Website.TYPE_CUSTOM);
			builder.withValue(ContactsContract.CommonDataKinds.Website.LABEL,
					"Profile Link");
			operationList.add(builder.build());

			builder = ContentProviderOperation
					.newInsert(ContactsContract.Data.CONTENT_URI);
			builder.withValueBackReference(
					ContactsContract.Data.RAW_CONTACT_ID, 0);
			builder.withValue(ContactsContract.Data.MIMETYPE,
					"vnd.android.cursor.item/vnd.com.teamboid.twitter.account");
			builder.withValue(ContactsContract.Data.DATA1, user.getScreenName());
			builder.withValue(ContactsContract.Data.DATA2, "Twitter Profile");
			builder.withValue(ContactsContract.Data.DATA3, "View profile");
			operationList.add(builder.build());

			try {
				Log.d("sync", "Adding " + user.getScreenName() + " to Android");
				mContext.getContentResolver().applyBatch(
						ContactsContract.AUTHORITY, operationList);
			} catch (Exception e) {
				Log.d("sync", "Couldn't add " + user.getScreenName());
			}
		}

		private void deleteContact(long rawContactId) {
			Uri uri = ContentUris
					.withAppendedId(RawContacts.CONTENT_URI, rawContactId)
					.buildUpon()
					.appendQueryParameter(
							ContactsContract.CALLER_IS_SYNCADAPTER, "true")
					.build();
			ContentProviderClient client = mContext.getContentResolver()
					.acquireContentProviderClient(
							ContactsContract.AUTHORITY_URI);
			try {
				client.delete(uri, null, null);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			client.release();
		}

		class TempoaryContactDetails {
			public String version;
			public Long id;

			public TempoaryContactDetails(String v, Long rawid) {
				id = rawid;
				version = v;
			}
		}

		HashMap<String, TempoaryContactDetails> existingAccounts;

		@Override
		public void onPerformSync(Account account, Bundle extras,
				String authority, ContentProviderClient provider,
				SyncResult syncResult) {
			this.account = account;
			// Here we can actually sync
			existingAccounts = new HashMap<String, TempoaryContactDetails>();

			// Step 1: Get all existing contacts with username, raw contact ID
			// (to remove) and version
			Uri rawContactUri = RawContacts.CONTENT_URI
					.buildUpon()
					.appendQueryParameter(RawContacts.ACCOUNT_NAME,
							account.name)
					.appendQueryParameter(RawContacts.ACCOUNT_TYPE,
							account.type).build();
			Cursor c1 = mContext.getContentResolver().query(
					rawContactUri,
					new String[] { BaseColumns._ID, RawContacts.SYNC1,
							RawContacts.SYNC4 }, null, null, null);
			while (c1.moveToNext()) {
				existingAccounts.put(
						c1.getString(2),
						new TempoaryContactDetails(c1.getString(1), c1
								.getLong(0)));
			}

			// Step 2: Get the total number of contacts we need to download
			int total = getTotalNumber();
			int got = 0;

			// Step 3: Start downloading contacts
			Log.d("contactsync", "Starting with a total of " + got + " out of "
					+ total);
			while (got < total) {
				Log.d("contactsync", "Downloading more users...");
				User[] users = getTimeline();

				if (users == null) {
					Log.d("contactsync", "Could not download users?");
					syncResult.delayUntil = 60 * 60 * 2; // sync again in 2
															// hours
					return;
				}

				for (User user : users) {
					if (existingAccounts.containsKey(user.getScreenName())) {
						// If the account is out of date, re-add otherwise we
						// leave it
						if (!existingAccounts.get(user.getScreenName()).version
								.equals(CONTACT_VERSION)) {
							deleteContact(existingAccounts.get(user
									.getScreenName()).id);
							addContact(user);
						}
						// Delete out of array, so we don't bin the contact
						existingAccounts.remove(user.getScreenName());
					} else {
						addContact(user);
					}
				}
				got += users.length;

				Log.d("contactsync", "At a total of " + got + " out of "
						+ total);
			}

			Log.d("contactsync", "Deleting " + existingAccounts.size()
					+ " dead accounts from system");
			for (TempoaryContactDetails acc : existingAccounts.values()) {
				deleteContact(acc.id);
			}

			Log.d("contactsync", "Sync has completed. Party!");
		}
	}
}
