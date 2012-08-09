package com.teamboid.twitter.contactsync;

import java.util.ArrayList;
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
	@Override
	public IBinder onBind(Intent arg0) {
		return getSyncAdapter().getSyncAdapterBinder();
	}
	
	SyncAdapterImpl instance;
	SyncAdapterImpl getSyncAdapter(){
		if(instance == null) instance = new SyncAdapterImpl(this);
		return instance;
	}
	
	private static class SyncAdapterImpl extends BaseTwitterSync{
		public SyncAdapterImpl(
				ContactSyncAdapterService contactSyncAdapterService) {
			super(contactSyncAdapterService);
		}
		
		public void addContact(User user){
			ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
			
			ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI);
			builder.withValue(RawContacts.ACCOUNT_NAME, account.name);
			builder.withValue(RawContacts.ACCOUNT_TYPE, account.type);
			builder.withValue(RawContacts.SYNC1, user.getScreenName());
			operationList.add(builder.build());
			
			builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
			builder.withValueBackReference(ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID, 0);
			builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
			builder.withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, user.getName());
			operationList.add(builder.build());
			
			builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
			builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
			builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE);
			builder.withValue(ContactsContract.CommonDataKinds.Nickname.NAME, user.getScreenName());
			builder.withValue(ContactsContract.CommonDataKinds.Nickname.TYPE, ContactsContract.CommonDataKinds.Nickname.TYPE_CUSTOM);
			builder.withValue(ContactsContract.CommonDataKinds.Nickname.LABEL, "Twitter Screen Name");
			operationList.add(builder.build());
			
			builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
			builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
			builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE);
			builder.withValue(ContactsContract.CommonDataKinds.Note.NOTE, user.getDescription());
			operationList.add(builder.build());
			
			builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
			builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
			builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE);
			builder.withValue(ContactsContract.CommonDataKinds.Website.URL, user.getUrl());
			builder.withValue(ContactsContract.CommonDataKinds.Website.TYPE, ContactsContract.CommonDataKinds.Website.TYPE_CUSTOM);
			builder.withValue(ContactsContract.CommonDataKinds.Website.LABEL, "Profile Link");
			operationList.add(builder.build());
			
			builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
			builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
			builder.withValue(ContactsContract.Data.MIMETYPE, "vnd.android.cursor.item/vnd.com.teamboid.twitter.account");
			builder.withValue(ContactsContract.Data.DATA1, user.getScreenName());
			builder.withValue(ContactsContract.Data.DATA2, "Twitter Profile");
			builder.withValue(ContactsContract.Data.DATA3, "View profile");
			operationList.add(builder.build());
			
			try{
				Log.d("sync", "Adding " + user.getScreenName() + " to Android");
				mContext.getContentResolver().applyBatch(ContactsContract.AUTHORITY, operationList);
			} catch(Exception e){
				Log.d("sync", "Couldn't add " + user.getScreenName());
			}
		}
		
		private void deleteContact(long rawContactId) {
			Uri uri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId).buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
			ContentProviderClient client = mContext.getContentResolver().acquireContentProviderClient(ContactsContract.AUTHORITY_URI);
			try {
				client.delete(uri, null, null);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			client.release();
		}
		
		@Override
		public void onPerformSync(Account account, Bundle extras,
				String authority, ContentProviderClient provider,
				SyncResult syncResult) {
			this.account = account;
			// Here we can actually sync
			
			// Step 1: Remove all of our existing contacts, as they are not required (we have to download all of them anyway)
			Uri rawContactUri = RawContacts.CONTENT_URI.buildUpon().appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name).appendQueryParameter(
					RawContacts.ACCOUNT_TYPE, account.type).build();
			Cursor c1 = mContext.getContentResolver().query(rawContactUri, new String[] { BaseColumns._ID, RawContacts.SYNC1 }, null, null, null);
			while (c1.moveToNext()) {
				deleteContact(c1.getLong(0)); 
			}
			
			// Step 2: Get the total number of contacts we need to download
			int total = getTotalNumber();
			int got = 0;
			
			// Step 3: Start downloading contacts
			Log.d("contactsync", "Starting with a total of " + got + " out of " + total);
			while(got < total){
				Log.d("contactsync", "Downloading more users...");
				User[] users = getTimeline();
				
				if(users == null){
					Log.d("contactsync", "Could not download users?");
					syncResult.delayUntil = 60 * 60 * 2; // sync again in 2 hours
					return;
				}
				
				for(User user : users){
					addContact(user);
				}
				got += users.length;
				
				Log.d("contactsync", "At a total of " + got + " out of " + total);
			}
			
		}
	}
}
