package com.teamboid.twitter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

/**
 * The service that stays running the background; authorizes, loads, and manages the current user's accounts.
 * @author Aidan Follestad
 */
public class AccountService extends Service {

	public static Twitter pendingClient;
	public static Activity activity;
	private static ArrayList<Account> accounts;
	public static ArrayList<FeedListAdapter> feedAdapters;
	public static ArrayList<MediaFeedListAdapter> mediaAdapters;
	public static ArrayList<MessageConvoAdapter> messageAdapters;
	public static TrendsListAdapter trendsAdapter;
	public static SearchFeedListAdapter nearbyAdapter;
	public static ArrayList<SearchFeedListAdapter> searchFeedAdapters;
	public static int configShortURLLength;
	public static int charactersPerMedia;
	public static long selectedAccount;

	public static ArrayList<Account> getAccounts() {
		if(accounts == null) accounts = new ArrayList<Account>();
		return accounts;
	}
	public static boolean existsAccount(long accId) {
		boolean found = false;
		for(int i = 0; i < accounts.size(); i++) {
			if(accounts.get(i).getId() == accId) {
				found = true;
				break;
			}
		}
		return found;
	}
	public static void setAccount(int index, Account acc) {
		if(accounts == null) accounts = new ArrayList<Account>();
		accounts.set(index, acc);
	}
	public static Account getCurrentAccount() {
		if(selectedAccount == 0) return null;
		Account toReturn = null;
		for(Account acc : getAccounts()) {
			if(acc.getUser().getId() == selectedAccount) {
				toReturn = acc;
				break;
			}
		}
		return toReturn;
	}
	public static void removeAccount(Context activity, Account acc) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		prefs.edit().remove(Long.toString(acc.getId()) + "_columns").commit();
		prefs.edit().remove(Long.toString(acc.getId()) + "_muting").commit();
		activity.getSharedPreferences("accounts", 0).edit().remove(acc.getToken()).commit();
		for(int i = 0; i < accounts.size(); i++) {
			if(accounts.get(i).getToken().equals(acc.getToken())) {
				accounts.remove(i);
				break;
			}
		}
	}
	
	public static ConfigurationBuilder getConfiguration(String token, String secret){
		return new ConfigurationBuilder()
			.setDebugEnabled(true)
			.setOAuthConsumerKey("5LvP1d0cOmkQleJlbKICtg")
			.setOAuthConsumerSecret("j44kDQMIDuZZEvvCHy046HSurt8avLuGeip2QnOpHKI")
			.setOAuthAccessToken(token)
			.setOAuthAccessTokenSecret(secret)
			.setUseSSL(PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("enable_ssl", false));
	}
	
	public static void verifyAccount(final String verifier) {
		final Toast act = Toast.makeText(activity, activity.getString(R.string.authorizing_account), Toast.LENGTH_LONG);
		act.show();
		new Thread(new Runnable() {
			public void run() {
				try {
					if(pendingClient == null) {
						activity.runOnUiThread(new Runnable() {
							@Override
							public void run() { 
								act.cancel();
								Toast.makeText(activity, activity.getString(R.string.authorization_error), Toast.LENGTH_LONG).show();
								activity.sendBroadcast(new Intent(AccountManager.END_LOAD));
							}
						});
						return;
					}
					final AccessToken accessToken = pendingClient.getOAuthAccessToken(verifier);
					ConfigurationBuilder cb = getConfiguration(accessToken.getToken(), accessToken.getTokenSecret());
					final Twitter toAdd = new TwitterFactory(cb.build()).getInstance();
					final User toAddUser = toAdd.verifyCredentials();
					ArrayList<Account> accs = getAccounts();
					for(Account user : accs) {
						if(user.getUser().getId() == toAddUser.getId()) {
							activity.runOnUiThread(new Runnable() {
								@Override
								public void run() { 
									act.cancel();
									Toast.makeText(activity, activity.getString(R.string.account_already_added), Toast.LENGTH_LONG).show();
									activity.sendBroadcast(new Intent(AccountManager.END_LOAD));
								}
							});
							return;
						}
					}
					activity.getSharedPreferences("accounts", 0).edit().putString(accessToken.getToken(), accessToken.getTokenSecret()).commit();
					accounts.add(new Account(activity, toAdd, accessToken.getToken()).setSecret(accessToken.getTokenSecret()).setUser(toAddUser));
					pendingClient = null;
					activity.runOnUiThread(new Runnable() {
						@Override
						public void run() { 
							act.cancel();
							activity.sendBroadcast(new Intent(AccountManager.END_LOAD));
						}
					});
				} catch (final TwitterException e) {
					e.printStackTrace();
					activity.runOnUiThread(new Runnable() {
						@Override
						public void run() { 
							act.cancel();
							Toast.makeText(activity, activity.getString(R.string.authorization_error) + " " + e.getErrorMessage(), Toast.LENGTH_LONG).show();
							activity.sendBroadcast(new Intent(AccountManager.END_LOAD));
						}
					});
				}
			}
		}).start();
	}

	public static void loadAccounts() {
		if(activity == null) return;
		final Map<String, ?> accountStore = activity.getSharedPreferences("accounts", 0).getAll();
		if(accountStore.size() == 0) {
			activity.startActivity(new Intent(activity, AccountManager.class));			
			return;
		} else if(getAccounts().size() == accountStore.size()) return;
		if(!NetworkUtils.haveNetworkConnection(activity)) {
			Toast.makeText(activity, activity.getString(R.string.no_internet), Toast.LENGTH_LONG).show();
			return;
		}
		final int lastAccountCount = getAccounts().size();
		final ProgressDialog dialog = ProgressDialog.show(activity, "", activity.getString(R.string.loading_accounts), true);
		new Thread(new Runnable() {
			public void run() {
				for(final String token : accountStore.keySet()) {
					boolean skip = false;
					for(int i = 0; i < accounts.size(); i++) {
						Account acc = accounts.get(i);
						if(acc.getToken().equals(token)) {
							skip = true;
							break;
						}
					}
					if(skip) continue;
					ConfigurationBuilder cb = getConfiguration(token, accountStore.get(token).toString());
					final Twitter toAdd = new TwitterFactory(cb.build()).getInstance();
					try {
						final User accountUser = toAdd.verifyCredentials();
						accounts.add(new Account(activity, toAdd, token).setSecret(accountStore.get(token).toString()).setUser(accountUser));
					} catch (final TwitterException e) {
						e.printStackTrace();
						activity.runOnUiThread(new Runnable() {
							@Override
							public void run() { Toast.makeText(activity, activity.getString(R.string.failed_load_account) + " " + e.getErrorMessage(), Toast.LENGTH_LONG).show(); }
						});
					}
				}
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if(getAccounts().size() > 0) {
							if(getAccounts().size() != lastAccountCount) {
								selectedAccount = accounts.get(0).getId();
								activity.sendBroadcast(new Intent(AccountManager.END_LOAD).putExtra("last_account_count", lastAccountCount == 0));
							}
						} else activity.startActivity(new Intent(activity, AccountManager.class));
						activity.invalidateOptionsMenu();
						dialog.dismiss();
					}
				});
			}
		}).start();
	}

	private static void loadTwitterConfig() {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		long lastConfigUpdate = prefs.getLong("last_config_update", new Date().getTime() - 86400000);
		configShortURLLength = 21;
		charactersPerMedia = 21;
		if(lastConfigUpdate <= (new Date().getTime() - 86400000)) {
			Log.i("BOID", "Loading Twitter config (this should only happen once every 24 hours)...");
			new Thread(new Runnable() {
				public void run() {
					try {
						final Twitter tempClient = new TwitterFactory().getInstance();
						tempClient.setOAuthConsumer("5LvP1d0cOmkQleJlbKICtg", "j44kDQMIDuZZEvvCHy046HSurt8avLuGeip2QnOpHKI");
						TwitterAPIConfiguration config = tempClient.getAPIConfiguration();
						configShortURLLength = config.getShortURLLength();
						charactersPerMedia = config.getCharactersReservedPerMedia();
						prefs.edit().putInt("shorturl_length", config.getShortURLLength()).putLong("last_config_update", new Date().getTime())
							.putInt("mediachars_length", config.getCharactersReservedPerMedia()).commit();
					} catch(final TwitterException e) {
						e.printStackTrace();
						configShortURLLength = 21;
						charactersPerMedia = 21;
						activity.runOnUiThread(new Runnable() {
							@Override
							public void run() { 
								Toast.makeText(activity, activity.getString(R.string.failed_fetch_config).replace("{reason}", e.getErrorMessage()), Toast.LENGTH_LONG).show();
							}
						});
					}
				}
			}).start();
		}
	}

	public static FeedListAdapter getFeedAdapter(Activity activity, String id, long account) {
		if(feedAdapters == null) feedAdapters = new ArrayList<FeedListAdapter>();
		FeedListAdapter toReturn = null;
		for(FeedListAdapter adapt : feedAdapters) {
			if(id.equals(adapt.ID) && account == adapt.account) {
				toReturn = adapt;
				break;
			}
		}
		if(toReturn == null) {
			toReturn = new FeedListAdapter(activity, id, account);
			feedAdapters.add(toReturn);
		}
		return toReturn;
	}
	public static void clearFeedAdapter(Activity activity, String id, long account) {
		if(feedAdapters == null) return;
		for(int i = 0; i < feedAdapters.size(); i++) {
			FeedListAdapter curAdapt = feedAdapters.get(i); 
			if(curAdapt.ID.equals(id) && curAdapt.account == account) {
				curAdapt.clear();
				feedAdapters.set(i, curAdapt);
				break;
			}
		}
	}
	public static MediaFeedListAdapter getMediaFeedAdapter(Activity activity, String id, long account) {
		if(mediaAdapters == null) mediaAdapters = new ArrayList<MediaFeedListAdapter>();
		MediaFeedListAdapter toReturn = null;
		for(MediaFeedListAdapter adapt : mediaAdapters) {
			if(id.equals(adapt.ID) && account == adapt.account) {
				toReturn = adapt;
				break;
			}
		}
		if(toReturn == null) {
			toReturn = new MediaFeedListAdapter(activity, id, account);
			mediaAdapters.add(toReturn);
		}
		return toReturn;
	}
	public static MessageConvoAdapter getMessageConvoAdapter(Activity activity, long account) {
		if(messageAdapters == null) messageAdapters = new ArrayList<MessageConvoAdapter>();
		MessageConvoAdapter toReturn = null;
		for(MessageConvoAdapter adapt : messageAdapters) {
			if(account == adapt.account) {
				toReturn = adapt;
				break;
			}
		}
		if(toReturn == null) {
			toReturn = new MessageConvoAdapter(activity, account);
			messageAdapters.add(toReturn);
		}
		return toReturn;
	}
	public static TrendsListAdapter getTrendsAdapter(Activity activity) {
		if(trendsAdapter == null) trendsAdapter = new TrendsListAdapter(activity);
		return trendsAdapter;
	}
	public static SearchFeedListAdapter getSearchFeedAdapter(Activity activity, String id, long account) {
		if(searchFeedAdapters == null) searchFeedAdapters = new ArrayList<SearchFeedListAdapter>();
		SearchFeedListAdapter toReturn = null;
		for(SearchFeedListAdapter adapt : searchFeedAdapters) {
			if(id.equals(adapt.ID) && account == adapt.account) {
				toReturn = adapt;
				break;
			}
		}
		if(toReturn == null) {
			toReturn = new SearchFeedListAdapter(activity, id, account);
			searchFeedAdapters.add(toReturn);
		}
		return toReturn;
	}
	public static SearchFeedListAdapter getNearbyAdapter(Activity activity) {
		if(nearbyAdapter == null) nearbyAdapter = new SearchFeedListAdapter(activity, 0);
		return nearbyAdapter;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		accounts = new ArrayList<Account>();
		loadTwitterConfig();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}
	@Override
	public void onDestroy() { super.onDestroy(); }
	@Override
	public IBinder onBind(Intent intent) { return null; }

	public static Account getAccount(long accId) {
		Account result = null;
		for(int i = 0; i < accounts.size(); i++) {
			if(accounts.get(i).getId() == accId) {
				result = accounts.get(i);
			}
		}
		return result;
	}
}