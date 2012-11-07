package com.teamboid.twitter.columns;

import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitterapi.client.Paging;
import com.teamboid.twitterapi.status.Status;

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
	public Status[] fetch(long maxId, long sinceId) {
		try{
			Paging paging = new Paging(50);
			if(maxId != -1){
				paging.setMaxId(maxId);
			}
			if(sinceId != -1){
				paging.setSinceId(sinceId);
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
