package com.teamboid.twitter;

import java.util.ArrayList;

import com.handlerexploit.prime.widgets.RemoteImageView;

import android.app.Activity;
import twitter4j.DirectMessage;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * The list adapter used for the messages tab, displays a list of conversations that contain messages.
 * @author Aidan Follestad
 */
public class MessageConvoAdapter extends BaseAdapter {

	public static class DMConversation {

		public DMConversation(Long toId, String toName, String toScreenName, DirectMessage initMsg) {
			_toId = toId;
			_toName = toName;
			_toScreen = toScreenName;
			messages = new ArrayList<DirectMessage>();
			messages.add(initMsg);
		}

		private long _toId;
		private String _toName;
		private String _toScreen;
		private ArrayList<DirectMessage> messages;

		public long getToId() { return _toId; }
		public String getToName() { return _toName; }
		public String getToScreenName() { return _toScreen; }
		public DirectMessage getLastMessage() {
			if(messages.size() == 0) return null;
			return messages.get(messages.size() - 1);
		}
		public long getLastSender() {
			if(getLastMessage() == null) return 0;
			return getLastMessage().getSenderId();
		}
		public boolean getLastSenderIsMe() { return (getLastSender() == AccountService.getCurrentAccount().getId()); }
		public ArrayList<DirectMessage> getMessages() { return messages; }

		public void add(DirectMessage msg) {
			if(!contains(msg)) messages.add(findAppropIndex(msg), msg);
		}
		private boolean contains(DirectMessage toFind) {
			Boolean found = false;
			ArrayList<DirectMessage> itemCache = messages;
			for(DirectMessage msg : itemCache) {
				if(msg.getId() == toFind.getId()) {
					found = true;
					break;
				}
			}
			return found;
		}
		private int findAppropIndex(DirectMessage msg) {
			int toReturn = 0;
			ArrayList<DirectMessage> itemCache = messages;
			for(DirectMessage t : itemCache) {
				if(t.getCreatedAt().after(msg.getCreatedAt())) break;
				toReturn++;
			}
			return toReturn;
		}
	}

	public MessageConvoAdapter(Activity _context) {
		context = _context;
		items = new ArrayList<DMConversation>();
	}

	private Activity context;
	private ArrayList<DMConversation> items;
	public ListView list;
	
	public void add(DMConversation[] convos) {
		for(DMConversation c : convos) items.add(c);
		notifyDataSetChanged();
	}
	public int add(final DirectMessage[] msges) {
		int before = items.size();
		int added = 0;
		for(DirectMessage msg : msges) {
			boolean foundConvo = false;
			for(DMConversation convo : items) {
					if(msg.getSenderId() == AccountService.getCurrentAccount().getId()) {
						if(msg.getRecipientId() == convo.getToId()) foundConvo = true;
					} else if(msg.getSenderId() == convo.getToId()) foundConvo = true;
					if(foundConvo) {
						added++;
						convo.add(msg);
						break;
					}
			}
			if(!foundConvo) {
				long toId = msg.getSenderId();
				String toName = msg.getSender().getName();
				String toScreen = msg.getSender().getScreenName();
				if(toId == AccountService.getCurrentAccount().getId()) {
					toId = msg.getRecipientId();
					toName = msg.getRecipient().getName();
					toScreen = msg.getRecipient().getScreenName();
				}
				items.add(new DMConversation(toId, toName, toScreen, msg));
			}
			notifyDataSetChanged();
		}
		if(before == 0) return added;
		else if(added == before) return 0;
		else return (items.size() - before);
	}
	public DMConversation find(String screenName) {
		DMConversation toReturn = null;
		for(DMConversation convo : items) {
			if(convo.getToScreenName().equals(screenName)) {
				toReturn = convo;
				break;
			}
		}
		return toReturn;
	}
	public void remove(int index) {
		items.remove(index);
		notifyDataSetChanged();
	}
	public void clear() {
		items.clear();
		notifyDataSetChanged();
	}
	public DMConversation[] toArray() { return items.toArray(new DMConversation[0]); }
	
	@Override
	public int getCount() { return items.size(); }
	@Override
	public Object getItem(int position) { return items.get(position); }
	@Override
	public long getItemId(int position) { return items.get(position).getToId();  }
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		RelativeLayout toReturn = null;
		if(convertView != null) toReturn = (RelativeLayout)convertView;
		else toReturn = (RelativeLayout)LayoutInflater.from(context).inflate(R.layout.dm_convo_item, null);
		final DMConversation curItem = items.get(position);
		RemoteImageView profileImgView = (RemoteImageView)toReturn.findViewById(R.id.dmConvoProfileImg);
		profileImgView.setImageURL("https://api.twitter.com/1/users/profile_image?screen_name=" + curItem.getToScreenName() + "&size=bigger");
		profileImgView.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) { 
				context.startActivity(new Intent(context, ProfileScreen.class).putExtra("screen_name", curItem.getToScreenName())
						.putExtra("account", AccountService.getCurrentAccount().getId()).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
			}
		});
		final TextView messageTxt = (TextView)toReturn.findViewById(R.id.dmConvoMessageTxt); 
		if(curItem.getLastSenderIsMe()) {
			((ImageView)toReturn.findViewById(R.id.dmConvoReplyIndicator)).setVisibility(View.VISIBLE);
		} else {
			((ImageView)toReturn.findViewById(R.id.dmConvoReplyIndicator)).setVisibility(View.GONE);
			RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)messageTxt.getLayoutParams();
			layoutParams.addRule(RelativeLayout.RIGHT_OF, R.id.dmConvoProfileImg);
			layoutParams.addRule(RelativeLayout.BELOW, R.id.dmConvoScreenNameTxt);
			messageTxt.setLayoutParams(layoutParams);
		}
		((TextView)toReturn.findViewById(R.id.dmConvoScreenNameTxt)).setText(curItem.getToName());
		messageTxt.setText(Utilities.twitterifyText(context, curItem.getLastMessage().getText().replace("\n", " ").trim(), null, null, true));
		return toReturn;
	}
}