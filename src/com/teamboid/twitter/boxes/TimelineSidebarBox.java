package com.teamboid.twitter.boxes;

import com.handlerexploit.prime.RemoteImageView;
import com.teamboid.twitter.R;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;
import com.teamboid.twitterapi.user.User;

import android.app.Activity;
import android.view.View;
import android.view.ViewStub;
import android.widget.TextView;

/**
 * I am a box that deals with the timeline Sidebar controls
 * @author kennydude
 *
 */
public class TimelineSidebarBox {
	public static void setup(Activity a){
		// Phase 1: Check if we are a tablet/large-screen
		if(a.findViewById(R.id.sidebar) != null){
			// Phase 2: Load
			
			ViewStub vs = (ViewStub) a.findViewById(R.id.sidebar);
			View sidebar = vs.inflate();
			
			try{
				User me = AccountService.getCurrentAccount().getUser();
				
				RemoteImageView pic = (RemoteImageView) sidebar.findViewById(R.id.me);
				pic.setImageURL( Utilities.getUserImage(me.getScreenName(), a, me) );
				
				TextView tiv = (TextView) sidebar.findViewById(R.id.meText);
				tiv.setText( me.getScreenName() );
				
				tiv = (TextView)sidebar.findViewById(R.id.meTweets);
				tiv.setText( me.getStatusCount() + "" );
				
				tiv = (TextView)sidebar.findViewById(R.id.meFollowers);
				tiv.setText( me.getFollowersCount() + "" );
				
				tiv = (TextView) sidebar.findViewById(R.id.meFollowing);
				tiv.setText(me.getFriendsCount() + "");
			} catch(Exception e){}
		}
	}
}
