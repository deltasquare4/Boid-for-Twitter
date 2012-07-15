package com.teamboid.twitter.columns;

import java.util.ArrayList;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Tweet;
import twitter4j.TwitterException;
import twitter4j.User;
import android.app.Activity;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.teamboid.twitter.Account;
import com.teamboid.twitter.ProfileScreen;
import com.teamboid.twitter.R;
import com.teamboid.twitter.TimelineCAB;
import com.teamboid.twitter.TweetViewer;
import com.teamboid.twitter.listadapters.FeedListAdapter;
import com.teamboid.twitter.listadapters.MessageConvoAdapter.DMConversation;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;

/**
 * Represents the column that displays the feed of a user in the profile screen.
 * 
 * @author Aidan Follestad
 */
public class PaddedProfileTimelineFragment extends ProfilePaddedFragment
{

	private ProfileScreen context;
	private String screenName;

	@Override
	public void onAttach(Activity act)
	{
		super.onAttach(act);
		context = (ProfileScreen) act;
	}

	private FeedListAdapter getAdapter()
	{
		return context.adapter;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);
		Status tweet = (Status) getAdapter().getItem(position);
		if (tweet.isRetweet())
			tweet = tweet.getRetweetedStatus();
		context.startActivity(new Intent(context, TweetViewer.class).putExtra(
				"sr_tweet", Utilities.serializeObject(tweet)).addFlags(
				Intent.FLAG_ACTIVITY_CLEAR_TOP));
	}

	@Override
	public void onStart()
	{
		super.onStart();
		getListView().setOnScrollListener(new AbsListView.OnScrollListener()
		{
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState)
			{
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount)
			{
				if (totalItemCount > 0
						&& (firstVisibleItem + visibleItemCount) >= totalItemCount
						&& totalItemCount > visibleItemCount)
				{
					performRefresh(true);
				}
				if (firstVisibleItem == 0
						&& context.getActionBar().getTabCount() > 0)
				{
					if (!PreferenceManager.getDefaultSharedPreferences(context)
							.getBoolean("enable_iconic_tabs", true))
					{
						context.getActionBar()
								.getTabAt(getArguments().getInt("tab_index"))
								.setText(R.string.tweets_str);
					}
					else
					{
						context.getActionBar()
								.getTabAt(getArguments().getInt("tab_index"))
								.setText("");
					}
				}
			}
		});
		getListView().setOnItemLongClickListener(
				new AdapterView.OnItemLongClickListener()
				{
					@Override
					public boolean onItemLongClick(AdapterView<?> arg0,
							View arg1, int index, long id)
					{
						TimelineCAB.performLongPressAction(getListView(),
								context.adapter, index);
						return true;
					}
				});
		setRetainInstance(true);
		setEmptyText(getString(R.string.no_tweets));
		screenName = getArguments().getString("query");
		reloadAdapter(true);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		if (getView() == null)
			return;
		getAdapter().restoreLastViewed(getListView());
	}

	@Override
	public void onPause()
	{
		super.onPause();
		savePosition();
	}

	@Override
	public void performRefresh(final boolean paginate)
	{
		if (context == null || isLoading || getAdapter() == null)
			return;
		isLoading = true;
		if (getAdapter().getCount() == 0 && getView() != null)
			setListShown(false);
		getAdapter().setLastViewed(getListView());
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				Paging paging = new Paging(1, 50);
				if (paginate)
					paging.setMaxId(getAdapter().getItemId(
							getAdapter().getCount() - 1));
				final Account acc = AccountService.getCurrentAccount();
				if (acc != null)
				{
					try
					{
						final ResponseList<Status> feed = acc.getClient()
								.getUserTimeline(screenName, paging);
						context.runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								setEmptyText(context
										.getString(R.string.no_tweets));
								int beforeLast = getAdapter().getCount() - 1;
								int addedCount = getAdapter().add(
										feed.toArray(new Status[0]));
								if (getView() != null)
								{
									if (paginate && addedCount > 0)
										getListView().smoothScrollToPosition(
												beforeLast + 1);
									else if (getView() != null
											&& getAdapter() != null)
										getAdapter().restoreLastViewed(
												getListView());
								}
								if (!PreferenceManager
										.getDefaultSharedPreferences(context)
										.getBoolean("enable_iconic_tabs", true))
								{
									context.getActionBar()
											.getTabAt(
													getArguments().getInt(
															"tab_index"))
											.setText(
													context.getString(R.string.tweets_str)
															+ " ("
															+ Integer
																	.toString(addedCount)
															+ ")");
								}
								else
									context.getActionBar()
											.getTabAt(
													getArguments().getInt(
															"tab_index"))
											.setText(
													Integer.toString(addedCount));
							}
						});
					}
					catch (final TwitterException e)
					{
						e.printStackTrace();
						context.runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								setEmptyText(context
										.getString(R.string.error_str));
								Toast.makeText(context, e.getErrorMessage(),
										Toast.LENGTH_SHORT).show();
							}
						});
					}
				}
				context.runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						if (getView() != null)
							setListShown(true);
						isLoading = false;
					}
				});
			}
		}).start();
	}

	@Override
	public void reloadAdapter(boolean firstInitialize)
	{
		if (AccountService.getCurrentAccount() != null)
		{
			if (getAdapter() != null && !firstInitialize && getView() != null)
				getAdapter().setLastViewed(getListView());
			if (getAdapter() == null)
			{
				context.adapter = new FeedListAdapter(context, null,
						AccountService.getCurrentAccount().getId());
			}
			setListAdapter(getAdapter());
			if (getAdapter().getCount() == 0)
				performRefresh(false);
			else if (getView() != null && getAdapter() != null)
				getAdapter().restoreLastViewed(getListView());
		}
	}

	@Override
	public void savePosition()
	{
		if (getView() != null && getAdapter() != null)
			getAdapter().setLastViewed(getListView());
	}

	@Override
	public void restorePosition()
	{
		if (getView() != null && getAdapter() != null)
			getAdapter().restoreLastViewed(getListView());
	}

	@Override
	public void jumpTop()
	{
		if (getView() != null)
			getListView().setSelectionFromTop(0, 0);
	}

	@Override
	public void filter()
	{
	}

	@Override
	public Status[] getSelectedStatuses()
	{
		if (context.adapter == null)
			return null;
		ArrayList<Status> toReturn = new ArrayList<Status>();
		SparseBooleanArray checkedItems = getListView()
				.getCheckedItemPositions();
		if (checkedItems != null)
		{
			for (int i = 0; i < checkedItems.size(); i++)
			{
				if (checkedItems.valueAt(i))
				{
					toReturn.add((Status) context.adapter.getItem(checkedItems
							.keyAt(i)));
				}
			}
		}
		return toReturn.toArray(new Status[0]);
	}

	@Override
	public User[] getSelectedUsers()
	{
		return null;
	}

	@Override
	public Tweet[] getSelectedTweets()
	{
		return null;
	}

	@Override
	public DMConversation[] getSelectedMessages()
	{
		return null;
	}

}
