package com.teamboid.twitter;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import org.apache.http.message.BasicNameValuePair;

import twitter4j.User;

import android.app.Activity;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

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

	public void setUser(User _user) {
		user = _user;
		values.clear();
		NumberFormat nf = NumberFormat.getNumberInstance();
		DecimalFormat df = (DecimalFormat)nf;
		df.applyPattern("###,###,###,###,###.###");
		values.add(new BasicNameValuePair(mContext.getString(R.string.description_str), user.getDescription().replace("\n", " ").trim()));
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
		if(convertView == null) toReturn = (RelativeLayout)LayoutInflater.from(mContext).inflate(R.layout.info_list_item, null);
		else toReturn = (RelativeLayout)convertView;
		BasicNameValuePair curItem = values.get(position);
		TextView title = (TextView)toReturn.findViewById(R.id.infoListItemTitle);
		title.setText(curItem.getName());
		FeedListAdapter.ApplyFontSize(title, mContext);
		TextView body = (TextView)toReturn.findViewById(R.id.infoListItemBody);
		if(position == 0) {
			body.setText(Utilities.twitterifyText(mContext, curItem.getValue(), null, null, true));
			body.setMovementMethod(LinkMovementMethod.getInstance());
		} else body.setText(curItem.getValue());
		FeedListAdapter.ApplyFontSize(body, mContext);
		return toReturn;
	}
}
