package com.teamboid.twitter.cab;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Fragment;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.Toast;

import com.teamboid.twitter.R;
import com.teamboid.twitter.TabsAdapter.BaseListFragment;
import com.teamboid.twitter.listadapters.MessageConvoAdapter.DMConversation;
import com.teamboid.twitter.services.AccountService;

import com.teamboid.twitterapi.dm.DirectMessage;

/**
 * The contextual action bar for any lists/columns that display twitter4j.User objects.
 *
 * @author Aidan Follestad
 */
public class MessageConvoCAB {

    public static Activity context;

    public static DMConversation[] getSelectedConvos() {
        ArrayList<DMConversation> toReturn = new ArrayList<DMConversation>();
        for (int i = 0; i < context.getActionBar().getTabCount(); i++) {
            Fragment frag = context.getFragmentManager().findFragmentByTag("page:" + Integer.toString(i));
            if (frag instanceof BaseListFragment) {
                DMConversation[] toAdd = ((BaseListFragment) frag).getSelectedMessages();
                if (toAdd != null && toAdd.length > 0) {
                    for (DMConversation u : toAdd) toReturn.add(u);
                }
            }
        }
        return toReturn.toArray(new DMConversation[0]);
    }


    public static final AbsListView.MultiChoiceModeListener choiceListener = new AbsListView.MultiChoiceModeListener() {

        private void updateTitle(int selectedConvoLength) {
            if (selectedConvoLength == 1) {
                actionMode.setTitle(R.string.one_convo_selected);
            } else {
                actionMode.setTitle(context.getString(R.string.x_convos_selected).replace("{X}", Integer.toString(selectedConvoLength)));
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
            inflater.inflate(R.menu.convo_cab, menu);
            actionMode = mode;
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            final DMConversation[] selConvos = getSelectedConvos();
            mode.finish();

            switch (item.getItemId()) {
                case R.id.deleteAction: {
                    for (final DMConversation convo : selConvos) {
                        new Thread(new Runnable() {
                            public void run() {
                                for (DirectMessage msg : convo.getMessages()) {
                                    try {
                                        AccountService.getCurrentAccount().getClient().destroyDirectMessage(msg.getId());
                                    } catch (final Exception e) {
                                        e.printStackTrace();
                                        context.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(context, context.getString(R.string.failed_delete_dm)
                                                        .replace("{user}", convo.getToScreenName()) + " " +
                                                        e.getMessage(), Toast.LENGTH_LONG).show();
                                                AccountService.getMessageConvoAdapter(context, AccountService.getCurrentAccount().getId()).add(new DMConversation[]{convo});
                                            }
                                        });
                                    }
                                }
                            }
                        }).start();
                        context.runOnUiThread(new Runnable() {
                            public void run() {
                                AccountService.getMessageConvoAdapter(context, AccountService.getCurrentAccount().getId()).remove(convo);
                            }
                        });
                    }
                    return true;
                }
                default: {
                    return false;
                }
            }
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            DMConversation[] selConvos = getSelectedConvos();
            updateTitle(selConvos.length);
        }
    };
}
