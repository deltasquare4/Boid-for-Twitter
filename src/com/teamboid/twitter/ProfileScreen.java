package com.teamboid.twitter;

import java.util.ArrayList;

import twitter4j.ResponseList;
import twitter4j.TwitterException;
import twitter4j.User;
import twitter4j.UserList;

import com.handlerexploit.prime.utils.ImageManager;
import com.handlerexploit.prime.utils.ImageManager.OnImageReceivedListener;
import com.teamboid.twitter.TabsAdapter.BaseGridFragment;
import com.teamboid.twitter.TabsAdapter.BaseListFragment;
import com.teamboid.twitter.TabsAdapter.MediaTimelineFragment;
import com.teamboid.twitter.TabsAdapter.PaddedProfileTimelineFragment;
import com.teamboid.twitter.TabsAdapter.ProfileAboutFragment;
import com.teamboid.twitter.TabsAdapter.ProfileTimelineFragment;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
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
	public User user;
	public boolean isBlocked;
	
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
        ActionBar ab = getActionBar();
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayShowHomeEnabled(false);
        setContentView(R.layout.profile_screen);
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
			mTabsAdapter.addTab(bar.newTab().setIcon(getTheme().obtainStyledAttributes(new int[] { R.attr.timelineTab }).getDrawable(0)), PaddedProfileTimelineFragment.class, 0, screenName);
			mTabsAdapter.addTab(bar.newTab().setIcon(getTheme().obtainStyledAttributes(new int[] { R.attr.aboutTab }).getDrawable(0)), ProfileAboutFragment.class, 1, screenName);
			mTabsAdapter.addTab(bar.newTab().setIcon(getTheme().obtainStyledAttributes(new int[] { R.attr.mediaTab }).getDrawable(0)), MediaTimelineFragment.class, 2, screenName, false);
		} else {
			mTabsAdapter.addTab(bar.newTab().setText(R.string.tweets_str), PaddedProfileTimelineFragment.class, 0, screenName);
			mTabsAdapter.addTab(bar.newTab().setText(R.string.about_str), ProfileAboutFragment.class, 1, screenName);
			mTabsAdapter.addTab(bar.newTab().setText(R.string.media_title), MediaTimelineFragment.class, 2, screenName, false);
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
		} else {
			inflater.inflate(R.menu.profile_actionbar, menu);
			if(user != null) {
				if(!isBlocked) menu.findItem(R.id.blockAction).setEnabled(true);
				else menu.findItem(R.id.blockAction).setVisible(false);
				menu.findItem(R.id.reportAction).setEnabled(true);
			}
		}
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
			cols.add(ProfileTimelineFragment.ID + "@" + getIntent().getStringExtra("screen_name"));
			prefs.edit().putString(Long.toString(AccountService.getCurrentAccount().getId()) + "_columns", Utilities.arrayToJson(this, cols)).commit();
			startActivity(new Intent(this, TimelineScreen.class).putExtra("new_column", true));
			return true;
		case R.id.messageAction:
			startActivity(new Intent(getApplicationContext(), ConversationScreen.class).putExtra("screen_name", getIntent().getStringExtra("screen_name")));
			return true;
		case R.id.blockAction:
			if(user == null) return false;
			block();
			return true;
		case R.id.reportAction:
			if(user == null) return false;
			report();
			return true;
		case R.id.refreshAction:
			Fragment frag = getFragmentManager().findFragmentByTag("page:" + Integer.toString(getActionBar().getSelectedNavigationIndex()));
			if(frag != null) {
				if(frag instanceof BaseListFragment) ((BaseListFragment)frag).performRefresh(false);
				else if(frag instanceof BaseGridFragment) ((BaseGridFragment)frag).performRefresh(false); 
			}
			return true;
		case R.id.addToListAction:
			final Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.loading_lists), Toast.LENGTH_SHORT);
			toast.show();
			new Thread(new Runnable() {
				public void run() {
					Account acc = AccountService.getCurrentAccount();
					try {
						final ResponseList<UserList> lists = acc.getClient().getAllUserLists(acc.getId());
						runOnUiThread(new Runnable() {
							public void run() { 
								toast.cancel();
								showAddToListDialog(lists.toArray(new UserList[0]));
							}
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
			return super.onOptionsItemSelected(item);
		}
	}
	
	public void block() {
		AlertDialog.Builder diag = new AlertDialog.Builder(this);
		diag.setTitle(R.string.block_str);
		diag.setMessage(R.string.confirm_block_str);
		diag.setPositiveButton(R.string.yes_str, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final Toast toast = Toast.makeText(ProfileScreen.this, R.string.blocking_str, Toast.LENGTH_LONG);
				toast.show();
				new Thread(new Runnable() {
					public void run() {
						try {
							AccountService.getCurrentAccount().getClient().createBlock(user.getId());
						} catch (TwitterException e) {
							e.printStackTrace();
							runOnUiThread(new Runnable() {
								public void run() { Toast.makeText(getApplicationContext(), R.string.failed_block_str, Toast.LENGTH_LONG).show(); }
							});
							return;
						}
						runOnUiThread(new Runnable() {
							public void run() {
								toast.cancel();
								Toast.makeText(ProfileScreen.this, R.string.blocked_str, Toast.LENGTH_SHORT).show();
								recreate(); //TODO Recreation doesn't seem to update the screen with blocked info for some reason
							}
						});
					}
				}).start();
			}
		});
		diag.setNegativeButton(R.string.no_str, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) { dialog.dismiss(); }
		});
		diag.create().show();
	}
	
	public void report() {
		AlertDialog.Builder diag = new AlertDialog.Builder(this);
		diag.setTitle(R.string.report_str);
		diag.setMessage(R.string.confirm_report_str);
		diag.setPositiveButton(R.string.yes_str, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final Toast toast = Toast.makeText(ProfileScreen.this, R.string.reporting_str, Toast.LENGTH_LONG);
				toast.show();
				new Thread(new Runnable() {
					public void run() {
						try {
							AccountService.getCurrentAccount().getClient().reportSpam(user.getId());
						} catch (TwitterException e) {
							e.printStackTrace();
							runOnUiThread(new Runnable() {
								public void run() { Toast.makeText(getApplicationContext(), R.string.failed_report_str, Toast.LENGTH_LONG).show(); }
							});
							return;
						}
						runOnUiThread(new Runnable() {
							public void run() {
								toast.cancel();
								Toast.makeText(ProfileScreen.this, R.string.reported_str, Toast.LENGTH_SHORT).show();
								recreate();
							}
						});
					}
				}).start();
			}
		});
		diag.setNegativeButton(R.string.no_str, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) { dialog.dismiss(); }
		});
		diag.create().show();
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

	void setHeaderBackground(String url){
		ImageManager.getInstance(this).get(url, new OnImageReceivedListener(){
			@Override
			public void onImageReceived(String arg0, Bitmap bitmap) {
				((ImageView)findViewById(R.id.img)).setImageBitmap(bitmap);
			}
		});
	}
	
	/**
	 * Sets up our own views for this
	 */
	public void setupViews() {		
		ImageManager.getInstance(this).get(Utilities.getUserImage(user.getScreenName(), this), new OnImageReceivedListener(){
			@Override
			public void onImageReceived(String arg0, Bitmap bitmap) {
				((ImageView)findViewById(R.id.userItemProfilePic)).setImageBitmap(Utilities.getRoundedImage(bitmap, 90F));
			}
		});
		TextView tv = (TextView)findViewById(R.id.profileTopLeftDetail);
		tv.setText(user.getName() + "\n@" + user.getScreenName());
		tv = (TextView)findViewById(R.id.profileBottomLeftDetail);
		tv.setText(user.getStatusesCount() + " | " + user.getFriendsCount() + " | " + user.getFollowersCount());
		tv = (TextView)findViewById(R.id.profileLastTweeted);
		tv.setText(getString(R.string.last_tweeted).replace("{time}", Utilities.friendlyTimeMedium(user.getStatus().getCreatedAt())));
		tv = (TextView)findViewById(R.id.profileLocation);
		if(user.getLocation().trim().isEmpty()){
			tv.setVisibility(View.GONE);
		} else tv.setText(user.getLocation());
		((ViewPager)findViewById(R.id.pager)).setOnPageChangeListener(new OnPageChangeListener(){

			@Override
			public void onPageScrollStateChanged(int arg0) { }

			@Override
			public void onPageScrolled(int position, float offset, int offsetPixels) {
				if(position >= 1) findViewById(R.id.profileHeader).setX(-offsetPixels);
			}
			@Override
			public void onPageSelected(int position) {
				findViewById(R.id.profileHeader).setVisibility( position > 1 ? View.GONE : View.VISIBLE );
				//findViewById(R.id.profileHeader).animate().alpha(position > 1 ? 0 : 1 );
				mTabsAdapter.onPageSelected(position);
			}
			
		});
	}
	
	/**
	 * Set first media
	 */
	public void setupMediaView(){
		try{
			MediaFeedListAdapter.MediaFeedItem m = mediaAdapter.get(0);
			setHeaderBackground(m.imgurl);
		} catch(Exception e){
			e.printStackTrace();
			// Here we should divert to profile bg?
			setHeaderBackground(user.getProfileBackgroundImageUrl());
		}
	}
	
	public void showAddToListDialog(final UserList[] lists) {
		if(lists == null || lists.length == 0) {
			Toast.makeText(getApplicationContext(), R.string.no_lists, Toast.LENGTH_SHORT).show();
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIconAttribute(R.attr.cloudIcon);
		builder.setTitle(R.string.lists_str);
		ArrayList<String> items = new ArrayList<String>();
		for(UserList l : lists) items.add(l.getFullName());
		builder.setItems(items.toArray(new String[0]), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				final UserList curList = lists[item];
				final Toast toast = Toast.makeText(getApplicationContext(), R.string.adding_user_list, Toast.LENGTH_LONG);
				toast.show();
				new Thread(new Runnable() {
					public void run() {
						try { AccountService.getCurrentAccount().getClient().addUserListMember(curList.getId(), user.getId()); }
						catch (final TwitterException e) {
							e.printStackTrace();
							runOnUiThread(new Runnable() {
								public void run() {
									toast.cancel();
									Toast.makeText(getApplicationContext(), e.getErrorMessage(), Toast.LENGTH_SHORT).show();
								}
							});
							return;
						}
						runOnUiThread(new Runnable() {
							public void run() {
								toast.cancel();
								Toast.makeText(getApplicationContext(), R.string.added_user_list, Toast.LENGTH_SHORT).show();
							}
						});
					}
				}).start();
			}
		});
		builder.create().show();
	}
}
