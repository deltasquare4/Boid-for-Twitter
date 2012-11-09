package com.teamboid.twitter.columns;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
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
public class MyListsFragment extends BaseListFragment<UserList> {
	public static final String ID = "COLUMNTYPE:MYLISTS";

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		final UserList curList = (UserList)getListAdapter().getItem(position);
		Intent intent = new Intent(getActivity(), TweetListActivity.class)
						.putExtra("mode", TweetListActivity.USER_LIST)
						.putExtra("list_name", curList.getName())
						.putExtra("list_ID", curList.getId());
		getActivity().startActivity(intent);
	}

	@Override
	public void onStart() {
		super.onStart();
		final ListView list = getListView();
		list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int index, long id) {
				// Note: the Adapter has an AlertDialog
				((UserListDisplayAdapter)getListAdapter()).destroyOrUnsubscribe(index);
				return false;
			}
		});
		setRetainInstance(true);
		setEmptyText(getString(R.string.no_tweets));
	}

	@Override
	public Status[] getSelectedStatuses() { return null; }

	@Override
	public User[] getSelectedUsers() { return null; }

	@Override
	public Tweet[] getSelectedTweets() { return null; }

	@Override
	public DMConversation[] getSelectedMessages() { return null; }
	
	@Override
	public String getColumnName() {
		return AccountService.getCurrentAccount().getId() + ".mylists";
	}

	@Override
	public void setupAdapter() {
		if (AccountService.getCurrentAccount() != null) {
			setListAdapter( AccountService.getMyListsAdapter(getActivity()) );
		}
	}

	@Override
	public UserList[] fetch(long maxId, long sinceId) {
		try{
			Paging paging = new Paging(50);
			if(maxId != -1){
				paging.setMaxId(maxId);
			} if(sinceId != -1){
				paging.setSinceId(sinceId);
			}
			
			return AccountService.getCurrentAccount().getClient().getLists();
		} catch(Exception e){
			e.printStackTrace();
			showError(e.getMessage());
			return null;
		}
	}
}
