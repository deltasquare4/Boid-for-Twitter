package com.teamboid.twitter;

import java.util.ArrayList;

import twitter4j.ResponseList;
import twitter4j.SavedSearch;
import twitter4j.TwitterException;
import twitter4j.UserList;

import com.handlerexploit.prime.utils.ImageManager;
import com.handlerexploit.prime.utils.ImageManager.OnImageReceivedListener;
import com.teamboid.twitter.SendTweetTask.Result;
import com.teamboid.twitter.TabsAdapter.BaseGridFragment;
import com.teamboid.twitter.TabsAdapter.BaseListFragment;
import com.teamboid.twitter.TabsAdapter.BaseSpinnerFragment;
import com.teamboid.twitter.TabsAdapter.FavoritesFragment;
import com.teamboid.twitter.TabsAdapter.MediaTimelineFragment;
import com.teamboid.twitter.TabsAdapter.MentionsFragment;
import com.teamboid.twitter.TabsAdapter.MessagesFragment;
import com.teamboid.twitter.TabsAdapter.NearbyFragment;
import com.teamboid.twitter.TabsAdapter.SavedSearchFragment;
import com.teamboid.twitter.TabsAdapter.TimelineFragment;
import com.teamboid.twitter.TabsAdapter.TrendsFragment;
import com.teamboid.twitter.TabsAdapter.UserListFragment;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The activity that represents the main timeline screen.
 * @author Aidan Follestad
 */
public class TimelineScreen extends Activity {

	private int lastTheme;
	private boolean lastDisplayReal;
	private boolean lastIconic;
	private TabsAdapter mTabsAdapter;
	private boolean showProgress;
	private boolean newColumn;

	private SendTweetArrayAdapter sentTweetBinder;

	public class SendTweetUpdater extends BroadcastReceiver{
		@Override
		public void onReceive(Context arg0, Intent intent) {
			if(intent.getAction().equals(AccountManager.END_LOAD)) {
				loadColumns(intent.getBooleanExtra("last_account_count", false), false);
				accountsLoaded();
				return;
			}
			try{
				sentTweetBinder = new SendTweetArrayAdapter(TimelineScreen.this, 0, 0, SendTweetService.getInstance().tweets);
				((ListView)findViewById(R.id.progress_content)).setAdapter(sentTweetBinder);
				
				if(SendTweetService.getInstance().tweets != null){
					sentTweetBinder.notifyDataSetInvalidated();
					if(SendTweetService.getInstance().tweets.size() > 0){
						findViewById(R.id.progress).setVisibility(View.VISIBLE);
						findViewById(R.id.progress).setAlpha(1.0f);
						((SlidingDrawer)findViewById(R.id.progress)).close();
					} else{
						if(intent.hasExtra("delete") || intent.hasExtra("dontrefresh")) {
							findViewById(R.id.progress).setVisibility(View.GONE);
						} else {
							//TODO This is NOT supported on Gingerbread. If we wanted to in the future, use NineOldDrroids
							ViewPropertyAnimator vpa = findViewById(R.id.progress).animate();
							vpa.setStartDelay(300);
							vpa.setDuration(3000);
							vpa.setListener(new AnimatorListener(){
								@Override
								public void onAnimationCancel(Animator arg0) {}

								@Override
								public void onAnimationEnd(Animator arg0) {
									findViewById(R.id.progress).setVisibility(View.GONE);
									findViewById(R.id.progress_handle).setBackgroundColor(getResources().getColor(android.R.color.background_dark));
									performRefresh();
								}

								@Override
								public void onAnimationRepeat(Animator arg0) {}
								@Override
								public void onAnimationStart(Animator arg0) {
									TextView tv = (TextView)findViewById(R.id.progress_handle);
									tv.setText(R.string.sent_tweet);
									tv.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
								}

							});
							vpa.alpha(0);
						}
						return;
					}
					boolean errors = false;
					for(SendTweetTask stt : SendTweetService.getInstance().tweets){
						if(stt.result.errorCode != Result.WAITING && stt.result.sent == false){
							errors = true;
						}
					}
					TextView tv = (TextView)findViewById(R.id.progress_handle);
					if(errors == true){
						tv.setText(getString(R.string.send_error_tweets).replace("{sending}", SendTweetService.getInstance().tweets.size() + ""));
					} else{
						if(SendTweetService.getInstance().tweets.size() == 1){
							tv.setText(R.string.sending_tweet);
						} else{
							tv.setText(getString(R.string.sending_tweets).replace("{sending}", SendTweetService.getInstance().tweets.size() + ""));
						}
					}
				}
			} catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	SendTweetUpdater receiver = new SendTweetUpdater();

	private void initialize(Bundle savedInstanceState) {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());		
		if(!prefs.contains("enable_profileimg_download")) prefs.edit().putBoolean("enable_profileimg_download", true).commit();
		if(!prefs.contains("enable_media_download")) prefs.edit().putBoolean("enable_media_download", true).commit();
		if(!prefs.contains("enable_drafts")) prefs.edit().putBoolean("enable_drafts", true).commit();
		if(!prefs.contains("enable_iconic_tabs")) prefs.edit().putBoolean("enable_iconic_tabs", true).commit();
		if(!prefs.contains("textual_userlist_tabs")) prefs.edit().putBoolean("textual_userlist_tabs", true).commit();
		if(!prefs.contains("textual_savedsearch_tabs")) prefs.edit().putBoolean("textual_savedsearch_tabs", true).commit();
		if(!prefs.contains("boid_theme")) prefs.edit().putString("boid_theme", "0").commit();
		if(!prefs.contains("upload_service")) prefs.edit().putString("upload_service", "twitter").commit();
		if(!prefs.contains("enable_inline_previewing")) prefs.edit().putBoolean("enable_inline_previewing", true).commit();
		ActionBar ab = getActionBar();
		ab.setDisplayShowTitleEnabled(false);
		ab.setDisplayShowHomeEnabled(false);
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		startService(new Intent(this, AccountService.class));
	}

	@Override
	protected void onNewIntent (Intent intent){
		if(AccountService.getAccounts().size() > 0) {
			setIntent(intent);
			accountsLoaded();
		}
		if(intent.getExtras() != null && intent.getExtras().containsKey("new_column")) {
			newColumn = true;
			recreate();
		}
	}

	public void loadColumns(boolean firstLoad, boolean accountSwitched) {
		if(AccountService.getAccounts().size() == 0) {
			return;
		} else {
			long lastSel = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getLong("last_sel_account", 0l);
			if(!AccountService.existsAccount(lastSel)) {
				lastSel = AccountService.getAccounts().get(0).getId();
				PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putLong("last_sel_account", lastSel).commit();
			}
			if(AccountService.getCurrentAccount() == null || (firstLoad && AccountService.selectedAccount != lastSel)) {
				AccountService.selectedAccount = lastSel;
			}
		}
		if(mTabsAdapter == null) {
			mTabsAdapter = new TabsAdapter(this, (ViewPager)findViewById(R.id.pager));
			mTabsAdapter.filterDefaultColumnSelection = true;
		} else {
			mTabsAdapter.filterDefaultColumnSelection = true;
			mTabsAdapter.clear();
		}
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		ArrayList<String> cols = Utilities.jsonToArray(this, prefs.getString(Long.toString(AccountService.getCurrentAccount().getId()) + "_columns", ""));
		if(cols.size() == 0) {
			cols.add(TimelineFragment.ID);
			cols.add(MentionsFragment.ID);
			cols.add(MessagesFragment.ID);
			cols.add(TrendsFragment.ID);
			prefs.edit().putString(Long.toString(AccountService.getCurrentAccount().getId()) + "_columns", Utilities.arrayToJson(this, cols)).commit();
		}
		int index = 0;
		boolean iconic = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("enable_iconic_tabs", true);
		for(String c : cols) {
			if(c.equals(TimelineFragment.ID)) {
				Tab toAdd = getActionBar().newTab();
				if(iconic) {
					Drawable icon = getTheme().obtainStyledAttributes(new int[] { R.attr.timelineTab }).getDrawable(0);
					toAdd.setIcon(icon);
				} else toAdd.setText(R.string.timeline_str);
				mTabsAdapter.addTab(toAdd, TimelineFragment.class, index);
			} else if(c.equals(MentionsFragment.ID)) {
				Tab toAdd = getActionBar().newTab();
				if(iconic) {
					Drawable icon = getTheme().obtainStyledAttributes(new int[] { R.attr.mentionsTab }).getDrawable(0);
					toAdd.setIcon(icon);
				} else toAdd.setText(R.string.mentions_str);
				mTabsAdapter.addTab(toAdd, MentionsFragment.class, index);
			} else if(c.equals(MessagesFragment.ID)) {
				Tab toAdd = getActionBar().newTab();
				if(iconic) {
					Drawable icon = getTheme().obtainStyledAttributes(new int[] { R.attr.messagesTab }).getDrawable(0);
					toAdd.setIcon(icon);
				} else toAdd.setText(R.string.messages_str);
				mTabsAdapter.addTab(toAdd, MessagesFragment.class, index);
			} else if(c.equals(TrendsFragment.ID)) {
				Tab toAdd = getActionBar().newTab();
				if(iconic) {
					Drawable icon = getTheme().obtainStyledAttributes(new int[] { R.attr.trendsTab }).getDrawable(0);
					toAdd.setIcon(icon);
				} else toAdd.setText(R.string.trends_str);
				mTabsAdapter.addTab(toAdd, TrendsFragment.class, index);
			} else if(c.equals(FavoritesFragment.ID)) {
				Tab toAdd = getActionBar().newTab();
				if(iconic) {
					Drawable icon = getTheme().obtainStyledAttributes(new int[] { R.attr.favoritesTab }).getDrawable(0);
					toAdd.setIcon(icon);
				}
				else toAdd.setText(R.string.favorites_str);
				mTabsAdapter.addTab(toAdd, FavoritesFragment.class, index);
			} else if(c.startsWith(SavedSearchFragment.ID + "@")) {
				Tab toAdd = getActionBar().newTab();
				String query = c.substring(SavedSearchFragment.ID.length() + 1);
				if(iconic) {
					Drawable icon = getTheme().obtainStyledAttributes(new int[] { R.attr.savedSearchTab }).getDrawable(0);
					toAdd.setIcon(icon);
					if(prefs.getBoolean("textual_savedsearch_tabs", true)) {
						toAdd.setText(query);
					}
				} else toAdd.setText(query);
				mTabsAdapter.addTab(toAdd, SavedSearchFragment.class, index, c.substring(SavedSearchFragment.ID.length() + 1));
			} else if(c.startsWith(UserListFragment.ID + "@")) {
				String name = c.substring(UserListFragment.ID.length() + 1);
				int id = Integer.parseInt(name.substring(name.indexOf("@") + 1, name.length()));
				name = name.substring(0, name.indexOf("@")).replace("%40", "@");
				if(name.startsWith("@" + AccountService.getCurrentAccount().getUser().getScreenName())) {
					name = name.substring(name.indexOf("/") + 1);
				}
				Tab toAdd = getActionBar().newTab();
				if(iconic) {
					Drawable icon = getTheme().obtainStyledAttributes(new int[] { R.attr.userListTab }).getDrawable(0);
					toAdd.setIcon(icon);
					if(prefs.getBoolean("textual_userlist_tabs", true)) toAdd.setText(name);
				} else toAdd.setText(name);
				mTabsAdapter.addTab(toAdd, UserListFragment.class, index, name, id);
			} else if(c.equals(NearbyFragment.ID)) {
				Tab toAdd = getActionBar().newTab();
				if(iconic) {
					Drawable icon = getTheme().obtainStyledAttributes(new int[] { R.attr.nearbyTab }).getDrawable(0);
					toAdd.setIcon(icon);
				}
				else toAdd.setText(R.string.nearby_str);
				mTabsAdapter.addTab(toAdd, NearbyFragment.class, index);
			} else if(c.equals(MediaTimelineFragment.ID)) {
				Tab toAdd = getActionBar().newTab();
				if(iconic) {
					Drawable icon = getTheme().obtainStyledAttributes(new int[] { R.attr.mediaTab }).getDrawable(0);
					toAdd.setIcon(icon);
				}
				else toAdd.setText(R.string.media_title);
				mTabsAdapter.addTab(toAdd, MediaTimelineFragment.class, index);
			}
			index++;
		}
		if(accountSwitched) {
			AccountService.clearAdapters();
		}
		if(newColumn) {
			newColumn = false;
			getActionBar().setSelectedNavigationItem(getActionBar().getTabCount() - 1);		
		} else {
			int defaultColumn = prefs.getInt(Long.toString(AccountService.getCurrentAccount().getId()) + "_default_column", 0);
			if(defaultColumn > (getActionBar().getTabCount() - 1)) {
				defaultColumn = getActionBar().getTabCount() - 1;
				prefs.edit().putInt(Long.toString(AccountService.getCurrentAccount().getId()) + "_default_column", defaultColumn).apply();
			}
			getActionBar().setSelectedNavigationItem(defaultColumn);
			ViewPager pager = (ViewPager)findViewById(R.id.pager);
			pager.setAdapter(mTabsAdapter);
			pager.setCurrentItem(defaultColumn);
		}
		mTabsAdapter.filterDefaultColumnSelection = false;
		invalidateOptionsMenu();
	}

	public void recreateAdapters() {
		if(AccountService.feedAdapters != null) {
			for(int i = 0; i < AccountService.feedAdapters.size(); i++) {
				Utilities.recreateFeedAdapter(this, AccountService.feedAdapters.get(i));
			} 
		}
		if(AccountService.messageAdapter != null) {
			Utilities.recreateMessageAdapter(this, AccountService.messageAdapter);
		}
		if(AccountService.trendsAdapter != null) AccountService.trendsAdapter = new TrendsListAdapter(this);
		if(AccountService.searchFeedAdapters != null) {
			for(int i = 0; i < AccountService.searchFeedAdapters.size(); i++) {
				Utilities.recreateSearchAdapter(this, AccountService.searchFeedAdapters.get(i));
			}
		}
		if(AccountService.nearbyAdapter != null) {
			Utilities.recreateSearchAdapter(this, AccountService.nearbyAdapter);
		}
		if(AccountService.mediaAdapters != null) {
			for(int i = 0; i < AccountService.mediaAdapters.size(); i++) {
				Utilities.recreateMediaFeedAdapter(this, AccountService.mediaAdapters.get(i));
			}
		}
		for(int i = 0; i < getActionBar().getTabCount(); i++) {
			Fragment frag = getFragmentManager().findFragmentByTag("page:" + Integer.toString(i));
			if(frag instanceof BaseListFragment) ((BaseListFragment)frag).reloadAdapter(false);
			else if(frag instanceof BaseSpinnerFragment) ((BaseSpinnerFragment)frag).reloadAdapter(false);
			else ((BaseGridFragment)frag).reloadAdapter(false);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if(savedInstanceState != null) {
			if(savedInstanceState.containsKey("lastTheme") || savedInstanceState.containsKey("lastDisplayReal")) {
				lastTheme = savedInstanceState.getInt("lastTheme");
				lastDisplayReal = savedInstanceState.getBoolean("lastDisplayReal"); 
				setTheme(lastTheme);
				recreateAdapters();
			} else setTheme(Utilities.getTheme(getApplicationContext()));
			newColumn = savedInstanceState.getBoolean("newColumn", false);
			if(savedInstanceState.containsKey("lastIconic")) {
				lastIconic = savedInstanceState.getBoolean("lastIconic");
			} else lastIconic = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("enable_iconic_tabs", true);
		} else {
			if(getIntent().getExtras() != null && getIntent().getExtras().containsKey("new_column")) {
				newColumn = true;
				Intent toSet = getIntent();
				toSet.removeExtra("new_column");
				setIntent(toSet);
			}
			lastIconic = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("enable_iconic_tabs", true);
			lastDisplayReal = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("show_real_names", false);
			setTheme(Utilities.getTheme(getApplicationContext()));
		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		initialize(savedInstanceState);
	}

	/**
	 * Called when accounts are loaded on activity load (along with loadColumns(boolean, boolean))
	 */
	public void accountsLoaded(){
		if(Intent.ACTION_SEND.equals(getIntent().getAction())){
			startActivity(getIntent().setClass(this, ComposerScreen.class));
			finish();
		} else if(Intent.ACTION_VIEW.equals(getIntent().getAction())){
			if(getIntent().getData().getPath().contains("/status/")){
				startActivity(getIntent().setClass(this, TweetViewer.class));
				finish();
			} else {
				//TODO: Handle other URLs
			}
		}
		Intent s = new Intent(this, SendTweetService.class);
		s.setAction(SendTweetService.LOAD_TWEETS);
		startService(s);
	}

	@Override
	public void onResume() {
		super.onResume();
		if(lastTheme == 0) lastTheme = Utilities.getTheme(getApplicationContext());
		else if(lastTheme != Utilities.getTheme(getApplicationContext())) {
			lastTheme = Utilities.getTheme(getApplicationContext()); 
			recreate();
			return;
		} else if(lastIconic != PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("enable_iconic_tabs", true)) {
			lastIconic = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("enable_iconic_tabs", true);
			recreate();
			return;
		} else if(lastDisplayReal != PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("show_real_names", false)) {
			lastDisplayReal = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("show_real_names", true);
			recreate();
			return;
		}
		AccountService.activity = this;
		AccountService.loadTwitterConfig();
		if(getActionBar().getTabCount() == 0 && AccountService.getAccounts().size() > 0) loadColumns(false, false);
		if(AccountService.selectedAccount > 0 && AccountService.getAccounts().size() > 0) {
			if(!AccountService.existsAccount(AccountService.selectedAccount)) {
				AccountService.selectedAccount = AccountService.getAccounts().get(0).getId();
				loadColumns(false, true);
			}
		}
		invalidateOptionsMenu();
		IntentFilter filter = new IntentFilter();
		filter.addAction(SendTweetService.UPDATE_STATUS);
		filter.addAction(AccountManager.END_LOAD);
		registerReceiver(receiver, filter);
	}

	@Override
	public void onPause() {
		super.onPause();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		prefs.edit().remove("last_profilepic_wipe").apply();
		CachingUtils.clearCache(this);
		CachingUtils.clearMediaCache(this);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		try { unregisterReceiver(receiver); }
		catch(Exception e) { }
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt("lastTheme", lastTheme);
		outState.putBoolean("lastDisplayReal", lastDisplayReal);
		outState.putBoolean("lastIconic", lastIconic);
		outState.putBoolean("newColumn", newColumn);
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_actionbar, menu);
		final ArrayList<Account> accs = AccountService.getAccounts();
		final MenuItem switcher = menu.findItem(R.id.accountSwitcher);
		if(accs.size() < 2) {
			menu.findItem(R.id.accountSwitcher).setVisible(false);
			menu.findItem(R.id.myProfileAction).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);			
		} else {
			for(int i = 0; i < accs.size(); i++) {
				ImageManager imageManager = ImageManager.getInstance(this);
				final int index = i;
				imageManager.get("https://api.twitter.com/1/users/profile_image?screen_name=" + accs.get(i).getUser().getScreenName() + "&size=bigger", new OnImageReceivedListener() {
					@Override
					public void onImageReceived(String source, Bitmap bitmap) {
						switcher.getSubMenu().add("@" + accs.get(index).getUser().getScreenName()).setIcon(new BitmapDrawable(getResources(), bitmap)); 
					}
				});
			}
		}
		if(showProgress) {
			MenuItem refresh = menu.findItem(R.id.refreshAction);
			refresh.setEnabled(false);
			refresh.setActionView(new ProgressBar(this, null, android.R.attr.progressBarStyle));
			refresh.expandActionView();
		}
		if(AccountService.getCurrentAccount() != null) switcher.setTitle("@" + AccountService.getCurrentAccount().getUser().getScreenName());
		return true;
	}

	private void addColumn(String id) {
		if(AccountService.getAccounts().size() == 0) return;
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		ArrayList<String> cols = Utilities.jsonToArray(this, prefs.getString(Long.toString(AccountService.getCurrentAccount().getId()) + "_columns", ""));
		cols.add(id);
		prefs.edit().putString(Long.toString(AccountService.getCurrentAccount().getId()) + "_columns", Utilities.arrayToJson(this, cols)).commit();
		loadColumns(false, false);
		getActionBar().setSelectedNavigationItem(getActionBar().getTabCount() - 1);

	}

	private Boolean performRefresh(){
		if(AccountService.getAccounts().size() == 0) return false;
		Fragment frag = getFragmentManager().findFragmentByTag("page:" + Integer.toString(getActionBar().getSelectedNavigationIndex()));
		if(frag != null) {
			if(frag instanceof NearbyFragment) ((NearbyFragment)frag).location = null;
			else if(frag instanceof TrendsFragment) ((TrendsFragment)frag).location = null;
			if(frag instanceof BaseListFragment) ((BaseListFragment)frag).performRefresh(false);
			else if(frag instanceof BaseGridFragment) ((BaseGridFragment)frag).performRefresh(false);
			else if(frag instanceof BaseSpinnerFragment) ((BaseSpinnerFragment)frag).performRefresh(false);
		}
		return true;
	}

	private void removeColumn(int index) {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		final String prefName = Long.toString(AccountService.getCurrentAccount().getId()) + "_default_column";
		int beforeDefCol = prefs.getInt(prefName, 0);
		if(beforeDefCol > 0) prefs.edit().putInt(prefName, beforeDefCol - 1).commit();
		ArrayList<String> cols = Utilities.jsonToArray(this, prefs.getString(Long.toString(AccountService.getCurrentAccount().getId()) + "_columns", ""));
		cols.remove(index);
		prefs.edit().putString(Long.toString(AccountService.getCurrentAccount().getId()) + "_columns", Utilities.arrayToJson(this, cols)).commit();
		int postIndex = index - 1;
		if(postIndex < 0) postIndex = 0;
		loadColumns(false, false);
		getActionBar().setSelectedNavigationItem(postIndex);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refreshAction:
			performRefresh();
			return true;
		case R.id.manageAccountsAction:
			startActivity(new Intent(this, AccountManager.class));
			return true;
		case R.id.searchAction:
			if(AccountService.getAccounts().size() == 0) return false;
			super.onSearchRequested();
			return true;
		case R.id.newTweetAction:
			if(AccountService.getAccounts().size() == 0) return false;
			startActivity(new Intent(getApplicationContext(), ComposerScreen.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
			return true;
		case R.id.removeColumnAction:
			if(getActionBar().getTabCount() == 0) return false;
			removeColumn(getActionBar().getSelectedNavigationIndex());
			return true;
		case R.id.mutingAction:
			startActivityForResult(new Intent(this, MutingManager.class), 600);
			return true;
		case R.id.myProfileAction:
			if(AccountService.getAccounts().size() == 0) return false;
			startActivity(new Intent(getApplicationContext(), ProfileScreen.class).putExtra("screen_name", AccountService.getCurrentAccount().getUser().getScreenName())
					.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
			return true;
		case R.id.settingsAction:
			startActivity(new Intent(getApplicationContext(), SettingsScreen.class));
			return true;
		case R.id.addTimelineColAction:
			addColumn(TimelineFragment.ID);
			return true;
		case R.id.addMentionsColAction:
			addColumn(MentionsFragment.ID);
			return true;
		case R.id.addMessagesColAction:
			addColumn(MessagesFragment.ID);
			return true;
		case R.id.addTrendsColAction:
			addColumn(TrendsFragment.ID);
			return true;
		case R.id.addNearbyColAction:
			addColumn(NearbyFragment.ID);
			return true;
		case R.id.addMediaColAction:
			addColumn(MediaTimelineFragment.ID);
			return true;
		case R.id.addSavedSearchColAction:
			
			Toast.makeText(getApplicationContext(), getString(R.string.loading_savedsearches), Toast.LENGTH_SHORT).show();
			new Thread(new Runnable() {
				public void run() {
					Account acc = AccountService.getCurrentAccount();
					try {
						final ResponseList<SavedSearch> lists = acc.getClient().getSavedSearches();
						runOnUiThread(new Runnable() {
							public void run() { showSavedSearchColumnAdd(lists.toArray(new SavedSearch[0])); }
						});
					} catch (TwitterException e) {
						e.printStackTrace();
						runOnUiThread(new Runnable() {
							public void run() { showSavedSearchColumnAdd(null); }
						});
					}
				}
			}).start();
			return true;
		case R.id.addFavoritesColAction:
			addColumn(FavoritesFragment.ID);
			return true;
		case R.id.addUserListColAction:
			Toast.makeText(getApplicationContext(), getString(R.string.loading_lists), Toast.LENGTH_SHORT).show();
			new Thread(new Runnable() {
				public void run() {
					Account acc = AccountService.getCurrentAccount();
					try {
						final ResponseList<UserList> lists = acc.getClient().getAllUserLists(acc.getId());
						runOnUiThread(new Runnable() {
							public void run() { showUserListColumnAdd(lists.toArray(new UserList[0])); }
						});
					} catch (TwitterException e) {
						e.printStackTrace();
						runOnUiThread(new Runnable() {
							public void run() { Toast.makeText(getApplicationContext(), getString(R.string.failed_load_lists), Toast.LENGTH_LONG).show(); }
						});
					}
				}
			}).start();
			return true;
		default:
			for(Account acc : AccountService.getAccounts()) {
				if(acc.getUser().getScreenName().equals(item.getTitle().toString().substring(1))) {
					if(AccountService.selectedAccount == acc.getId()) break;
					AccountService.selectedAccount = acc.getId();
					PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putLong("last_sel_account", acc.getId()).commit();
					loadColumns(false, true);
					break;
				}
			}
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == 600) {
			for(int i = 0; i < getActionBar().getTabCount(); i++) {
				Fragment frag = getFragmentManager().findFragmentByTag("page:" + Integer.toString(i));
				if(frag != null && frag instanceof BaseListFragment) {
					((BaseListFragment)frag).filter();
				}
			}
		}
	}

	private void showUserListColumnAdd(final UserList[] lists) {
		if(lists == null) return;
		else if(lists.length == 0) {
			Toast.makeText(getBaseContext(), getString(R.string.no_lists), Toast.LENGTH_SHORT).show();
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIconAttribute(R.attr.cloudIcon);
		builder.setTitle(R.string.lists_str);
		ArrayList<String> items = new ArrayList<String>();
		for(UserList l : lists) items.add(l.getFullName());
		builder.setItems(items.toArray(new String[0]), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				UserList curList = lists[item];
				addColumn(UserListFragment.ID + "@" + curList.getFullName().replace("@", "%40") + "@" + Integer.toString(curList.getId()));
			}
		});
		builder.create().show();
	}
	
	private void showSavedSearchColumnAdd(final SavedSearch[] lists) {
		final Dialog diag = new Dialog(this);
		diag.setTitle(R.string.savedsearch_str);
		diag.setCancelable(true);
		diag.setContentView(R.layout.savedsearch_dialog);
		ArrayList<String> items = new ArrayList<String>();
		for(SavedSearch l : lists) items.add(l.getName());
		final ListView list = (ListView)diag.findViewById(android.R.id.list); 
		list.setAdapter(new ArrayAdapter<String>(this, R.layout.trends_list_item, items));
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int index, long id) {
				SavedSearch curList = lists[index];
				addColumn(SavedSearchFragment.ID + "@" + curList.getQuery().replace("@", "%40"));
				diag.dismiss();
			}
		});
		list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int index, long id) {
				Toast.makeText(TimelineScreen.this, R.string.swipe_to_delete_items, Toast.LENGTH_LONG).show();
				return false;
			}
		});
		final EditText input = (EditText)diag.findViewById(android.R.id.input);
		input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if(actionId == EditorInfo.IME_ACTION_GO) {
					final String query = input.getText().toString().trim();
					diag.dismiss();
					addColumn(SavedSearchFragment.ID + "@" + query.replace("@", "%40"));
					new Thread(new Runnable() {
						public void run() {
							try { AccountService.getCurrentAccount().getClient().createSavedSearch(query); }
							catch(Exception e) {
								e.printStackTrace();
								runOnUiThread(new Runnable() {
									public void run() { Toast.makeText(getApplicationContext(), R.string.savedsearch_upload_error, Toast.LENGTH_SHORT).show(); }
								});
								return;
							}
							runOnUiThread(new Runnable() {
								public void run() { Toast.makeText(getApplicationContext(), R.string.savedsearch_uploaded, Toast.LENGTH_SHORT).show(); }
							});
						}
					}).start();
				}
				return false;
			}
		});
		diag.show();
	}
}