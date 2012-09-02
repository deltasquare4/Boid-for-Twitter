package com.teamboid.twitter.contactsync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitterapi.client.Authorizer;
import com.teamboid.twitterapi.client.Twitter;
import com.teamboid.twitterapi.utilities.Utils;

public abstract class BaseTwitterSync extends AbstractThreadedSyncAdapter {
	public Context mContext;
	public Account account;

	public BaseTwitterSync(Context context) {
		super(context, true);
		mContext = context;
	}

	long _id = -1;

	public void setupSync(Account account){
		this.account = account;
	}
	
	public Long getId() {
		if (_id != -1)
			return _id;

		android.accounts.AccountManager am = android.accounts.AccountManager
				.get(mContext);
		_id = Long.parseLong(am.getUserData(account, "accId"));
		return _id;
	}

	public Twitter getTwitter() {
		SharedPreferences sp = mContext.getSharedPreferences("profiles-v2",
				Context.MODE_PRIVATE);
		String s = sp.getString(getId() + "", null);
		if (s == null)
			return null;

		com.teamboid.twitter.Account toAdd = (com.teamboid.twitter.Account) Utils
				.deserializeObject(s);
		Log.d("contactsync", "Hello " + toAdd.getId());
		return Authorizer.create(AccountService.CONSUMER_KEY,
				AccountService.CONSUMER_SECRET, AccountService.CALLBACK_URL)
				.getAuthorizedInstance(toAdd.getToken(), toAdd.getSecret());
	}
}
