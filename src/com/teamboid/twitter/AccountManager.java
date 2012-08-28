package com.teamboid.twitter;

import java.util.List;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.android.gcm.GCMRegistrar;
import com.teamboid.twitter.contactsync.AndroidAccountHelper;
import com.teamboid.twitter.listadapters.AccountListAdapter;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.BoidActivity;
import com.teamboid.twitter.utilities.Utilities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

/**
 * The activity that represents the account manager, allows the user to add and
 * remove accounts.
 * 
 * @author Aidan Follestad
 */
public class AccountManager extends PreferenceActivity {

	

	public static class AccountFragment extends PreferenceFragment {

		@Override
		public void onDestroy() {
			super.onDestroy();
			getActivity().unregisterReceiver(pupdater);
		}

		boolean realChange = false;
		int accountId;
		ProgressDialog pd;
		BroadcastReceiver pupdater;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			addPreferencesFromResource(R.xml.prefs_accounts);
			getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
			accountId = this.getArguments().getInt("accountId");
			pd = new ProgressDialog(getActivity());
			pd.setMessage(getText(R.string.push_wait));
			pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

			pupdater = new BroadcastReceiver() {
				@Override
				public void onReceive(Context arg0, Intent arg1) {
					pd.setProgress(arg1.getIntExtra("progress", 1000));
					if (arg1.getIntExtra("progress", 0) == 1000) {
						pd.dismiss();
						if (arg1.getBooleanExtra("error", false)) {
							Toast.makeText(getActivity(), R.string.push_error,
									Toast.LENGTH_LONG).show();
						} else {
							Toast.makeText(getActivity(),
									R.string.push_registered,
									Toast.LENGTH_SHORT).show();
							findPreference(accountId + "_c2dm")
									.getSharedPreferences().edit()
									.putBoolean(accountId + "_c2dm", true)
									.commit();
							realChange = true;
							((SwitchPreference) findPreference(accountId
									+ "_c2dm")).setChecked(true);
							realChange = false;
						}
					}
				}

			};
			IntentFilter i = new IntentFilter();
			i.addAction(GCMIntentService.BROADCAST);
			getActivity().registerReceiver(pupdater, i);

			setKey("c2dm", accountId);
			setKey("c2dm_mentions", accountId);
			setKey("c2dm_messages", accountId);
			setKey("c2dm_vibrate", accountId);
			setKey("c2dm_ringtone", accountId);
			setKey("c2dm_messages_priv", accountId);
			setKey("contactsync_on", accountId);

			findPreference(accountId + "_c2dm_mentions")
					.setOnPreferenceChangeListener(
							new RemotePushSettingChange("replies"));
			findPreference(accountId + "_c2dm_messages")
					.setOnPreferenceChangeListener(
							new RemotePushSettingChange("dm"));

			SwitchPreference syncPref = ((SwitchPreference) findPreference(accountId
					+ "_contactsync_on"));
			syncPref.setChecked(ContentResolver.getSyncAutomatically(
					AndroidAccountHelper.getAccount(getActivity(),
							AccountService.getAccount(accountId)),
					ContactsContract.AUTHORITY));
			syncPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(final Preference preference,
						Object newValue) {
					ContentResolver.setSyncAutomatically(AndroidAccountHelper
							.getAccount(getActivity(),
									AccountService.getAccount(accountId)),
							ContactsContract.AUTHORITY, (Boolean) newValue);
					return true;
				}
			});

			GCMRegistrar.checkDevice(getActivity());
			GCMRegistrar.checkManifest(getActivity());
			final String regId = GCMRegistrar.getRegistrationId(getActivity());
			SwitchPreference c2dmPref = ((SwitchPreference) findPreference(accountId + "_c2dm"));
			c2dmPref.setChecked( !regId.equals("") );
			c2dmPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(
						final Preference preference, Object newValue) {
					GCMIntentService.setRegisteringFor(getActivity(), accountId);
					if((Boolean)newValue == true){ // Register
						GCMRegistrar.register(getActivity(), GCMIntentService.SENDER_ID);
					} else{ // Unregister
						GCMRegistrar.unregister(getActivity());
					}
					pd.show();
					
					return true;
				}
			});
		}

		void setKey(String key, int accountId) {
			findPreference("{user}_" + key).setKey(accountId + "_" + key);
		}

		public class RemotePushSettingChange implements
				Preference.OnPreferenceChangeListener {
			String remote_setting;
			Boolean real_change = false;

			public RemotePushSettingChange(String remote_setting) {
				this.remote_setting = remote_setting;
			}

			@Override
			public boolean onPreferenceChange(final Preference preference,
					final Object newValue) {
				if (real_change == true){
					real_change = false;
					return true;
				}
				pd.setProgress(0);
				pd.show();
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							DefaultHttpClient dhc = new DefaultHttpClient();
							HttpGet get = new HttpGet(GCMIntentService.SERVER
									+ "/edit/" + accountId + "/"
									+ remote_setting + "/"
									+ ((Boolean) newValue ? "on" : "off"));
							org.apache.http.HttpResponse r = dhc.execute(get);
							if (r.getStatusLine().getStatusCode() == 200) {
								getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										pd.dismiss();
										Toast.makeText(getActivity(),
												R.string.push_updated,
												Toast.LENGTH_SHORT).show();
									}
								});
							} else
								throw new Exception("NON 200 RESPONSE ;__;");
						} catch (Exception e) {
							e.printStackTrace();
							getActivity().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									pd.dismiss();
									Toast.makeText(getActivity(),
											R.string.push_error,
											Toast.LENGTH_LONG).show();
									real_change = true;
									((SwitchPreference) preference)
											.setChecked(!(Boolean) newValue);
								}
							});
						}
					}
				}).start();
				return true;
			}
		}
	}

	public static String END_LOAD = "com.teamboid.twitter.DONE_LOADING_ACCOUNTS";

	private int lastTheme;
	public AccountListAdapter adapter;

	public class UpdateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context arg0, Intent intent) {
			if (intent.getAction().equals(END_LOAD)) {
				setProgressBarIndeterminateVisibility(false);
				adapter.notifyDataSetChanged();
			}
		}
	}

	UpdateReceiver receiver = new UpdateReceiver();

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
		boid = new BoidActivity(this);
		boid.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		setProgressBarIndeterminateVisibility(false);
		if (this.getIntent().hasExtra(EXTRA_SHOW_FRAGMENT)) {
			Log.d("acc", "Showing frag");
			return;
		}
		IntentFilter ifilter = new IntentFilter();
		ifilter.addAction(END_LOAD);
		registerReceiver(receiver, ifilter);
		adapter = new AccountListAdapter(this);
		if(adapter.getCount() == 0) {
			Toast.makeText(getApplicationContext(), R.string.no_accounts, Toast.LENGTH_LONG).show();
		}
		getActionBar().setDisplayHomeAsUpEnabled(true);
		final ListView listView = getListView();
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
					long id) {
				// Pretend to click a header.
				Header h = new Header();
				h.fragment = "com.teamboid.twitter.AccountManager$AccountFragment";
				h.title = AccountService.getAccount(id).getUser().getName();
				h.breadCrumbTitle = AccountService.getAccount(id).getUser()
						.getName();
				Bundle b = new Bundle();
				b.putInt("accountId", (int) id);
				h.fragmentArguments = b;
				onHeaderClick(h, pos);
			}
		});
		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int pos, long arg3) {
				final Account acc = (Account) adapter.getItem(pos);
				AlertDialog.Builder ab = new AlertDialog.Builder(
						AccountManager.this)
						.setTitle(R.string.remove_account)
						.setMessage(
								getString(R.string.confirm_remove_account)
										.replace("{account}",
												acc.getUser().getScreenName()))
						.setPositiveButton(R.string.yes_str,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										dialog.dismiss();
										AccountService.removeAccount(
												AccountManager.this, acc);
										adapter.notifyDataSetChanged();
									}
								})
						.setNegativeButton(R.string.no_str,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										dialog.dismiss();
									}
								});
				ab.create().show();
				return false;
			}
		});
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		boid.onSaveInstanceState(outState);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (lastTheme == 0)
			lastTheme = Utilities.getTheme(getApplicationContext());
		else if (lastTheme != Utilities.getTheme(getApplicationContext())) {
			lastTheme = Utilities.getTheme(getApplicationContext());
			recreate();
			return;
		}
		setListAdapter(adapter);
	}

	@Override
	public void onBackPressed() {
		if (AccountService.getAccounts().size() == 0)
			return;
		else
			finish();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			setProgressBarIndeterminateVisibility(true);
			AccountService.verifyAccount(this,
					data.getStringExtra("oauth_verifier"));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.accountmanager_actionbar, menu);
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		if (prefs.getBoolean("enable_ssl", false)) {
			menu.findItem(R.id.toggleSslAction).setTitle(
					R.string.disable_ssl_str);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			if (AccountService.getAccounts().size() == 0)
				return false;
			super.onBackPressed();
			return true;
		case R.id.addAccountAction:
			item.setEnabled(false);
			startAuth();
			item.setEnabled(true);
			return true;
		case R.id.toggleSslAction:
			final SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext());
			final boolean newValue = !prefs.getBoolean("enable_ssl", false);
			final AlertDialog.Builder diag = new AlertDialog.Builder(
					AccountManager.this);
			diag.setTitle(R.string.enable_ssl_str);
			diag.setMessage(R.string.enable_ssl_confirm_str);
			diag.setPositiveButton(R.string.yes_str,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							prefs.edit().putBoolean("enable_ssl", newValue)
									.commit();
							invalidateOptionsMenu();
							for (Account acc : AccountService.getAccounts()) {
								acc.setClient(acc.getClient().setSslEnabled(
										newValue));
								AccountService.setAccount(getApplicationContext(), acc);
							}
						}
					});
			diag.setNegativeButton(R.string.no_str,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			diag.create().show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void startAuth() {
		new Thread(new Runnable() {
			public void run() {
				try {
					startActivityForResult(new Intent(AccountManager.this,
							LoginHandler.class).putExtra("url", AccountService
							.getAuthorizer().getAuthorizeUrl()), 600);
				} catch (final Exception e) {
					e.printStackTrace();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(
									getApplicationContext(),
									getString(R.string.authorization_error)
											+ "; " + e.getMessage(),
									Toast.LENGTH_LONG).show();
						}
					});
				}
			}
		}).start();
	}

	@Override
	public void onDestroy() {
		boid.onDestroy();
		super.onDestroy();
		try {
			unregisterReceiver(receiver);
		} catch (Exception e) {
		}
	}
	
	BoidActivity boid;
}
