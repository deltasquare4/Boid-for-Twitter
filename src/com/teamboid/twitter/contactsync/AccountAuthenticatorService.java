package com.teamboid.twitter.contactsync;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

public class AccountAuthenticatorService extends Service {

	public IBinder onBind(Intent intent) {
		IBinder ret = null;
		if (intent.getAction().equals(
				android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT))
			ret = getAuthenticator().getIBinder();
		return ret;
	}

	AccountAuthenticatorImpl instance;

	AccountAuthenticatorImpl getAuthenticator() {
		if (instance == null)
			instance = new AccountAuthenticatorImpl(this);
		return instance;
	}

	private static class AccountAuthenticatorImpl extends
			AbstractAccountAuthenticator {
		Context mContext;

		public AccountAuthenticatorImpl(Context context) {
			super(context);
			mContext = context;
		}

		@Override
		public Bundle addAccount(AccountAuthenticatorResponse response,
				String arg1, String arg2, String[] arg3, Bundle arg4)
				throws NetworkErrorException {
			Bundle reply = new Bundle();

			Intent i = new Intent(mContext, AccountManager.class);
			i.putExtra(
					android.accounts.AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
					response);
			reply.putParcelable(android.accounts.AccountManager.KEY_INTENT, i);

			return reply;
		}

		@Override
		public Bundle confirmCredentials(AccountAuthenticatorResponse arg0,
				Account arg1, Bundle arg2) throws NetworkErrorException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Bundle editProperties(AccountAuthenticatorResponse arg0,
				String arg1) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Bundle getAuthToken(AccountAuthenticatorResponse arg0,
				Account arg1, String arg2, Bundle arg3)
				throws NetworkErrorException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getAuthTokenLabel(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Bundle hasFeatures(AccountAuthenticatorResponse arg0,
				Account arg1, String[] arg2) throws NetworkErrorException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Bundle updateCredentials(AccountAuthenticatorResponse arg0,
				Account arg1, String arg2, Bundle arg3)
				throws NetworkErrorException {
			// TODO Auto-generated method stub
			return null;
		}
	}

}
