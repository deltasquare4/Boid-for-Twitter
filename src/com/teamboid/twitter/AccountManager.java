package com.teamboid.twitter;

import java.util.List;

import com.teamboid.twitter.contactsync.AndroidAccountHelper;
import com.teamboid.twitter.listadapters.AccountListAdapter;
import com.teamboid.twitter.notifications.NotificationService;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.BoidActivity;
import com.teamboid.twitter.utilities.Utilities;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import me.kennydude.awesomeprefs.ListPreference;
import me.kennydude.awesomeprefs.Preference;
import me.kennydude.awesomeprefs.PreferenceActivity;
import me.kennydude.awesomeprefs.PreferenceFragment;
import android.preference.PreferenceManager;
import me.kennydude.awesomeprefs.SwitchPreference;
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
 * DEPRECATED. Moving to SettingsScreen in wake of AwesomePrefs, that are darn cool
 * 
 * @author Aidan Follestad
 */
@Deprecated
public class AccountManager extends PreferenceActivity {

	private int lastTheme;
	public AccountListAdapter adapter;

	public class UpdateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context arg0, Intent intent) {
			if (intent.getAction().equals(AccountService.END_LOAD)) {
				setProgressBarIndeterminateVisibility(false);
				adapter.notifyDataSetChanged();
			}
		}
	}

	UpdateReceiver receiver = new UpdateReceiver();

	/*
	 * AwesomePrefs deprecated this
	@Override
	public void onBuildHeaders(List<Header> target) {
		// Tricks Android into thinking we're wanting a proper Preference View
		Header h = new Header();
		h.title = "Hi";
		h.fragment = "null";
		target.add(h);
	}
	*/

	@Override
	public void onCreate(Bundle savedInstanceState) {
		boid = new BoidActivity(this);
		boid.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		setProgressBarIndeterminateVisibility(false);
		
		/*
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
		*/
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
		//setListAdapter(adapter);
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
		//if (this.getIntent().hasExtra(EXTRA_SHOW_FRAGMENT)) return true;
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
