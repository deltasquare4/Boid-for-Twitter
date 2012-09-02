package com.teamboid.twitter.contactsync;

import java.util.HashMap;

import com.teamboid.twitter.Account;
import com.teamboid.twitter.notifications.NotificationService;

import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.provider.ContactsContract;

public class AndroidAccountHelper {
	public static final String ACCOUNT_TYPE = "com.teamboid.twitter.account";
	
	public static final Boolean AUTO_SYNC_CONTACTS = false;
	public static final Boolean AUTO_SYNC_NAMES = true;
	public static final Boolean AUTO_SYNC_NOTIFICATIONS = true;

	public static void addAccount(Context c, Account a) {
		if (accountExists(c, a))
			return;

		AccountManager am = AccountManager.get(c);

		Bundle userdata = new Bundle();
		userdata.putString("accId", a.getId() + "");

		android.accounts.Account toAdd = _getAccount(a);
		am.addAccountExplicitly(toAdd, "TRY.HARDER.HACKER", userdata);
		am.setUserData(toAdd, "accId", a.getId() + "");
		
		// Contacts
		setServiceSync(ContactsContract.AUTHORITY, AUTO_SYNC_CONTACTS, toAdd);
		
		// Autocomplete
		setServiceSync(AutocompleteService.AUTHORITY, AUTO_SYNC_NAMES, toAdd);
		
		// Notifications
		setServiceSync(NotificationService.AUTHORITY, AUTO_SYNC_NOTIFICATIONS, toAdd);
	}
	
	public static void setServiceSync(String Authority, boolean on, android.accounts.Account account){
		ContentResolver.setSyncAutomatically(account,
				Authority, on);
		if(on)
			ContentResolver.requestSync(account, Authority, new Bundle());
	}

	public static HashMap<String, android.accounts.Account> getAccounts(
			Context c) {
		AccountManager am = AccountManager.get(c);
		android.accounts.Account[] acc = am.getAccountsByType(ACCOUNT_TYPE);
		HashMap<String, android.accounts.Account> r = new HashMap<String, android.accounts.Account>();
		for (android.accounts.Account a : acc) {
			r.put(am.getUserData(a, "accId"), a);
		}
		return r;
	}

	static android.accounts.Account _getAccount(Account a) {
		return new android.accounts.Account(a.getUser().getScreenName(),
				ACCOUNT_TYPE);
	}

	public static android.accounts.Account getAccount(Context c, Account ba) {
		AccountManager am = AccountManager.get(c);
		android.accounts.Account[] acc = am.getAccountsByType(ACCOUNT_TYPE);
		for (android.accounts.Account a : acc) {
			if (am.getUserData(a, "accId").equals(ba.getId() + "")) {
				return a;
			}
		}
		return null;
	}

	public static boolean accountExists(Context c, Account a) {
		AccountManager am = AccountManager.get(c);
		android.accounts.Account[] accs = am.getAccountsByType(ACCOUNT_TYPE);
		for (android.accounts.Account acc : accs) {
			if (am.getUserData(acc, "accId").equals(a.getId() + "")) {
				return true;
			}
		}
		return false;
	}
}
