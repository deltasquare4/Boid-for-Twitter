package com.teamboid.twitter;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;
import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import com.teamboid.twitter.TabsAdapter.BaseGridFragment;
import com.teamboid.twitter.TabsAdapter.BaseListFragment;
import com.teamboid.twitter.TabsAdapter.BaseSpinnerFragment;

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
	public static MessageConvoAdapter messageAdapter;
	public static TrendsListAdapter trendsAdapter;
	public static SearchFeedListAdapter nearbyAdapter;
	public static ArrayList<SearchFeedListAdapter> searchFeedAdapters;
	public static int configShortURLLength;
	public static int charactersPerMedia;
	public static long selectedAccount;

	public static void clearAdapters() {
		if(feedAdapters != null) feedAdapters.clear();
		if(mediaAdapters != null) mediaAdapters.clear();
		if(messageAdapter != null) messageAdapter = null;
		if(trendsAdapter != null) trendsAdapter = null;
		if(nearbyAdapter != null) nearbyAdapter = null;
		if(searchFeedAdapters != null) searchFeedAdapters.clear();
		for(int i = 0; i < activity.getActionBar().getTabCount(); i++) {
			Fragment frag = activity.getFragmentManager().findFragmentByTag("page:" + Integer.toString(i));
			if(frag != null) {
				if(frag instanceof BaseListFragment) ((BaseListFragment)frag).reloadAdapter(false);
				else if(frag instanceof BaseSpinnerFragment) ((BaseSpinnerFragment)frag).reloadAdapter(false);
				else ((BaseGridFragment)frag).reloadAdapter(false);
			}
		}
	}
	
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

	public enum NotifyType {
		mentions, messages
	}

	//	public void showNotification(Account acc, NotifyType type, int unreadCount) {
	//		NotificationManager mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
	//		Notification notification = new Notification(R.drawable.statusbar_icon, "New mentions!", System.currentTimeMillis());
	//		PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), TimelineScreen.class), 0);
	//		notification.setLatestEventInfo(getApplicationContext(), "@" + acc.getUser().getScreenName(), 
	//				Integer.toString(unreadCount) + " new " + type.toString() + "!", contentIntent);
	//		mNotificationManager.notify(acc.getUser().getScreenName() + "_" + type.toString(), 1, notification);
	//		//TODO
	//	}
	
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

	private static void loadAccounts() {
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
							selectedAccount = accounts.get(0).getId();
							activity.sendBroadcast(new Intent(AccountManager.END_LOAD).putExtra("last_account_count", lastAccountCount == 0));
						} else activity.startActivity(new Intent(activity, AccountManager.class));
						activity.invalidateOptionsMenu();
						dialog.dismiss();
					}
				});
			}
		}).start();
	}

	public static void loadTwitterConfig() {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		long lastConfigUpdate = prefs.getLong("last_config_update", new Date().getTime() - 86400000);
		configShortURLLength = 21;
		charactersPerMedia = 21;
		if(lastConfigUpdate <= (new Date().getTime() - 86400000)) {
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
					activity.runOnUiThread(new Runnable() {
						@Override
						public void run() { loadAccounts(); }
					});
				}
			}).start();
		} else loadAccounts();
	}

	public static FeedListAdapter getFeedAdapter(Activity activity, String id) {
		if(feedAdapters == null) feedAdapters = new ArrayList<FeedListAdapter>();
		FeedListAdapter toReturn = null;
		for(FeedListAdapter adapt : feedAdapters) {
			if(id.equals(adapt.ID)) {
				toReturn = adapt;
				break;
			}
		}
		if(toReturn == null) {
			toReturn = new FeedListAdapter(activity, id);
			feedAdapters.add(toReturn);
		}
		return toReturn;
	}
	public static MediaFeedListAdapter getMediaFeedAdapter(Activity activity, String id) {
		if(mediaAdapters == null) mediaAdapters = new ArrayList<MediaFeedListAdapter>();
		MediaFeedListAdapter toReturn = null;
		for(MediaFeedListAdapter adapt : mediaAdapters) {
			if(id.equals(adapt.ID)) {
				toReturn = adapt;
				break;
			}
		}
		if(toReturn == null) {
			toReturn = new MediaFeedListAdapter(activity, id);
			mediaAdapters.add(toReturn);
		}
		return toReturn;
	}
	public static MessageConvoAdapter getMessageConvoAdapter(Activity activity) {
		if(messageAdapter == null) messageAdapter = new MessageConvoAdapter(activity);
		return messageAdapter;
	}
	public static TrendsListAdapter getTrendsAdapter(Activity activity) {
		if(trendsAdapter == null) trendsAdapter = new TrendsListAdapter(activity);
		return trendsAdapter;
	}
	public static SearchFeedListAdapter getSearchFeedAdapter(Activity activity, String id) {
		if(searchFeedAdapters == null) searchFeedAdapters = new ArrayList<SearchFeedListAdapter>();
		SearchFeedListAdapter toReturn = null;
		for(SearchFeedListAdapter adapt : searchFeedAdapters) {
			if(id.equals(adapt.ID)) {
				toReturn = adapt;
				break;
			}
		}
		if(toReturn == null) {
			toReturn = new SearchFeedListAdapter(activity, id);
			searchFeedAdapters.add(toReturn);
		}
		return toReturn;
	}
	public static SearchFeedListAdapter getNearbyAdapter(Activity activity) {
		if(nearbyAdapter == null) nearbyAdapter = new SearchFeedListAdapter(activity);
		return nearbyAdapter;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		accounts = new ArrayList<Account>();
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