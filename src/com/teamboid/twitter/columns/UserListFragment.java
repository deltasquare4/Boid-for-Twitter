package com.teamboid.twitter.columns;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.teamboid.twitter.Account;
import com.teamboid.twitter.R;
import com.teamboid.twitter.TweetViewer;
import com.teamboid.twitter.TabsAdapter.BaseListFragment;
import com.teamboid.twitter.cab.TimelineCAB;
import com.teamboid.twitter.listadapters.FeedListAdapter;
import com.teamboid.twitter.listadapters.MessageConvoAdapter.DMConversation;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;
import com.teamboid.twitterapi.client.Paging;
import com.teamboid.twitterapi.search.Tweet;
import com.teamboid.twitterapi.status.Status;
import com.teamboid.twitterapi.user.User;
import com.teamboid.twitterapi.utilities.Utils;

/**
 * Represents the user list column type, displays a stream of Status objects retrieved through the user's own or subscribed lists.
 *
 * @author Aidan Follestad
 */
public class UserListFragment extends BaseListFragment {

    private FeedListAdapter adapt;
    private Activity context;
    public static final String ID = "COLUMNTYPE:USERLIST";
    private String listName;
    private int listID;

    @Override
    public void onAttach(Activity act) {
        super.onAttach(act);
        context = act;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Status tweet = (Status) adapt.getItem(position);
        if (tweet.isRetweet()) tweet = tweet.getRetweetedStatus();
        context.startActivity(new Intent(context, TweetViewer.class)
        .putExtra("sr_tweet", Utils.serializeObject(tweet))
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    @Override
    public void onStart() {
        super.onStart();
        listName = getArguments().getString("list_name");
        listID = getArguments().getInt("list_id");
        getListView().setOnScrollListener(
                new AbsListView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(AbsListView view, int scrollState) {
                    }

                    @Override
                    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                        if (totalItemCount > 0 && (firstVisibleItem + visibleItemCount) >= totalItemCount && totalItemCount > visibleItemCount) {
                            performRefresh(true);
                        }
                        if (firstVisibleItem == 0 && context.getActionBar().getTabCount() > 0) {
                            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                            if (!prefs.getBoolean("enable_iconic_tabs", true) || prefs.getBoolean("textual_userlist_tabs", true)) {
                                context.getActionBar()
                                        .getTabAt(getArguments().getInt("tab_index")).setText(listName);
                            } else {
                                context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText("");
                            }
                        }
                    }
                });
        getListView().setOnItemLongClickListener(
                new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int index, long id) {
                        TimelineCAB.performLongPressAction(getListView(), adapt, index);
                        return true;
                    }
                });
        setRetainInstance(true);
        setEmptyText(getString(R.string.no_tweets));
        reloadAdapter(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null && adapt != null)
            adapt.restoreLastViewed(getListView());
    }

    @Override
    public void onPause() {
        super.onPause();
        savePosition();
    }

    @Override
    public void performRefresh(final boolean paginate) {
        if (context == null || isLoading || adapt == null)
            return;
        isLoading = true;
        if (adapt.getCount() == 0 && getView() != null)
            setListShown(false);
        adapt.setLastViewed(getListView());
        new Thread(new Runnable() {
            @Override
            public void run() {
                Paging paging = new Paging(50);
                if (paginate) paging.setMaxId(adapt.getItemId(adapt.getCount() - 1));
                final Account acc = AccountService.getCurrentAccount();
                if (acc != null) {
                    try {
                        final Status[] feed = acc.getClient().getListTimeline(listID, paging);
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setEmptyText(context
                                        .getString(R.string.no_tweets));
                                int beforeLast = adapt.getCount() - 1;
                                int addedCount = adapt.add(feed);
                                if (addedCount > 0 || beforeLast > 0) {
                                    if (getView() != null) {
                                        if (paginate && addedCount > 0) {
                                            getListView().smoothScrollToPosition(beforeLast + 1);
                                        } else if (getView() != null && adapt != null) {
                                            adapt.restoreLastViewed(getListView());
                                        }
                                    }
                                    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                                    if (!prefs.getBoolean("enable_iconic_tabs", true) || prefs.getBoolean("textual_userlist_tabs", true)) {
                                        context.getActionBar().getTabAt(getArguments().getInt("tab_index"))
                                                .setText(listName + " (" + Integer.toString(addedCount) + ")");
                                    } else {
                                        context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(Integer.toString(addedCount));
                                    }
                                }
                            }
                        });
                    } catch (final Exception e) {
                        e.printStackTrace();
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setEmptyText(context.getString(R.string.error_str));
                                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (getView() != null)
                            setListShown(true);
                        isLoading = false;
                    }
                });
            }
        }).start();
    }

    @Override
    public void reloadAdapter(boolean firstInitialize) {
        if (context == null && getActivity() != null)
            context = getActivity();
        if (AccountService.getCurrentAccount() != null) {
            if (adapt != null && !firstInitialize && getView() != null)
                adapt.setLastViewed(getListView());
            adapt = AccountService.getFeedAdapter(
                    context,
                    UserListFragment.ID + "@"
                            + listName.replace("@", "%40") + "@"
                            + Integer.toString(listID), AccountService
                    .getCurrentAccount().getId());
            setListAdapter(adapt);
            if (adapt.getCount() == 0)
                performRefresh(false);
            else if (getView() != null && adapt != null)
                adapt.restoreLastViewed(getListView());
        }
    }

    @Override
    public void savePosition() {
        if (getView() != null && adapt != null)
            adapt.setLastViewed(getListView());
    }

    @Override
    public void restorePosition() {
        if (getView() != null && adapt != null)
            adapt.restoreLastViewed(getListView());
    }

    @Override
    public void jumpTop() {
        if (getView() != null)
            getListView().setSelectionFromTop(0, 0);
    }

    @Override
    public void filter() {
    }

    @Override
    public Status[] getSelectedStatuses() {
        if (adapt == null) return null;
        ArrayList<Status> toReturn = new ArrayList<Status>();
        SparseBooleanArray checkedItems = getListView().getCheckedItemPositions();
        if (checkedItems != null) {
            for (int i = 0; i < checkedItems.size(); i++) {
                if (checkedItems.valueAt(i)) {
                    toReturn.add((Status) adapt.getItem(checkedItems.keyAt(i)));
                }
            }
        }
        return toReturn.toArray(new Status[0]);
    }

    @Override
    public User[] getSelectedUsers() {
        return null;
    }

    @Override
    public Tweet[] getSelectedTweets() {
        return null;
    }

    @Override
    public DMConversation[] getSelectedMessages() {
        return null;
    }
}
