package com.teamboid.twitter.cab;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.teamboid.twitter.ComposerScreen;
import com.teamboid.twitter.R;
import com.teamboid.twitter.TweetListActivity;
import com.teamboid.twitter.TabsAdapter.BaseListFragment;
import com.teamboid.twitter.columns.ProfilePaddedFragment;
import com.teamboid.twitter.columns.TimelineFragment;
import com.teamboid.twitter.listadapters.FeedListAdapter;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;
import com.teamboid.twitterapi.client.Twitter;
import com.teamboid.twitterapi.status.Status;

/**
 * The contextual action bar for any lists/columns that display twitter4j.Status objects.
 *
 * @author Aidan Follestad
 */
public class TimelineCAB {

    public static Activity context;

    public static void clearSelectedItems() {
        if (context instanceof TweetListActivity) {
            ((TweetListActivity) context).getListView().clearChoices();
            ListView list = ((TweetListActivity) context).getListView();
            ((BaseAdapter) list.getAdapter()).notifyDataSetChanged();
        } else {
            for (int i = 0; i < context.getActionBar().getTabCount(); i++) {
                Fragment frag = context.getFragmentManager().findFragmentByTag("page:" + Integer.toString(i));
                if (frag instanceof BaseListFragment || frag instanceof ProfilePaddedFragment) {
                    ((BaseListFragment) frag).getListView().clearChoices();
                    ((BaseAdapter) ((BaseListFragment) frag).getListView().getAdapter()).notifyDataSetChanged();
                }
            }
        }
    }

    public static Status[] getSelectedTweets() {
        ArrayList<Status> toReturn = new ArrayList<Status>();
        if (context instanceof TweetListActivity) {
            TweetListActivity activity = (TweetListActivity) context;
            SparseBooleanArray checkedItems = activity.getListView().getCheckedItemPositions();
            if (checkedItems != null) {
                for (int i = 0; i < checkedItems.size(); i++) {
                    if (checkedItems.valueAt(i)) {
                        toReturn.add((Status) activity.binder.getItem(checkedItems.keyAt(i)));
                    }
                }
            }
        } else {
            for (int i = 0; i < context.getActionBar().getTabCount(); i++) {
                Fragment frag = context.getFragmentManager().findFragmentByTag("page:" + Integer.toString(i));
                if (frag instanceof BaseListFragment || frag instanceof ProfilePaddedFragment) {
                    Status[] toAdd = ((BaseListFragment) frag).getSelectedStatuses();
                    if (toAdd != null && toAdd.length > 0) {
                        for (Status s : toAdd) toReturn.add(s);
                    }
                }
            }
        }
        return toReturn.toArray(new Status[0]);
    }

    public static void reinsertStatus(Status status) {
        if (context instanceof TweetListActivity) {
            ListAdapter adapter = ((TweetListActivity) context).getListView().getAdapter();
            ((FeedListAdapter) adapter).update(status);
        } else {
            for (int i = 0; i < context.getActionBar().getTabCount(); i++) {
                Fragment frag = context.getFragmentManager().findFragmentByTag("page:" + Integer.toString(i));
                if (frag instanceof BaseListFragment) {
                    ListAdapter adapter = ((BaseListFragment) frag).getListView().getAdapter();
                    if (adapter instanceof FeedListAdapter) {
                        ((FeedListAdapter) adapter).update(status);
                    }
                }
            }
        }
    }

    public static void removeStatus(Status status) {
        if (context instanceof TweetListActivity) {
            ListAdapter adapter = ((TweetListActivity) context).getListView().getAdapter();
            ((FeedListAdapter) adapter).remove(status.getId());
        } else {
            for (int i = 0; i < context.getActionBar().getTabCount(); i++) {
                Fragment frag = context.getFragmentManager().findFragmentByTag("page:" + Integer.toString(i));
                if (frag instanceof BaseListFragment) {
                    ListAdapter adapter = ((BaseListFragment) frag).getListView().getAdapter();
                    if (adapter instanceof FeedListAdapter) {
                        ((FeedListAdapter) adapter).remove(status.getId());
                    }
                }
            }
        }
    }

    public static void updateTitle() {
        Status[] selTweets = TimelineCAB.getSelectedTweets();
        if (selTweets.length == 1) {
            TimelineCAB.TimelineActionMode.setTitle(R.string.one_tweet_selected);
        } else {
            TimelineCAB.TimelineActionMode.setTitle(context.getString(R.string.x_tweets_Selected).replace("{X}", Integer.toString(selTweets.length)));
        }
    }

    public static void updateMenuItems(Status[] selTweets, Menu menu) {
        if (selTweets == null) return;
        if (selTweets.length > 1) {
            boolean allFavorited = true;
            for (Status t : selTweets) {
                if (!t.isFavorited()) {
                    allFavorited = false;
                    break;
                }
            }
            boolean allMine = true;
            for (Status status : selTweets) {
                if (status.getUser().getId() != AccountService.getCurrentAccount().getId()) {
                    allMine = false;
                    break;
                }
            }
            if (allMine) {
                menu.findItem(R.id.retweetAction).setVisible(false);
                menu.findItem(R.id.deleteAction).setVisible(true);
            }
            MenuItem fav = menu.findItem(R.id.favoriteAction);
            if (allFavorited) {
                fav.setTitle(R.string.unfavorite_str);
                fav.setIcon(context.getTheme().obtainStyledAttributes(new int[]{R.attr.favoriteIcon}).getDrawable(0));
            } else fav.setTitle(R.string.favorite_str);
        } else {
            final Status status = getSelectedTweets()[0];
            if (status.getUser().getId() == AccountService.getCurrentAccount().getId()) {
                menu.findItem(R.id.retweetAction).setVisible(false);
                menu.findItem(R.id.deleteAction).setVisible(true);
            }
            MenuItem fav = menu.findItem(R.id.favoriteAction);
            if (status.isFavorited()) {
                fav.setTitle(R.string.unfavorite_str);
                fav.setIcon(context.getTheme().obtainStyledAttributes(new int[]{R.attr.favoriteIcon}).getDrawable(0));
            } else fav.setTitle(R.string.favorite_str);
        }
    }

    public static boolean performLongPressAction(ListView list, BaseAdapter adapt, int index) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("cab", true)) {
            int beforeChecked = TimelineCAB.getSelectedTweets().length;
            if (list.isItemChecked(index)) {
            	list.setItemChecked(index, false);
            } else list.setItemChecked(index, true);
            if (TimelineCAB.TimelineActionMode == null) {
                context.startActionMode(TimelineCAB.TimelineActionModeCallback);
            } else {
                final Status[] tweets = TimelineCAB.getSelectedTweets();
                if (tweets.length == 0) {
                    TimelineCAB.TimelineActionMode.finish();
                } else {
                    if (beforeChecked == 1 && tweets.length > 1) {
                        TimelineCAB.TimelineActionMode.getMenu().clear();
                        TimelineCAB.TimelineActionMode.getMenuInflater().inflate(R.menu.multi_tweet_cab, TimelineCAB.TimelineActionMode.getMenu());
                    } else if (beforeChecked > 1 && tweets.length == 1) {
                        TimelineCAB.TimelineActionMode.getMenu().clear();
                        TimelineCAB.TimelineActionMode.getMenuInflater().inflate(R.menu.single_tweet_cab, TimelineCAB.TimelineActionMode.getMenu());
                    }
                    TimelineCAB.updateTitle();
                    TimelineCAB.updateMenuItems(tweets, TimelineCAB.TimelineActionMode.getMenu());
                }
            }
        } else {
            Status item = (Status) adapt.getItem(index);
            context.startActivity(new Intent(context, ComposerScreen.class)
                    .putExtra("reply_to", item.getId())
                    .putExtra("reply_to_name", item.getUser().getScreenName())
                    .putExtra("append", Utilities.getAllMentions(item))
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
        return false;
    }

    public static ActionMode TimelineActionMode;
    public static ActionMode.Callback TimelineActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            TimelineCAB.TimelineActionMode = mode;
            MenuInflater inflater = mode.getMenuInflater();
            Status[] selTweets = TimelineCAB.getSelectedTweets();
            if (selTweets.length > 1) inflater.inflate(R.menu.multi_tweet_cab, menu);
            else inflater.inflate(R.menu.single_tweet_cab, menu);
            updateTitle();
            updateMenuItems(selTweets, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, final MenuItem item) {
            final Status[] selTweets = getSelectedTweets();
            TimelineCAB.clearSelectedItems();
            mode.finish();
            final Twitter cl = AccountService.getCurrentAccount().getClient();
            switch (item.getItemId()) {
                case R.id.replyAction:
                    context.startActivity(new Intent(context, ComposerScreen.class)
                            .putExtra("reply_to", selTweets[0].getId())
                            .putExtra("reply_to_name", selTweets[0].getUser().getScreenName())
                            .putExtra("append", Utilities.getAllMentions(selTweets[0].getUser().getScreenName(), selTweets[0].getText()))
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    return true;
                case R.id.favoriteAction:
                    for (Status t : selTweets) {
                        if (t.isRetweet()) t = t.getRetweetedStatus();
                        final Status tweet = t;
                        if (tweet.isFavorited()) {
                            new Thread(new Runnable() {
                                public void run() {
                                    try {
                                        final Status unfavorited = cl.destroyFavorite(tweet.getId());
                                        unfavorited.setFavorited(false);
                                        context.runOnUiThread(new Runnable() {
                                            public void run() {
                                                TimelineCAB.reinsertStatus(unfavorited);
                                            }
                                        });
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        context.runOnUiThread(new Runnable() {
                                            public void run() {
                                                Toast.makeText(context, context.getString(R.string.failed_unfavorite).replace("{user}", tweet.getUser().getScreenName()), Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }
                                }
                            }).start();
                        } else {
                            new Thread(new Runnable() {
                                public void run() {
                                    try {
                                        final Status favorited = cl.createFavorite(tweet.getId());
                                        favorited.setFavorited(true);
                                        context.runOnUiThread(new Runnable() {
                                            public void run() {
                                                TimelineCAB.reinsertStatus(favorited);
                                            }
                                        });
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        context.runOnUiThread(new Runnable() {
                                            public void run() {
                                                Toast.makeText(context, context.getString(R.string.failed_favorite).replace("{user}", tweet.getUser().getScreenName()), Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }
                                }
                            }).start();
                        }
                    }
                    return true;
                case R.id.retweetAction:
                    for (Status t : selTweets) {
                        if (t.isRetweet()) t = t.getRetweetedStatus();
                        final Status tweet = t;
                        new Thread(new Runnable() {
                            public void run() {
                                try {
                                    final Status result = AccountService.getCurrentAccount().getClient().retweetStatus(tweet.getId());
                                    context.runOnUiThread(new Runnable() {
                                        public void run() {
                                            AccountService.getFeedAdapter(context, TimelineFragment.ID, AccountService.getCurrentAccount().getId()).add(new Status[]{result});
                                        }
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    context.runOnUiThread(new Runnable() {
                                        public void run() {
                                            Toast.makeText(context, context.getString(R.string.failed_retweet).replace("{user}", tweet.getUser().getScreenName()), Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            }
                        }).start();
                    }
                    return true;
                case R.id.shareAction:
                    Status toShare = selTweets[0];
                    if (toShare.isRetweet()) toShare = toShare.getRetweetedStatus();
                    String text = toShare.getText() + "\n\n(via @" + toShare.getUser().getScreenName() + ", http://twitter.com/" + toShare.getUser().getScreenName() + "/status/" + Long.toString(toShare.getId()) + ")";
                    context.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text),
                            context.getString(R.string.share_str)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    return true;
                case R.id.copyAction:
                    Status toCopy = selTweets[0];
                    if (toCopy.isRetweet()) toCopy = toCopy.getRetweetedStatus();
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("Boid_Tweet", toCopy.getText()));
                    Toast.makeText(context, context.getString(R.string.copied_str).replace("{user}", toCopy.getUser().getScreenName()), Toast.LENGTH_SHORT).show();
                    return true;
                case R.id.deleteAction:
                    for (final Status t : selTweets) {
                        new Thread(new Runnable() {
                            public void run() {
                                try {
                                    Twitter cl = AccountService.getCurrentAccount().getClient();
                                    final Status deleted = cl.destroyStatus(t.getId());
                                    context.runOnUiThread(new Runnable() {
                                        public void run() {
                                            TimelineCAB.removeStatus(deleted);
                                        }
                                    });
                                } catch (final Exception e) {
                                    e.printStackTrace();
                                    context.runOnUiThread(new Runnable() {
                                        public void run() {
                                            Toast.makeText(context, R.string.failed_delete_status +
                                                    " " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            }
                        }).start();
                    }
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            TimelineCAB.clearSelectedItems();
            TimelineCAB.TimelineActionMode = null;
        }
    };
}