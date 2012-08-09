package com.teamboid.twitter;

import java.util.ArrayList;

import com.handlerexploit.prime.ImageManager;
import com.handlerexploit.prime.ImageManager.OnImageReceivedListener;
import com.teamboid.twitter.TabsAdapter.BaseGridFragment;
import com.teamboid.twitter.TabsAdapter.BaseListFragment;
import com.teamboid.twitter.cab.TimelineCAB;
import com.teamboid.twitter.columns.MediaTimelineFragment;
import com.teamboid.twitter.columns.PaddedProfileTimelineFragment;
import com.teamboid.twitter.columns.ProfileAboutFragment;
import com.teamboid.twitter.columns.ProfileTimelineFragment;
import com.teamboid.twitter.listadapters.FeedListAdapter;
import com.teamboid.twitter.listadapters.MediaFeedListAdapter;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.LoaderManager;

import com.teamboid.twitterapi.list.UserList;
import com.teamboid.twitterapi.relationship.Relationship;
import com.teamboid.twitterapi.user.FollowingType;
import com.teamboid.twitterapi.user.User;


/**
 * The activity that represents the profile viewer.
 * 
 * @author Aidan Follestad
 */
public class ProfileScreen 	extends Activity {
	public static final int LOAD_CONTACT_ID = 1;

	private int lastTheme;
	private boolean showProgress;
	public FeedListAdapter adapter;
	public MediaFeedListAdapter mediaAdapter;
	public User user;

	public void showProgress(boolean visible) {
		if(showProgress == visible) return;
		showProgress = visible;
		setProgressBarIndeterminateVisibility(visible);
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
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
		final ImageView profileImg = (ImageView)findViewById(R.id.userItemProfilePic);
		profileImg.setImageBitmap(Utilities.getRoundedImage(BitmapFactory.decodeResource(getResources(), R.drawable.sillouette), 90F));
		
		if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            try {
                String screenName = getIntent().getData().getPathSegments().get(0);
                initializeTabs(savedInstanceState, screenName);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.error_str, Toast.LENGTH_SHORT).show();
                finish();
            }
        }  else if(getIntent().hasExtra("screen_name")){
			initializeTabs(savedInstanceState, getIntent().getStringExtra("screen_name"));
		} else if(getIntent().getDataString().contains("com.android.contacts")){ // Loading from Contact
			getLoaderManager().initLoader(LOAD_CONTACT_ID, null, new LoaderManager.LoaderCallbacks<Cursor>(){

				@Override
				public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
					return new CursorLoader(ProfileScreen.this, getIntent().getData(), new String[]{ContactsContract.Data.DATA1}, null, null, null);
				}

				@Override
				public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
					cursor.moveToNext();
					initializeTabs(savedInstanceState, cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA1)));
				}

				@Override
				public void onLoaderReset(Loader<Cursor> arg0) {}
				
			});
			setTitle(R.string.please_wait);
		}    
	}

	private TabsAdapter mTabsAdapter;
	private void initializeTabs(Bundle savedInstanceState, String screenName) {
		Log.d("boid", "Showing " + screenName);
		setTitle("@" + screenName);
		mTabsAdapter = new TabsAdapter(this, (ViewPager)findViewById(R.id.pager));
		final ActionBar bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		boolean iconic = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("enable_iconic_tabs", true);
		if(iconic) {
			mTabsAdapter.addTab(bar.newTab().setIcon(getTheme().obtainStyledAttributes(new int[] { R.attr.timelineTab }).getDrawable(0)), 
					PaddedProfileTimelineFragment.class, 0, screenName);
			mTabsAdapter.addTab(bar.newTab().setIcon(getTheme().obtainStyledAttributes(new int[] { R.attr.aboutTab }).getDrawable(0)),
					ProfileAboutFragment.class, 1, screenName);
			mTabsAdapter.addTab(bar.newTab().setIcon(getTheme().obtainStyledAttributes(new int[] { R.attr.mediaTab }).getDrawable(0)),
					MediaTimelineFragment.class, 2, screenName, false);
		} else {
			mTabsAdapter.addTab(bar.newTab().setText(R.string.tweets_str), PaddedProfileTimelineFragment.class, 0, screenName);
			mTabsAdapter.addTab(bar.newTab().setText(R.string.about_str), ProfileAboutFragment.class, 1, screenName);
			mTabsAdapter.addTab(bar.newTab().setText(R.string.media_title), MediaTimelineFragment.class, 2, screenName, false);
		}
		if(savedInstanceState != null) getActionBar().setSelectedNavigationItem(savedInstanceState.getInt("lastTab", 0));
	}

	public void loadFollowingInfo() {
		new Thread(new Runnable() {
			public void run() {
				final Account acc = AccountService.getCurrentAccount();
				try {
                    Relationship x = acc.getClient().getRelationship(AccountService.getCurrentAccount().getId(), user.getId());
					getAboutFragment().getAdapter().updateIsBlocked(x.isSourceBlockingTarget());
					
					if(getAboutFragment().getAdapter().isBlocked()) {
						runOnUiThread(new Runnable() {
							public void run() {   
								getActionBar().setSelectedNavigationItem(1);
								getAboutFragment().getAdapter().notifyDataSetChanged();
								invalidateOptionsMenu();
							}
						});
						return;
					}

					getAboutFragment().getAdapter().updateIsFollowedBy(x.isSourceFollowedByTarget());
					getAboutFragment().getAdapter().updateIsFollowing(x.isSourceFollowingTarget());
					
				} catch (final Exception e) {
					e.printStackTrace();
					runOnUiThread(new Runnable() {
						public void run() { 
							Toast.makeText(getApplicationContext(), getString(R.string.failed_check_relationship)
                                    .replace("{user}", user.getScreenName()), Toast.LENGTH_SHORT).show();
							getAboutFragment().getAdapter().setIsError(true);
						}
					});
					return;
				}
				if(user.getFollowingType() == FollowingType.REQUEST_SENT) {
					getAboutFragment().getAdapter().updateRequestSent(true);
					return;
				}
				runOnUiThread(new Runnable() {
					public void run() { getAboutFragment().getAdapter().notifyDataSetChanged(); }
				});
				
			}
		}).start();
	}

	@Override
	public void onResume() {
		super.onResume();
		if(lastTheme == 0) lastTheme = Utilities.getTheme(getApplicationContext());
		else if(lastTheme != Utilities.getTheme(getApplicationContext())) {
			lastTheme = Utilities.getTheme(getApplicationContext());
			recreate();
		}
		TimelineCAB.context = this;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		TimelineCAB.clearSelectedItems();
		if(TimelineCAB.TimelineActionMode != null) {
			TimelineCAB.TimelineActionMode.finish();
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
				if(!getAboutFragment().getAdapter().isBlocked()) menu.findItem(R.id.blockAction).setEnabled(true);
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
			super.onBackPressed();
			return true;
		case R.id.editAction:
			//TODO
			Toast.makeText(getApplicationContext(), "Coming soon!", Toast.LENGTH_SHORT).show();
			return true;
		case R.id.mentionAction:
			startActivity(new Intent(this, ComposerScreen.class)
				.putExtra("append", "@" + getIntent().getStringExtra("screen_name") + " ")
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
			return true;
		case R.id.pinAction:		
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			ArrayList<String> cols = Utilities.jsonToArray(prefs.getString(
                    Long.toString(AccountService.getCurrentAccount().getId()) + "_columns", ""));
			cols.add(ProfileTimelineFragment.ID + "@" + getIntent().getStringExtra("screen_name"));
			prefs.edit().putString(Long.toString(AccountService.getCurrentAccount().getId()) +
                    "_columns", Utilities.arrayToJson(cols)).commit();
			startActivity(new Intent(this, TimelineScreen.class).putExtra("new_column", true));
			finish();
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
						final UserList[] lists = acc.getClient().getLists(acc.getId());
						runOnUiThread(new Runnable() {
							public void run() { 
								toast.cancel();
								showAddToListDialog(lists);
							}
						});
					} catch (Exception e) {
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
						} catch (Exception e) {
							e.printStackTrace();
							runOnUiThread(new Runnable() {
								public void run() { Toast.makeText(getApplicationContext(), R.string.failed_block_str, Toast.LENGTH_LONG).show(); }
							});
							return;
						}
						runOnUiThread(new Runnable() {
							public void run() {
								toast.cancel();
								Toast.makeText(ProfileScreen.this, R.string.success_blocked_str, Toast.LENGTH_SHORT).show();
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
						} catch (Exception e) {
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

	void setHeaderBackground(String url) {
		if(url.startsWith("http")) {
			ImageManager.getInstance(this).get(url, new OnImageReceivedListener() {
				@Override
				public void onImageReceived(String arg0, Bitmap bitmap) {
					((ImageView)findViewById(R.id.img)).setImageBitmap(bitmap);
				}
			});
		} else if(user.getProfileBackgroundColor() != null) {
			((ImageView)findViewById(R.id.img)).setImageDrawable(new ColorDrawable(
					Color.parseColor("#" + user.getProfileBackgroundColor())));
		}
	}

	public ProfileAboutFragment getAboutFragment() {
		return (ProfileAboutFragment)getFragmentManager().findFragmentByTag("page:1");
	}

	/**
	 * Sets up our own views for this
	 */
	public void setupViews() {
		final ImageView profileImg = (ImageView)findViewById(R.id.userItemProfilePic);
		ImageManager.getInstance(this).get(Utilities.getUserImage(user.getScreenName(), this), new OnImageReceivedListener(){
			@Override
			public void onImageReceived(String arg0, Bitmap bitmap) {
				profileImg.setImageBitmap(Utilities.getRoundedImage(bitmap, 90F));
			}
		});
		TextView tv = (TextView)findViewById(R.id.profileTopLeftDetail);
		tv.setText(user.getName() + "\n@" + user.getScreenName());
		((ViewPager)findViewById(R.id.pager)).setOnPageChangeListener(new OnPageChangeListener() {
			int lastPage = -1;
			
			@Override
			public void onPageScrollStateChanged(int state) {
				if(state != ViewPager.SCROLL_STATE_DRAGGING && lastPage == 2) {
					findViewById(R.id.profileHeader).setVisibility(View.GONE);
				} else findViewById(R.id.profileHeader).setVisibility(View.VISIBLE);
				if(state == ViewPager.SCROLL_STATE_IDLE) {
					findViewById(R.id.profileHeader).setX(0);
				}
			}

			@Override
			public void onPageScrolled(int position, float offset, int offsetPixels) {
				lastPage = position;
				if(position == 2 && offset >= 0) return;
				if(position >= 1) findViewById(R.id.profileHeader).setX(-offsetPixels);
			}
			@Override
			public void onPageSelected(int position) {
				mTabsAdapter.onPageSelected(position);
			}
		});
	}

	/**
	 * Set first media
	 */
	public void setupMediaView(){
		runOnUiThread(new Runnable(){

			@Override
			public void run() {
				try{
					MediaFeedListAdapter.MediaFeedItem m = mediaAdapter.get(0);
					setHeaderBackground(m.imgurl);
				} catch(Exception e){
					e.printStackTrace();
					// Here we should divert to profile bg?
					// setHeaderBackground(user.get());
				}
			}
			
		});
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
						try { AccountService.getCurrentAccount().getClient().createListMembers(curList.getId(), new long[] { user.getId() }); }
						catch (final Exception e) {
							e.printStackTrace();
							runOnUiThread(new Runnable() {
								public void run() {
									toast.cancel();
									Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
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
