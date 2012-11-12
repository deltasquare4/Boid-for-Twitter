package com.teamboid.twitter.columns;


import com.teamboid.twitter.R;
import com.teamboid.twitter.TabsAdapter.BaseTimelineFragment;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitterapi.client.Paging;
import com.teamboid.twitterapi.status.Status;

/**
 * Represents the column that displays the feed of a user in the profile screen.
 * 
 * @author Aidan Follestad
 */
public class ProfileTimelineFragment extends BaseTimelineFragment {
	public static final String ID = "COLUMNTYPE:PROFILE_FEED";
	private String screenName;

	public boolean cacheContents(){
		if(getArguments().containsKey("home"))
			return true;
		return false;
	}
	
	@Override
	public int getPaddingTop(){
		if(getArguments().containsKey("home"))
			return 0;
		return getActivity().getResources().getDimensionPixelSize(R.dimen.profilePadding);
	}

	@Override
	public void onStart() {
		super.onStart();
		screenName = getArguments().getString("query");
	}
	
	@Override
	public String getColumnName() {
		return "timeline." + screenName + ".user";
	}

	@Override
	public String getAdapterId() {
		if(getArguments().containsKey("home"))
			return "COLUMNTYPE:PROFILE_FEED";
		return "PROFILE:ID";
	}

	@Override
	public Status[] fetch(long maxId, long sinceId) {
		try{
			Paging paging = new Paging(50);
			if(maxId != -1)
				paging.setMaxId(maxId);
			if(sinceId != -1)
				paging.setSinceId(sinceId);
			
			return AccountService.getCurrentAccount().getClient().getUserTimeline(
					screenName, paging, true);
		} catch(Exception e){
			e.printStackTrace();
			showError(e.getMessage());
			return null;
		}
	}

}
