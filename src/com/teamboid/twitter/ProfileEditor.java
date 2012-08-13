package com.teamboid.twitter;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.EditText;
import android.widget.Toast;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;
import com.teamboid.twitter.widgets.TimelineWidgetViewService;
import com.teamboid.twitterapi.user.User;

import java.util.ArrayList;

/**
 * The activity that represents the profile viewer.
 * 
 * @author Aidan Follestad
 */
public class ProfileEditor extends Activity {

	private int lastTheme;
    private boolean showProgress;
    private Account toSet;
    private int index;

    public void showProgress(boolean visible) {
        if(showProgress == visible) return;
        showProgress = visible;
        setProgressBarIndeterminateVisibility(visible);
        if(showProgress) {
            findViewById(R.id.nameTxt).setEnabled(false);
            findViewById(R.id.urlTxt).setEnabled(false);
            findViewById(R.id.locationTxt).setEnabled(false);
            findViewById(R.id.descriptionTxt).setEnabled(false);
        } else {
            findViewById(R.id.nameTxt).setEnabled(true);
            findViewById(R.id.urlTxt).setEnabled(true);
            findViewById(R.id.locationTxt).setEnabled(true);
            findViewById(R.id.descriptionTxt).setEnabled(true);
        }
        invalidateOptionsMenu();
    }

    @Override
	public void onCreate(final Bundle savedInstanceState) {
		if(savedInstanceState != null) {
			if(savedInstanceState.containsKey("lastTheme")) {
				lastTheme = savedInstanceState.getInt("lastTheme");
				setTheme(lastTheme);
			} else setTheme(Utilities.getTheme(getApplicationContext()));
		}  else setTheme(Utilities.getTheme(getApplicationContext()));
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
        setProgressBarIndeterminateVisibility(false);
		getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.profile_editor);
        setTitle("@" + getIntent().getStringExtra("screen_name"));
        loadAccount();
	}

    private void loadAccount() {
        showProgress(true);
        final ArrayList<Account> accs = AccountService.getAccounts();

        //Load the account's user object again to make 100% sure the information is up to date, after the accounts been
        //cached in local preferences and everything.
        for(int i = 0; i < accs.size(); i++) {
            if(accs.get(i).getUser().getScreenName().equals(getIntent().getStringExtra("screen_name"))) {
                index = i;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            toSet = accs.get(index);
                            final User user = toSet.getClient().verifyCredentials();
                            toSet.setUser(user);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() { displayAccount(); }
                            });
                        } catch (final Exception e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), getString(R.string.failed_get_accountinfo), Toast.LENGTH_LONG).show();
                                    //Display the last known local account information if it fails to re-get the information.
                                    displayAccount();
                                }
                            });
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() { showProgress(false); }
                        });
                    }
                }).start();
                break;
            }
        }


    }
    private void displayAccount() {
        ((EditText)findViewById(R.id.nameTxt)).setText(toSet.getUser().getName());
        ((EditText)findViewById(R.id.urlTxt)).setText(toSet.getUser().getUrl());
        ((EditText)findViewById(R.id.locationTxt)).setText(toSet.getUser().getLocation());
        ((EditText)findViewById(R.id.descriptionTxt)).setText(toSet.getUser().getDescription());
        //Update the account's information in the account service. This function also updates the account in the local preferences cache.
        AccountService.setAccount(this, index, toSet);
    }

    private void commitChanges() {
        showProgress(true);
        final String name = ((EditText)findViewById(R.id.nameTxt)).getText().toString();
        final String url = ((EditText)findViewById(R.id.urlTxt)).getText().toString();
        final String location = ((EditText)findViewById(R.id.locationTxt)).getText().toString();
        final String description = ((EditText)findViewById(R.id.descriptionTxt)).getText().toString();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final User user = toSet.getClient().updateProfile(name, url, location, description);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toSet.setUser(user);
                            displayAccount();
                        }
                    });
                } catch (final Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showProgress(false);
                            Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showProgress(false);
                        finish();
                    }
                });
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.config_activity_actionbar, menu);
        if(showProgress) {
            menu.findItem(R.id.doneAction).setEnabled(false);
        } else menu.findItem(R.id.doneAction).setEnabled(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            case R.id.doneAction:
                commitChanges();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
