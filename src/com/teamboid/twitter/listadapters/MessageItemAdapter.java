package com.teamboid.twitter.listadapters;

import java.util.ArrayList;

import com.handlerexploit.prime.RemoteImageView;
import com.teamboid.twitter.Account;
import com.teamboid.twitter.ProfileScreen;
import com.teamboid.twitter.R;
import com.teamboid.twitter.listadapters.MessageConvoAdapter.DMConversation;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;

import twitter4j.DirectMessage;

import android.app.Activity;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * The list adapter used in the message conversation viewer, displays a list of messages in a conversation.
 * @author Aidan Follestad
 */
public class MessageItemAdapter extends BaseAdapter {

	public MessageItemAdapter(Activity _context) {
		context = _context;
		items = new ArrayList<DirectMessage>();
	}

	private Activity context;
	private ArrayList<DirectMessage> items;

	public void add(DirectMessage msg) {
		items.add(msg);
		notifyDataSetChanged();
	}
	public void add(DirectMessage[] msges) {
		for(DirectMessage msg : msges) add(msg);
	}
	public void setConversation(DMConversation convo) {
		clear();
		items.addAll(convo.getMessages());
		notifyDataSetChanged();
	}
	public void clear() {
		items.clear();
		notifyDataSetChanged();
	}

	@Override
	public int getCount() { return items.size(); }
	@Override
	public Object getItem(int position) { return items.get(position); }
	@Override
	public long getItemId(int position) { return items.get(position).getId(); }	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		RelativeLayout toReturn = null;
		final DirectMessage curItem = items.get(position);
		final Account acc = AccountService.getCurrentAccount();
		//Can't use convertView here, cause it's possible that this index was a sent item before and now it's a received item.
		if(curItem.getSenderId() == acc.getId()) {
			toReturn = (RelativeLayout)LayoutInflater.from(context).inflate(R.layout.dm_item_sent, null);
		} else toReturn = (RelativeLayout)LayoutInflater.from(context).inflate(R.layout.dm_item, null);		
		final RemoteImageView profileImgView = (RemoteImageView)toReturn.findViewById(R.id.dmItemProfileImg);
		if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("enable_profileimg_download", true)) {
			if(curItem.getSenderId() != acc.getId()) {
				profileImgView.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) { 
						String toOpen = curItem.getSenderScreenName();
						if(toOpen.equals(acc.getUser().getScreenName())) {
							toOpen = curItem.getRecipientScreenName();
						}
						context.startActivity(new Intent(context, ProfileScreen.class).putExtra("screen_name", toOpen).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
					}
				});
			}
			profileImgView.setImageResource(R.drawable.sillouette);
			profileImgView.setImageURL(Utilities.getUserImage(curItem.getSenderScreenName(), context));
		} else{
			profileImgView.setVisibility(View.GONE);
		}
		((TextView)toReturn.findViewById(R.id.dmItemTimeTxt)).setText(Utilities.friendlyTimeLong(context.getApplicationContext(), curItem.getCreatedAt()));
		TextView msgTxt = (TextView)toReturn.findViewById(R.id.dmItemMessageTxt); 
		FeedListAdapter.ApplyFontSize(msgTxt, context);
		msgTxt.setText(Utilities.twitterifyText(context, curItem.getText(), null, null, true));
		msgTxt.setMovementMethod(LinkMovementMethod.getInstance());
		return toReturn;
	}
}
