package com.teamboid.twitter;

import com.teamboid.twitter.cab.TimelineCAB;
import com.teamboid.twitter.listadapters.FeedListAdapter;
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
import com.teamboid.twitterapi.client.Paging;
import com.teamboid.twitterapi.status.Status;

/**
 * @author Aidan Follestad
 */
public class TweetListActivity extends ListActivity {

	private int lastTheme;
	private boolean showProgress;
	private Status[] tweets = null;
	private boolean allowPagination = true;
	public FeedListAdapter binder;
	private ProgressDialog progDialog;
	
	public void showProgress(final boolean visible) {
		if(showProgress == visible && progDialog != null) {
			return;
		}
		runOnUiThread(new Runnable() {
			@Override
			public void run() { 
				setProgressBarIndeterminateVisibility(visible);
				if(showProgress) {
					progDialog.dismiss();
					showProgress = false;
				} else {
					progDialog = ProgressDialog.show(TweetListActivity.this, "", getString(R.string.loading_str), true);
					showProgress = true;
				}
			}
		});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if(savedInstanceState != null) {
			if(savedInstanceState.containsKey("lastTheme")) {
				lastTheme = savedInstanceState.getInt("lastTheme");
				setTheme(lastTheme);
			} else setTheme(Utilities.getTheme(getApplicationContext()));
			if(savedInstanceState.containsKey("showProgress")) showProgress(true);
		}  else setTheme(Utilities.getTheme(getApplicationContext()));
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		binder = new FeedListAdapter(this, null, AccountService.getCurrentAccount().getId());
		setListAdapter(binder);
		refresh();
		getListView().setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int index, long id) {
				Status tweet = (Status)binder.getItem(index);
				if(tweet.isRetweet()) tweet = tweet.getRetweetedStatus();
				startActivity(new Intent(getApplicationContext(), TweetViewer.class).putExtra("tweet_id", id).putExtra("user_name", tweet.getUser().getName()).putExtra("user_id", tweet.getUser().getId())
						.putExtra("screen_name", tweet.getUser().getScreenName()).putExtra("content", tweet.getText()).putExtra("timer", tweet.getCreatedAt().getTime())
						.putExtra("via", tweet.getSource()).putExtra("isFavorited", tweet.isFavorited()).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
			}
		});
		getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) { }
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				if(totalItemCount > 0 && (firstVisibleItem + visibleItemCount) >= totalItemCount && totalItemCount > visibleItemCount) {
					refresh();
				}
			}
		});
		getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
		getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int index, long id) {
				TimelineCAB.performLongPressAction(getListView(), binder, index);
				return true;
			}
		});		
		setProgressBarIndeterminateVisibility(false);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		TimelineCAB.context = this;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		TimelineCAB.clearSelectedItems();
		if(TimelineCAB.TimelineActionMode != null) {
			TimelineCAB.TimelineActionMode.finish();
		}
	}

	public void refresh() {
		if(!allowPagination) return;
		showProgress(true);
		new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					Paging paging = new Paging(20);
					switch(getIntent().getIntExtra("mode", -1)) {
					case USER_FAVORITES:
						runOnUiThread(new Runnable() {
							@Override
							public void run() { setTitle(getString(R.string.user_favorites).replace("{user}", getIntent().getStringExtra("username"))); }
						});
						if(binder.getCount() > 0) paging.setMaxId(binder.getItemId(binder.getCount() - 1));
						tweets = AccountService.getCurrentAccount().getClient()
                                .getFavorites(paging, getIntent().getStringExtra("username"));
						break;
					case USER_LIST:
						runOnUiThread(new Runnable() {
							@Override
							public void run() { setTitle(getIntent().getStringExtra("list_name")); }
						});
						if(binder.getCount() > 0) paging.setMaxId(binder.getItemId(binder.getCount() - 1));
						tweets = AccountService.getCurrentAccount().getClient()
                                .getListTimeline(getIntent().getIntExtra("list_ID", 0), paging);
						break;
					}
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if(tweets == null || tweets.length == 0) {
								allowPagination = false;
								showProgress(false);
								return;
							}
							int addedCount = binder.add(tweets);
							if(addedCount == 0) allowPagination = false;
							showProgress(false);
						}
					});
				} catch(final Exception e) { 
					e.printStackTrace();
					runOnUiThread(new Runnable() {
						@Override
						public void run() { Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show(); }
					});
				}
			}
		}).start();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt("lastTheme", lastTheme);
		if(showProgress) {
			showProgress(false);
			outState.putBoolean("showProgress", true);
		}
		super.onSaveInstanceState(outState);
	}

	public static final int USER_FAVORITES = 1;
	public static final int USER_LIST = 2;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			//startActivity(new Intent(this, TimelineScreen.class));
			//finish();
			
			super.onBackPressed(); //Back button should go back, not restart a new activity
			
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
