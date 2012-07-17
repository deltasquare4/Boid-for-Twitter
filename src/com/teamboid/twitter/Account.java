package com.teamboid.twitter;

import android.content.Context;
import android.content.SharedPreferences;
import twitter4j.Twitter;
import twitter4j.User;

/**
 * @author Aidan Follestad
 */
public class Account {

	public Account(Context context, Twitter client, String token) {
		_client = client;
		_token = token;
		prefs = context.getSharedPreferences("account_" + token, 0);
	}

	private Twitter _client;
	private String _token;
	private SharedPreferences prefs;
	private User _user;

	public Twitter getClient() { return _client; }
	public String getToken() { return _token; }
	public String getSecret() { return prefs.getString("secret", ""); }
	public User getUser() { return _user; }
	public long getId() { return _user.getId(); }

	public Account setSecret(String secret) {
		prefs.edit().putString("secret", secret).commit();
		return this;
	}
	public Account setUser(User user) {
		_user = user;
		return this;
	}
	public Account setClient(Twitter client) {
		_client = client;
		return this;
	}
	
	public String toString(){
		return "BoidUser["+ getId() + "]";
	}
}