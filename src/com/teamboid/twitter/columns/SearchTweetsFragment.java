package com.teamboid.twitter.columns;

import java.util.ArrayList;
import android.content.Intent;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.teamboid.twitter.Account;
import com.teamboid.twitter.ComposerScreen;
import com.teamboid.twitter.R;
import com.teamboid.twitter.TweetViewer;
import com.teamboid.twitter.TabsAdapter.BaseListFragment;
import com.teamboid.twitter.listadapters.SearchFeedListAdapter;
import com.teamboid.twitter.listadapters.MessageConvoAdapter.DMConversation;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;
import com.teamboid.twitterapi.client.Paging;
import com.teamboid.twitterapi.search.SearchQuery;
import com.teamboid.twitterapi.search.Tweet;
import com.teamboid.twitterapi.status.Status;
import com.teamboid.twitterapi.user.User;

/**
 * Represents the column used in the search screen that displays Tweet search
 * results.
 * 
 * @author Aidan Follestad
 */
public class SearchTweetsFragment extends BaseListFragment<Tweet> {
	private String query;
	public static final String ID = "COLUMNTYPE:SEARCH";

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Tweet tweet = (Tweet) getAdapter().getItem(position);
		getActivity().startActivity(new Intent(getActivity(), TweetViewer.class)
				.putExtra("tweet_id", id)
				.putExtra("user_name", tweet.getFromUser())
				.putExtra("user_id", tweet.getFromUserId())
				.putExtra("screen_name", tweet.getFromUser())
				.putExtra("content", tweet.getText())
				.putExtra("timer", tweet.getCreatedAt().getTime())
				.putExtra("via", tweet.getSource())
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
	}

	@Override
	public void onStart() {
		query = getArguments().getString("query");
		
		super.onStart();
		getListView().setOnItemLongClickListener(
				new AdapterView.OnItemLongClickListener() {
					@Override
					public boolean onItemLongClick(AdapterView<?> arg0,
							View arg1, int index, long id) {
						Tweet toReply = (Tweet) getAdapter()
								.getItem(index);
						getActivity().startActivity(new Intent(getActivity(),
								ComposerScreen.class)
								.putExtra("reply_to_tweet", toReply)
								.putExtra("reply_to_name", toReply.getFromUser())
								.putExtra("append", Utilities.getAllMentions(toReply.getFromUser(), toReply.getText()))
								.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
						return false;
					}
				});
		setRetainInstance(true);
		setEmptyText(getString(R.string.no_results));
	}

	@Override
	public Status[] getSelectedStatuses() {
		return null;
	}

	@Override
	public User[] getSelectedUsers() {
		return null;
	}

	@Override
	public Tweet[] getSelectedTweets() {
		ArrayList<Tweet> toReturn = new ArrayList<Tweet>();
		SparseBooleanArray checkedItems = getListView()
				.getCheckedItemPositions();
		if (checkedItems != null) {
			for (int i = 0; i < checkedItems.size(); i++) {
				if (checkedItems.valueAt(i)) {
					toReturn.add((Tweet) getAdapter()
							.getItem(checkedItems.keyAt(i)));
				}
			}
		}
		return toReturn.toArray(new Tweet[0]);
	}

	@Override
	public DMConversation[] getSelectedMessages() {
		return null;
	}
	
	@Override
	public String getColumnName() {
		return AccountService.getCurrentAccount().getId() + ".saved-" + query.replace("/", "_");
	}

	@Override
	public void setupAdapter() {
		if (AccountService.getCurrentAccount() != null) {
			if (getAdapter() == null) {
				setListAdapter( new SearchFeedListAdapter(getActivity(),
						SearchTweetsFragment.ID, AccountService
								.getCurrentAccount().getId(), query) );
			}
		}
	}

	@Override
	public Tweet[] fetch(long maxId, long sinceId) {
		try{
			Paging paging = new Paging(50);
			if (maxId != -1)
				paging.setMaxId(maxId);
			SearchQuery q = SearchQuery.create(query, paging);
			final Account acc = AccountService.getCurrentAccount();
			if (acc != null)
				return acc.getClient().search(q).getResults();
			else
				throw new Exception("Account Error");
		} catch(Exception e){
			e.printStackTrace();
			showError(e.getMessage());
		}
		return null;
	}
}
