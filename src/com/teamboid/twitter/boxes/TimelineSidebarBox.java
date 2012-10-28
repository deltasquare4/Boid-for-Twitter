package com.teamboid.twitter.boxes;

import com.handlerexploit.prime.RemoteImageView;
import com.teamboid.twitter.R;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;
import com.teamboid.twitterapi.user.User;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewStub;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * I am a box that deals with the timeline Sidebar controls
 * @author kennydude
 *
 */
public class TimelineSidebarBox {
	public static void setup(final Activity a){
		// Phase 1: Check if we are a tablet/large-screen
		if(a.findViewById(R.id.sidebar) != null){
			// Phase 2: Load
			
			ViewStub vs = (ViewStub) a.findViewById(R.id.sidebar);
			final View sidebar = vs.inflate();
			
			SharedPreferences s = PreferenceManager.getDefaultSharedPreferences(a);
			sidebar.setVisibility(s.getInt("Tab-SideShow", View.VISIBLE));
			
			try{
				User me = AccountService.getCurrentAccount().getUser();
				
				RemoteImageView pic = (RemoteImageView) sidebar.findViewById(R.id.me);
				pic.setImageURL( Utilities.getUserImage(me.getScreenName(), a, me) );
				
				TextView tiv = (TextView) sidebar.findViewById(R.id.meText);
				tiv.setText( Html.fromHtml("<big>" + me.getScreenName() + "</big><br/>" + me.getName()  ));
				
				tiv = (TextView)sidebar.findViewById(R.id.meTweets);
				tiv.setText( Html.fromHtml( "<big>" + me.getStatusCount() + "</big><br/>" + a.getString(R.string.tweet_str)) );
				
				tiv = (TextView)sidebar.findViewById(R.id.meFollowers);
				tiv.setText( Html.fromHtml("<big>"+ me.getFollowersCount() + "</big><br/>" + a.getString(R.string.followers_str) ));
				
				tiv = (TextView) sidebar.findViewById(R.id.meFollowing);
				tiv.setText(Html.fromHtml( "<big>"+ me.getFriendsCount() + "</big><br/>" + a.getString(R.string.friends_str)));
			} catch(Exception e){}
			
			ImageButton slider = (ImageButton)a.findViewById(R.id.sidebarDrawer);
			slider.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View arg0) {
					if(sidebar.getVisibility() == View.VISIBLE){
						sidebar.setVisibility(View.GONE);
					} else{
						sidebar.setVisibility(View.VISIBLE);
					}
					SharedPreferences s = PreferenceManager.getDefaultSharedPreferences(a);
					Editor e = s.edit();
					e.putInt("Tab-SideShow", sidebar.getVisibility());
					e.commit();
				}
				
			});
		}
	}
}
