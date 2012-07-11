package com.teamboid.twitter;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import org.apache.http.message.BasicNameValuePair;

import com.handlerexploit.prime.widgets.RemoteImageView;

import twitter4j.TwitterException;
import twitter4j.User;

import android.app.Activity;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The list adapter used for the "About" tab in the profile activity.
 * @author Aidan Follestad
 */
public class ProfileAboutAdapter extends BaseAdapter {

	public ProfileAboutAdapter(Activity context) { 
		mContext = context;
		values = new ArrayList<BasicNameValuePair>();
		following = FollowingType.UNKNOWN;
	}

	private Activity mContext;
	public User user;
	public ArrayList<BasicNameValuePair> values;
	private FollowingType following;

	public void setUser(User _user) {
		user = _user;
		values.clear();
		NumberFormat nf = NumberFormat.getNumberInstance();
		DecimalFormat df = (DecimalFormat)nf;
		df.applyPattern("###,###,###,###,###.###");
		if(user.getId() != AccountService.getCurrentAccount().getId()) {
			values.add(new BasicNameValuePair(mContext.getString(R.string.follows_you), mContext.getString(R.string.unknown_str)));
		}
		if(_user.getURL() != null) values.add(new BasicNameValuePair(mContext.getString(R.string.website_str), _user.getURL().toString()));
		else values.add(new BasicNameValuePair(mContext.getString(R.string.website_str), mContext.getString(R.string.none_str)));
		values.add(new BasicNameValuePair(mContext.getString(R.string.tweets_str), df.format(_user.getStatusesCount())));
		if(_user.getLocation() != null && !_user.getLocation().isEmpty()) {
			values.add(new BasicNameValuePair(mContext.getString(R.string.location_str), _user.getLocation()));
		} else values.add(new BasicNameValuePair(mContext.getString(R.string.location_str), mContext.getString(R.string.unknown_str))); 
		values.add(new BasicNameValuePair(mContext.getString(R.string.friends_str), df.format(_user.getFriendsCount())));
		values.add(new BasicNameValuePair(mContext.getString(R.string.followers_str), df.format(_user.getFollowersCount())));
		values.add(new BasicNameValuePair(mContext.getString(R.string.favorites_str), df.format(_user.getFavouritesCount())));
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		if(user == null) return 0;
		else return (values.size() + 1);
	}

	@Override
	public Object getItem(int position) {
		if(position == 0) return user;
		else return values.get(position - 1).getValue();
	}

	@Override
	public long getItemId(int position) { return position; }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		RelativeLayout toReturn = null;
		if(position == 0) {
			//Can't recycle here, causes crashing
			toReturn = (RelativeLayout)LayoutInflater.from(mContext).inflate(R.layout.profile_about, null);
			final RemoteImageView profileImg = (RemoteImageView)toReturn.findViewById(R.id.userItemProfilePic);
			profileImg.setImageResource(R.drawable.sillouette);
			profileImg.setImageURL(Utilities.getUserImage(user.getScreenName(), mContext));
			((TextView)toReturn.findViewById(R.id.userItemName)).setText(user.getName());
			TextView desc = (TextView)toReturn.findViewById(R.id.userItemDescription);
			if(user.getDescription() != null && user.getDescription().trim().length() > 0) {
				desc.setText(Utilities.twitterifyText(mContext, user.getDescription().replace("\n", " ").trim(), null, null, true));
				desc.setMovementMethod(LinkMovementMethod.getInstance());
			} else desc.setText(R.string.nodescription_str);
			if(user.isVerified()) ((ImageView)toReturn.findViewById(R.id.userItemVerified)).setVisibility(View.VISIBLE);
			else ((ImageView)toReturn.findViewById(R.id.userItemVerified)).setVisibility(View.GONE);
			if(user.getId() == AccountService.getCurrentAccount().getId()) {
				toReturn.findViewById(R.id.followButton).setVisibility(View.GONE);
			} else {
				final Account acc = AccountService.getCurrentAccount();
				final Button followBtn = (Button)toReturn.findViewById(R.id.followButton);
				followBtn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						((ProfileScreen)mContext).showProgress(true);
						followBtn.setEnabled(false);
						new Thread(new Runnable() {
							public void run() {
								if(following == FollowingType.UNFOLLOWED) {
									try {
										acc.getClient().createFriendship(user.getId());
										mContext.runOnUiThread(new Runnable() {
											public void run() {
												following = FollowingType.FOLLOWED;
												followBtn.setText(R.string.unfollow_str);
											}
										});
									} catch (final TwitterException e) {
										e.printStackTrace();
										mContext.runOnUiThread(new Runnable() {
											public void run() { Toast.makeText(mContext, e.getErrorMessage(), Toast.LENGTH_SHORT).show(); }
										});
									}
								} else if(following == FollowingType.FOLLOWED) {
									try {
										acc.getClient().destroyFriendship(user.getId());
										mContext.runOnUiThread(new Runnable() {
											public void run() {
												following = FollowingType.UNFOLLOWED;
												followBtn.setText(R.string.follow_str);
											}
										});
									} catch (final TwitterException e) {
										e.printStackTrace();
										mContext.runOnUiThread(new Runnable() {
											public void run() { Toast.makeText(mContext, e.getErrorMessage(), Toast.LENGTH_SHORT).show(); }
										});
									}
								} else if(following == FollowingType.BLOCKED) {
									try {
										acc.getClient().destroyBlock(user.getId());
										mContext.runOnUiThread(new Runnable() {
											public void run() {
												following = FollowingType.UNFOLLOWED;
												followBtn.setText(R.string.follow_str);
												((ProfileScreen)mContext).recreate();
											}
										});
									} catch (final TwitterException e) {
										e.printStackTrace();
										mContext.runOnUiThread(new Runnable() {
											public void run() { Toast.makeText(mContext, e.getErrorMessage(), Toast.LENGTH_SHORT).show(); }
										});
									}
								}
								mContext.runOnUiThread(new Runnable() {
									public void run() { 
										((ProfileScreen)mContext).showProgress(false);
										followBtn.setEnabled(true);
									}
								});
							}
						}).start();
					}
				});
				if(following.equals(FollowingType.UNKNOWN)) {
					followBtn.setEnabled(false);
					if(user.isFollowRequestSent()) {
						following = FollowingType.REQUEST_SENT;
						followBtn.setText(R.string.request_sent);
					} else {
						new Thread(new Runnable() {
							public void run() {
								try {
									final boolean isBlocked = acc.getClient().existsBlock(user.getId());
									mContext.runOnUiThread(new Runnable() {
										public void run() { 
											if(isBlocked) {
												following = FollowingType.BLOCKED;
												followBtn.setText(R.string.unblock_str);
												followBtn.setEnabled(true);
											}
											((ProfileScreen)mContext).isBlocked = isBlocked;
											if(isBlocked) ((ProfileScreen)mContext).getActionBar().setSelectedNavigationItem(1);
											((ProfileScreen)mContext).invalidateOptionsMenu();
										}
									});
									if(isBlocked) return;
								} catch (final TwitterException e) {
									e.printStackTrace();
									mContext.runOnUiThread(new Runnable() {
										public void run() {
											Toast.makeText(mContext, R.string.failed_check_blocked, Toast.LENGTH_SHORT).show();
											followBtn.setText(R.string.error_str);
										}
									});
									return;
								}
								try {
									final boolean isFollowing = acc.getClient().existsFriendship(acc.getUser().getScreenName(), user.getScreenName());
									mContext.runOnUiThread(new Runnable() {
										public void run() {
											followBtn.setEnabled(true);
											if(isFollowing) {
												following = FollowingType.FOLLOWED;
												followBtn.setText(R.string.unfollow_str);

											} else {
												following = FollowingType.UNFOLLOWED;
												followBtn.setText(R.string.follow_str);
											}
										}
									});
								} catch (final TwitterException e) {
									e.printStackTrace();
									mContext.runOnUiThread(new Runnable() {
										public void run() {
											Toast.makeText(mContext, R.string.failed_check_following, Toast.LENGTH_SHORT).show();
											followBtn.setText(R.string.error_str);
										}
									});
									return;
								}
								try {
									final boolean isFollowedBy = acc.getClient().existsFriendship(user.getScreenName(), acc.getUser().getScreenName());
									mContext.runOnUiThread(new Runnable() {
										public void run() {
											values.set(0, new BasicNameValuePair(mContext.getString(R.string.follows_you), 
													(isFollowedBy == true) ? mContext.getString(R.string.yes_str) : mContext.getString(R.string.no_str)));
											notifyDataSetChanged();
										}
									});
								} catch (final TwitterException e) {
									e.printStackTrace();
									mContext.runOnUiThread(new Runnable() {
										public void run() { Toast.makeText(mContext, R.string.failed_check_followed_by, Toast.LENGTH_SHORT).show(); }
									});
									return;
								}
							}
						}).start();
					}
				} else if(following.equals(FollowingType.BLOCKED)) {
					followBtn.setText(R.string.unblock_str);
					followBtn.setEnabled(true);
				} else if(following.equals(FollowingType.FOLLOWED)) {
					followBtn.setText(R.string.unfollow_str);
					followBtn.setEnabled(true);
				} else if(following.equals(FollowingType.UNFOLLOWED)) {
					followBtn.setText(R.string.follow_str);
					followBtn.setEnabled(true);
				} else if(following.equals(FollowingType.REQUEST_SENT)) {
					followBtn.setText(R.string.request_sent);
					followBtn.setEnabled(false);
				}
			}
		} else {
			BasicNameValuePair curItem = values.get(position - 1);
			//Can't recycle here, causes crashing
			toReturn = (RelativeLayout)LayoutInflater.from(mContext).inflate(R.layout.info_list_item, null);
			((TextView)toReturn.findViewById(R.id.infoListItemTitle)).setText(curItem.getName());
			((TextView)toReturn.findViewById(R.id.infoListItemBody)).setText(curItem.getValue());
		}
		return toReturn;
	}

	public static enum FollowingType {
		FOLLOWED, UNFOLLOWED, BLOCKED, REQUEST_SENT, UNKNOWN
	}
}
