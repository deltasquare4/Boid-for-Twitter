package com.teamboid.twitter;

import java.util.ArrayList;

import com.teamboid.twitter.cab.UserListCAB;
import com.teamboid.twitter.listadapters.SearchUsersListAdapter;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.teamboid.twitterapi.client.Twitter;
import com.teamboid.twitterapi.user.User;

/**
 * @author kennydude
 */
public class UserListActivity extends ListActivity {

	private int lastTheme;
	private boolean showProgress;
	private ArrayList<Long> ids;
	private boolean allowPagination = true;
	public SearchUsersListAdapter binder;
	private ProgressDialog progDialog;

	public void showProgress(final boolean visible) {
		if (showProgress == visible && progDialog != null) {
			return;
		}
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				setProgressBarIndeterminateVisibility(visible);
				if (showProgress) {
					progDialog.dismiss();
					showProgress = false;
				} else {
					progDialog = ProgressDialog.show(UserListActivity.this, "",
							getString(R.string.loading_str), true);
					showProgress = true;
				}
			}
		});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey("lastTheme")) {
				lastTheme = savedInstanceState.getInt("lastTheme");
				setTheme(lastTheme);
			} else
				setTheme(Utilities.getTheme(getApplicationContext()));
			if (savedInstanceState.containsKey("showProgress"))
				showProgress(true);
		} else
			setTheme(Utilities.getTheme(getApplicationContext()));
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		binder = new SearchUsersListAdapter(this);
		setListAdapter(binder);
		refresh();
		getListView().setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
					long arg3) {
				startActivity(new Intent(getApplicationContext(),
						ProfileScreen.class).putExtra("screen_name",
						((User) binder.getItem(pos)).getScreenName()).addFlags(
						Intent.FLAG_ACTIVITY_CLEAR_TOP));
			}
		});
		getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
		getListView().setMultiChoiceModeListener(UserListCAB.choiceListener);
		getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				if (totalItemCount > 0
						&& (firstVisibleItem + visibleItemCount) >= totalItemCount
						&& totalItemCount > visibleItemCount) {
					paginate();
				}
			}
		});
		setProgressBarIndeterminateVisibility(false);
	}

	@Override
	public void onResume() {
		super.onResume();
		UserListCAB.context = this;
	}

	public void paginate() {
		if (ids.size() == 0 || !allowPagination) {
			return;
		}
		final Twitter cl = AccountService.getCurrentAccount().getClient();
		showProgress(true);
		final ArrayList<Long> toLookup = new ArrayList<Long>();
		for (int i = 0; i < 20; i++) {
			if (i >= ids.size()) {
				break;
			}
			toLookup.add(ids.get(i));
		}
		final Long[] lookup = new Long[toLookup.size()];
		for (int i = 0; i < toLookup.size(); i++) {
			lookup[i] = toLookup.get(i);
		}
		for (int i = 0; i < 20; i++) {
			if (i >= ids.size()) {
				break;
			}
			ids.remove(i);
		}
		new Thread(new Runnable() {
			public void run() {
				try {
					final User[] res = cl.lookupUsers(lookup);
					runOnUiThread(new Runnable() {
						public void run() {
							if (res == null || res.length == 0) {
								allowPagination = false;
							} else {
								int addedCount = binder.add(res);
								if (addedCount == 0)
									allowPagination = false;
							}
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
				runOnUiThread(new Runnable() {
					public void run() {
						showProgress(false);
					}
				});
			}
		}).start();
	}

	public void refresh() {
		binder.clear();
		new Thread(new Runnable() {
			private ArrayList<Long> arrayToList(Long[] array) {
				ArrayList<Long> toReturn = new ArrayList<Long>();
				for (long l : array)
					toReturn.add(l);
				return toReturn;
			}

			@Override
			public void run() {
				ids = new ArrayList<Long>();
				try {
					showProgress(true);
					switch (getIntent().getIntExtra("mode", -1)) {
					case FOLLOWERS_LIST:
						setTitle(getString(R.string.user_followers).replace(
								"{user}",
								getIntent().getStringExtra("username")));
						ids = arrayToList(AccountService
								.getCurrentAccount()
								.getClient()
								.getFollowers(
										getIntent().getLongExtra("user", 0),
										-1l).getIds());
						break;
					case FOLLOWING_LIST:
						setTitle(getString(R.string.user_following).replace(
								"{user}",
								getIntent().getStringExtra("username")));
						ids = arrayToList(AccountService
								.getCurrentAccount()
								.getClient()
								.getFriends(
										getIntent().getLongExtra("user", 0),
										-1l).getIds());
						break;
					}
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							paginate();
						}
					});
				} catch (final Exception e) {
					e.printStackTrace();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(getApplicationContext(),
									e.getLocalizedMessage(), Toast.LENGTH_LONG)
									.show();
						}
					});
				}
			}
		}).start();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt("lastTheme", lastTheme);
		if (showProgress) {
			showProgress(false);
			outState.putBoolean("showProgress", true);
		}
		super.onSaveInstanceState(outState);
	}

	public static final int FOLLOWERS_LIST = 1;
	public static final int FOLLOWING_LIST = 2;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			super.onBackPressed();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}