package com.teamboid.twitter.columns;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

import com.teamboid.twitter.Account;
import com.teamboid.twitter.ConversationScreen;
import com.teamboid.twitter.R;
import com.teamboid.twitter.TabsAdapter.BaseListFragment;
import com.teamboid.twitter.cab.MessageConvoCAB;
import com.teamboid.twitter.listadapters.MessageConvoAdapter;
import com.teamboid.twitter.listadapters.MessageConvoAdapter.DMConversation;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.notifications.NotificationService;
import com.teamboid.twitterapi.dm.DirectMessage;
import com.teamboid.twitterapi.search.Tweet;
import com.teamboid.twitterapi.status.Status;
import com.teamboid.twitterapi.user.User;

/**
 * Represents the column that displays the current user's direct messaging conversations.
 *
 * @author Aidan Follestad
 */
public class MessagesFragment extends BaseListFragment<DMConversation> {
    public static final String ID = "COLUMNTYPE:MESSAGES";

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        startActivity(new Intent(getActivity(), ConversationScreen.class)
                .putExtra("screen_name", ((DMConversation) getAdapter().getItem(position)).getToScreenName())
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    @Override
    public void onStart() {
        super.onStart();
        getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        getListView().setMultiChoiceModeListener(MessageConvoCAB.choiceListener);
        setRetainInstance(true);
        setEmptyText(getString(R.string.no_messages));
    }

    @Override
    public void performRefresh(final boolean paginate) {
        if (context == null || isLoading || adapt == null)
            return;
        isLoading = true;
        if (adapt.getCount() == 0 && getView() != null)
            setListShown(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Account acc = AccountService.getCurrentAccount();
                if (acc != null) {
                    try {
                        final ArrayList<DirectMessage> messages = new ArrayList<DirectMessage>();
                        DirectMessage[] recv = acc.getClient().getDirectMessages(null);
                        if (recv != null && recv.length > 0) {
                            for (DirectMessage msg : recv) messages.add(msg);
                        }
                        DirectMessage[] sent = acc.getClient().getSentDirectMessages(null);
                        if (sent != null && sent.length > 0) {
                            for (DirectMessage msg : sent) messages.add(msg);
                        }
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setEmptyText(context.getString(R.string.no_messages));
                                adapt.add(messages.toArray(new DirectMessage[0]));
                            }
                        });
                    } catch (final Exception e) {
                        e.printStackTrace();
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setEmptyText(context.getString(R.string.error_str));
                                showError(e.getMessage());
                            }
                        });
                    }
                }
                NotificationService.setReadDMs(acc.getId(), context);
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (getView() != null) setListShown(true);
                        isLoading = false;
                    }
                });
            }
        }).start();
    }

    @Override
    public Status[] getSelectedStatuses() { return null; }

    @Override
    public User[] getSelectedUsers() { return null; }

    @Override
    public Tweet[] getSelectedTweets() { return null; }

    @Override
    public DMConversation[] getSelectedMessages() {
        if (getAdapter() == null) return null;
        ArrayList<DMConversation> toReturn = new ArrayList<DMConversation>();
        SparseBooleanArray checkedItems = getListView().getCheckedItemPositions();
        if (checkedItems != null) {
            for (int i = 0; i < checkedItems.size(); i++) {
                if (checkedItems.valueAt(i)) {
                    toReturn.add((DMConversation) getAdapter().getItem(checkedItems.keyAt(i)));
                }
            }
        }
        return toReturn.toArray(new DMConversation[0]);
    }

	@Override
	public String getColumnName() {
		return AccountService.getCurrentAccount().getId() + ".dm_list";
	}

	@Override
	public void setupAdapter() {
		MessageConvoAdapter adapt = AccountService.getMessageConvoAdapter(getActivity(),
                AccountService.getCurrentAccount().getId());
		adapt.list = getListView();
		setListAdapter(adapt);
	}

	@Override
	public DMConversation[] fetch(long maxId, long sinceId) {
		// TODO Auto-generated method stub
		return null;
	}
}
