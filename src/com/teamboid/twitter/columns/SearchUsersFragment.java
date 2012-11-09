package com.teamboid.twitter.columns;

import java.util.ArrayList;
import android.content.Intent;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import com.teamboid.twitter.ProfileScreen;
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
public class SearchUsersFragment extends BaseListFragment<User> {
	private String query;
	int page = 1;

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		getActivity().startActivity(new Intent(getActivity(), ProfileScreen.class)
				.putExtra("screen_name", ((User)getListAdapter().getItem(position)).getScreenName())
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
	}

	@Override
	public void onStart() {
		query = getArguments().getString("query");
		super.onStart();
        getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        getListView().setMultiChoiceModeListener(UserListCAB.choiceListener);
	}
	
	@Override
	public Status[] getSelectedStatuses() { return null; }

	@Override
	public User[] getSelectedUsers() {
		if(getListAdapter() == null) return null;
		ArrayList<User> toReturn = new ArrayList<User>(); 
		SparseBooleanArray checkedItems = getListView().getCheckedItemPositions();
		if(checkedItems != null) {
			for(int i = 0; i < checkedItems.size(); i++) {
				if(checkedItems.valueAt(i)) {
					toReturn.add((User) getListAdapter().getItem(checkedItems.keyAt(i)));
				}
			}
		}
		return toReturn.toArray(new User[0]);
	}

	@Override
	public Tweet[] getSelectedTweets() { return null; }

	@Override
	public DMConversation[] getSelectedMessages() { return null; }
	
	@Override
	public String getColumnName() {
		return "n/a";
	}
	
	@Override
	public void setupAdapter() {
		if (AccountService.getCurrentAccount() != null) {
			if (getListAdapter() == null)
				setListAdapter( new SearchUsersListAdapter(getActivity()) );
		}
	}

	@Override
	public User[] fetch(long maxId, long sinceId) {
		try{
			if(maxId != -1)
				page++;
			else
				page =1;
			
			return AccountService.getCurrentAccount().getClient().searchUsers(query, page, 50);
		} catch(Exception e){
			e.printStackTrace();
			showError(e.getMessage());
			return null;
		}
	}
}
