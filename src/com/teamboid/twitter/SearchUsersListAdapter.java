package com.teamboid.twitter;

import java.util.ArrayList;

import com.handlerexploit.prime.widgets.RemoteImageView;

import twitter4j.Tweet;
import twitter4j.User;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * The list adapter used for activities that search for users.
 * @author Aidan Follestad
 */
public class SearchUsersListAdapter extends BaseAdapter {

	public SearchUsersListAdapter(Context context) {
		mContext = context;
		users = new ArrayList<User>();
	}

	private Context mContext;
	private ArrayList<User> users;
	public ListView list;
	public int savedIndex;
	public int savedIndexTop;
	
	public boolean add(User tweet) {
		boolean added = false;
		if(!update(tweet)) {
			users.add(tweet);
			added = true;
		}
		notifyDataSetChanged();
		return added;
	}
	public int add(User[] toAdd) {
		int before = users.size();
		int added = 0;
		for(User user : toAdd) {
			if(add(user)) added++;
		}
		if(before == 0) return 0;
		else if(added == before) return 0;
		else return (users.size() - before);
	}
	public void remove(int index) {
		users.remove(index);
		notifyDataSetChanged();
	}
	public void clear() {
		users.clear();
		notifyDataSetChanged();
	}

	public Tweet[] toArray() { return users.toArray(new Tweet[0]); }
	
	public Boolean update(User toFind) {
		Boolean found = false;
		for(int i = 0; i < users.size(); i++) {
			if(users.get(i).getId() == toFind.getId()) {
				found = true;
				users.set(i, toFind);
				break;
			}
		}
		return found;
	}

	@Override
	public int getCount() { return users.size(); }
	@Override
	public Object getItem(int position) {
		return users.get(position);
	}
	@Override
	public long getItemId(int position) {
		return users.get(position).getId();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		RelativeLayout toReturn = null;
		if(convertView != null) toReturn = (RelativeLayout)convertView;
		else toReturn = (RelativeLayout)LayoutInflater.from(mContext).inflate(R.layout.user_list_item, null);
		final User user = (User)getItem(position);	
		RemoteImageView remoteImageView = (RemoteImageView)toReturn.findViewById(R.id.userItemProfilePic);
		remoteImageView.setImageURL(user.getProfileImageURL().toString());
		((TextView)toReturn.findViewById(R.id.userItemName)).setText(user.getName());
		if(user.getDescription() != null && !user.getDescription().trim().isEmpty()) {
			((TextView)toReturn.findViewById(R.id.userItemDescription)).setText(
					Utilities.twitterifyText(mContext, user.getDescription().replace("\n", " ").trim(), null, null, false));
		} else ((TextView)toReturn.findViewById(R.id.userItemDescription)).setText(mContext.getApplicationContext().getString(R.string.nodescription_str)); 
		if(user.isVerified()) ((ImageView)toReturn.findViewById(R.id.userItemVerified)).setVisibility(View.VISIBLE);
		else ((ImageView)toReturn.findViewById(R.id.userItemVerified)).setVisibility(View.GONE);
		return toReturn;
	}
}