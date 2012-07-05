package com.teamboid.twitter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import twitter4j.User;

import java.util.ArrayList;

import com.handlerexploit.prime.widgets.RemoteImageView;

/**
 * The list adapter used in the account manager, displays the accounts currenetly added in the AccountService.
 * @author Aidan Follestad
 */
public class AccountListAdapter extends BaseAdapter {

	public AccountListAdapter(Activity context) {
		mContext = context;
		selectedItems = new ArrayList<Long>();
	}

	private Activity mContext;
	public ArrayList<Long> selectedItems;
	
	@Override
	public int getCount() { return AccountService.getAccounts().size(); }
	@Override
	public Object getItem(int position) { return AccountService.getAccounts().get(position); }
	@Override
	public long getItemId(int position) { return AccountService.getAccounts().get(position).getUser().getId(); }
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		RelativeLayout toReturn = null;
		if(convertView != null) toReturn = (RelativeLayout)convertView;
		else toReturn = (RelativeLayout)LayoutInflater.from(mContext).inflate(R.layout.account_list_item, null);
		final Account account = (Account)getItem(position);
		final User curUser = account.getUser();
		RemoteImageView profilePic = (RemoteImageView)toReturn.findViewById(R.id.accountItemProfilePic);
		profilePic.setImageResource(R.drawable.silouette);
		profilePic.setImageURL(Utilities.getUserImage(curUser.getScreenName(), mContext));
		((TextView)toReturn.findViewById(R.id.accountItemName)).setText(curUser.getName());
		if(curUser.getDescription() != null && !curUser.getDescription().trim().isEmpty()) {
			((TextView)toReturn.findViewById(R.id.accountItemDescription)).setText(Utilities.twitterifyText(
					mContext, curUser.getDescription(), null, null, false));
		} else ((TextView)toReturn.findViewById(R.id.accountItemDescription)).setText(mContext.getApplicationContext().getString(R.string.nodescription_str));
		if(selectedItems.contains(curUser.getId())) {
			toReturn.setBackgroundColor(mContext.getTheme().obtainStyledAttributes(new int[] { R.attr.selectedItemColor }).getColor(0, 0));
		}
		return toReturn;
	}
}