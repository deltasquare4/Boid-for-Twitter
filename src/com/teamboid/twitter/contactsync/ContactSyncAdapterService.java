package com.teamboid.twitter.contactsync;

import java.util.ArrayList;
import java.util.List;

import com.teamboid.twitter.AccountManager;
import com.teamboid.twitter.services.AccountService;

import com.teamboid.twitterapi.client.Paging;
import com.teamboid.twitterapi.client.Twitter;
import com.teamboid.twitterapi.relationship.IDs;
import com.teamboid.twitterapi.user.User;
import com.teamboid.twitterapi.utilities.Utils;

import android.accounts.Account;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
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
			
			return (Twitter) Utils.deserializeObject( s );
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
		
		User[] getTimeline(Paging paging){
			try{
				Twitter client = getTwitter();
				String type = getWhatToSync();
				
				if(type.equals("following")){
					IDs ids = client.getFriends( getId(), paging.getMaxId() );
					return client.lookupUsers( ids.getIds() );
				} else if(type.equals("followers")){
					IDs ids = client.getFollowers( getId(), paging.getMaxId() );
					return client.lookupUsers( ids.getIds() );
				}
			}catch(Exception e){ e.printStackTrace(); return null; }
			return null;
		}
		
		@Override
		public void onPerformSync(Account account, Bundle extras,
				String authority, ContentProviderClient provider,
				SyncResult syncResult) {
			this.account = account;
			// Here we can actually sync
			
			int total = getTotalNumber();
			int got = 0;
			Paging p = new Paging(0);
			
			while(got > total){
				User[] users = getTimeline(p);
				
				if(users == null){
					// Error?
					return;
				}
				
				for(User user : users){
					addContact(user);
					
					// TODO: add to some fast cache for username autocompletion
				}
				got += users.length;
				
				p.setMaxId( p.getMaxId() + got );
			}
			
		}
		
		
		
	}
}
