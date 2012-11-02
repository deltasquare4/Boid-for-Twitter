package com.teamboid.twitter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.teamboid.twitter.columns.ColumnCacheManager;
import com.teamboid.twitter.listadapters.MessageConvoAdapter.DMConversation;
import com.teamboid.twitterapi.search.Tweet;
import com.teamboid.twitterapi.status.Status;
import com.teamboid.twitterapi.user.User;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
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
		public void reloadAdapter(boolean b);
	}
	public abstract class BoidAdapter<T> extends ArrayAdapter<T>{
		public BoidAdapter(Context context) {
			super(context, 0);
		}
		public abstract long getItemId(int position);
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
		public BoidAdapter<T> adapter;
		
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
		
		@Override
		public void onStart() {
			if(getActivity() == null) return;
			
			new Thread(new Runnable(){

				@SuppressWarnings("unchecked")
				@Override
				public void run() {
					setupAdapter();
					
					// Try and load a cached result if we have one
					final List<Serializable> contents = ColumnCacheManager.getCache(getActivity(), getColumnName());
					if(contents != null){
						adapter.addAll((Collection<? extends T>) contents);
						adapter.notifyDataSetChanged();
					} else{
						setLoading(true);
						T[] t = fetch(-1);
						if(t != null){
							adapter.addAll(t);
							setLoading(false);
							adapter.notifyDataSetChanged();
							
							if(cacheContents()){
								ArrayList<T> y = new ArrayList<T>();
								for(T x : t){
									y.add(x);
								}
								saveCachedContents(y);
							}
						}
					}
				}
			}).start();
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
		
		public void performRefresh(){
			setLoading(true);
			new Thread(new Runnable(){
				public void run(){
					T[] t = fetch(-1);
					if(t != null){
						adapter.clear();
						adapter.addAll(t);
					}
				}
			}).start();
		}
		
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
		}
	}
		/*
		public List<Serializable> statusToSerializableArray(
				ArrayList<Status> data) {
			List<Serializable> ret = new ArrayList<Serializable>();
			for(Status s : data){
				ret.add(s);
			}
			return ret;
		}
		
		boolean hasLoaded = false;
		
		public void onDisplay() {
			if(!hasLoaded) {
				new Thread(new Runnable(){

					@Override
					public void run() {
						final List<Serializable> contents = ColumnCacheManager.getCache(getActivity(), getColumnName());
						
						if(getActivity() != null){
							getActivity().runOnUiThread(new Runnable(){
	
								@Override
								public void run() {
									reloadAdapter(true);
									
									if(contents != null){
										showCachedContents(contents);
										Log.d("boid", getColumnName() + " loaded from cache :)");
									} else{ performRefresh(false); }
									
									onReadyToLoad();
								}
								
							});
						}
					}
					
				}).start();
				hasLoaded = true;
			}
		};
		public void onReadyToLoad() {};

		

		public abstract void performRefresh(boolean paginate);

		public abstract void reloadAdapter(boolean firstInitialize);

		public abstract void savePosition();

		public abstract void restorePosition();

		public abstract void jumpTop();

		public abstract void filter();

		public abstract Status[] getSelectedStatuses();

		public abstract User[] getSelectedUsers();

		public abstract Tweet[] getSelectedTweets();

		public abstract DMConversation[] getSelectedMessages();

		
		
		
	}
*/
	public static abstract class BaseSpinnerFragment extends ListFragment
			implements IBoidFragment {
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