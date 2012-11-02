package com.teamboid.twitter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.handlerexploit.prime.ImageManager.LowPriorityThreadFactory;
import com.teamboid.twitter.cab.TimelineCAB;
import com.teamboid.twitter.columns.ColumnCacheManager;
import com.teamboid.twitter.columns.MentionsFragment;
import com.teamboid.twitter.listadapters.MessageConvoAdapter.DMConversation;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitterapi.search.Tweet;
import com.teamboid.twitterapi.status.Status;
import com.teamboid.twitterapi.user.User;
import com.teamboid.twitterapi.utilities.Utils;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * The adapter used for columns in the TimelineScreen.
 * 
 * @author Aidan Follestad
 */
public class TabsAdapter extends TaggedFragmentAdapter {

	private final Activity mContext;
	private final ActionBar mActionBar;
	public final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

	static final class TabInfo {
		private final Class<?> clss;
		private final Bundle args;
		public boolean aleadySelected;

		TabInfo(Class<?> _class, Bundle _args) {
			clss = _class;
			args = _args;
		}
	}

	/**
	 * Get the Fragment that's live RIGHT NOW! :D
	 * 
	 * @author kennydude
	 * @param pos
	 * @return
	 */
	public Fragment getLiveItem(Integer pos) {
		return mContext.getFragmentManager().findFragmentByTag("page:" + pos);
	}

	public TabsAdapter(Activity activity) {
		super(activity.getFragmentManager());
		mContext = activity;
		mActionBar = activity.getActionBar();
	}

	public void addTab(ActionBar.Tab tab, Class<?> clss, int index) {
		Bundle args = new Bundle();
		args.putInt("tab_index", index);
		TabInfo info = new TabInfo(clss, args);
		tab.setTag(info);
		mTabs.add(info);
		mActionBar.addTab(tab);
		notifyDataSetChanged();
	}

	public void addTab(ActionBar.Tab tab, Class<?> clss, int index, String query) {
		Bundle args = new Bundle();
		args.putInt("tab_index", index);
		if (query != null)
			args.putString("query", query);
		TabInfo info = new TabInfo(clss, args);
		tab.setTag(info);
		mTabs.add(info);
		mActionBar.addTab(tab);
		notifyDataSetChanged();
	}

	public void addTab(ActionBar.Tab tab, Class<?> clss, int index,
			String screenName, boolean manualRefresh) {
		Bundle args = new Bundle();
		args.putInt("tab_index", index);
		if (screenName != null)
			args.putString("screenName", screenName);
		args.putBoolean("manualRefresh", manualRefresh);
		TabInfo info = new TabInfo(clss, args);
		tab.setTag(info);
		mTabs.add(info);
		mActionBar.addTab(tab);
		notifyDataSetChanged();
	}

	public void addTab(ActionBar.Tab tab, Class<?> clss, int index,
			String listName, int listId) {
		Bundle args = new Bundle();
		args.putInt("tab_index", index);
		if (listName != null)
			args.putString("list_name", listName);
		if (listId > 0)
			args.putInt("list_id", listId);
		TabInfo info = new TabInfo(clss, args);
		tab.setTag(info);
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
	public int getCount() {
		return mTabs.size();
	}

	@Override
	public Fragment getItem(int position) {
		TabInfo info = mTabs.get(position);
		return Fragment.instantiate(mContext, info.clss.getName(), info.args);
	}

	public interface IBoidFragment {
		public boolean isRefreshing();
		public void onDisplay();
		public void performRefresh();
	}
	public static abstract class BoidAdapter<T> extends ArrayAdapter<T>{
		public BoidAdapter(Context context) {
			super(context, 0);
		}
		public abstract long getItemId(int position);
		/**
		 * Reverse of getItemId()
		 * @param id ID returned by getItemId()
		 * @return
		 */
		public abstract int getPosition(long id);
	}

	/**
	 * Basic List Fragment
	 * 
	 * 10000x more simplistic!
	 * 
	 * Basically, there is going to be less code in the actual fragments (no more copy and pasting thread work)
	 * which will make them easier to change and fix issues with.
	 * 
	 * @author kennydude
	 */
	public static abstract class BaseListFragment<T extends Serializable> extends ListFragment
			implements IBoidFragment {
		private static ExecutorService execService = Executors.newCachedThreadPool(new LowPriorityThreadFactory());
		
		@SuppressWarnings("unchecked")
		public BoidAdapter<T> getAdapter(){
			return (BoidAdapter<T>) getListView().getAdapter();
		}
		
		// Abstracts
		public abstract String getColumnName();
		public abstract Status[] getSelectedStatuses();
		public abstract User[] getSelectedUsers();
		public abstract Tweet[] getSelectedTweets();
		public abstract DMConversation[] getSelectedMessages();
		public abstract void setupAdapter();
		// Fetches data from network; DO NOT THREAD!!!!!!! max_id may be -1
		public abstract T[] fetch( long maxId );
		
		// Overriden by profile screens/not as frequently used parts
		public boolean cacheContents(){ return true; }
		
		@SuppressWarnings("unchecked")
		public void saveCachedContents(List<T> contents){
			ColumnCacheManager.saveCache(getActivity(), getColumnName(), (List<Serializable>) contents);
		}
		
		long getCurrentTop(){
			return getAdapter().getItemId( getListView().getFirstVisiblePosition() );
		}
		
		@Override
		public void onStart() {
			super.onStart();
			if(getActivity() == null) return;
			
			getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) {
				}

				@Override
				public void onScroll(AbsListView view, int firstVisibleItem,
						int visibleItemCount, int totalItemCount) {
					if (totalItemCount > 0
							&& (firstVisibleItem + visibleItemCount) >= totalItemCount
							&& totalItemCount > visibleItemCount) {
						loadMore();
					}
					if (firstVisibleItem == 0
							&& getActivity().getActionBar().getTabCount() > 0) {
						if (!PreferenceManager.getDefaultSharedPreferences(getActivity())
								.getBoolean("enable_iconic_tabs", true)) {
							getActivity().getActionBar()
									.getTabAt(getArguments().getInt("tab_index"))
									.setText(R.string.mentions_str);
						} else {
							getActivity().getActionBar()
									.getTabAt(getArguments().getInt("tab_index"))
									.setText("");
						}
					}
				}
			});
			
			execService.execute(new Runnable(){

				@SuppressWarnings("unchecked")
				@Override
				public void run() {
					setupAdapter();
					
					// Try and load a cached result if we have one
					final List<Serializable> contents = ColumnCacheManager.getCache(getActivity(), getColumnName());
					if(contents != null){
						getAdapter().addAll((Collection<? extends T>) contents);
						if(getAdapter().getFilter() != null)
							getAdapter().getFilter().filter("");
						getAdapter().notifyDataSetChanged();
					} else{
						performRefresh();
					}
				}
			});
		}
		
		public boolean isLoading;
		
		public void setLoading(boolean loading){
			isLoading = loading;
			getActivity().invalidateOptionsMenu();
		}

		@Override
		public boolean isRefreshing() {
			return isLoading;
		}
		
		public void onDisplay() {
			// todo - when swished to
		}
		
		public void showError(final String message){
			if(getActivity() == null) return; 
			getActivity().runOnUiThread(new Runnable(){

				@Override
				public void run() {
					final TextView tv = (TextView) (getView().findViewById(R.id.error));
					tv.setText(message);
					tv.setAlpha(1);
					tv.setVisibility(View.VISIBLE);
					tv.animate().setStartDelay(3000).setListener(new AnimatorListener(){
						@Override public void onAnimationCancel(Animator arg0) {}
						@Override public void onAnimationRepeat(Animator arg0) {}
						@Override public void onAnimationStart(Animator arg0) {}
						
						@Override public void onAnimationEnd(Animator arg0) {
							tv.setVisibility(View.GONE);
						}
						
					}).alpha(0);
				}
				
			});
		}
		
		public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
			return inflater.inflate(R.layout.list_content, container, false); 
		}

		@Override
		public void setEmptyText(CharSequence text) {
			if (getView() != null)
				((TextView)getView().findViewById(android.R.id.empty)).setText(text);
		}
		
		@Override
		public void setListShown(boolean shown) {
			View mProgressContainer = getView().findViewById(R.id.progressContainer);
			View mListContainer = getView().findViewById(R.id.listContainer);
			if (shown) {
				mProgressContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
				mListContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
				mProgressContainer.setVisibility(View.GONE);
				mListContainer.setVisibility(View.VISIBLE);
			} else {
				mProgressContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
				mListContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
				mProgressContainer.setVisibility(View.VISIBLE);
				mListContainer.setVisibility(View.GONE);
			}
		}
		
		public void loadMore(){
			if(isLoading == true) return; // No double loading for you!
			setLoading(true);
			execService.execute(new Runnable(){

				@Override
				public void run() {
					T[] t = fetch( getAdapter().getItemId( getAdapter().getCount() - 1 ) );
					setLoading(false);
					if(t != null){
						getAdapter().addAll(t);
						if(getAdapter().getFilter() != null)
							getAdapter().getFilter().filter("");
						getAdapter().notifyDataSetChanged();
					}
				}
				
			});
		}
		
		public void performRefresh(){
			setLoading(true);
			execService.execute(new Runnable(){
				public void run(){
					T[] t = fetch(-1);
					setLoading(false);
					if(t != null){
						long id = getCurrentTop();
						
						getAdapter().clear();
						getAdapter().addAll(t);
						if(getAdapter().getFilter() != null)
							getAdapter().getFilter().filter("");
						getAdapter().notifyDataSetChanged();
						
						getListView().setSelection( getAdapter().getPosition(id) );
						
						if(cacheContents()){
							ArrayList<T> y = new ArrayList<T>();
							for(T x : t){
								y.add(x);
							}
							saveCachedContents(y);
						}
						if(t.length > 0){
							// Set the tab unread count
							if (!PreferenceManager
									.getDefaultSharedPreferences(getActivity()).getBoolean(
											"enable_iconic_tabs", true)) {
								getActivity().getActionBar()
										.getTabAt(
												getArguments().getInt(
														"tab_index"))
										.setText(
												getActivity().getString(R.string.mentions_str)
														+ " ("
														+ Integer
																.toString(t.length)
														+ ")");
							} else {
								getActivity().getActionBar()
										.getTabAt(
												getArguments().getInt(
														"tab_index"))
										.setText(
												Integer.toString(t.length));
							}
						}
					}
				}
			});
		}
		
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
		}
		
		public void jumpTop() {
			if (getView() != null)
				getListView().setSelectionFromTop(0, 0);
		}
		
	}
	
	// Timeline shared stuff
	public static abstract class BaseTimelineFragment extends BaseListFragment<Status>{
		public abstract String getAdapterId();
		
		@Override
		public void onListItemClick(ListView l, View v, int index, long id) {
			super.onListItemClick(l, v, index, id);
			Status tweet = (Status) getAdapter().getItem(index);
			if (tweet.isRetweet())
				tweet = tweet.getRetweetedStatus();
			getActivity().startActivity(new Intent(getActivity(), TweetViewer.class).putExtra(
					"sr_tweet", Utils.serializeObject(tweet)).addFlags(
					Intent.FLAG_ACTIVITY_CLEAR_TOP));
		}
		
		@Override
		public Status[] getSelectedStatuses() {
			if (getAdapter() == null && getView() == null) {
				Log.d("BOID CAB",
						"Adapter or view is null, getSelectedStatuses() cancelled...");
				return null;
			}
			ArrayList<Status> toReturn = new ArrayList<Status>();
			SparseBooleanArray choices = getListView().getCheckedItemPositions();
			for (int i = 0; i < choices.size(); i++) {
				if (choices.valueAt(i)) {
					toReturn.add((Status) getAdapter().getItem(choices.keyAt(i)));
				}
			}
			Log.d("BOID CAB", "getSelectedStatuses() returning " + toReturn.size()
					+ " items!");
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

		@Override
		public void setupAdapter(){
			getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
			getListView().setMultiChoiceModeListener(TimelineCAB.choiceListener);
			if (AccountService.getCurrentAccount() != null) {
				setListAdapter( AccountService.getFeedAdapter(getActivity(), getAdapterId(),
									AccountService.getCurrentAccount().getId()) );
			}
		}
	}
	
	public static abstract class BaseSpinnerFragment extends ListFragment
			implements IBoidFragment {
		// TODO: Should inherit BaseListFragment
		
		public void onDisplay() {
		};

		public boolean isLoading;
		private boolean isShown;

		@Override
		public boolean isRefreshing() {
			return isLoading;
		}

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
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			return inflater.inflate(R.layout.spinner_list_fragment, container,
					false);
		}

		public Spinner getSpinner() {
			if (getView() == null)
				return null;
			return (Spinner) getView().findViewById(R.id.fragSpinner);
		}

		@Override
		public void setEmptyText(CharSequence text) {
			if (getView() == null)
				return;
			((TextView) getView().findViewById(android.R.id.empty))
					.setText(text);
		}

		@Override
		public void setListShown(boolean visible) {
			if (getView() == null)
				return;
			isShown = visible;
			getView().findViewById(android.R.id.progress).setVisibility(
					(visible == false) ? View.VISIBLE : View.GONE);
			getListView().setVisibility(
					(visible == true) ? View.VISIBLE : View.GONE);
			boolean condition = (getListAdapter() == null || getListAdapter()
					.isEmpty()) && isShown;
			getView().findViewById(android.R.id.empty).setVisibility(
					condition ? View.VISIBLE : View.GONE);
		}
	}

	public static abstract class BaseGridFragment extends Fragment implements
			IBoidFragment {

		public boolean isLoading = false;
		private boolean isShown;

		@Override
		public boolean isRefreshing() {
			return isLoading;
		}

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
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			return inflater.inflate(R.layout.grid_activity, container, false);
		}

		public void setListShown(boolean shown) {
			if (getView() == null)
				return;
			isShown = shown;
			getView().findViewById(android.R.id.list).setVisibility(
					(shown == true) ? View.VISIBLE : View.GONE);
			getView().findViewById(android.R.id.progress).setVisibility(
					(shown == false) ? View.VISIBLE : View.GONE);
			boolean condition = (getGridView().getAdapter() == null || getGridView()
					.getAdapter().isEmpty()) && isShown;
			getView().findViewById(android.R.id.empty).setVisibility(
					condition ? View.VISIBLE : View.GONE);
		}

		public GridView getGridView() {
			if (getView() == null)
				return null;
			return (GridView) getView().findViewById(android.R.id.list);
		}

		public void setEmptyText(String text) {
			if (getView() == null)
				return;
			((TextView) getView().findViewById(android.R.id.empty))
					.setText(text);
		}

		public void setListAdapter(ListAdapter adapt) {
			if (getView() == null)
				return;
			((ListView) getView().findViewById(android.R.id.list))
					.setAdapter(adapt);
		}
	}

}