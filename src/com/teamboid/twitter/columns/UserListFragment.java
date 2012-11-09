package com.teamboid.twitter.columns;

import com.teamboid.twitter.TabsAdapter.BaseTimelineFragment;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitterapi.client.Paging;
import com.teamboid.twitterapi.status.Status;

/**
 * Represents the user list column type, displays a stream of Status objects retrieved through the user's own or subscribed lists.
 *
 * @author Aidan Follestad
 */
public class UserListFragment extends BaseTimelineFragment {
    public static final String ID = "COLUMNTYPE:USERLIST";
    private String listName;
    private int listID;

    @Override
    public void onStart() {
        listName = getArguments().getString("list_name");
        listID = getArguments().getInt("list_id");
        super.onStart();
    }
    
    @Override
	public String getColumnName() {
		return AccountService.getCurrentAccount().getId() + ".list." + listID;
	}

	@Override
	public String getAdapterId() {
		return UserListFragment.ID + "@"
                + listName.replace("@", "%40") + "@"
                + Integer.toString(listID);
	}

	@Override
	public Status[] fetch(long maxId, long sinceId) {
		try{
			Paging paging = new Paging(50);
            if (maxId != -1)
            	paging.setMaxId(maxId);
            if(sinceId != -1)
            	paging.setSinceId(sinceId);
			return AccountService.getCurrentAccount().getClient().getListTimeline(listID, paging);
		} catch(Exception e){
			e.printStackTrace();
			showError(e.getMessage());
			return null;
		}
	}

}
