package com.teamboid.twitter;

import java.util.ArrayList;

import twitter4j.TwitterException;
import twitter4j.UserList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class UserListDisplayAdapter extends BaseAdapter {

	public UserListDisplayAdapter(Activity context) {
		mContext = context;
		lists = new ArrayList<UserList>();
	}

	private Activity mContext;
	private ArrayList<UserList> lists;

	public void add(UserList list) {
		if(!contains(list)) lists.add(list);
		notifyDataSetChanged();
	}
	public void add(UserList[] lists) {
		for(UserList l : lists) add(l);
	}
	public boolean contains(UserList list)  {
		for(int i = 0; i < lists.size(); i++) {
			if(lists.get(i).getId() == list.getId()) return true;
		}
		return false;
	}
	public void remove(int id) {
		for(int i = 0; i < lists.size(); i++) {
			if(lists.get(i).getId() == id) {
				lists.remove(i);
				break;
			}
		}
		notifyDataSetChanged();
	}
	public void clear() {
		lists.clear();
		notifyDataSetChanged();
	}

	@Override
	public int getCount() { return lists.size(); }

	@Override
	public Object getItem(int position) { return lists.get(position); }

	public int getListId(int position) { return lists.get(position).getId(); }

	@Override
	public long getItemId(int position) {
		return lists.get(position).getId();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView toReturn = null;
		if(convertView != null) toReturn = (TextView)convertView;
		else toReturn = (TextView)LayoutInflater.from(mContext).inflate(R.layout.trends_list_item, null);
		toReturn.setText(lists.get(position).getFullName());
		return toReturn;
	}

	public void destroyOrUnsubscribe(final int pos) {
		final int id = lists.get(pos).getId();
		if(lists.get(pos).getUser().getId() == AccountService.getCurrentAccount().getId()) {
			final Toast toast = Toast.makeText(mContext, R.string.deleting_list_str, Toast.LENGTH_LONG);
			toast.show();
			new Thread(new Runnable() {
				public void run() {
					try { AccountService.getCurrentAccount().getClient().destroyUserList(getListId(pos)); }
					catch(final TwitterException e) {
						e.printStackTrace();
						mContext.runOnUiThread(new Runnable() {
							public void run() {
								toast.cancel();
								Toast.makeText(mContext, e.getErrorMessage(), Toast.LENGTH_LONG).show();
							}
						});
						return;
					}
					mContext.runOnUiThread(new Runnable() {
						public void run() { 
							toast.cancel();
							Toast.makeText(mContext, R.string.deleted_list_str, Toast.LENGTH_SHORT).show();
							remove(id);
						}
					});
				}
			}).start();
		} else {
			final Toast toast = Toast.makeText(mContext, R.string.unsubscribing_list_str, Toast.LENGTH_LONG);
			toast.show();
			new Thread(new Runnable() {
				public void run() {
					try { AccountService.getCurrentAccount().getClient().destroyUserListSubscription(getListId(pos)); }
					catch(final TwitterException e) {
						e.printStackTrace();
						mContext.runOnUiThread(new Runnable() {
							public void run() {
								toast.cancel();
								Toast.makeText(mContext, e.getErrorMessage(), Toast.LENGTH_LONG).show();
							}
						});
						return;
					}
					mContext.runOnUiThread(new Runnable() {
						public void run() { 
							toast.cancel();
							Toast.makeText(mContext, R.string.unsubscribed_list_str, Toast.LENGTH_SHORT).show();
							remove(id);
						}
					});
				}
			}).start();
		}
	}

	public UserList[] toArray() { return lists.toArray(new UserList[0]); }
}
