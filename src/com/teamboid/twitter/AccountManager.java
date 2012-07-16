package com.teamboid.twitter;

import java.util.List;

import com.teamboid.twitter.listadapters.AccountListAdapter;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;
import com.teamboid.twitter.views.SwipeDismissListViewTouchListener;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceActivity.Header;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

/**
 * The activity that represents the account manager, allows the user to add and remove accounts.
 * @author Aidan Follestad
 */
public class AccountManager extends PreferenceActivity {
	public static String END_LOAD = "com.teamboid.twitter.DONE_LOADING_ACCOUNTS";
	
	
	public static class AccountFragment extends PreferenceFragment{
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.prefs_accounts);
			final int accountId = this.getArguments().getInt("accountId");
			
			// TODO: Do something here
		}
	}

	private int lastTheme;
	public AccountListAdapter adapter;


	public class UpdateReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context arg0, Intent intent) {
			if(intent.getAction().equals(END_LOAD)) {
				setProgressBarIndeterminateVisibility(false);
				adapter.notifyDataSetChanged();
				String access = intent.getStringExtra("access_token");
				for(int i = 0; i < adapter.getCount(); i++) {
					Account acc = (Account)adapter.getItem(i);
					if(acc.getToken().equals(access)) {
						showFollowDialog(acc);
						break;
					}
				}
			}
		}
	}
	UpdateReceiver receiver = new UpdateReceiver();

	private void showFollowDialog(final Account acc) {
		new Thread(new Runnable() {
			public void run() {
				try {
					final boolean following = acc.getClient().existsFriendship(acc.getUser().getScreenName(), "boidapp");
					if(!following) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								AlertDialog.Builder diag = new AlertDialog.Builder(AccountManager.this);
								diag.setTitle("@boidapp");
								diag.setMessage(getString(R.string.follow_boidapp_prompt).replace("{account}", acc.getUser().getScreenName()));
								diag.setPositiveButton(R.string.yes_str, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
										new Thread(new Runnable() {
											public void run() {
												try { acc.getClient().createFriendship("boidapp"); }
												catch (final TwitterException e) {
													e.printStackTrace();
													runOnUiThread(new Runnable() {
														@Override
														public void run() { Toast.makeText(getApplicationContext(), R.string.failed_follow_boidapp, Toast.LENGTH_LONG).show(); }
													});
												}
											}
										}).start();
									}
								});
								diag.setNegativeButton(R.string.no_str, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) { dialog.dismiss(); }
								});
								diag.create().show();
							}
						});
					}
				} catch (final TwitterException e) { 
					e.printStackTrace();
					runOnUiThread(new Runnable() {
						public void run() { 
							Toast.makeText(getApplicationContext(), getString(R.string.failed_check_following_boidapp)
									.replace("{account}", acc.getUser().getScreenName()) + " " + e.getErrorMessage(), Toast.LENGTH_LONG).show();
						}
					});
				}
			}
		}).start();
	}
	
	@Override
	public void onBuildHeaders(List<Header> target) {
		// Tricks Android into thinking we're wanting a proper Preference View
		Header h = new Header();
		h.title = "Hi";
		h.fragment = "null";
		target.add(h);
	}
	


	@Override
	public void onCreate(Bundle savedInstanceState) {
		if(savedInstanceState != null && savedInstanceState.containsKey("lastTheme")) {
			lastTheme = savedInstanceState.getInt("lastTheme");
			setTheme(lastTheme);
		} else setTheme(Utilities.getTheme(getApplicationContext()));
		super.onCreate(savedInstanceState);
		
		if(this.getIntent().hasExtra(EXTRA_SHOW_FRAGMENT)){
			Log.d("acc", "Showing frag");
			return;
		}
		// requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		// setContentView(R.layout.account_manager);
		// setProgressBarIndeterminateVisibility(false);
		IntentFilter ifilter = new IntentFilter();
		ifilter.addAction(END_LOAD);
		registerReceiver(receiver, ifilter);
		adapter = new AccountListAdapter(this);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		final ListView listView = getListView();
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
					long arg3) {
				Log.d("listview", "Move to fragment");
				// Pretend to click a header.
				Header h = new Header();
				h.fragment = "com.teamboid.twitter.AccountManager$AccountFragment";
				Bundle b = new Bundle();
				b.putInt("accountId", pos);
				h.fragmentArguments = b;
				onHeaderClick(h, pos);
			}
			
		});
		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Toast.makeText(AccountManager.this, R.string.swipe_to_delete_accounts, Toast.LENGTH_LONG).show();
				return false;
			}
		});
		SwipeDismissListViewTouchListener touchListener =
				new SwipeDismissListViewTouchListener(listView,
						new SwipeDismissListViewTouchListener.OnDismissCallback() {
					@Override
					public void onDismiss(ListView listView, final int[] reverseSortedPositions) {
						AlertDialog.Builder ab = new AlertDialog.Builder(AccountManager.this);
						ab.setMessage("Are you sure?");
						
						ab.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								for (int i : reverseSortedPositions) {
									AccountService.removeAccount(AccountManager.this, (Account)adapter.getItem(i));
									adapter.notifyDataSetChanged();
								}
							}
						});
						ab.show();
					}
				});
		listView.setOnTouchListener(touchListener);
		listView.setOnScrollListener(touchListener.makeScrollListener());
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt("lastTheme", lastTheme);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onResume() {
		super.onResume();
		if(lastTheme == 0) lastTheme = Utilities.getTheme(getApplicationContext());
		else if(lastTheme != Utilities.getTheme(getApplicationContext())) {
			lastTheme = Utilities.getTheme(getApplicationContext());
			recreate();
			return;
		}
		setListAdapter(adapter);
	}

	@Override
	public void onBackPressed() {
		if(AccountService.getAccounts().size() == 0) return;
		else finish();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == RESULT_OK) {
			setProgressBarIndeterminateVisibility(true);
			AccountService.verifyAccount(data.getStringExtra("oauth_verifier"));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.accountmanager_actionbar, menu);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()); 
		if(prefs.getBoolean("enable_ssl", false)) {
			menu.findItem(R.id.toggleSslAction).setTitle(R.string.disable_ssl_str);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			if(AccountService.getAccounts().size() == 0) return false;
			finish();
			return true;
		case R.id.addAccountAction:
			item.setEnabled(false);
			startAuth();
			item.setEnabled(true);
			return true;
		case R.id.toggleSslAction:
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()); 
			final boolean newValue = !prefs.getBoolean("enable_ssl", false);
			if(newValue) {
				final AlertDialog.Builder diag = new AlertDialog.Builder(AccountManager.this);
				diag.setTitle(R.string.enable_ssl_str);
				diag.setMessage(R.string.enable_ssl_confirm_str);
				diag.setPositiveButton(R.string.yes_str, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						prefs.edit().putBoolean("enable_ssl", newValue).commit();
						invalidateOptionsMenu();
						int index = 0;
						for(Account acc : AccountService.getAccounts()) {
							ConfigurationBuilder cb = new ConfigurationBuilder().setOAuthConsumerKey("5LvP1d0cOmkQleJlbKICtg")
									.setOAuthConsumerSecret("j44kDQMIDuZZEvvCHy046HSurt8avLuGeip2QnOpHKI")
									.setOAuthAccessToken(acc.getToken()).setOAuthAccessTokenSecret(acc.getSecret())
									.setUseSSL(!newValue).setJSONStoreEnabled(true);
							final Twitter toAdd = new TwitterFactory(cb.build()).getInstance();
							acc.setClient(toAdd);
							AccountService.setAccount(index, acc);
							index++;
						}
					}
				});
				diag.setNegativeButton(R.string.no_str, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) { dialog.dismiss(); }
				});
				diag.create().show();
			} else {
				prefs.edit().putBoolean("enable_ssl", newValue).commit();
				invalidateOptionsMenu();
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void startAuth() {
		AccountService.pendingClient = new TwitterFactory().getInstance();
		AccountService.pendingClient.setOAuthConsumer("5LvP1d0cOmkQleJlbKICtg", "j44kDQMIDuZZEvvCHy046HSurt8avLuGeip2QnOpHKI");
		new Thread(new Runnable() {
			public void run() {
				try {
					final RequestToken requestToken = AccountService.pendingClient.getOAuthRequestToken("boid://auth");
					startActivityForResult(new Intent(AccountManager.this, LoginHandler.class).putExtra("url", requestToken.getAuthorizationURL()), 600);
				} catch (final TwitterException e) {
					e.printStackTrace();
					runOnUiThread(new Runnable() {
						@Override
						public void run() { Toast.makeText(getApplicationContext(), getString(R.string.authorization_error) + "; " + e.getErrorMessage(), Toast.LENGTH_LONG).show(); }
					});
				}
			}
		}).start();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		try{ unregisterReceiver(receiver); }
		catch(Exception e){}
	}
}
