package com.teamboid.twitter;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import org.apache.http.message.BasicNameValuePair;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

import android.app.Activity;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
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
	}

	private Activity mContext;
	public User user;
	public ArrayList<BasicNameValuePair> values;

	private boolean isUnknown = true;
	private boolean isRequestSent;
	private boolean isBlocked = false;
	private boolean isFollowing = false;
	private boolean isFollowedBy = false;

	public void updateRequestSent(final boolean requestSent) {
		mContext.runOnUiThread(new Runnable() {
			public void run() {
				isRequestSent = requestSent;
				isUnknown = (!isBlocked && !isFollowing && !isFollowedBy && !isRequestSent);
				notifyDataSetChanged();
			}
		});
	}
	public void updateIsBlocked(final boolean blocked) {
		mContext.runOnUiThread(new Runnable() {
			public void run() {
				isBlocked = blocked;
				isUnknown = (!isBlocked && !isFollowing && !isFollowedBy && !isRequestSent);
			}
		});
	}
	public boolean isBlocked() { return isBlocked; }
	public void updateIsFollowing(final boolean following) {
		mContext.runOnUiThread(new Runnable() {
			public void run() {
				isFollowing = following;
				isUnknown = (!isBlocked && !isFollowing && !isFollowedBy && !isRequestSent);
			}
		});
	}
	public boolean isFollowing() { return isFollowing; }
	public void updateIsFollowedBy(final boolean followsYou) {
		mContext.runOnUiThread(new Runnable() {
			public void run() {
				isFollowedBy = followsYou;
				isUnknown = (!isBlocked && !isFollowing && !isFollowedBy && !isRequestSent);
			}
		});
	}
	public boolean isFollowedBy() { return isFollowedBy; }

	public void setUser(User _user) {
		user = _user;
		values.clear();
		NumberFormat nf = NumberFormat.getNumberInstance();
		DecimalFormat df = (DecimalFormat)nf;
		df.applyPattern("###,###,###,###,###.###");
		String desc = user.getDescription().replace("\n", " ").trim();
		if(!desc.isEmpty()) {
			values.add(new BasicNameValuePair(mContext.getString(R.string.description_str), desc));
		}
		if(_user.getURL() != null) {
			values.add(new BasicNameValuePair(mContext.getString(R.string.website_str), _user.getURL().toString()));
		}
		values.add(new BasicNameValuePair(mContext.getString(R.string.tweets_str), df.format(_user.getStatusesCount())));
		if(_user.getLocation() != null && !_user.getLocation().isEmpty()) {
			values.add(new BasicNameValuePair(mContext.getString(R.string.location_str), _user.getLocation()));
		} 
		values.add(new BasicNameValuePair(mContext.getString(R.string.friends_str), df.format(_user.getFriendsCount())));
		values.add(new BasicNameValuePair(mContext.getString(R.string.followers_str), df.format(_user.getFollowersCount())));
		values.add(new BasicNameValuePair(mContext.getString(R.string.favorites_str), df.format(_user.getFavouritesCount())));
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		if(user == null) return 0;
		if(AccountService.getCurrentAccount().getUser().getId() == user.getId()) {
			return values.size();
		} else return values.size() + 1;
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
		if(position == 0 && AccountService.getCurrentAccount().getUser().getId() != user.getId()) {
			toReturn = (RelativeLayout)LayoutInflater.from(mContext).inflate(R.layout.profile_info_tab, null);
			final Button followBtn = (Button)toReturn.findViewById(R.id.profileFollowBtn);
			if(isUnknown) {
				followBtn.setText(R.string.loading_str);
				followBtn.setEnabled(false);
			} else if(isRequestSent) {
				followBtn.setText(R.string.request_sent_str);
				followBtn.setEnabled(false);
			}
			else if(isBlocked) {
				followBtn.setText(R.string.unblock_str);
				followBtn.setEnabled(true);
			} else {
				if(!isFollowing) {
					if(isFollowedBy) followBtn.setText(R.string.follow_back_str);
					else followBtn.setText(R.string.follow_str);
					followBtn.setEnabled(true);
				} else followBtn.setText(R.string.unfollow_str);
			}
			followBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) { performFollowClick(followBtn); }
			});
		} else {
			toReturn = (RelativeLayout)LayoutInflater.from(mContext).inflate(R.layout.info_list_item, null);
			BasicNameValuePair curItem = null;
			if(AccountService.getCurrentAccount().getUser().getId() == user.getId()) {
				curItem = values.get(position);
			} else curItem = values.get(position - 1);
			TextView title = (TextView)toReturn.findViewById(R.id.infoListItemTitle);
			title.setText(curItem.getName());
			FeedListAdapter.ApplyFontSize(title, mContext);
			TextView body = (TextView)toReturn.findViewById(R.id.infoListItemBody);
			if(position == 1) {
				body.setText(Utilities.twitterifyText(mContext, curItem.getValue(), null, null, true));
				body.setMovementMethod(LinkMovementMethod.getInstance());
			} else body.setText(curItem.getValue());
			FeedListAdapter.ApplyFontSize(body, mContext);
		}
		return toReturn;
	}

	private void performFollowClick(final Button btn) {
		final Twitter cl = AccountService.getCurrentAccount().getClient();
		btn.setEnabled(false);
		new Thread(new Runnable() {
			public void run() {
				if(isBlocked) {
					try { cl.destroyBlock(user.getId()); }
					catch (final TwitterException e) {
						e.printStackTrace();
						mContext.runOnUiThread(new Runnable() {
							public void run() { 
								Toast.makeText(mContext, mContext.getString(R.string.failed_unblock_str).replace("{user}", user.getScreenName()) + " " + e.getErrorMessage(), Toast.LENGTH_LONG).show();
							}
						});
						return;
					}
					mContext.runOnUiThread(new Runnable() {
						public void run() { 
							isBlocked = false;
							notifyDataSetChanged();
						}
					});
				} else if(isFollowing) {
					try { cl.destroyFriendship(user.getId()); }
					catch (final TwitterException e) {
						e.printStackTrace();
						mContext.runOnUiThread(new Runnable() {
							public void run() { 
								Toast.makeText(mContext, mContext.getString(R.string.failed_unfollow_str).replace("{user}", user.getScreenName()) + " " + e.getErrorMessage(), Toast.LENGTH_LONG).show();
							}
						});
						return;
					}
					mContext.runOnUiThread(new Runnable() {
						public void run() { 
							isFollowing = false;
							notifyDataSetChanged();
						}
					});
				} else if(!isFollowing) {
					try { cl.createFriendship(user.getId()); }
					catch (final TwitterException e) {
						e.printStackTrace();
						mContext.runOnUiThread(new Runnable() {
							public void run() { 
								Toast.makeText(mContext, mContext.getString(R.string.failed_follow_str).replace("{user}", user.getScreenName()) + " " + e.getErrorMessage(), Toast.LENGTH_LONG).show();
							}
						});
						return;
					}
					mContext.runOnUiThread(new Runnable() {
						public void run() { 
							isFollowing = true;
							notifyDataSetChanged();
						}
					});
				}
				mContext.runOnUiThread(new Runnable() {
					public void run() { btn.setEnabled(true); }
				});
			}
		}).start();
	}
}