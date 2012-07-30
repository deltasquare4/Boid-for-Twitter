package com.teamboid.twitter;

import java.util.ArrayList;

import com.teamboid.twitter.listadapters.MessageConvoAdapter.DMConversation;
import com.teamboid.twitter.services.AccountService;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import com.teamboid.twitterapi.search.Tweet;
import com.teamboid.twitterapi.status.Status;
import com.teamboid.twitterapi.user.User;

/**
 * The adapter used for columns in the TimelineScreen.
 * 
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
		mViewPager.setOffscreenPageLimit(4);
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
		if (query != null)
			args.putString("query", query);
		TabInfo info = new TabInfo(clss, args);
		tab.setTag(info);
		tab.setTabListener(this);
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
		tab.setTabListener(this);
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
	public int getCount() {
		return mTabs.size();
	}

	@Override
	public Fragment getItem(int position) {
		TabInfo info = mTabs.get(position);
		return Fragment.instantiate(mContext, info.clss.getName(), info.args);
	}

	@Override
	public void onPageScrolled(int position, float positionOffset,
			int positionOffsetPixels) {
	}

	@Override
	public void onPageSelected(int position) {
		mActionBar.setSelectedNavigationItem(position);
		mContext.invalidateOptionsMenu();
	}

	@Override
	public void onPageScrollStateChanged(int state) {
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		if (!filterDefaultColumnSelection) {
			final String prefName = Long.toString(AccountService
					.getCurrentAccount().getId()) + "_default_column";
			PreferenceManager.getDefaultSharedPreferences(mContext).edit()
			.putInt(prefName, tab.getPosition()).apply();
		}
		TabInfo curInfo = mTabs.get(tab.getPosition());
		mViewPager.setCurrentItem(tab.getPosition());
		curInfo.aleadySelected = true;
		mTabs.set(tab.getPosition(), curInfo);
		mContext.invalidateOptionsMenu();
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		if (mTabs.size() == 0 || tab.getPosition() > mTabs.size())
			return;
		TabInfo curInfo = mTabs.get(tab.getPosition());
		curInfo.aleadySelected = false;
		mTabs.set(tab.getPosition(), curInfo);
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		boolean selected = mTabs.get(tab.getPosition()).aleadySelected;
		if (selected) {
			Fragment frag = mContext.getFragmentManager().findFragmentByTag(
					"page:" + tab.getPosition());
			if (frag != null) {
				if (frag instanceof BaseListFragment)
					((BaseListFragment) frag).jumpTop();
				else if (frag instanceof BaseSpinnerFragment)
					((BaseSpinnerFragment) frag).jumpTop();
				else if (frag instanceof BaseGridFragment)
					((BaseGridFragment) frag).jumpTop();
			}
		}
	}
	
	public Fragment getCurrentFragment(){
		return mContext.getFragmentManager().findFragmentByTag("page:" + mViewPager.getCurrentItem());
	}
	
	public interface IBoidFragment{
		public boolean isRefreshing();
	}

	public static abstract class BaseListFragment extends ListFragment implements IBoidFragment {

		public boolean isLoading;
		@Override
		public boolean isRefreshing(){ return isLoading; }

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

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
		}

		@Override
		public void setEmptyText(CharSequence text) {
			if (getView() != null)
				super.setEmptyText(text);
		}
	}

	public static abstract class BaseSpinnerFragment extends ListFragment implements IBoidFragment {

		public boolean isLoading;
		private boolean isShown;
		@Override
		public boolean isRefreshing(){ return isLoading; }

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
			if (getView() == null) return null;
			return (Spinner)getView().findViewById(R.id.fragSpinner);
		}

		@Override
		public void setEmptyText(CharSequence text) {
			if (getView() == null) return;
			((TextView) getView().findViewById(android.R.id.empty)).setText(text);
		}

		@Override
		public void setListShown(boolean visible) {
			if (getView() == null) return;
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

	public static abstract class BaseGridFragment extends Fragment implements IBoidFragment {

		public boolean isLoading;
		private boolean isShown;
		
		@Override
		public boolean isRefreshing(){ return isLoading; }

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