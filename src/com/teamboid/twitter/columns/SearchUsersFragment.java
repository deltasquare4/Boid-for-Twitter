package com.teamboid.twitter.columns;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ActionBar.Tab;
import android.content.Intent;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.teamboid.twitter.Account;
import com.teamboid.twitter.ProfileScreen;
import com.teamboid.twitter.R;
import com.teamboid.twitter.SearchScreen;
import com.teamboid.twitter.TabsAdapter.BaseListFragment;
import com.teamboid.twitter.cab.UserListCAB;
import com.teamboid.twitter.listadapters.SearchUsersListAdapter;
import com.teamboid.twitter.listadapters.MessageConvoAdapter.DMConversation;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitterapi.search.Tweet;
import com.teamboid.twitterapi.status.Status;
import com.teamboid.twitterapi.user.User;

/**
 * The column used in the search screen to display User search results.
 * @author Aidan Follestad
 */
public class SearchUsersFragment extends BaseListFragment {

	private SearchScreen context;
	private String query;
	private int page;

	@Override
	public void onAttach(Activity act) {
		super.onAttach(act);
		context = (SearchScreen) act;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		context.startActivity(new Intent(context, ProfileScreen.class)
		.putExtra("screen_name", ((User) context.userAdapter.getItem(position)).getScreenName())
		.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
	}

	@Override
	public void onStart() {
		super.onStart();
        getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        getListView().setMultiChoiceModeListener(UserListCAB.choiceListener);
		getListView().setOnScrollListener(
				new AbsListView.OnScrollListener() {
					@Override
					public void onScrollStateChanged(AbsListView view, int scrollState) { }
					@Override
					public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
						if (totalItemCount > 0 && (firstVisibleItem + visibleItemCount) >= totalItemCount && totalItemCount > visibleItemCount)
							performRefresh(true);
						if (context.userAdapter != null && getView() != null) {
							context.userAdapter.savedIndex = firstVisibleItem;
							View v = getListView().getChildAt(0);
							context.userAdapter.savedIndexTop = (v == null) ? 0 : v.getTop();
						}
						if (firstVisibleItem == 0 && context.getActionBar().getTabCount() > 0) {
							context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(R.string.users_str);
						}
					}
				});
		setRetainInstance(true);
		setEmptyText(getString(R.string.no_results));
		query = getArguments().getString("query");
		reloadAdapter(true);
	}

	@Override
	public void performRefresh(final boolean paginate) {
		if(context == null || isLoading || context.userAdapter == null) return;
		isLoading = true;
		if(context.userAdapter.getCount() == 0 && getView() != null) setListShown(false);
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (paginate) page++;
				else page = 1;
				final Account acc = AccountService.getCurrentAccount();
				if (acc != null) {
					try {
						final User[] feed = acc.getClient().searchUsers(query, page, 50);
						context.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								setEmptyText(context.getString(R.string.no_results));
								int beforeLast = context.userAdapter.getCount() - 1;
								int addedCount = context.userAdapter.add(feed);
								if (beforeLast > 0) {
									if (getView() != null && addedCount > 0) {
										if(paginate && addedCount > 0) getListView().smoothScrollToPosition(beforeLast + 1);
										else getListView().setSelectionFromTop(context.userAdapter.savedIndex+ addedCount, context.userAdapter.savedIndexTop);
									}
									Tab curTab = context.getActionBar().getTabAt(getArguments().getInt("tab_index"));
									String curTitle = "";
									if (curTab.getText() != null) curTitle = curTab.getText().toString();
									if (curTitle != null && !curTitle.isEmpty() && curTitle.contains("(")) {
										curTitle = curTitle.substring(0, curTitle.lastIndexOf("(") - 2);
										curTitle += (" (" + Integer.toString(addedCount) + ")");
									} else curTitle = context.getString(R.string.users_str)+ " (" + Integer.toString(addedCount) + ")";
									context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(curTitle);
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
						if (getView() != null)
							setListShown(true);
						isLoading = false;
					}
				});
			}
		}).start();
	}

	@Override
	public void reloadAdapter(boolean firstInitialize) {
		if (AccountService.getCurrentAccount() != null) {
			if (context.userAdapter == null)
				context.userAdapter = new SearchUsersListAdapter(context);
			if (getView() != null)
				context.userAdapter.list = getListView();
			setListAdapter(context.userAdapter);
			if (context.userAdapter.getCount() == 0)
				performRefresh(false);
			else if (context.userAdapter.savedIndex > 0
					&& context.userAdapter.list != null) {
				getListView().setSelectionFromTop(
						context.userAdapter.savedIndex,
						context.userAdapter.savedIndexTop);
			}
		}
	}

	@Override
	public void savePosition() {
	}

	@Override
	public void restorePosition() {
	}

	@Override
	public void jumpTop() {
		if (getView() != null)
			getListView().setSelectionFromTop(0, 0);
	}

	@Override
	public void filter() {
	}

	@Override
	public Status[] getSelectedStatuses() { return null; }

	@Override
	public User[] getSelectedUsers() {
		if(context.userAdapter == null) return null;
		ArrayList<User> toReturn = new ArrayList<User>(); 
		SparseBooleanArray checkedItems = getListView().getCheckedItemPositions();
		if(checkedItems != null) {
			for(int i = 0; i < checkedItems.size(); i++) {
				if(checkedItems.valueAt(i)) {
					toReturn.add((User)context.userAdapter.getItem(checkedItems.keyAt(i)));
				}
			}
		}
		return toReturn.toArray(new User[0]);
	}

	@Override
	public Tweet[] getSelectedTweets() { return null; }

	@Override
	public DMConversation[] getSelectedMessages() { return null; }
}
