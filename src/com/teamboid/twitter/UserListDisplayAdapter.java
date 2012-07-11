package com.teamboid.twitter;

import java.util.ArrayList;

import twitter4j.UserList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class UserListDisplayAdapter extends BaseAdapter {

	public UserListDisplayAdapter(Context context) {
		mContext = context;
		lists = new ArrayList<UserList>();
	}
	
	private Context mContext;
	private ArrayList<UserList> lists;
	
	public void add(UserList list) {
		lists.add(list);
		notifyDataSetChanged();
	}
	public void add(UserList[] lists) {
		for(UserList l : lists) add(l);
	}
	public void remove(int index) {
		lists.remove(index);
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

	public UserList[] toArray() { return lists.toArray(new UserList[0]); }
}
