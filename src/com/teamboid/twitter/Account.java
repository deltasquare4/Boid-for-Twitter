package com.teamboid.twitter;

import java.io.Serializable;

import android.content.Context;
import com.teamboid.twitterapi.client.Twitter;
import com.teamboid.twitterapi.user.User;

/**
 * @author Aidan Follestad
 */
public class Account implements Serializable {

	private static final long serialVersionUID = 5774596574060143207L;

	public Account() { }
	public Account(Twitter client) {
		_client = client;
		try {
			_accessToken = client.getAccessToken();
			_accessSecret = client.getAccessSecret();
		} catch(Exception e) { e.printStackTrace(); }
	}
	
	private String _accessToken;
	private String _accessSecret;
	private User _user;
	private transient Twitter _client;

	public Twitter getClient() { return _client; }
	public String getToken() { return _accessToken; }
	public String getSecret() { return _accessSecret; }
	public User getUser() { return _user; }
	public long getId() { return _user.getId(); }

	public Account setUser(User user) {
		_user = user;
		return this;
	}
	public Account setClient(Twitter client) {
		_client = client;
		return this;
	}
	
	public String toString() {
		return "BoidUser["+ getId() + "]";
	}
}