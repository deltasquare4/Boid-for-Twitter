package com.teamboid.twitter;

import java.io.Serializable;

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
	/**
	 * Stores a long of when we last actually got our user
	 */
	private Long lastRefreshed = -1L;
	private transient Twitter _client;

	public Twitter getClient() { return _client; }
	public String getToken() { return _accessToken; }
	public String getSecret() { return _accessSecret; }
	public User getUser() { return _user; }
	public long getId() { return _user.getId(); }

	public Account setUser(User user) {
		_user = user;
		lastRefreshed = System.currentTimeMillis() / 1000; // seconds
		return this;
	}
	public Account setClient(Twitter client) {
		_client = client;
		return this;
	}
	
	public void refreshUserIfNeeded() throws Exception{
		if(lastRefreshed == null){ lastRefreshed = System.currentTimeMillis(); return; }
		if(lastRefreshed <= (System.currentTimeMillis() / 1000) - (60*60*12)){
			// Account is older than 12 hours, request in the background to reload
			new Thread(new Runnable(){

				@Override
				public void run() {
					try {
						_user = _client.verifyCredentials();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
			}).start();
		}
	}
	
	public String toString() {
		return "BoidUser["+ getId() + "]";
	}
}