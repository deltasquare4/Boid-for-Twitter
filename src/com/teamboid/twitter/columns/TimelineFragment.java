package com.teamboid.twitter.columns;

import com.teamboid.twitter.TabsAdapter.BaseTimelineFragment;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitterapi.client.Paging;
import com.teamboid.twitterapi.status.Status;

/**
 * Represents the column that displays the user's home timeline feed.
 * @author Aidan Follestad
 */
public class TimelineFragment extends BaseTimelineFragment {
	public static final String ID = "COLUMNTYPE:TIMELINE";

	@Override
	public String getColumnName() {
		return AccountService.getCurrentAccount().getId() + ".timeline";
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
			return AccountService.getCurrentAccount().getClient().getHomeTimeline(paging);
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
