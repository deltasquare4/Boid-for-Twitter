package com.teamboid.twitter.contactsync;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitterapi.client.Authorizer;
import com.teamboid.twitterapi.client.Twitter;
import com.teamboid.twitterapi.relationship.IDs;
import com.teamboid.twitterapi.user.User;
import com.teamboid.twitterapi.utilities.Utils;

import android.accounts.Account;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.content.res.AssetFileDescriptor;
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
	
	private static class SyncAdapterImpl extends AbstractThreadedSyncAdapter{
		Context mContext;
		Account account;
		public SyncAdapterImpl(Context context) {
			super(context, true);
			mContext = context;
		}
		
		private static void saveBitmapToRawContact(Context context, long rawContactId, byte[] photo) throws Exception {
		    Uri rawContactUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId);
		    Uri outputFileUri =
		        Uri.withAppendedPath(rawContactUri, RawContacts.DisplayPhoto.CONTENT_DIRECTORY);
		    AssetFileDescriptor descriptor = context.getContentResolver().openAssetFileDescriptor(
		        outputFileUri, "rw");
		    FileOutputStream stream = descriptor.createOutputStream();
		    try {
		      stream.write(photo);
		    } finally {
		      stream.close();
		      descriptor.close();
		    }
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
		
		long _id = -1;
		Long getId(){
			if(_id != -1) return _id;
			
			android.accounts.AccountManager am = android.accounts.AccountManager.get(mContext);
			_id = Long.parseLong( am.getUserData(account, "accId") );
			return _id;
		}
		
		Twitter getTwitter(){
			SharedPreferences sp = mContext.getSharedPreferences("profiles-v2", Context.MODE_PRIVATE);
			String s = sp.getString(getId() + "", null);
			if(s == null) return null;
			
			com.teamboid.twitter.Account toAdd = (com.teamboid.twitter.Account) Utils.deserializeObject( s );
			Log.d("contactsync", "Hello " + toAdd.getId());
			return Authorizer.create(AccountService.CONSUMER_KEY, AccountService.CONSUMER_SECRET, AccountService.CALLBACK_URL)
					.getAuthorizedInstance(toAdd.getToken(), toAdd.getSecret());
		}
		
		String getWhatToSync(){ // TODO: Actually make this return something the user wants
			return "following";
		}
		
		int getTotalNumber(){
			try{
				Twitter client = getTwitter();
				String type = getWhatToSync();
				
				if(type.equals("following")){
					return (int) client.verifyCredentials().getFriendsCount();
				} else if(type.equals("followers")){
					return (int) client.verifyCredentials().getFollowersCount();
				}
			}catch(Exception e){ e.printStackTrace(); return -1; }
			return -1;
		}
		
		// Notes:
		// This works by having a queue `idQueue` which contains up to 1000 ids
		// which is how twitter works, but when it empties we grab more if needed
		// and we drain them out into batches of 100 to query Twitter with
		Queue<Long> idQueue = new LinkedList<Long>();
		long cursor = -1;
		
		User[] getTimeline(){
			try{
				Twitter client = getTwitter();
				String type = getWhatToSync();
				
				if(idQueue.isEmpty()){ // If we have no more IDs Left in the queue
					IDs ids = null;
					if(type.equals("following")){
						ids = client.getFriends( getId(), cursor );
					} else if(type.equals("followers")){
						ids = client.getFollowers( getId(), cursor );
					} else{
						Log.d("contactsync", "Righto, someone is hacking our app. Let's just let it crash");
					}
					
					cursor = ids.getNextCursor();
					for(Long id : ids.getIds()){
						idQueue.add(id);
					}
					// Now the queue is stocked up with up to 5000 IDs (as twitter says).
				}
				
				// Off-load up to 100 ids from the queue
				Long[] ids = new Long[ idQueue.size() >= 100 ? 100 : idQueue.size() ];
				for(int i = 0; i < ids.length; i++){
					ids[i] = idQueue.remove();
				}
				
				// Now fetch information about them
				return client.lookupUsers( ids );
			}catch(Exception e){ e.printStackTrace(); return null; }
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
					
					// TODO: add to some fast cache for username autocompletion
				}
				got += users.length;
				
				Log.d("contactsync", "At a total of " + got + " out of " + total);
			}
			
		}
	}
}
