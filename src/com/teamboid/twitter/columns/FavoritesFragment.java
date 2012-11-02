package com.teamboid.twitter.columns;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

import com.teamboid.twitter.Account;
import com.teamboid.twitter.R;
import com.teamboid.twitter.TabsAdapter.BaseListFragment;
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
 * Represents the column that displays the current user's favorited Tweets.
 * 
 * @author Aidan Follestad
 */
public class FavoritesFragment extends TimelineFragment {
	public static final String ID = "COLUMNTYPE:FAVORITES";

	@Override
	public String getColumnName() {
		return AccountService.getCurrentAccount().getId() + ".favorites";
	}

	@Override
	public Status[] fetch(long maxId) {
		try{
			Paging paging = new Paging(50);
			if(maxId != -1){
				paging.setMaxId(maxId);
			}
			return AccountService.getCurrentAccount().getClient().getFavorites(paging);
		} catch(Exception e){
			e.printStackTrace();
			showError(e.getMessage());
			return null;
		}
	}

	@Override
	public String getAdapterId() {
		return ID;
	}
}
