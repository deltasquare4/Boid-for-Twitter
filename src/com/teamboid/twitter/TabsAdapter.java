package com.teamboid.twitter;

import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.message.BasicNameValuePair;

import com.teamboid.twitter.MessageConvoAdapter.DMConversation;

import twitter4j.DirectMessage;
import twitter4j.GeoLocation;
import twitter4j.Paging;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Trends;
import twitter4j.Tweet;
import twitter4j.TwitterException;
import twitter4j.User;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The adapter used for columns in the TimelineScreen.
 * @author Aidan Follestad
 */
public class TabsAdapter extends TaggedFragmentAdapter implements ActionBar.TabListener, ViewPager.OnPageChangeListener {

	private final Activity mContext;
	private final ActionBar mActionBar;
	private final ViewPager mViewPager;
	private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();
	public boolean filterDefaultColumnSelection = true; 

	static final class TabInfo {
		private final Class<?> clss;
		private final Bundle args;
		public boolean aleadySelected;
		TabInfo(Class<?> _class, Bundle _args) {
			clss = _class;
			args = _args;
		}
	}
	public TabsAdapter(Activity activity, ViewPager pager) {
		super(activity.getFragmentManager());
		mContext = activity;
		mActionBar = activity.getActionBar();
		mViewPager = pager;
		mViewPager.setAdapter(this);
		mViewPager.setOnPageChangeListener(this);
	}
	public void addTab(ActionBar.Tab tab, Class<?> clss, int index) {
		Bundle args = new Bundle();
		args.putInt("tab_index", index);
		TabInfo info = new TabInfo(clss, args);
		tab.setTag(info);
		tab.setTabListener(this);
		mTabs.add(info);
		mActionBar.addTab(tab);
		notifyDataSetChanged();
	}
	public void addTab(ActionBar.Tab tab, Class<?> clss, int index, String query) {
		Bundle args = new Bundle();
		args.putInt("tab_index", index);
		if(query != null) args.putString("query", query);
		TabInfo info = new TabInfo(clss, args);
		tab.setTag(info);
		tab.setTabListener(this);
		mTabs.add(info);
		mActionBar.addTab(tab);
		notifyDataSetChanged();
	}
	public void addTab(ActionBar.Tab tab, Class<?> clss, int index, String screenName, boolean manualRefresh) {
		Bundle args = new Bundle();
		args.putInt("tab_index", index);
		if(screenName != null) args.putString("screenName", screenName);
		args.putBoolean("manualRefresh", manualRefresh);
		TabInfo info = new TabInfo(clss, args);
		tab.setTag(info);
		tab.setTabListener(this);
		mTabs.add(info);
		mActionBar.addTab(tab);
		notifyDataSetChanged();
	}
	public void addTab(ActionBar.Tab tab, Class<?> clss, int index, String listName, int listId) {
		Bundle args = new Bundle();
		args.putInt("tab_index", index);
		if(listName != null) args.putString("list_name", listName);
		if(listId > 0) args.putInt("list_id", listId);
		TabInfo info = new TabInfo(clss, args);
		tab.setTag(info);
		tab.setTabListener(this);
		mTabs.add(info);
		mActionBar.addTab(tab);
		notifyDataSetChanged();
	}
	public void remove(int index) {
		mTabs.remove(index);
		mActionBar.removeTabAt(index);
		notifyDataSetChanged();
	}
	public void clear() {
		mTabs.clear();
		mActionBar.removeAllTabs();
		notifyDataSetChanged();
	}
	@Override
	public int getCount() { return mTabs.size(); }
	@Override
	public Fragment getItem(int position) {
		TabInfo info = mTabs.get(position);
		return Fragment.instantiate(mContext, info.clss.getName(), info.args);
	}
	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }
	@Override
	public void onPageSelected(int position) { mActionBar.setSelectedNavigationItem(position); }
	@Override
	public void onPageScrollStateChanged(int state) { }
	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		if(!filterDefaultColumnSelection) {
			final String prefName = Long.toString(AccountService.getCurrentAccount().getId()) + "_default_column";
			PreferenceManager.getDefaultSharedPreferences(mContext).edit().putInt(prefName, tab.getPosition()).apply();
		}
		TabInfo curInfo = mTabs.get(tab.getPosition());
		mViewPager.setCurrentItem(tab.getPosition());
		curInfo.aleadySelected = true;
		mTabs.set(tab.getPosition(), curInfo);
		mContext.invalidateOptionsMenu();
	}
	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		if(mTabs.size() == 0 || tab.getPosition() > mTabs.size()) return;
		TabInfo curInfo = mTabs.get(tab.getPosition());
		curInfo.aleadySelected = false;
		mTabs.set(tab.getPosition(), curInfo);
	}
	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		boolean selected = mTabs.get(tab.getPosition()).aleadySelected;
		if(selected) {
			Fragment frag = mContext.getFragmentManager().findFragmentByTag("page:" + tab.getPosition());
			if(frag != null) {
				if(frag instanceof BaseListFragment) ((BaseListFragment)frag).jumpTop();
				else if(frag instanceof BaseSpinnerFragment) ((BaseSpinnerFragment)frag).jumpTop();
				else if(frag instanceof BaseGridFragment) ((BaseGridFragment)frag).jumpTop();
			}
		}
	}

	public static abstract class BaseListFragment extends ListFragment {

		public boolean isLoading;

		public abstract void performRefresh(boolean paginate);
		public abstract void reloadAdapter(boolean firstInitialize);
		public abstract void savePosition();
		public abstract void restorePosition();
		public abstract void jumpTop();
		public abstract void filter();

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
		}
		
		@Override
		public void setEmptyText(CharSequence text) {
			if(getView() != null) super.setEmptyText(text);
		}
	}
	public static abstract class BaseSpinnerFragment extends ListFragment {

		public boolean isLoading;
		private boolean isShown;

		public abstract void performRefresh(boolean paginate);
		public abstract void reloadAdapter(boolean firstInitialize);
		public abstract void savePosition();
		public abstract void restorePosition();
		public abstract void jumpTop();
		public abstract void filter();

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			return inflater.inflate(R.layout.spinner_list_fragment, container, false);
		}

		public Spinner getSpinner() {
			if(getView() == null) return null;
			return (Spinner)getView().findViewById(R.id.fragSpinner);
		}
		@Override
		public void setEmptyText(CharSequence text) {
			if(getView() == null) return;
			((TextView)getView().findViewById(android.R.id.empty)).setText(text);
		}
		@Override
		public void setListShown(boolean visible) {
			if(getView() == null) return;
			isShown = visible;
			getView().findViewById(android.R.id.progress).setVisibility((visible == false) ? View.VISIBLE : View.GONE);
			getListView().setVisibility((visible == true) ? View.VISIBLE : View.GONE);
			boolean condition = (getListAdapter() == null || getListAdapter().isEmpty()) && isShown;
			getView().findViewById(android.R.id.empty).setVisibility(condition ? View.VISIBLE : View.GONE);
		}
	}
	public static abstract class BaseGridFragment extends Fragment {

		public boolean isLoading;
		private boolean isShown;

		public abstract void performRefresh(boolean paginate);
		public abstract void reloadAdapter(boolean firstInitialize);
		public abstract void savePosition();
		public abstract void jumpTop();
		public abstract void filter();

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			return inflater.inflate(R.layout.grid_activity, container, false);
		}

		public void setListShown(boolean shown) {
			if(getView() == null) return;
			isShown = shown;
			getView().findViewById(android.R.id.list).setVisibility((shown == true) ? View.VISIBLE : View.GONE);
			getView().findViewById(android.R.id.progress).setVisibility((shown == false) ? View.VISIBLE : View.GONE);
			boolean condition = (getGridView().getAdapter() == null || getGridView().getAdapter().isEmpty()) && isShown;
			getView().findViewById(android.R.id.empty).setVisibility(condition ? View.VISIBLE : View.GONE);
		}
		public GridView getGridView() { 
			if(getView() == null) return null;
			return (GridView)getView().findViewById(android.R.id.list);
		}
		public void setEmptyText(String text) {
			if(getView() == null) return;
			((TextView)getView().findViewById(android.R.id.empty)).setText(text);
		}
		public void setListAdapter(ListAdapter adapt) {
			if(getView() == null) return;
			((ListView)getView().findViewById(android.R.id.list)).setAdapter(adapt);
		}
	}

	public static class TimelineFragment extends BaseListFragment {

		private Activity context;
		private FeedListAdapter adapt;
		public static final String ID = "COLUMNTYPE:TIMELINE";
		
		@Override
		public void onAttach(Activity act) {
			super.onAttach(act);
			context = act;
		}

		@Override
		public void onListItemClick(ListView l, View v, int index, long id) {
			super.onListItemClick(l, v, index, id);
			Status tweet = (Status)adapt.getItem(index);
			if(tweet.isRetweet()) tweet = tweet.getRetweetedStatus();
			context.startActivity(new Intent(context, TweetViewer.class)
				.putExtra("sr_tweet", Utilities.serializeObject(tweet))
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
		}
		
		@Override
		public void onStart() {
			super.onStart();
			getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) { }
				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
					if(totalItemCount > 0 && (firstVisibleItem + visibleItemCount) >= (totalItemCount - 2) && totalItemCount > visibleItemCount) performRefresh(true);
					if(firstVisibleItem == 0 && context.getActionBar().getTabCount() > 0) {
						if(!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("enable_iconic_tabs", true)) context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(R.string.timeline_str);
						else context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText("");
					}
				}
			});
			getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int index, long id) {
					Status item = (Status)adapt.getItem(index);
					context.startActivity(new Intent(context, ComposerScreen.class).putExtra("reply_to", item.getId())
							.putExtra("reply_to_name", item.getUser().getScreenName()).putExtra("append", Utilities.getAllMentions(item))
							.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
					return false;
				}
			});
			setRetainInstance(true);
			setEmptyText(getString(R.string.no_tweets));
			reloadAdapter(true);
		}

		@Override
		public void onResume() {
			super.onResume();
			if(getView() != null && adapt != null) adapt.restoreLastViewed(getListView());
		}

		@Override
		public void onPause() {
			super.onPause();
			savePosition();
		}

		@Override
		public void performRefresh(final boolean paginate) {
			if(context == null || isLoading || adapt == null) return;	
			isLoading = true;
			if(adapt.getCount() == 0 && getView() != null) setListShown(false);
			adapt.setLastViewed(getListView());
			new Thread(new Runnable() {
				public void run() {
					Paging paging = new Paging(1, 50);
					if(paginate) paging.setMaxId(adapt.getItemId(adapt.getCount() - 1));
					final Account acc = AccountService.getCurrentAccount();
					if(acc != null) {
						try {
							final ResponseList<Status> feed = acc.getClient().getHomeTimeline(paging);
							context.runOnUiThread(new Runnable() {
								public void run() {
									setEmptyText(context.getString(R.string.no_tweets));
									int beforeLast = adapt.getCount() - 1;
									final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
									int addedCount = adapt.add(feed.toArray(new Status[0]), prefs.getBoolean("enable_muting", true));
									if(addedCount > 0 || beforeLast > 0) {
										if(getView() != null) {
											if(paginate && addedCount > 0) getListView().smoothScrollToPosition(beforeLast + 1);
											else if(getView() != null && adapt != null) adapt.restoreLastViewed(getListView());
										}
										if(!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("enable_iconic_tabs", true)) {
											context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(context.getString(R.string.timeline_str) + " (" + Integer.toString(addedCount) + ")");
										} else context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(Integer.toString(addedCount));
									}
								}
							});
						} catch(TwitterException e) {
							e.printStackTrace();
							context.runOnUiThread(new Runnable() {
								public void run() { 
									setEmptyText(context.getString(R.string.error_str));
								}
							});
						}
					}
					context.runOnUiThread(new Runnable() {
						public void run() { 
							if(getView() != null) setListShown(true);
							isLoading = false;
						}
					});
				}
			}).start();
		}

		@Override
		public void reloadAdapter(boolean firstInitialize) {
			if(context == null && getActivity() != null) context = getActivity();
			if(AccountService.getCurrentAccount() != null) {
				if(adapt != null && !firstInitialize && getView() != null) adapt.setLastViewed(getListView());
				adapt = AccountService.getFeedAdapter(context, TimelineFragment.ID, AccountService.getCurrentAccount().getId());
				setListAdapter(adapt);
				if(adapt.getCount() == 0) performRefresh(false);
				else if(getView() != null && adapt != null) {
					adapt.restoreLastViewed(getListView());
					filter();
				}
			}
		}

		@Override
		public void savePosition() { if(getView() != null && adapt != null) adapt.setLastViewed(getListView()); }

		@Override
		public void restorePosition() { if(getView() != null && adapt != null) adapt.restoreLastViewed(getListView()); }
		
		@Override
		public void jumpTop() {
			if(getView() != null) getListView().setSelectionFromTop(0, 0);
		}

		@Override
		public void filter() {
			if(getListView() == null || adapt == null) return;
			AccountService.clearFeedAdapter(context, TimelineFragment.ID, AccountService.getCurrentAccount().getId());
			performRefresh(false);
		}
	}

	public static class MentionsFragment extends BaseListFragment {

		private FeedListAdapter adapt;
		private Activity context;
		public static final String ID = "COLUMNTYPE:MENTIONS";

		@Override
		public void onAttach(Activity act) {
			super.onAttach(act);
			context = act;
		}
		
		@Override
		public void onListItemClick(ListView l, View v, int position, long id) {
			super.onListItemClick(l, v, position, id);
			Status tweet = (Status)adapt.getItem(position);
			if(tweet.isRetweet()) tweet = tweet.getRetweetedStatus();
			context.startActivity(new Intent(context, TweetViewer.class)
				.putExtra("sr_tweet", Utilities.serializeObject(tweet))
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
		}
		
		@Override
		public void onStart() {
			super.onStart();
			getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) { }
				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
					if(totalItemCount > 0 && (firstVisibleItem + visibleItemCount) >= (totalItemCount - 2) && totalItemCount > visibleItemCount) performRefresh(true);
					if(firstVisibleItem == 0 && context.getActionBar().getTabCount() > 0) {
						if(!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("enable_iconic_tabs", true)) context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(R.string.mentions_str);
						else context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText("");
					}
				}
			});
			getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int index, long id) {
					Status item = (Status)adapt.getItem(index);
					context.startActivity(new Intent(context, ComposerScreen.class).putExtra("reply_to", item.getId())
							.putExtra("reply_to_name", item.getUser().getScreenName()).putExtra("append", Utilities.getAllMentions(item))
							.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
					return false;
				}
			});
			setRetainInstance(true);
			setEmptyText(getString(R.string.no_mentions));
			reloadAdapter(true);
		}

		@Override
		public void onResume() {
			super.onResume();
			if(getView() != null && adapt != null) adapt.restoreLastViewed(getListView());
		}

		@Override
		public void onPause() {
			super.onPause();
			savePosition();
		}

		@Override
		public void performRefresh(final boolean paginate) {
			if(context == null || isLoading || adapt == null) return;
			isLoading = true;
			if(adapt.getCount() == 0 && getView() != null) setListShown(false);
			adapt.setLastViewed(getListView());
			new Thread(new Runnable() {
				public void run() {
					Paging paging = new Paging(1, 50);
					if(paginate) paging.setMaxId(adapt.getItemId(adapt.getCount() - 1));
					final Account acc = AccountService.getCurrentAccount();
					if(acc != null) {
						try {
							final ResponseList<Status> feed = acc.getClient().getMentions(paging);
							context.runOnUiThread(new Runnable() {
								public void run() {
									setEmptyText(context.getString(R.string.no_mentions));
									int beforeLast = adapt.getCount() - 1;
									int addedCount = adapt.add(feed.toArray(new Status[0]));
									if(addedCount > 0 || beforeLast > 0) {
										if(getView() != null) {
											if(paginate && addedCount > 0) getListView().smoothScrollToPosition(beforeLast + 1);
											else if(getView() != null && adapt != null) adapt.restoreLastViewed(getListView());
										}
										if(!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("enable_iconic_tabs", true)) {
											context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(context.getString(R.string.mentions_str) + " (" + Integer.toString(addedCount) + ")");
										} else context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(Integer.toString(addedCount));
									}
								}
							});
						} catch(TwitterException e) {
							e.printStackTrace();
							context.runOnUiThread(new Runnable() {
								public void run() { 
									setEmptyText(context.getString(R.string.error_str));
								}
							});
						}
					}
					context.runOnUiThread(new Runnable() {
						public void run() { 
							if(getView() != null) setListShown(true);
							isLoading = false;
						}
					});
				}
			}).start();
		}

		@Override
		public void reloadAdapter(boolean firstInitialize) {
			if(context == null && getActivity() != null) context = (Activity)getActivity();
			if(AccountService.getCurrentAccount() != null) {
				if(adapt != null && !firstInitialize && getView() != null) adapt.setLastViewed(getListView());
				adapt = AccountService.getFeedAdapter(context, MentionsFragment.ID, AccountService.getCurrentAccount().getId());
				setListAdapter(adapt);
				if(adapt.getCount() == 0) performRefresh(false);
				else if(getView() != null && adapt != null) adapt.restoreLastViewed(getListView());
			}
		}
		
		@Override
		public void savePosition() { if(getView() != null && adapt != null) adapt.setLastViewed(getListView()); }

		@Override
		public void restorePosition() { if(getView() != null && adapt != null) adapt.restoreLastViewed(getListView()); }
		
		@Override
		public void jumpTop() {
			if(getView() != null) getListView().setSelectionFromTop(0, 0);
		}

		@Override
		public void filter() {
			// TODO Auto-generated method stub

		}
	}

	public static class MessagesFragment extends BaseListFragment {

		private MessageConvoAdapter adapt;
		private Activity context;
		public static final String ID = "COLUMNTYPE:MESSAGES";
		private boolean isDeleting;

		@Override
		public void onAttach(Activity act) {
			super.onAttach(act);
			context = (Activity)act;
		}
		
		@Override
		public void onListItemClick(ListView l, View v, int position, long id) {
			super.onListItemClick(l, v, position, id);
			startActivity(new Intent(context, ConversationScreen.class).putExtra("screen_name",
					((DMConversation)adapt.getItem(position)).getToScreenName()).
					addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
		}
		
		@Override
		public void onStart() {
			super.onStart();
			getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1, final int index, long id) {
					if(isDeleting) return true;
					AlertDialog.Builder diag = new AlertDialog.Builder(context);
					diag.setTitle(R.string.delete_str);
					diag.setMessage(R.string.confirm_delete_convo);
					diag.setPositiveButton(R.string.yes_str, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							deleteConvo(index, (DMConversation)adapt.getItem(index));
							dialog.dismiss();
						}
					});
					diag.setNegativeButton(R.string.no_str, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) { dialog.dismiss(); }
					});
					diag.create().show();
					return false;
				}
			});
			setRetainInstance(true);
			setEmptyText(getString(R.string.no_messages));
			reloadAdapter(true);
		}

		private void deleteConvo(final int index, final DMConversation convo) {
			if(isDeleting) return;
			isDeleting = true;
			final Account acc = AccountService.getCurrentAccount();
			if(getView() != null) setListShown(false);
			new Thread(new Runnable() {
				public void run() {
					int deletedCount = 0;
					for(DirectMessage msg : convo.getMessages()) {
						try { 
							acc.getClient().destroyDirectMessage(msg.getId());
							deletedCount++;
						} catch (TwitterException e) { e.printStackTrace(); }
					}
					final int count = deletedCount;
					context.runOnUiThread(new Runnable() {
						public void run() {
							if(count == convo.getMessages().size()) adapt.remove(index);
							else Toast.makeText(context, R.string.failed_delete_convo, Toast.LENGTH_SHORT).show();
							if(getView() != null) setListShown(true);
							isDeleting = false;
						}
					});
				}
			}).start();
		}

		@Override
		public void performRefresh(final boolean paginate) {
			if(context == null || isLoading || adapt == null) return;
			isLoading = true;
			if(adapt.getCount() == 0 && getView() != null) setListShown(false);
			new Thread(new Runnable() {
				public void run() {
					final Account acc = AccountService.getCurrentAccount();
					if(acc != null) {
						try {
							ResponseList<DirectMessage> temp = acc.getClient().getDirectMessages();
							temp.addAll(acc.getClient().getSentDirectMessages());
							final ResponseList<DirectMessage> messages = temp; 
							context.runOnUiThread(new Runnable() {
								public void run() {
									setEmptyText(context.getString(R.string.no_messages));
									adapt.add(messages.toArray(new DirectMessage[0]));
								}
							});
						} catch(TwitterException e) {
							e.printStackTrace();
							context.runOnUiThread(new Runnable() {
								public void run() { 
									setEmptyText(context.getString(R.string.error_str));
								}
							});
						}
					}
					context.runOnUiThread(new Runnable() {
						public void run() { 
							if(getView() != null) setListShown(true);
							isLoading = false;
						}
					});
				}
			}).start();
		}

		@Override
		public void reloadAdapter(boolean firstInitialize) {
			if(context == null && getActivity() != null) context = (Activity)getActivity();
			if(AccountService.getCurrentAccount() != null) {
				adapt = AccountService.getMessageConvoAdapter(context, AccountService.getCurrentAccount().getId());
				if(getView() != null) adapt.list = getListView();
				setListAdapter(adapt);
				if(adapt.getCount() == 0) performRefresh(false);
			}
		}

		@Override
		public void restorePosition() { }
		
		@Override
		public void savePosition() { }

		@Override
		public void jumpTop() {
			if(getView() != null) getListView().setSelectionFromTop(0, 0);
		}

		@Override
		public void filter() {
			// TODO Auto-generated method stub

		}
	}

	public static class TrendsFragment extends BaseSpinnerFragment {

		private TrendsListAdapter adapt;
		private Activity context;
		public static final String ID = "COLUMNTYPE:TRENDS";
		private boolean isGettingLocation;
		public GeoLocation location; 

		@Override
		public void onAttach(Activity act) {
			super.onAttach(act);
			context = (Activity)act;
		}

		private boolean filterSelected;
		
		@Override
		public void onListItemClick(ListView l, View v, int position, long id) {
			super.onListItemClick(l, v, position, id);
			context.startActivity(new Intent(context, SearchScreen.class).putExtra("query", 
					(String)adapt.getItem(position)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
		}
		
		@Override
		public void onStart() {
			super.onStart();
			setRetainInstance(true);
			setEmptyText(getString(R.string.no_trends));
			final ArrayAdapter<String> spinAdapt = new ArrayAdapter<String>(context, R.layout.spinner_item);
			String[] toAdd = context.getResources().getStringArray(R.array.trend_sources);
			for(String t : toAdd) spinAdapt.add(t);
			filterSelected = true;
			getSpinner().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1, int index, long arg3) {
					if(filterSelected) return;
					PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("last_trend_source", index).apply();
					performRefresh(false);
				}
				@Override
				public void onNothingSelected(AdapterView<?> arg0) { }
			});			
			getSpinner().setAdapter(spinAdapt);
			filterSelected = false;
			getSpinner().setSelection(PreferenceManager.getDefaultSharedPreferences(context).getInt("last_trend_source", 0));
			reloadAdapter(true);
		}

		@Override
		public void performRefresh(final boolean paginate) {
			if(context == null || isLoading || adapt == null || getView() == null || getSpinner() == null) return;
			else if(location == null && getSpinner().getSelectedItemPosition() == 2) {
				getLocation();
				return;
			}
			isLoading = true;		
			adapt.clear();
			if(getView() != null) setListShown(false);
			new Thread(new Runnable() {
				public void run() {
					final Account acc = AccountService.getCurrentAccount();
					if(acc != null) {
						try {
							ArrayList<Trends> temp = new ArrayList<Trends>();
							switch(getSpinner().getSelectedItemPosition()) {
							default:
								temp.add(acc.getClient().getDailyTrends().get(0));
								break;
							case 1:
								temp.add(acc.getClient().getWeeklyTrends().get(0));
								break;
							case 2:
								final ResponseList<twitter4j.Location> locs = acc.getClient().getAvailableTrends(location);
								temp.add(acc.getClient().getLocationTrends(locs.get(0).getWoeid()));
								break;
							}
							final Trends[] trends = temp.toArray(new Trends[0]);
							context.runOnUiThread(new Runnable() {
								public void run() {
									setEmptyText(context.getString(R.string.no_trends));
									adapt.add(trends);
								}
							});
						} catch(TwitterException e) {
							e.printStackTrace();
							context.runOnUiThread(new Runnable() {
								public void run() { 
									setEmptyText(context.getString(R.string.error_str));
								}
							});
						}
					}
					context.runOnUiThread(new Runnable() {
						public void run() { 
							setListShown(true);
							isLoading = false;
						}
					});
				}
			}).start();
		}

		@Override
		public void reloadAdapter(boolean firstInitialize) {
			if(context == null && getActivity() != null) context = (Activity)getActivity();
			adapt = AccountService.getTrendsAdapter(context);
			setListAdapter(adapt);
			if(adapt.getCount() == 0) performRefresh(false);
		}

		@Override
		public void savePosition() { }

		@Override
		public void restorePosition() { }
		
		@Override
		public void jumpTop() {
			if(getView() != null) getListView().setSelectionFromTop(0, 0);
		}

		@Override
		public void filter() {
			// TODO Auto-generated method stub

		}

		private void getLocation() {
			if(isGettingLocation) return;
			isGettingLocation = true;
			final LocationManager locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
			LocationListener locationListener = new LocationListener() {
				public void onLocationChanged(Location loc) {
					locationManager.removeUpdates(this);
					isGettingLocation = false;
					location = new GeoLocation(loc.getLatitude(), loc.getLongitude());
					performRefresh(false);
				}
				public void onStatusChanged(String provider, int status, Bundle extras) {}
				public void onProviderEnabled(String provider) {}
				public void onProviderDisabled(String provider) {}
			};
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
		}
	}

	public static class FavoritesFragment extends BaseListFragment {

		private FeedListAdapter adapt;
		private Activity context;
		public static final String ID = "COLUMNTYPE:FAVORITES";

		@Override
		public void onAttach(Activity act) {
			super.onAttach(act);
			context = (Activity)act;
		}

		@Override
		public void onListItemClick(ListView l, View v, int position, long id) {
			super.onListItemClick(l, v, position, id);
			Status tweet = (Status)adapt.getItem(position);
			if(tweet.isRetweet()) tweet = tweet.getRetweetedStatus();
			context.startActivity(new Intent(context, TweetViewer.class)
				.putExtra("sr_tweet", Utilities.serializeObject(tweet))
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
		}
		
		@Override
		public void onStart() {			
			super.onStart();
			getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) { }
				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
					if(totalItemCount > 0 && (firstVisibleItem + visibleItemCount) >= (totalItemCount - 2) && totalItemCount > visibleItemCount) performRefresh(true);
					if(firstVisibleItem == 0 && context.getActionBar().getTabCount() > 0) {
						if(!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("enable_iconic_tabs", true)) context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(R.string.favorites_str);
						else context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText("");
					}
				}
			});
			getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int index, long id) {
					Status item = (Status)adapt.getItem(index);
					context.startActivity(new Intent(context, ComposerScreen.class).putExtra("reply_to", item.getId())
							.putExtra("reply_to_name", item.getUser().getScreenName()).putExtra("append", Utilities.getAllMentions(item))
							.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
					return false;
				}
			});
			setRetainInstance(true);
			setEmptyText(getString(R.string.no_favorites));
			reloadAdapter(true);
		}

		@Override
		public void onResume() {
			super.onResume();
			if(getView() != null && adapt != null) adapt.restoreLastViewed(getListView());
		}

		@Override
		public void onPause() {
			super.onPause();
			savePosition();
		}

		@Override
		public void performRefresh(final boolean paginate) {
			if(context == null || isLoading || adapt == null) return;
			isLoading = true;
			if(adapt.getCount() == 0 && getView() != null) setListShown(false);
			adapt.setLastViewed(getListView());
			new Thread(new Runnable() {
				public void run() {
					Paging paging = new Paging(1, 50);
					if(paginate) paging.setMaxId(adapt.getItemId(adapt.getCount() - 1));
					final Account acc = AccountService.getCurrentAccount();
					if(acc != null) {
						try {
							final ResponseList<Status> feed = acc.getClient().getFavorites(paging);
							context.runOnUiThread(new Runnable() {
								public void run() {
									setEmptyText(context.getString(R.string.no_favorites));
									int beforeLast = adapt.getCount() - 1;
									int addedCount = adapt.add(feed.toArray(new Status[0]));
									if(addedCount > 0 || beforeLast > 0) {
										if(getView() != null) {
											if(paginate && addedCount > 0) getListView().smoothScrollToPosition(beforeLast + 1);
											else if(getView() != null && adapt != null) adapt.restoreLastViewed(getListView());
										}
										if(!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("enable_iconic_tabs", true)) {
											context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(context.getString(R.string.favorites_str) + " (" + Integer.toString(addedCount) + ")");
										} else context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(Integer.toString(addedCount));
									}
								}
							});
						} catch(TwitterException e) {
							e.printStackTrace();
							context.runOnUiThread(new Runnable() {
								public void run() { 
									setEmptyText(context.getString(R.string.error_str));
								}
							});
						}
					}
					context.runOnUiThread(new Runnable() {
						public void run() { 
							if(getView() != null) setListShown(true);
							isLoading = false;
						}
					});
				}
			}).start();
		}

		@Override
		public void reloadAdapter(boolean firstInitialize) {
			if(context == null && getActivity() != null) context = (Activity)getActivity();
			if(AccountService.getCurrentAccount() != null) {
				if(adapt != null && !firstInitialize && getView() != null) adapt.setLastViewed(getListView());
				adapt = AccountService.getFeedAdapter(context, FavoritesFragment.ID, AccountService.getCurrentAccount().getId());
				setListAdapter(adapt);
				if(adapt.getCount() == 0) performRefresh(false);
				else if(getView() != null && adapt != null) adapt.restoreLastViewed(getListView());
			}
		}
		
		@Override
		public void savePosition() { if(getView() != null && adapt != null) adapt.setLastViewed(getListView()); }

		@Override
		public void restorePosition() { if(getView() != null && adapt != null) adapt.restoreLastViewed(getListView()); }
		
		@Override
		public void jumpTop() {
			if(getView() != null) getListView().setSelectionFromTop(0, 0);
		}

		@Override
		public void filter() {
			// TODO Auto-generated method stub

		}
	}

	public static class SearchTweetsFragment extends BaseListFragment {

		private SearchScreen context;
		private String query;

		@Override
		public void onAttach(Activity act) {
			super.onAttach(act);
			context = (SearchScreen)act;
		}
		
		@Override
		public void onListItemClick(ListView l, View v, int position, long id) {
			super.onListItemClick(l, v, position, id);
			Tweet tweet = (Tweet)context.tweetAdapter.getItem(position);
			context.startActivity(new Intent(context, TweetViewer.class).putExtra("tweet_id", id).putExtra("user_name", tweet.getFromUserName()).putExtra("user_id", tweet.getFromUserId())
					.putExtra("screen_name", tweet.getFromUser()).putExtra("content", tweet.getText()).putExtra("timer", tweet.getCreatedAt().getTime())
					.putExtra("via", tweet.getSource()).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
		}
		
		@Override
		public void onStart() {
			super.onStart();
			getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) { }
				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
					if(totalItemCount > 0 && (firstVisibleItem + visibleItemCount) >= (totalItemCount - 2) && totalItemCount > visibleItemCount) performRefresh(true);
					if(firstVisibleItem == 0 && context.getActionBar().getTabCount() > 0) context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(R.string.tweets_str);
				}
			});
			getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int index, long id) {
					Tweet item = (Tweet)context.tweetAdapter.getItem(index);
					context.startActivity(new Intent(context, ComposerScreen.class).putExtra("reply_to", item.getId())
							.putExtra("reply_to_name", item.getFromUser()).putExtra("append", Utilities.getAllMentions(item))
							.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
					return false;
				}
			});
			setRetainInstance(true);
			setEmptyText(getString(R.string.no_results));
			query = getArguments().getString("query");
			reloadAdapter(true);
		}

		@Override
		public void performRefresh(final boolean paginate) {
			if(context == null || isLoading || context.tweetAdapter == null) return;
			isLoading = true;
			if(context.tweetAdapter.getCount() == 0 && getView() != null) setListShown(false);
			if(getView() != null && context.tweetAdapter != null) context.tweetAdapter.setLastViewed(getListView());
			new Thread(new Runnable() {
				public void run() {
					Query q = new Query(query);
					if(paginate) q.setMaxId(context.tweetAdapter.getItemId(context.tweetAdapter.getCount() - 1));
					final Account acc = AccountService.getCurrentAccount();
					if(acc != null) {
						try {
							final QueryResult feed = acc.getClient().search(q);
							context.runOnUiThread(new Runnable() {
								public void run() {
									setEmptyText(context.getString(R.string.no_results));
									int beforeLast = context.tweetAdapter.getCount() - 1;
									int addedCount = context.tweetAdapter.add(feed.getTweets().toArray(new Tweet[0]));
									if(addedCount > 0 || beforeLast > 0) {
										if(getView() != null) {
											if(paginate && addedCount > 0) getListView().smoothScrollToPosition(beforeLast + 1);
											else context.tweetAdapter.restoreLastViewed(getListView());
										}
										Tab curTab = context.getActionBar().getTabAt(getArguments().getInt("tab_index"));
										String curTitle = "";
										if(curTab.getText() != null) curTitle = curTab.getText().toString();
										if(curTitle != null && !curTitle.isEmpty() && curTitle.contains("(")) {
											curTitle = curTitle.substring(0, curTitle.lastIndexOf("(") - 2);
											curTitle += (" (" + Integer.toString(addedCount) + ")"); 
										} else curTitle = context.getString(R.string.tweets_str) + " (" + Integer.toString(addedCount) + ")";
										context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(curTitle); 
									}
								}
							});
						} catch(TwitterException e) {
							e.printStackTrace();
							context.runOnUiThread(new Runnable() {
								public void run() { 
									setEmptyText(context.getString(R.string.error_str));
								}
							});
						}
					}
					context.runOnUiThread(new Runnable() {
						public void run() { 
							if(getView() != null) setListShown(true);
							isLoading = false;
						}
					});
				}
			}).start();
		}

		@Override
		public void reloadAdapter(boolean firstInitialize) {
			if(AccountService.getCurrentAccount() != null) {
				if(context.tweetAdapter == null) context.tweetAdapter = new SearchFeedListAdapter(context, null, AccountService.getCurrentAccount().getId());
				if(getView() != null) context.tweetAdapter.list = getListView();
				setListAdapter(context.tweetAdapter);
				if(context.tweetAdapter.getCount() == 0) performRefresh(false);
			}
		}
		
		@Override
		public void savePosition() { }

		@Override
		public void restorePosition() { }
		
		@Override
		public void jumpTop() {
			if(getView() != null) getListView().setSelectionFromTop(0, 0);
		}

		@Override
		public void filter() {
			// TODO Auto-generated method stub

		}
	}

	public static class SearchUsersFragment extends BaseListFragment {

		private SearchScreen context;
		private String query;
		private int page;

		@Override
		public void onAttach(Activity act) {
			super.onAttach(act);
			context = (SearchScreen)act;
		}
		
		@Override
		public void onListItemClick(ListView l, View v, int position, long id) {
			super.onListItemClick(l, v, position, id);
			context.startActivity(new Intent(context, ProfileScreen.class).putExtra("screen_name", ((User)context.userAdapter.getItem(position)).getScreenName())
					.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
		}
		
		@Override
		public void onStart() {
			super.onStart();
			getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) { }
				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
					if(totalItemCount > 0 && (firstVisibleItem + visibleItemCount) >= (totalItemCount - 2) && totalItemCount > visibleItemCount) performRefresh(true);
					if(context.userAdapter != null && getView() != null) {
						context.userAdapter.savedIndex = firstVisibleItem;
						View v = getListView().getChildAt(0);
						context.userAdapter.savedIndexTop = (v == null) ? 0 : v.getTop();
					}
					if(firstVisibleItem == 0 && context.getActionBar().getTabCount() > 0) {
						context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(R.string.users_str);
					}
				}
			});
			getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int index, long id) {
					User item = (User)context.userAdapter.getItem(index);
					context.startActivity(new Intent(context, ComposerScreen.class).putExtra("append", "@" + item.getScreenName() + " ")
							.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
					return false;
				}
			});
			setRetainInstance(true);
			setEmptyText(getString(R.string.no_results));
			query = getArguments().getString("query");
			reloadAdapter(true);
		}

		@Override
		public void performRefresh(final boolean paginate) {
			if(context == null || isLoading || context.userAdapter == null) return;
			isLoading = true;
			if(context.userAdapter.getCount() == 0 && getView() != null) setListShown(false);
			new Thread(new Runnable() {
				public void run() {
					if(paginate) page++;
					else page = 1;
					final Account acc = AccountService.getCurrentAccount();
					if(acc != null) {
						try {
							final ResponseList<User> feed = acc.getClient().searchUsers(query, page);
							context.runOnUiThread(new Runnable() {
								public void run() {
									setEmptyText(context.getString(R.string.no_results));
									int beforeLast = context.userAdapter.getCount() - 1;
									int addedCount = context.userAdapter.add(feed.toArray(new User[0]));
									if(addedCount > 0 || beforeLast > 0) {
										if(getView() != null) {
											if(paginate && addedCount > 0) getListView().smoothScrollToPosition(beforeLast + 1);
											else getListView().setSelectionFromTop(context.userAdapter.savedIndex + addedCount, context.userAdapter.savedIndexTop);
										}
										Tab curTab = context.getActionBar().getTabAt(getArguments().getInt("tab_index"));
										String curTitle = "";
										if(curTab.getText() != null) curTitle = curTab.getText().toString();
										if(curTitle != null && !curTitle.isEmpty() && curTitle.contains("(")) {
											curTitle = curTitle.substring(0, curTitle.lastIndexOf("(") - 2);
											curTitle += (" (" + Integer.toString(addedCount) + ")"); 
										} else curTitle = context.getString(R.string.users_str) + " (" + Integer.toString(addedCount) + ")";
										context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(curTitle); 
									}
								}
							});
						} catch(TwitterException e) {
							e.printStackTrace();
							context.runOnUiThread(new Runnable() {
								public void run() { 
									setEmptyText(context.getString(R.string.error_str));
								}
							});
						}
					}
					context.runOnUiThread(new Runnable() {
						public void run() { 
							if(getView() != null) setListShown(true);
							isLoading = false;
						}
					});
				}
			}).start();
		}

		@Override
		public void reloadAdapter(boolean firstInitialize) {
			if(AccountService.getCurrentAccount() != null) {
				if(context.userAdapter == null) context.userAdapter = new SearchUsersListAdapter(context);
				if(getView() != null) context.userAdapter.list = getListView();
				setListAdapter(context.userAdapter);
				if(context.userAdapter.getCount() == 0) performRefresh(false);
				else if(context.userAdapter.savedIndex > 0 && context.userAdapter.list != null) {
					getListView().setSelectionFromTop(context.userAdapter.savedIndex, context.userAdapter.savedIndexTop);
				}
			}
		}
		
		@Override
		public void savePosition() { }
		
		@Override
		public void restorePosition() { }

		@Override
		public void jumpTop() {
			if(getView() != null) getListView().setSelectionFromTop(0, 0);	
		}

		@Override
		public void filter() {
			// TODO Auto-generated method stub

		}
	}

	public static class SavedSearchFragment extends BaseListFragment {

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
			Tweet tweet = (Tweet)adapt.getItem(position);
			context.startActivity(new Intent(context, TweetViewer.class).putExtra("tweet_id", id).putExtra("user_name", tweet.getFromUserName()).putExtra("user_id", tweet.getFromUserId())
					.putExtra("screen_name", tweet.getFromUser()).putExtra("content", tweet.getText()).putExtra("timer", tweet.getCreatedAt().getTime())
					.putExtra("via", tweet.getSource()).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
		}
		
		@Override
		public void onStart() {
			super.onStart();
			query = getArguments().getString("query");
			getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) { }
				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
					if(totalItemCount > 0 && (firstVisibleItem + visibleItemCount) >= (totalItemCount - 2) && totalItemCount > visibleItemCount) performRefresh(true);
					if(firstVisibleItem == 0 && context.getActionBar().getTabCount() > 0) {
						final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
						if(!prefs.getBoolean("enable_iconic_tabs", true) || prefs.getBoolean("textual_savedsearch_tabs", true)) {
							context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(query);						
						}
						else context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText("");
					}
				}
			});
			getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int index, long id) {
					Tweet item = (Tweet)adapt.getItem(index);
					context.startActivity(new Intent(context, ComposerScreen.class).putExtra("reply_to", item.getId())
							.putExtra("reply_to_name", item.getFromUser()).putExtra("append", Utilities.getAllMentions(item))
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
			if(context == null || isLoading || adapt == null) return;
			isLoading = true;
			if(adapt.getCount() == 0 && getView() != null) setListShown(false);
			adapt.setLastViewed(getListView());
			new Thread(new Runnable() {
				public void run() {
					Query q = new Query(query);
					if(paginate) q.setMaxId(adapt.getItemId(adapt.getCount() - 1));
					final Account acc = AccountService.getCurrentAccount();
					if(acc != null) {
						try {
							final QueryResult feed = acc.getClient().search(q);
							context.runOnUiThread(new Runnable() {
								public void run() {
									setEmptyText(context.getString(R.string.no_results));
									int beforeLast = adapt.getCount() - 1;
									int addedCount = adapt.add(feed.getTweets().toArray(new Tweet[0]));
									if(addedCount > 0 || beforeLast > 0) {
										if(getView() != null) {
											if(paginate && addedCount > 0) getListView().smoothScrollToPosition(beforeLast + 1);
											else adapt.restoreLastViewed(getListView());
										}
										final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
										if(!prefs.getBoolean("enable_iconic_tabs", true) || prefs.getBoolean("textual_savedsearch_tabs", true)) {
											context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(query + " (" + Integer.toString(addedCount) + ")");
										} else context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(Integer.toString(addedCount)); 
									}
								}
							});
						} catch(TwitterException e) {
							e.printStackTrace();
							context.runOnUiThread(new Runnable() {
								public void run() { 
									setEmptyText(context.getString(R.string.error_str));
								}
							});
						}
					}
					context.runOnUiThread(new Runnable() {
						public void run() { 
							if(getView() != null) setListShown(true);
							isLoading = false;
						}
					});
				}
			}).start();
		}

		@Override
		public void reloadAdapter(boolean firstInitialize) {
			if(context == null && getActivity() != null) context = (Activity)getActivity();
			if(AccountService.getCurrentAccount() != null) {
				if(adapt != null && !firstInitialize && getView() != null) adapt.setLastViewed(getListView());
				adapt = AccountService.getSearchFeedAdapter(context, SavedSearchFragment.ID + "@" + query, AccountService.getCurrentAccount().getId());
				if(getView() != null) adapt.list = getListView();
				setListAdapter(adapt);
				if(adapt.getCount() == 0) performRefresh(false);
				else adapt.restoreLastViewed(getListView());
			}
		}
		
		@Override
		public void savePosition() { if(getView() != null && adapt != null) adapt.setLastViewed(getListView()); }

		@Override
		public void restorePosition() { if(getView() != null && adapt != null) adapt.restoreLastViewed(getListView()); }

		@Override
		public void jumpTop() {
			if(getView() != null) getListView().setSelectionFromTop(0, 0);
		}

		@Override
		public void filter() {
			// TODO Auto-generated method stub

		}
	}

	public static class NearbyFragment extends BaseSpinnerFragment {

		private SearchFeedListAdapter adapt;
		private TimelineScreen context;
		public static final String ID = "COLUMNTYPE:NEARBY";
		public GeoLocation location;
		private String radius;
		private boolean isGettingLocation;

		@Override
		public void onAttach(Activity act) {
			super.onAttach(act);
			context = (TimelineScreen)act;
		}

		private boolean filterSelected;

		@Override
		public void onListItemClick(ListView l, View v, int position, long id) {
			super.onListItemClick(l, v, position, id);
			Tweet tweet = (Tweet)adapt.getItem(position);
			context.startActivity(new Intent(context, TweetViewer.class).putExtra("tweet_id", id).putExtra("user_name", tweet.getFromUserName()).putExtra("user_id", tweet.getFromUserId())
					.putExtra("screen_name", tweet.getFromUser()).putExtra("content", tweet.getText()).putExtra("timer", tweet.getCreatedAt().getTime())
					.putExtra("via", tweet.getSource()).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
		}
		
		@Override
		public void onStart() {
			super.onStart();
			getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) { }
				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
					if(totalItemCount > 0 && (firstVisibleItem + visibleItemCount) >= (totalItemCount - 2) && totalItemCount > visibleItemCount) {
						performRefresh(true);
					}
					if(firstVisibleItem == 0 && context.getActionBar().getTabCount() > 0) {
						if(!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("enable_iconic_tabs", true)) context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(R.string.nearby_str);
						else context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText("");
					}
				}
			});
			getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int index, long id) {
					Tweet item = (Tweet)adapt.getItem(index);
					context.startActivity(new Intent(context, ComposerScreen.class).putExtra("reply_to", item.getId())
							.putExtra("reply_to_name", item.getFromUser()).putExtra("append", Utilities.getAllMentions(item))
							.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
					return false;
				}
			});
			final ArrayAdapter<String> spinAdapt = new ArrayAdapter<String>(context, R.layout.spinner_item);
			spinAdapt.addAll(context.getResources().getStringArray(R.array.nearby_distances));
			filterSelected = true;
			getSpinner().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1, int index, long arg3) {
					if(filterSelected) return;
					PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("last_nearby_range", index).apply();
					radius = getSpinner().getSelectedItem().toString();
					radius = radius.substring(radius.indexOf(" ") + 1);
					performRefresh(false);
				}
				@Override
				public void onNothingSelected(AdapterView<?> arg0) { }
			});			
			getSpinner().setAdapter(spinAdapt);
			filterSelected = false;
			getSpinner().setSelection(PreferenceManager.getDefaultSharedPreferences(context).getInt("last_nearby_range", 0));
			setRetainInstance(true);
			reloadAdapter(true);
		}

		@Override
		public void onResume() {
			super.onResume();
			if(adapt.getCount() == 0) performRefresh(false);
		}

		private void getLocation() {
			if(isGettingLocation) return;
			isGettingLocation = true;
			final LocationManager locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
			LocationListener locationListener = new LocationListener() {
				public void onLocationChanged(Location loc) {
					locationManager.removeUpdates(this);
					isGettingLocation = false;
					location = new GeoLocation(loc.getLatitude(), loc.getLongitude());
					performRefresh(false);
				}
				public void onStatusChanged(String provider, int status, Bundle extras) {}
				public void onProviderEnabled(String provider) {}
				public void onProviderDisabled(String provider) {}
			};
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
		}

		@Override
		public void performRefresh(final boolean paginate) {
			if(context == null || isLoading || adapt == null && getView() != null) return;
			else if(location == null) {
				getLocation();
				return;
			}
			isLoading = true;
			if(!paginate) adapt.clear();
			if(getView() != null) adapt.setLastViewed(getListView());
			if(adapt.getCount() == 0) setListShown(false);
			new Thread(new Runnable() {
				public void run() {
					Query q = new Query("geocode:" + Double.toString(location.getLatitude()) + "," + Double.toString(location.getLongitude()) + "," + radius);
					if(paginate) q.setMaxId(adapt.getItemId(adapt.getCount() - 1));
					final Account acc = AccountService.getCurrentAccount();
					if(acc != null) {
						try {
							final QueryResult feed = acc.getClient().search(q);
							context.runOnUiThread(new Runnable() {
								public void run() {
									if(getView() != null) setEmptyText(context.getString(R.string.no_results));
									int beforeLast = adapt.getCount() - 1;
									int addedCount = adapt.add(feed.getTweets().toArray(new Tweet[0]));
									if(addedCount > 0 || beforeLast > 0) {
										if(getView() != null) {
											if(paginate && addedCount > 0) getListView().smoothScrollToPosition(beforeLast + 1);
											else adapt.restoreLastViewed(getListView());
										}
										if(!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("enable_iconic_tabs", true)) {
											context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(context.getString(R.string.nearby_str) + " (" + Integer.toString(addedCount) + ")");
										} else context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(Integer.toString(addedCount)); 
									}
								}
							});
						} catch(TwitterException e) {
							e.printStackTrace();
							context.runOnUiThread(new Runnable() {
								public void run() { 
									setEmptyText(context.getString(R.string.error_str));
								}
							});
						}
					}
					context.runOnUiThread(new Runnable() {
						public void run() {
							setListShown(true);
							isLoading = false;
						}
					});
				}
			}).start();
		}

		@Override
		public void reloadAdapter(boolean firstInitialize) {
			if(AccountService.getCurrentAccount() != null) {
				if(adapt != null && !firstInitialize && getView() != null) adapt.setLastViewed(getListView());
				adapt = AccountService.getNearbyAdapter(context);
				if(getView() != null) adapt.list = getListView();
				setListAdapter(adapt);
				if(adapt.getCount() == 0) performRefresh(false);
				else adapt.restoreLastViewed(getListView());
			}
		}
		
		@Override
		public void savePosition() { if(getView() != null && adapt != null) adapt.setLastViewed(getListView()); }

		@Override
		public void restorePosition() { if(getView() != null && adapt != null) adapt.restoreLastViewed(getListView()); }

		@Override
		public void jumpTop() {
			if(getView() != null) getListView().setSelectionFromTop(0, 0);
		}

		@Override
		public void filter() { }
	}

	public static class UserListFragment extends BaseListFragment {

		private FeedListAdapter adapt;
		private Activity context;
		public static final String ID = "COLUMNTYPE:USERLIST";
		private String listName;
		private int listID;

		@Override
		public void onAttach(Activity act) {
			super.onAttach(act);
			context = (Activity)act;
		}

		@Override
		public void onListItemClick(ListView l, View v, int position, long id) {
			super.onListItemClick(l, v, position, id);
			Status tweet = (Status)adapt.getItem(position);
			if(tweet.isRetweet()) tweet = tweet.getRetweetedStatus();
			context.startActivity(new Intent(context, TweetViewer.class)
				.putExtra("sr_tweet", Utilities.serializeObject(tweet))
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
		}
		
		@Override
		public void onStart() {
			super.onStart();
			listName = getArguments().getString("list_name");
			listID = getArguments().getInt("list_id");
			getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) { }
				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
					if(totalItemCount > 0 && (firstVisibleItem + visibleItemCount) >= (totalItemCount - 2) && totalItemCount > visibleItemCount) performRefresh(true);
					if(firstVisibleItem == 0 && context.getActionBar().getTabCount() > 0) {
						final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
						if(!prefs.getBoolean("enable_iconic_tabs", true) || prefs.getBoolean("textual_userlist_tabs", true)) {
							context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(listName);
						} else context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText("");
					}
				}
			});
			getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int index, long id) {
					Status item = (Status)adapt.getItem(index);
					context.startActivity(new Intent(context, ComposerScreen.class).putExtra("reply_to", item.getId())
							.putExtra("reply_to_name", item.getUser().getScreenName()).putExtra("append", Utilities.getAllMentions(item))
							.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
					return false;
				}
			});
			setRetainInstance(true);
			setEmptyText(getString(R.string.no_tweets));
			reloadAdapter(true);
		}

		@Override
		public void onResume() {
			super.onResume();
			if(getView() != null && adapt != null) adapt.restoreLastViewed(getListView());
		}

		@Override
		public void onPause() {
			super.onPause();
			savePosition();
		}

		@Override
		public void performRefresh(final boolean paginate) {
			if(context == null || isLoading || adapt == null) return;
			isLoading = true;
			if(adapt.getCount() == 0 && getView() != null) setListShown(false);
			adapt.setLastViewed(getListView());
			new Thread(new Runnable() {
				public void run() {
					Paging paging = new Paging(1, 50);
					if(paginate) paging.setMaxId(adapt.getItemId(adapt.getCount() - 1));
					final Account acc = AccountService.getCurrentAccount();
					if(acc != null) {
						try {
							final ResponseList<Status> feed = acc.getClient().getUserListStatuses(listID, paging);
							context.runOnUiThread(new Runnable() {
								public void run() {
									setEmptyText(context.getString(R.string.no_tweets));
									int beforeLast = adapt.getCount() - 1;
									int addedCount = adapt.add(feed.toArray(new Status[0]));
									if(addedCount > 0 || beforeLast > 0) {
										if(getView() != null) {
											if(paginate && addedCount > 0) getListView().smoothScrollToPosition(beforeLast + 1);
											else if(getView() != null && adapt != null) adapt.restoreLastViewed(getListView());
										}
										final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
										if(!prefs.getBoolean("enable_iconic_tabs", true) || prefs.getBoolean("textual_userlist_tabs", true)) {
											context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(listName + " (" + Integer.toString(addedCount) + ")");
										} else context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(Integer.toString(addedCount));
									}
								}
							});
						} catch(TwitterException e) {
							e.printStackTrace();
							context.runOnUiThread(new Runnable() {
								public void run() { 
									setEmptyText(context.getString(R.string.error_str));
								}
							});
						}
					}
					context.runOnUiThread(new Runnable() {
						public void run() { 
							if(getView() != null) setListShown(true);
							isLoading = false;
						}
					});
				}
			}).start();
		}

		@Override
		public void reloadAdapter(boolean firstInitialize) {
			if(context == null && getActivity() != null) context = (Activity)getActivity();
			if(AccountService.getCurrentAccount() != null) {
				if(adapt != null && !firstInitialize && getView() != null) adapt.setLastViewed(getListView());
				adapt = AccountService.getFeedAdapter(context, UserListFragment.ID + "@" + listName.replace("@", "%40") +
							"@" + Integer.toString(listID), AccountService.getCurrentAccount().getId());
				setListAdapter(adapt);
				if(adapt.getCount() == 0) performRefresh(false);
				else if(getView() != null && adapt != null) adapt.restoreLastViewed(getListView());
			}
		}
		
		@Override
		public void savePosition() { if(getView() != null && adapt != null) adapt.setLastViewed(getListView()); }

		@Override
		public void restorePosition() { if(getView() != null && adapt != null) adapt.restoreLastViewed(getListView()); }

		@Override
		public void jumpTop() {
			if(getView() != null) getListView().setSelectionFromTop(0, 0);
		}

		@Override
		public void filter() {
			// TODO Auto-generated method stub

		}
	}

	public static class MediaTimelineFragment extends BaseGridFragment {

		private Activity context;
		private MediaFeedListAdapter adapt;
		public static final String ID = "COLUMNTYPE:MEDIATIMELINE";
		private String screenName;
		private boolean manualRefresh;

		@Override
		public void onAttach(Activity act) {
			super.onAttach(act);
			context = act;
		}
		
		@Override
		public void onStart() {
			super.onStart();
			GridView grid = getGridView();
			grid.setOnScrollListener(new AbsListView.OnScrollListener() {
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) { }
				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
					if(totalItemCount > 0 && (firstVisibleItem + visibleItemCount) >= totalItemCount) performRefresh(true);
					if(firstVisibleItem == 0 && context.getActionBar().getTabCount() > 0) {
						if(!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("enable_iconic_tabs", true)) context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(R.string.media_title);
						else context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText("");
					}
				}
			});
			grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
					Status tweet = (Status)adapt.getItem(position);
					if(tweet.isRetweet()) tweet = tweet.getRetweetedStatus();
					context.startActivity(new Intent(context, TweetViewer.class)
						.putExtra("sr_tweet", Utilities.serializeObject(tweet))
						.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				}
			});
			grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int index, long id) {
					Status item = (Status)adapt.getItem(index);
					context.startActivity(new Intent(context, ComposerScreen.class).putExtra("reply_to", item.getId())
							.putExtra("reply_to_name", item.getUser().getScreenName()).putExtra("append", Utilities.getAllMentions(item))
							.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
					return false;
				}
			});
			setRetainInstance(true);
			screenName = (String)getArguments().get("screenName");
			manualRefresh = getArguments().getBoolean("manualRefresh", false);
			reloadAdapter(true);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedIinstnaceState) {
			return inflater.inflate(R.layout.grid_activity, null);
		}

		@Override
		public void onResume() {
			super.onResume();
			if(getView() != null && adapt != null) adapt.restoreLastViewed(getGridView());
			if(manualRefresh) setEmptyText(context.getString(R.string.manual_refresh_hint));
		}

		@Override
		public void onPause() {
			super.onPause();
			savePosition();
		}

		private int pageSkips;

		@Override
		public void performRefresh(final boolean paginate) {
			if(context == null || isLoading || adapt == null) return;	
			isLoading = true;
			if(getView() != null && adapt != null) {
				adapt.setLastViewed(getGridView());
				if(adapt.getCount() == 0) setListShown(false);
			}
			if(!paginate) pageSkips = 0;
			new Thread(new Runnable() {
				public void run() {
					Paging paging = new Paging(1, 50);
					if(paginate) paging.setMaxId(adapt.getItemId(adapt.getCount() - 1));
					final Account acc = AccountService.getCurrentAccount();
					if(acc != null) {
						try {
							ResponseList<Status> temp = null;
							if(screenName != null) temp = acc.getClient().getUserTimeline(screenName, paging);
							else temp = acc.getClient().getHomeTimeline(paging);
							final ResponseList<Status> feed = temp;
							context.runOnUiThread(new Runnable() {
								public void run() {
									if(!manualRefresh) setEmptyText(context.getString(R.string.no_media));
									else setEmptyText(context.getString(R.string.manual_refresh_hint));
									int beforeLast = adapt.getCount() - 1;
									int addedCount = adapt.add(feed.toArray(new Status[0]), true, MediaTimelineFragment.this);
									if(addedCount > 0 || beforeLast > 0) {
										pageSkips = 0;
										if(getView() != null) {
											if(paginate && addedCount > 0) getGridView().smoothScrollToPosition(beforeLast + 1);
											else if(getView() != null && adapt != null) adapt.restoreLastViewed(getGridView());
										}
										if(!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("enable_iconic_tabs", true)) {
											context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(context.getString(R.string.media_title) + " (" + Integer.toString(addedCount) + ")");
										} else context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(Integer.toString(addedCount));
									} else if(pageSkips < 8) {
										pageSkips++;
										performRefresh(true);
									}
								}
							});
						} catch(TwitterException e) {
							e.printStackTrace();
							context.runOnUiThread(new Runnable() {
								public void run() { setEmptyText(context.getString(R.string.error_str)); }
							});
						}
						if(pageSkips <= 5) return;
					}
					context.runOnUiThread(new Runnable() {
						public void run() { 
							isLoading = false;
							setListShown(true);
						}
					});
				}
			}).start();
		}

		@Override
		public void reloadAdapter(boolean firstInitialize) {
			if(AccountService.getCurrentAccount() != null) {
				if(adapt != null && !firstInitialize && getView() != null) adapt.setLastViewed(getGridView());
				if(screenName != null && !screenName.trim().isEmpty()) {
					if(((ProfileScreen)context).mediaAdapter == null) {
						((ProfileScreen)context).mediaAdapter = new MediaFeedListAdapter(context, null, AccountService.getCurrentAccount().getId());
					}
					adapt = ((ProfileScreen)context).mediaAdapter;
				} else {
					adapt = AccountService.getMediaFeedAdapter(context, MediaTimelineFragment.ID,AccountService.getCurrentAccount().getId());
				}
				getGridView().setAdapter(adapt);
				if(getView() != null) {
					if(adapt.getCount() > 0) {
						getView().findViewById(android.R.id.empty).setVisibility(View.GONE);
						adapt.restoreLastViewed(getGridView());
						filter();
					} else {
						getView().findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
						if(!manualRefresh) performRefresh(false);
					}
				}
			}
		}

		@Override
		public void savePosition() { if(getView() != null && adapt != null) adapt.setLastViewed(getGridView()); }

		@Override
		public void jumpTop() {
			if(getView() != null) getGridView().setSelection(0);
		}

		@Override
		public void filter() { }
	}

	public static class ProfileTimelineFragment extends BaseListFragment {

		private ProfileScreen context;
		private String screenName;

		@Override
		public void onAttach(Activity act) {
			super.onAttach(act);
			context = (ProfileScreen)act;
		}

		@Override
		public void onListItemClick(ListView l, View v, int position, long id) {
			super.onListItemClick(l, v, position, id);
			Status tweet = (Status)context.adapter.getItem(position);
			if(tweet.isRetweet()) tweet = tweet.getRetweetedStatus();
			context.startActivity(new Intent(context, TweetViewer.class)
				.putExtra("sr_tweet", Utilities.serializeObject(tweet))
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
		}
		
		@Override
		public void onStart() {
			super.onStart();
			getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) { }
				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
					if(totalItemCount > 0 && (firstVisibleItem + visibleItemCount) >= (totalItemCount - 2) && totalItemCount > visibleItemCount) performRefresh(true);
					if(firstVisibleItem == 0 && context.getActionBar().getTabCount() > 0) {
						if(!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("enable_iconic_tabs", true)) context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(R.string.tweets_str);
						else context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText("");
					}
				}
			});
			getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int index, long id) {
					Status item = (Status)((ProfileScreen)context).adapter.getItem(index);
					context.startActivity(new Intent(context, ComposerScreen.class).putExtra("reply_to", item.getId())
							.putExtra("reply_to_name", item.getUser().getScreenName()).putExtra("append", Utilities.getAllMentions(item))
							.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
					return false;
				}
			});
			setRetainInstance(true);
			setEmptyText(getString(R.string.no_tweets));
			screenName = getArguments().getString("query");
			reloadAdapter(true);
		}

		@Override
		public void onResume() {
			super.onResume();
			if(getView() != null && context.adapter != null) context.adapter.restoreLastViewed(getListView());
		}

		@Override
		public void onPause() {
			super.onPause();
			savePosition();
		}

		@Override
		public void performRefresh(final boolean paginate) {
			if(context == null || isLoading || context.adapter == null) return;	
			isLoading = true;
			if(context.adapter.getCount() == 0 && getView() != null) setListShown(false);
			context.adapter.setLastViewed(getListView());
			new Thread(new Runnable() {
				public void run() {
					Paging paging = new Paging(1, 50);
					if(paginate) paging.setMaxId(context.adapter.getItemId(context.adapter.getCount() - 1));
					final Account acc = AccountService.getCurrentAccount();
					if(acc != null) {
						try {
							final ResponseList<Status> feed = acc.getClient().getUserTimeline(screenName, paging);
							context.runOnUiThread(new Runnable() {
								public void run() {
									setEmptyText(context.getString(R.string.no_tweets));
									int beforeLast = context.adapter.getCount() - 1;
									int addedCount = context.adapter.add(feed.toArray(new Status[0]));
									if(getView() != null) {
										if(paginate && addedCount > 0) getListView().smoothScrollToPosition(beforeLast + 1);
										else if(getView() != null && context.adapter != null) context.adapter.restoreLastViewed(getListView());
									}
									if(!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("enable_iconic_tabs", true)) {
										context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(context.getString(R.string.tweets_str) + " (" + Integer.toString(addedCount) + ")");
									} else context.getActionBar().getTabAt(getArguments().getInt("tab_index")).setText(Integer.toString(addedCount));
								}
							});
						} catch(TwitterException e) {
							e.printStackTrace();
							context.runOnUiThread(new Runnable() {
								public void run() { 
									setEmptyText(context.getString(R.string.error_str));
								}
							});
						}
					}
					context.runOnUiThread(new Runnable() {
						public void run() { 
							if(getView() != null) setListShown(true);
							isLoading = false;
						}
					});
				}
			}).start();
		}

		@Override
		public void reloadAdapter(boolean firstInitialize) {
			if(AccountService.getCurrentAccount() != null) {
				if(context.adapter != null && !firstInitialize && getView() != null) context.adapter.setLastViewed(getListView());
				if(context.adapter == null) context.adapter = new FeedListAdapter(context, null, AccountService.getCurrentAccount().getId());
				setListAdapter(context.adapter);
				if(context.adapter.getCount() == 0) performRefresh(false);
				else if(getView() != null && context.adapter != null) context.adapter.restoreLastViewed(getListView());
			}
		}

		@Override
		public void savePosition() { if(getView() != null && context.adapter != null) context.adapter.setLastViewed(getListView()); }

		@Override
		public void restorePosition() { if(getView() != null && context.adapter != null) context.adapter.restoreLastViewed(getListView()); }

		@Override
		public void jumpTop() {
			if(getView() != null) getListView().setSelectionFromTop(0, 0);
		}

		@Override
		public void filter() { }
	}

	public static class ProfileAboutFragment extends BaseListFragment {

		private Activity context;
		private ProfileAboutAdapter adapt;
		private String screenName;

		@Override
		public void onAttach(Activity act) {
			super.onAttach(act);
			context = act;
		}
		
		@Override
		public void onListItemClick(ListView l, View v, int position, long id) {
			super.onListItemClick(l, v, position, id);
			if(position == 0) return;
			BasicNameValuePair pair = adapt.values.get(position - 1);
			if(pair.getName().equals(context.getString(R.string.website_str))) {
				if(!pair.getValue().equals(context.getString(R.string.none_str))) {
					String url = pair.getValue();
					try { context.startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)); }
					catch(Exception e) { e.printStackTrace(); }
				}
			} else if(pair.getName().equals(context.getString(R.string.tweets_str))) {
				context.getActionBar().setSelectedNavigationItem(0);
			} else if(pair.getName().equals(context.getString(R.string.location_str))) {
				String loc = null;
				try { loc = URLEncoder.encode(pair.getValue(), "UTF-8"); }
				catch(Exception e) { e.printStackTrace(); }
				if(!loc.equals(context.getString(R.string.unknown_str))) {
					try { context.startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(context.getString(R.string.google_url) + loc)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)); }
					catch(Exception e) { e.printStackTrace(); }
				}
			} else if(pair.getName().equals(context.getString(R.string.followers_str))) {
				Intent intent = new Intent(context, UserListActivity.class)
				.putExtra("mode", UserListActivity.FOLLOWERS_LIST)
				.putExtra("user", adapt.user.getId())
				.putExtra("username", adapt.user.getScreenName());
				context.startActivity(intent);
			} else if(pair.getName().equals(context.getString(R.string.friends_str))) {
				Intent intent = new Intent(context, UserListActivity.class)
				.putExtra("mode", UserListActivity.FOLLOWING_LIST)
				.putExtra("user", adapt.user.getId())
				.putExtra("username", adapt.user.getScreenName());
				context.startActivity(intent);
			} else if(pair.getName().equals(context.getString(R.string.favorites_str))) {
				Intent intent = new Intent(context, TweetListActivity.class)
				.putExtra("mode", TweetListActivity.USER_FAVORITES)
				.putExtra("username", adapt.user.getScreenName());
				context.startActivity(intent);
			}
		}
		
		@Override
		public void onStart() {
			super.onStart();
			setRetainInstance(true);
			screenName = getArguments().getString("query");
			reloadAdapter(true);
		}

		@Override
		public void performRefresh(final boolean paginate) {
			if(context == null || isLoading || adapt == null) return;	
			isLoading = true;
			if(adapt.getCount() == 0 && getView() != null) setListShown(false);
			new Thread(new Runnable() {
				public void run() {
					final Account acc = AccountService.getCurrentAccount();
					if(acc != null) {
						try {
							User temp = null;
							if(screenName.equals(acc.getUser().getScreenName())) {
								temp = acc.getClient().verifyCredentials();
								acc.setUser(temp);
								ArrayList<Account> accs = AccountService.getAccounts();
								for(int i = 0; i < accs.size(); i++) {
									if(accs.get(i).getId() == acc.getId()) AccountService.setAccount(i, acc);
								}
							} else temp = acc.getClient().showUser(screenName);
							final User user = temp;
							context.runOnUiThread(new Runnable() {
								public void run() {
									((ProfileScreen)context).user = user;
									((ProfileScreen)context).invalidateOptionsMenu();
									adapt.setUser(user);
								}
							});
						} catch(TwitterException e) {
							e.printStackTrace();
							context.runOnUiThread(new Runnable() {
								public void run() { 
									setEmptyText(context.getString(R.string.error_str));
								}
							});
						}
					}
					context.runOnUiThread(new Runnable() {
						public void run() { 
							if(getView() != null) setListShown(true);
							isLoading = false;
						}
					});
				}
			}).start();
		}

		@Override
		public void reloadAdapter(boolean firstInitialize) {
			if(AccountService.getCurrentAccount() != null) {
				adapt = new ProfileAboutAdapter(context);
				setListAdapter(adapt);
				if(adapt.getCount() == 0) performRefresh(false);
			}
		}

		@Override
		public void savePosition() { }
		
		@Override
		public void restorePosition() { }

		@Override
		public void jumpTop() {
			if(getView() != null) getListView().setSelectionFromTop(0, 0);
		}

		@Override
		public void filter() {
			// TODO Auto-generated method stub

		}
	}
}