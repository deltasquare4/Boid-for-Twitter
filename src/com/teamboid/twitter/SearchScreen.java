package com.teamboid.twitter;

import java.util.ArrayList;

import com.teamboid.twitter.TabsAdapter.BaseListFragment;
import com.teamboid.twitter.cab.UserListCAB;
import com.teamboid.twitter.columns.SavedSearchFragment;
import com.teamboid.twitter.columns.SearchTweetsFragment;
import com.teamboid.twitter.columns.SearchUsersFragment;
import com.teamboid.twitter.listadapters.SearchFeedListAdapter;
import com.teamboid.twitter.listadapters.SearchUsersListAdapter;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;

/**
 * The activity that represents the search screen.
 * @author Aidan Follestad
 */
public class SearchScreen extends Activity {

	private int lastTheme;
	private boolean showProgress;
	public SearchFeedListAdapter tweetAdapter;
	public SearchUsersListAdapter userAdapter;

	public void showProgress(boolean visible) {
		if(showProgress == visible) return;
		showProgress = visible;
		setProgressBarIndeterminateVisibility(visible);
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
    	if(savedInstanceState != null) {
	    	if(savedInstanceState.containsKey("lastTheme")) {
	    		lastTheme = savedInstanceState.getInt("lastTheme");
	    		setTheme(lastTheme);
	    	} else setTheme(Utilities.getTheme(getApplicationContext()));
	    	if(savedInstanceState.containsKey("showProgress")) showProgress(true);
	    }  else setTheme(Utilities.getTheme(getApplicationContext()));
    	requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.main);
        setProgressBarIndeterminateVisibility(false);
        initializeTabs(savedInstanceState);
    }
		
	private TabsAdapter mTabsAdapter;
	private void initializeTabs(Bundle savedInstanceState) {
		final String query = getIntent().getStringExtra("query");
		SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                SearchSuggestionsProvider.AUTHORITY, SearchSuggestionsProvider.MODE);
        suggestions.saveRecentQuery(query, null);
		setTitle(query);
		if(mTabsAdapter == null) mTabsAdapter = new TabsAdapter(this, (ViewPager)findViewById(R.id.pager));
		else mTabsAdapter.clear();
		final ActionBar bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		mTabsAdapter.addTab(bar.newTab().setText(getString(R.string.tweets_str)), SearchTweetsFragment.class, 0, query);
		mTabsAdapter.addTab(bar.newTab().setText(getString(R.string.users_str)), SearchUsersFragment.class, 1, query);
		if(query.startsWith("@") && !query.trim().contains(" ")) getActionBar().setSelectedNavigationItem(1);
		if(savedInstanceState != null) getActionBar().setSelectedNavigationItem(savedInstanceState.getInt("lastTab", 0));
	}

	@Override
	public void onResume() {
		super.onResume();
		if(lastTheme == 0) lastTheme = Utilities.getTheme(getApplicationContext());
		else if(lastTheme != Utilities.getTheme(getApplicationContext())) {
			lastTheme = Utilities.getTheme(getApplicationContext());
			recreate();
		}
		UserListCAB.context = this;
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.search_actionbar, menu);
		if(showProgress) {
			final MenuItem refreshAction = menu.findItem(R.id.refreshAction);
			refreshAction.setEnabled(false);
		}
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			//startActivity(new Intent(this, TimelineScreen.class));
			//finish();
			
			super.onBackPressed(); //Back button should go back, not restart a new activity
			
			return true;
		case R.id.newTweetAction:
			startActivity(new Intent(this, ComposerScreen.class).putExtra("append", getIntent().getStringExtra("query")));
			return true;
		case R.id.pinAction:		
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	    	ArrayList<String> cols = Utilities.jsonToArray(this, prefs.getString(Long.toString(AccountService.getCurrentAccount().getId()) + "_columns", ""));
			cols.add(SavedSearchFragment.ID + "@" + getIntent().getStringExtra("query"));
			prefs.edit().putString(Long.toString(AccountService.getCurrentAccount().getId()) + "_columns", Utilities.arrayToJson(this, cols)).commit();
			startActivity(new Intent(this, TimelineScreen.class).putExtra("new_column", true));
			return true;
		case R.id.refreshAction:
			for(int i = 0; i < getActionBar().getTabCount(); i++) {
				Fragment frag = getFragmentManager().findFragmentByTag("page:" + Integer.toString(i));
				if(frag != null) ((BaseListFragment)frag).performRefresh(false);
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt("lastTheme", lastTheme);
		outState.putInt("lastTab", getActionBar().getSelectedNavigationIndex());
		if(showProgress) {
    		showProgress(false);
    		outState.putBoolean("showProgress", true);
    	}
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		tweetAdapter.clear();
		userAdapter.clear();
	}
}
