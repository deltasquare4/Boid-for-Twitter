package com.teamboid.twitter.cab;

import java.util.ArrayList;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.Toast;

import com.teamboid.twitter.ConversationScreen;
import com.teamboid.twitter.R;
import com.teamboid.twitter.listadapters.MessageConvoAdapter;
import com.teamboid.twitter.services.AccountService;

import com.teamboid.twitterapi.dm.DirectMessage;

/**
 * The contextual action bar for the direct message conversation viewer.
 *
 * @author Aidan Follestad
 */
public class MessageItemCAB {

	public static Activity context;

	public static DirectMessage[] getSelectedMessages() {
		ArrayList<DirectMessage> toReturn = new ArrayList<DirectMessage>();
		if(context instanceof ConversationScreen) {
			ConversationScreen activity = (ConversationScreen)context;
			SparseBooleanArray checkedItems = activity.getListView().getCheckedItemPositions();
			if (checkedItems != null) {
				for (int i = 0; i < checkedItems.size(); i++) {
					if (checkedItems.valueAt(i)) {
						DirectMessage toAdd = (DirectMessage)activity.adapt.getItem(checkedItems.keyAt(i));
						toReturn.add(toAdd);
					}
				}
			}
		} else return null;
		return toReturn.toArray(new DirectMessage[0]);
	}


	public static final AbsListView.MultiChoiceModeListener choiceListener = new AbsListView.MultiChoiceModeListener() {

		private void updateTitle(int selectedConvoLength) {
			if (selectedConvoLength == 1) {
				actionMode.setTitle(R.string.one_msg_selected);
			} else {
				actionMode.setTitle(context.getString(R.string.x_msg_selected).replace("{X}", Integer.toString(selectedConvoLength)));
			}
		}

		private ActionMode actionMode;

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }

		@Override
		public void onDestroyActionMode(ActionMode mode) { }

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.dm_cab, menu);
			actionMode = mode;
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			final DirectMessage[] selMessages = getSelectedMessages();
			mode.finish();

			switch (item.getItemId()) {
			case R.id.copyAction: {
				String toSet = "";
				int index = 0;
				for (final DirectMessage msg : selMessages) {
					if(index > 0) toSet += "\n";
					toSet += msg;
					index++;
				}
				ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
				clipboard.setPrimaryClip(ClipData.newPlainText("Boid_DM", toSet));
				Toast.makeText(context, context.getString(R.string.clipboard_str), Toast.LENGTH_SHORT).show();
			}
			case R.id.deleteAction: {
				new Thread(new Runnable() {
					public void run() {
						final MessageConvoAdapter adapt = AccountService.getMessageConvoAdapter(context, AccountService.getCurrentAccount().getId());
						for (final DirectMessage msg : selMessages) {
							try {
								AccountService.getCurrentAccount().getClient().destroyDirectMessage(msg.getId());
								context.runOnUiThread(new Runnable() {
									@Override
									public void run() { 
										adapt.find(((ConversationScreen)context).toScreenName).remove(msg.getId());
									}
								});
							} catch (final Exception e) {
								e.printStackTrace();
								context.runOnUiThread(new Runnable() {
									@Override
									public void run() {
										Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
									}
								});
							}
							context.runOnUiThread(new Runnable() {
								public void run() {
									adapt.add(new DirectMessage[] { msg });
								}
							});
						}
					}
				}).start();
				return true;
			}
			default: {
				return false;
			}
			}
		}

		@Override
		public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
			DirectMessage[] selConvos = getSelectedMessages();
			updateTitle(selConvos.length);
		}
	};
}
