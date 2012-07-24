package com.teamboid.twitter.contactsync;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.teamboid.twitter.services.AccountService;

import twitter4j.IDs;
import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
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
		
		public void addContact(twitter4j.User user){
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
			
			AccountManager am = AccountManager.get(mContext);
			_id = Long.parseLong( am.getUserData(account, "accId") );
			return _id;
		}
		
		Twitter getTwitter(){
			AccountService.activity = mContext;
			AccountService.getAccounts(); // New Array
			AccountService.loadCachedAccounts();
			
			return AccountService.getAccount( getId() ).getClient();
		}
		
		String getWhatToSync(){ // TODO: Actually make this return something the user wants
			return "following";
		}
		
		int getTotalNumber(){
			try{
				Twitter client = getTwitter();
				String type = getWhatToSync();
				
				if(type.equals("following")){
					return client.verifyCredentials().getFriendsCount();
				} else if(type.equals("followers")){
					return client.verifyCredentials().getFollowersCount();
				}
			}catch(Exception e){ e.printStackTrace(); return -1; }
			return -1;
		}
		
		List<User> getTimeline(Paging paging){
			try{
				Twitter client = getTwitter();
				String type = getWhatToSync();
				
				if(type.equals("following")){
					IDs ids = client.getFriendsIDs( getId());
					return client.lookupUsers( ids.getIDs() );
				} else if(type.equals("followers")){
					IDs ids = client.getFollowersIDs( getId());
					return client.lookupUsers( ids.getIDs() );
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
			Paging p = new Paging();
			
			while(got > total){
				List<User> users = getTimeline(p);
				
				if(users == null){
					// Error?
					return;
				}
				
				for(User user : users){
					addContact(user);
					
					// TODO: add to some fast cache for username autocompletion
				}
				got += users.size();
				
				p.setPage( p.getPage() + 1 );
			}
			
		}
		
		
		
	}
	
	

}
