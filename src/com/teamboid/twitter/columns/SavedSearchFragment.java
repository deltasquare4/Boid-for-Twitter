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
import com.teamboid.twitter.ComposerScreen;
import com.teamboid.twitter.R;
import com.teamboid.twitter.TweetViewer;
import com.teamboid.twitter.TabsAdapter.BaseListFragment;
import com.teamboid.twitter.listadapters.SearchFeedListAdapter;
import com.teamboid.twitter.listadapters.MessageConvoAdapter.DMConversation;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;
import com.teamboid.twitterapi.client.Paging;
import com.teamboid.twitterapi.search.SearchQuery;
import com.teamboid.twitterapi.search.SearchResult;
import com.teamboid.twitterapi.search.Tweet;
import com.teamboid.twitterapi.status.Status;
import com.teamboid.twitterapi.user.User;

/**
 * Represents a saved search column (that goes on the TimelineScreen) that contains a static Tweet search query that's searched for whenever the column is refreshed.
 *
 * @author Aidan Follestad
 */
public class SavedSearchFragment extends BaseListFragment {

    private SearchFeedListAdapter adapt;
    private Activity context;
    public static final String ID = "COLUMNTYPE:SEARCH";
    private String query;

    @Override
    public void onAttach(Activity act) {
        super.onAttach(act);
        context = act;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Tweet tweet = (Tweet) adapt.getItem(position);
        context.startActivity(new Intent(context, TweetViewer.class)
                .putExtra("tweet_id", id)
                .putExtra("user_name", tweet.getFromUser())
                .putExtra("user_id", tweet.getFromUserId())
                .putExtra("screen_name", tweet.getFromUser())
                .putExtra("content", tweet.getText())
                .putExtra("timer", tweet.getCreatedAt().getTime())
                .putExtra("via", tweet.getSource())
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    @Override
    public void onStart() {
        super.onStart();
        query = getArguments().getString("query");
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
                            if (!prefs.getBoolean("enable_iconic_tabs", true) || prefs.getBoolean("textual_savedsearch_tabs", true)) {
                                context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(query);
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
                        Tweet item = (Tweet) adapt.getItem(index);
                        context.startActivity(new Intent(context, ComposerScreen.class)
                                .putExtra("reply_to", item.getId())
                                .putExtra("reply_to_name", item.getFromUser())
                                .putExtra("append", Utilities.getAllMentions(item))
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                        return false;
                    }
                });
        setRetainInstance(true);
        setEmptyText(getString(R.string.no_results));
        reloadAdapter(true);
    }

    @Override
    public void performRefresh(final boolean paginate) {
        if (context == null || isLoading || adapt == null) return;
        isLoading = true;
        if (adapt.getCount() == 0 && getView() != null) setListShown(false);
        adapt.setLastViewed(getListView());
        new Thread(new Runnable() {
            @Override
            public void run() {
                Paging paging = new Paging(50);
                if (paginate) paging.setMaxId(adapt.getItemId(adapt.getCount() - 1));
                SearchQuery q = SearchQuery.create(query, paging);
                final Account acc = AccountService.getCurrentAccount();
                if (acc != null) {
                    try {
                        final SearchResult feed = acc.getClient().search(q);
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setEmptyText(context.getString(R.string.no_results));
                                int beforeLast = adapt.getCount() - 1;
                                int addedCount = adapt.add(feed.getResults());
                                if (addedCount > 0 || beforeLast > 0) {
                                    if (getView() != null) {
                                        if (paginate && addedCount > 0) {
                                            getListView().smoothScrollToPosition(beforeLast + 1);
                                        } else adapt.restoreLastViewed(getListView());
                                    }
                                    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                                    if (!prefs.getBoolean("enable_iconic_tabs", true) || prefs.getBoolean("textual_savedsearch_tabs", true)) {
                                        context.getActionBar().getTabAt(getArguments().getInt("tab_index"))
                                                .setText(query + " (" + Integer.toString(addedCount) + ")");
                                    } else {
                                        context.getActionBar().getTabAt(getArguments().getInt("tab_index"))
                                                .setText(Integer.toString(addedCount));
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
                        if (getView() != null) setListShown(true);
                        isLoading = false;
                    }
                });
            }
        }).start();
    }

    @Override
    public void reloadAdapter(boolean firstInitialize) {
        if (context == null && getActivity() != null) context = getActivity();
        if (AccountService.getCurrentAccount() != null) {
            if (adapt != null && !firstInitialize && getView() != null) {
                adapt.setLastViewed(getListView());
            }
            adapt = AccountService.getSearchFeedAdapter(context, SavedSearchFragment.ID + "@" + query,
                    AccountService.getCurrentAccount().getId());
            if (getView() != null) adapt.list = getListView();
            setListAdapter(adapt);
            if (adapt.getCount() == 0) performRefresh(false);
            else adapt.restoreLastViewed(getListView());
        }
    }

    @Override
    public void savePosition() {
        if (getView() != null && adapt != null) adapt.setLastViewed(getListView());
    }

    @Override
    public void restorePosition() {
        if (getView() != null && adapt != null) adapt.restoreLastViewed(getListView());
    }

    @Override
    public void jumpTop() {
        if (getView() != null) getListView().setSelectionFromTop(0, 0);
    }

    @Override
    public void filter() {
    }

    @Override
    public Status[] getSelectedStatuses() {
        return null;
    }

    @Override
    public User[] getSelectedUsers() {
        return null;
    }

    @Override
    public Tweet[] getSelectedTweets() {
        if (adapt == null) return null;
        ArrayList<Tweet> toReturn = new ArrayList<Tweet>();
        SparseBooleanArray checkedItems = getListView().getCheckedItemPositions();
        if (checkedItems != null) {
            for (int i = 0; i < checkedItems.size(); i++) {
                if (checkedItems.valueAt(i)) {
                    toReturn.add((Tweet) adapt.getItem(checkedItems.keyAt(i)));
                }
            }
        }
        return toReturn.toArray(new Tweet[0]);
    }

    @Override
    public DMConversation[] getSelectedMessages() {
        return null;
    }
}
