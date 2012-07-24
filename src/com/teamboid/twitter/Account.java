package com.teamboid.twitter;

import org.json.JSONException;
import org.json.JSONObject;

import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;

import android.content.Context;
import android.content.SharedPreferences;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.Configuration;

/**
 * @author Aidan Follestad
 */
public class Account {

	public Account(Context context, Twitter client, String token) {
		_client = client;
		_token = token;
		prefs = context.getSharedPreferences("account_" + token, 0);
	}
	
	public JSONObject serialize() throws JSONException{
		JSONObject r = new JSONObject();
		r.put("key", _token);
		r.put("secret", getSecret());
		r.put("user", Utilities.serializeObject( getUser()));
		return r;
	}
	
	public static Account unserialize( Context c, JSONObject i ) throws JSONException{
		Configuration co = AccountService.getConfiguration(i.getString("key"), i.getString("secret")).build();
		Twitter client = new TwitterFactory(co).getInstance();
		Account r = new Account( c, client, i.getString("key") );
		r.setUser( (User) Utilities.deserializeObject( i.getString("user") ) );
		r.setSecret( i.getString("secret") );
		return r;
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