package com.teamboid.twitter.columns;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.teamboid.twitter.Account;
import com.teamboid.twitter.R;
import com.teamboid.twitter.TweetListActivity;
import com.teamboid.twitter.TabsAdapter.BaseListFragment;
import com.teamboid.twitter.listadapters.UserListDisplayAdapter;
import com.teamboid.twitter.listadapters.MessageConvoAdapter.DMConversation;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitterapi.client.Paging;
import com.teamboid.twitterapi.list.UserList;
import com.teamboid.twitterapi.search.Tweet;
import com.teamboid.twitterapi.status.Status;
import com.teamboid.twitterapi.user.User;

/**
 * Represents the column that lists the user lists of the current user, these user lists are created/subscribed to on Twitter's website. 
 * @author Aidan Follestad
 */
public class MyListsFragment extends BaseListFragment {

	private UserListDisplayAdapter adapt;
	private Activity context;
	public static final String ID = "COLUMNTYPE:MYLISTS";

	@Override
	public void onAttach(Activity act) {
		super.onAttach(act);
		context = act;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		final UserList curList = (UserList)adapt.getItem(position);
		Intent intent = new Intent(context, TweetListActivity.class)
		.putExtra("mode", TweetListActivity.USER_LIST)
		.putExtra("list_name", curList.getName())
		.putExtra("list_ID", curList.getId());
		context.startActivity(intent);
	}

	@Override
	public void onStart() {
		super.onStart();
		final ListView list = getListView();
		list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int index, long id) {
				adapt.destroyOrUnsubscribe(index);
				return false;
			}
		});
		setRetainInstance(true);
		setEmptyText(getString(R.string.no_tweets));
		reloadAdapter(true);
	}

	@Override
	public void performRefresh(final boolean paginate) {
		if (context == null || isLoading || adapt == null)
			return;
		isLoading = true;
		if (adapt.getCount() == 0 && getView() != null)
			setListShown(false);
		new Thread(new Runnable() {
			@Override
			public void run() {
				Paging paging = new Paging(50);
				if (paginate)
					paging.setMaxId(adapt.getItemId(adapt.getCount() - 1));
				final Account acc = AccountService.getCurrentAccount();
				if (acc != null) {
					try {
						final UserList[] lists = acc.getClient().getLists();
						context.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								setEmptyText(context.getString(R.string.no_lists));
								adapt.add(lists);
							}
						});
					} catch (final Exception e) {
						e.printStackTrace();
						context.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								setEmptyText(context.getString(R.string.error_str));
								Toast.makeText(context,	e.getMessage(), Toast.LENGTH_SHORT).show();
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
		if (context == null && getActivity() != null)
			context = getActivity();
		if (AccountService.getCurrentAccount() != null) {
			adapt = AccountService.getMyListsAdapter(context);
			setListAdapter(adapt);
			if (adapt.getCount() == 0)
				performRefresh(false);
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
	public void filter() { }

	@Override
	public Status[] getSelectedStatuses() { return null; }

	@Override
	public User[] getSelectedUsers() { return null; }

	@Override
	public Tweet[] getSelectedTweets() { return null; }

	@Override
	public DMConversation[] getSelectedMessages() { return null; }
}
