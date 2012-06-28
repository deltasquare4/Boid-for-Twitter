package com.teamboid.twitter;

import java.util.ArrayList;

import com.teamboid.twitter.TabsAdapter.BaseGridFragment;
import com.teamboid.twitter.TabsAdapter.BaseListFragment;
import com.teamboid.twitter.TabsAdapter.MediaTimelineFragment;
import com.teamboid.twitter.TabsAdapter.ProfileAboutFragment;
import com.teamboid.twitter.TabsAdapter.ProfileTimelineFragment;
import com.teamboid.twitter.TabsAdapter.SavedSearchFragment;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

/**
 * The activity that represents the profile viewer.
 * @author Aidan Follestad
 */
public class ProfileScreen extends Activity {

	private int lastTheme;
	private boolean showProgress;
	public FeedListAdapter adapter;
	public MediaFeedListAdapter mediaAdapter;
	
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
		final String screenName = getIntent().getStringExtra("screen_name");
		setTitle("@" + screenName);
		mTabsAdapter = new TabsAdapter(this, (ViewPager)findViewById(R.id.pager));
		final ActionBar bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		boolean iconic = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("enable_iconic_tabs", true);
		if(iconic) {
			mTabsAdapter.addTab(bar.newTab().setIcon(getTheme().obtainStyledAttributes(new int[] { R.attr.timelineTab }).getDrawable(0)), ProfileTimelineFragment.class, 0, screenName);
			mTabsAdapter.addTab(bar.newTab().setIcon(getTheme().obtainStyledAttributes(new int[] { R.attr.aboutTab }).getDrawable(0)), ProfileAboutFragment.class, 1, screenName);
			mTabsAdapter.addTab(bar.newTab().setIcon(getTheme().obtainStyledAttributes(new int[] { R.attr.mediaTab }).getDrawable(0)), MediaTimelineFragment.class, 2, screenName, true);
		} else {
			mTabsAdapter.addTab(bar.newTab().setText(R.string.tweets_str), ProfileTimelineFragment.class, 0, screenName);
			mTabsAdapter.addTab(bar.newTab().setText(R.string.about_str), ProfileAboutFragment.class, 1, screenName);
			mTabsAdapter.addTab(bar.newTab().setText(R.string.media_title), MediaTimelineFragment.class, 2, screenName, true);
		}
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
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		if(AccountService.getCurrentAccount() != null && AccountService.getCurrentAccount().getUser().getScreenName().equals(getIntent().getStringExtra("screen_name"))) {
			inflater.inflate(R.menu.profile_self_actionbar, menu);
		} else inflater.inflate(R.menu.profile_actionbar, menu);
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
			startActivity(new Intent(this, TimelineScreen.class));
			finish();
			return true;
		case R.id.editAction:
			//TODO
			Toast.makeText(getApplicationContext(), "Coming soon!", Toast.LENGTH_SHORT).show();
			return true;
		case R.id.mentionAction:
			startActivity(new Intent(this, ComposerScreen.class).putExtra("append", "@" + getIntent().getStringExtra("screen_name") + " "));
			return true;
		case R.id.pinAction:		
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	    	ArrayList<String> cols = Utilities.jsonToArray(this, prefs.getString(Long.toString(AccountService.getCurrentAccount().getId()) + "_columns", ""));
			cols.add(SavedSearchFragment.ID + "@from:" + getIntent().getStringExtra("screen_name"));
			prefs.edit().putString(Long.toString(AccountService.getCurrentAccount().getId()) + "_columns", Utilities.arrayToJson(this, cols)).commit();
			startActivity(new Intent(this, TimelineScreen.class).putExtra("new_column", true));
			return true;
		case R.id.messageAction:
			startActivity(new Intent(getApplicationContext(), ConversationScreen.class).putExtra("screen_name", getIntent().getStringExtra("screen_name")));
			return true;
		case R.id.reportAction:
			//TODO
			Toast.makeText(getApplicationContext(), "Coming soon!", Toast.LENGTH_SHORT).show();
			return true;
		case R.id.refreshAction:
			for(int i = 0; i < getActionBar().getTabCount(); i++) {
				Fragment frag = getFragmentManager().findFragmentByTag("page:" + Integer.toString(i));
				if(frag != null) {
					if(frag instanceof BaseListFragment) ((BaseListFragment)frag).performRefresh(false);
					else if(frag instanceof BaseGridFragment) ((BaseGridFragment)frag).performRefresh(false); 
				}
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
}
