package com.teamboid.twitter.contactsync;

import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.provider.ContactsContract;

import com.teamboid.twitter.Account;

public class AndroidAccountHelper {
	public static final String ACCOUNT_TYPE = "com.teamboid.twitter.account";
	public static final Boolean AUTO_SYNC_CONTACTS = true;
	
	public static void addAccount(Context c, Account a){
		if(accountExists(c, a)) return;
		
		AccountManager am = AccountManager.get(c);
		
		Bundle userdata = new Bundle();
		userdata.putString("accId", a.getId() + "");
		
		android.accounts.Account toAdd = getAccount(a);
		am.addAccountExplicitly( toAdd, "TRY.HARDER.HACKER", userdata);
		ContentResolver.setSyncAutomatically(toAdd, ContactsContract.AUTHORITY, AUTO_SYNC_CONTACTS);
	}
	
	public static android.accounts.Account getAccount(Account a){
		return new android.accounts.Account(
				a.getUser().getScreenName(),
				ACCOUNT_TYPE);
	}
	
	public static boolean accountExists(Context c, Account a){
		AccountManager am = AccountManager.get(c);
		android.accounts.Account[] accs = am.getAccountsByType(ACCOUNT_TYPE);
		for(android.accounts.Account acc : accs){
			if( am.getUserData(acc, "accId").equals( a.getId()+"" ) ){
				return true;
			}
		}
		return false;
	}
}
