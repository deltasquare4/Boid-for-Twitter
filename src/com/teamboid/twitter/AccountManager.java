package com.teamboid.twitter;

import java.util.Calendar;
import java.util.List;

import com.teamboid.twitter.contactsync.AndroidAccountHelper;
import com.teamboid.twitter.listadapters.AccountListAdapter;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.services.NotificationService;
import com.teamboid.twitter.utilities.BoidActivity;
import com.teamboid.twitter.utilities.Utilities;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
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
		}

		int accountId;
		SharedPreferences sp;
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			addPreferencesFromResource(R.xml.prefs_accounts);
			
			getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
			getActivity().setTitle(getArguments().getString("accName"));
			accountId = this.getArguments().getInt("accountId");
			sp = PreferenceManager.getDefaultSharedPreferences(getActivity());

			setKey("c2dm", accountId);
			setKey("c2dm_period", accountId);
			setKey("c2dm_mention", accountId);
			setKey("c2dm_dm", accountId);
			setKey("c2dm_vibrate", accountId);
			setKey("c2dm_ringtone", accountId);
			setKey("c2dm_messages_priv", accountId);
			setKey("contactsync_on", accountId);

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
			
			SwitchPreference c2dmPref = ((SwitchPreference) findPreference(accountId + "_c2dm"));
			c2dmPref.setChecked(sp.getBoolean(accountId + "_c2dm", true));
			c2dmPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(
						final Preference preference, Object newValue) {
					
					Log.d("boid", "notification switched");
					NotificationService.setupAlarm(getActivity(), accountId, (Boolean)newValue);
					
					return true;
				}
			});
		}

		void setKey(String key, int accountId) {
			Preference p = findPreference("{user}_" + key);
			p.setKey(accountId + "_" + key);
			if(p instanceof SwitchPreference){
				((SwitchPreference)p).setChecked(sp.getBoolean(accountId + "_" + key, ((SwitchPreference) p).isChecked()));
			} //else if(p instanceof ListPreference){
			//	((ListPreference)p).setValue(((ListPreference)p).getValue());
			//}
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
				b.putString("accName", h.title.toString());
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
		if (this.getIntent().hasExtra(EXTRA_SHOW_FRAGMENT)) return true;
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
