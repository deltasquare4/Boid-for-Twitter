package com.teamboid.twitter.contactsync;

import android.accounts.Account;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;

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
		public SyncAdapterImpl(Context context) {
			super(context, true);
			mContext = context;
		}
		
		public void addContact(twitter4j.User user){
			
		}
		
		@Override
		public void onPerformSync(Account account, Bundle extras,
				String authority, ContentProviderClient provider,
				SyncResult syncResult) {
			// Here we can actually sync
		}
		
		
		
	}
	
	

}
