package com.teamboid.twitter.columns;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

import com.teamboid.twitter.Account;
import com.teamboid.twitter.ProfileScreen;
import com.teamboid.twitter.R;
import com.teamboid.twitter.TweetViewer;
import com.teamboid.twitter.cab.TimelineCAB;
import com.teamboid.twitter.listadapters.FeedListAdapter;
import com.teamboid.twitter.listadapters.MessageConvoAdapter.DMConversation;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitterapi.client.Paging;
import com.teamboid.twitterapi.search.Tweet;
import com.teamboid.twitterapi.status.Status;
import com.teamboid.twitterapi.user.User;
import com.teamboid.twitterapi.utilities.Utils;

/**
 * Represents the column that displays the feed of a user in the profile screen. 
 * @author Aidan Follestad
 */
public class PaddedProfileTimelineFragment extends ProfilePaddedFragment {

	private ProfileScreen context;
	private String screenName;

	@Override
	public void onAttach(Activity act) {
		super.onAttach(act);
		context = (ProfileScreen) act;
	}

	private FeedListAdapter getAdapter() {
		return context.adapter;
	}

	@Override
	public void onListItemClick(ListView l, View v, int index, long id) {
		super.onListItemClick(l, v, index, id);
        Status tweet = (Status)context.adapter.getItem(index);
        if (tweet.isRetweet()) tweet = tweet.getRetweetedStatus();
        context.startActivity(new Intent(context, TweetViewer.class)
                .putExtra("sr_tweet", Utils.serializeObject(tweet))
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
	}

	@Override
	public void onStart() {
		super.onStart();
        getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        getListView().setMultiChoiceModeListener(TimelineCAB.choiceListener);
		getListView().setOnScrollListener(
				new AbsListView.OnScrollListener() {
					@Override
					public void onScrollStateChanged(AbsListView view, int scrollState) { }
					@Override
					public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
						if (totalItemCount > 0 && (firstVisibleItem + visibleItemCount) >= totalItemCount && totalItemCount > visibleItemCount) {
							performRefresh(true);
						}
						if (firstVisibleItem == 0 && context.getActionBar().getTabCount() > 0) {
							if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("enable_iconic_tabs", true)) {
								context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(R.string.tweets_str);
							} else {
								context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText("");
							}
						}
					}
				});
		setRetainInstance(true);
		setEmptyText(getString(R.string.no_tweets));
		screenName = getArguments().getString("query");
		reloadAdapter(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (getView() == null || getAdapter() == null)
			return;
		getAdapter().restoreLastViewed(getListView());
	}

	@Override
	public void onPause() {
		super.onPause();
		savePosition();
	}

	@Override
	public void performRefresh(final boolean paginate) {
		if (context == null || isLoading || getAdapter() == null) return;
		isLoading = true;
		if (getAdapter().getCount() == 0 && getView() != null) setListShown(false);
		getAdapter().setLastViewed(getListView());
		new Thread(new Runnable() {
			@Override
			public void run() {
				Paging paging = new Paging(50);
				if (paginate) paging.setMaxId(getAdapter().getItemId(getAdapter().getCount() - 1));
				final Account acc = AccountService.getCurrentAccount();
				if (acc != null) {
					try {
						final Status[] feed = acc.getClient().getUserTimeline(screenName, paging, true);
						context.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								setEmptyText(context.getString(R.string.no_tweets));
								int beforeLast = getAdapter().getCount() - 1;
								int addedCount = getAdapter().add(feed);
								if (getView() != null && addedCount > 0) {
									if (paginate) {
										getListView().smoothScrollToPosition(beforeLast + 1);
									}
									else if (getView() != null && getAdapter() != null) {
										getAdapter().restoreLastViewed(getListView());
									}
								}
								if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("enable_iconic_tabs", true)) {
									context.getActionBar().getTabAt(getArguments().getInt("tab_index"))
									.setText(context.getString(R.string.tweets_str) + " (" + Integer.toString(addedCount) + ")");
								} else {
									context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(Integer.toString(addedCount));
								}
							}
						});
					} catch (final Exception e) {
						e.printStackTrace();
						context.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								setEmptyText(context.getString(R.string.error_str));
								Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
							}
						});
					}
				}
				context.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (getView() != null) setListShown(true);
						isLoading = false;
					}
				});
			}
		}).start();
	}

	@Override
	public void reloadAdapter(boolean firstInitialize) {
		if (AccountService.getCurrentAccount() != null) {
			if (getAdapter() != null && !firstInitialize && getView() != null) {
				getAdapter().setLastViewed(getListView());
			}
			if (getAdapter() == null) {
				context.adapter = new FeedListAdapter(context, null, AccountService.getCurrentAccount().getId());
			}
			setListAdapter(getAdapter());
			if (getAdapter().getCount() == 0) {
				performRefresh(false);
			}
			else if (getView() != null && getAdapter() != null) {
				getAdapter().restoreLastViewed(getListView());
			}
		}
	}

	@Override
	public void savePosition() {
		if (getView() != null && getAdapter() != null) {
			getAdapter().setLastViewed(getListView());
		}
	}

	@Override
	public void restorePosition() {
		if (getView() != null && getAdapter() != null) {
			getAdapter().restoreLastViewed(getListView());
		}
	}

	@Override
	public void jumpTop() {
		if (getView() != null) {
			getListView().setSelectionFromTop(0, 0);
		}
	}

	@Override
	public void filter() { }

	@Override
	public Status[] getSelectedStatuses() {
        if (context.adapter == null && getView() == null) return null;
        ArrayList<Status> toReturn = new ArrayList<Status>();
        SparseBooleanArray choices = getListView().getCheckedItemPositions();
        for (int i = 0; i < choices.size(); i++) {
            if(choices.valueAt(i)) {
                toReturn.add((Status)context.adapter.getItem(choices.keyAt(i)));
            }
        }
        return toReturn.toArray(new Status[0]);
	}

	@Override
	public User[] getSelectedUsers() { return null; }

	@Override
	public Tweet[] getSelectedTweets() { return null; }

	@Override
	public DMConversation[] getSelectedMessages() { return null; }

}
