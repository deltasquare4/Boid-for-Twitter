package com.teamboid.twitter.columns;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.Date;

import com.teamboid.twitterapi.client.Twitter;
import com.teamboid.twitterapi.relationship.Relationship;
import com.teamboid.twitterapi.user.FollowingType;
import com.teamboid.twitterapi.user.User;
import org.joda.time.DateTime;
import org.joda.time.Days;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.teamboid.twitter.Account;
import com.teamboid.twitter.R;
import com.teamboid.twitter.TabsAdapter.IBoidFragment;
import com.teamboid.twitter.TweetListActivity;
import com.teamboid.twitter.UserListActivity;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;

/**
 * Represents the column that displays details about a profile, it's padded to compesensate for the header in the profile screen.
 * @author Aidan Follestad
 */
public class ProfileAboutFragment extends Fragment implements IBoidFragment {
	boolean refreshing;
	String screenName;
	User currentUser;
	Relationship ourRelationship;
	
	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		return inflater.inflate(R.layout.profile_info_tab, container, false);
	}
	
	public void setScreenName(String name){
		screenName = name;
		performRefresh();
	}

	public boolean isBlocked(){ return ourRelationship.isSourceBlockingTarget(); }
	
	@Override
	public boolean isRefreshing() {
		return refreshing;
	}
	
	void setRefreshing(boolean r){
		refreshing = r;
		getActivity().invalidateOptionsMenu();
	}

	@Override
	public void onDisplay() { /* ignore */ }
	
	void setItem(int id, String title, String body){
		View c = getView().findViewById(id);
		c.setVisibility(View.VISIBLE);
		((TextView)c.findViewById(R.id.infoListItemTitle)).setText(title);
		((TextView)c.findViewById(R.id.infoListItemBody)).setText(body);
	}
	
	void hideItem(int id){
		getView().findViewById(id).setVisibility(View.GONE);
	}
	
	public static double round(double unrounded, int precision, int roundingMode) {
		if (unrounded == 0.0) {
			return 0.0;
		}
		BigDecimal bd = new BigDecimal(unrounded);
		BigDecimal rounded = bd.setScale(precision, roundingMode);
		return rounded.doubleValue();
	}

	@Override
	public void performRefresh() {
		setRefreshing(true);
		new Thread(new Runnable(){

			@Override
			public void run() {
				try{
					Account acc = AccountService.getCurrentAccount();
					
					if (screenName.equals(acc.getUser().getScreenName())) {
						currentUser = acc.getClient().verifyCredentials();
						AccountService.setAccount(getActivity(), currentUser);
					} else currentUser = acc.getClient().showUser(screenName);
					
					if(currentUser.isProtected()){
						// Do something here :/
						// Probably should signal the activity to display a protected version or summer
					}
					
					// Now we set all of the "rows"
					// They are manually entered because it does not change
					
					setItem(R.id.bio, getString(R.string.bio_str), currentUser.getDescription());
					setItem(R.id.tweetNo, getString(R.string.tweets_str), currentUser.getStatusCount() + "");
					
					double tweetsPerDay = currentUser.getStatusCount();
					int days = Days.daysBetween(new DateTime(currentUser.getCreatedAt()),
							new DateTime(new Date())).getDays();
					tweetsPerDay /= days;
					System.out.println("Dividing: " + tweetsPerDay + " / " + days);
					tweetsPerDay = round(tweetsPerDay, 2, BigDecimal.ROUND_HALF_UP);
					setItem(R.id.tweetsPerDay, getString(R.string.tweets_per_day), tweetsPerDay + "");
					
					if(currentUser.getUrl() != null){
						setItem(R.id.website, getString(R.string.website_str), currentUser.getUrl());
					} else{
						hideItem(R.id.website);
					}
					
					if(currentUser.getLocation() != null){
						setItem(R.id.location, getString(R.string.website_str), currentUser.getLocation());
					} else{
						hideItem(R.id.location);
					}
					
					setItem(R.id.favorites, getString(R.string.favorite_str), currentUser.getFavoritesCount()+"");
					setItem(R.id.friends, getString(R.string.friends_str), currentUser.getFriendsCount() + "");
					setItem(R.id.followers, getString(R.string.followers_str), currentUser.getFollowersCount()  +"");
					
					getView().findViewById(R.id.verified).setVisibility( currentUser.isVerified() ? View.VISIBLE : View.GONE );
					
					// Follow button logic
					updateFollowButton();
					
				} catch(Exception e){
					e.printStackTrace();
					// Display an error
				}
			}
			
		}).start();
	}
	
	void updateFollowButton(){
		try{
			ourRelationship = AccountService.getCurrentAccount().getClient().getRelationship(
					AccountService.getCurrentAccount().getId(),
					currentUser.getId());
		} catch(Exception e){ e.printStackTrace(); }
		
		Button button = ((Button)getView().findViewById(R.id.profileFollowBtn));
		
		if(ourRelationship == null){
			button.setText(R.string.error_str);
			button.setEnabled(false);
		} else{
			button.setEnabled(true);
			if(ourRelationship.isSourceBlockingTarget()){
				button.setText(R.string.unblock_str);
			} else if(currentUser.getFollowingType().equals(FollowingType.REQUEST_SENT)){
				button.setText(R.string.request_sent_str);
			} else{
				if (!ourRelationship.isSourceFollowingTarget()) {
					if (ourRelationship.isTargetFollowingSource())
						button.setText(R.string.follow_back_str);
					else
						button.setText(R.string.follow_str);
				} else {
					if (ourRelationship.isTargetFollowingSource())
						button.setText(R.string.follows_you_unfollow_str);
					else
						button.setText(R.string.unfollow_str);
				}
			}
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		setRetainInstance(true);
		if(getArguments() == null) return;
		screenName = getArguments().getString("query");
		if(screenName != null){
			performRefresh();
		}
		
		// Now attach even handlers
		// Follow/Unfollow button
		getView().findViewById(R.id.profileFollowBtn).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final ProgressDialog pd = new ProgressDialog(getActivity());
				pd.setMessage(getString(R.string.please_wait));
				pd.show();
				new Thread(new Runnable(){

					@Override
					public void run() {
						// Now we actually follow/unblock/whatever
						Twitter cl = AccountService.getCurrentAccount().getClient();
						try{
							if(ourRelationship.isSourceBlockingTarget()){
								cl.destroyBlock(currentUser.getId());
							} else if(ourRelationship.isSourceFollowingTarget()){
								cl.destroyFriendship(currentUser.getId());
							} else if(!ourRelationship.isSourceFollowingTarget()){
								cl.createFriendship(currentUser.getId());
							}
							
							// This will confirm the action worked
							updateFollowButton();
						} catch(Exception e){
							e.printStackTrace();
							// Show error
						}
						pd.dismiss();
					}
					
				}).start();
			}
		});
		
		// Rows
		getView().findViewById(R.id.website).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try {
					getActivity().startActivity(new Intent(Intent.ACTION_VIEW)
						.setData(Uri.parse(currentUser.getUrl())).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		getView().findViewById(R.id.location).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String loc = null;
				try { loc = URLEncoder.encode( currentUser.getLocation() , "UTF-8"); }
				catch (Exception e) { e.printStackTrace(); }
				Intent geo = new Intent(Intent.ACTION_VIEW);
				geo.setData(Uri.parse("geo:0,0?q=" + loc));
				if (Utilities.isIntentAvailable(getActivity(), geo)) startActivity(geo);
				else {
					geo.setData(Uri.parse("https://maps.google.com/maps?q=" + loc));
					startActivity(geo);
				}
			}
		});
		
		// User Lists
		View.OnClickListener listClick = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(), UserListActivity.class)
					.putExtra("mode", (Integer)v.getTag())
					.putExtra("user", currentUser.getId())
					.putExtra("username", currentUser.getScreenName());
				getActivity().startActivity(intent);
			}
		};
		
		getView().findViewById(R.id.followers).setTag(UserListActivity.FOLLOWERS_LIST);
		getView().findViewById(R.id.followers).setOnClickListener(listClick);
		getView().findViewById(R.id.friends).setTag(UserListActivity.FOLLOWING_LIST);
		getView().findViewById(R.id.friends).setOnClickListener(listClick);
		
		getView().findViewById(R.id.favorites).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(), TweetListActivity.class)
								.putExtra("mode", TweetListActivity.USER_FAVORITES)
								.putExtra("username", currentUser.getScreenName());
				getActivity().startActivity(intent);
			}
		});
		getView().findViewById(R.id.tweetNo).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO: Some logic so we can hinge this apart from Profile
				getActivity().getActionBar().setSelectedNavigationItem(0);
			}
		});
	}
	

}