package com.teamboid.twitter;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;

/**
 * The activity that represents the account manager, allows the user to add and remove accounts.
 * @author Aidan Follestad
 */
public class AccountManager extends ListActivity {

	private int lastTheme;
	public AccountListAdapter adapter;
	
	public static String END_LOAD = "com.teamboid.twitter.DONE_LOADING_ACCOUNTS";
	public class UpdateReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context arg0, Intent intent) {
			if(intent.getAction().equals(END_LOAD)) {
				setProgressBarIndeterminateVisibility(false);
				adapter.notifyDataSetChanged();
			}
		}
	}
	UpdateReceiver receiver = new UpdateReceiver();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		if(savedInstanceState != null && savedInstanceState.containsKey("lastTheme")) {
			lastTheme = savedInstanceState.getInt("lastTheme");
			setTheme(lastTheme);
		} else setTheme(Utilities.getTheme(getApplicationContext()));
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.account_manager);
		setProgressBarIndeterminateVisibility(false);
		IntentFilter ifilter = new IntentFilter();
		ifilter.addAction(END_LOAD);
		registerReceiver(receiver, ifilter);
		adapter = new AccountListAdapter(this);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int i, long id) {
				AlertDialog.Builder diag = new AlertDialog.Builder(AccountManager.this);
				diag.setTitle(R.string.delete_str);
				diag.setMessage(R.string.confirm_remove_account);
				diag.setPositiveButton(R.string.yes_str, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						AccountService.removeAccount(AccountManager.this, (Account)adapter.getItem(i));
						adapter.notifyDataSetChanged();
					}
				});
				diag.setNegativeButton(R.string.no_str, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) { dialog.dismiss(); }
				});
				diag.create().show();
				return true;
			}
		});
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
		else {
			startActivity(new Intent(this, TimelineScreen.class));
			finish(); //this fixes the issue with the account manager randomly appeared after back is pressed
		}
	}
	
	@Override
	public void onNewIntent(final Intent intent) {
		super.onNewIntent(intent);
		if(intent.getData() != null) {
			if(intent.getData().getScheme().equals("boid") && intent.getData().getHost().equals("auth")) {
				setProgressBarIndeterminateVisibility(true);
				AccountService.verifyAccount(intent.getData().getQueryParameter("oauth_verifier"));
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.accountmanager_actionbar, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if(AccountService.getAccounts().size() == 0) return false;
				startActivity(new Intent(this, TimelineScreen.class));
				finish();
				return true;
			case R.id.addAccountAction:
				item.setEnabled(false);
				AccountService.startAuthorization();
				item.setEnabled(true);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(receiver);
	}
}