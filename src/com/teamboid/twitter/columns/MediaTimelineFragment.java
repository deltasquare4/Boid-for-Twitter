package com.teamboid.twitter.columns;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.teamboid.twitter.Account;
import com.teamboid.twitter.R;
import com.teamboid.twitter.TweetViewer;
import com.teamboid.twitter.TabsAdapter.BaseGridFragment;
import com.teamboid.twitter.listadapters.MediaFeedListAdapter;
import com.teamboid.twitter.listadapters.MediaFeedListAdapter.MediaFeedItem;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;
import com.teamboid.twitterapi.client.Paging;
import com.teamboid.twitterapi.status.Status;

/**
 * Represents the column that acts like the {@link TimelineFragment}, but pulls
 * media out and displays the pictures in tiles.
 * 
 * @author Aidan Follestad
 */
public class MediaTimelineFragment extends BaseGridFragment {

	private Activity context;
	private MediaFeedListAdapter adapt;
	public static final String ID = "COLUMNTYPE:MEDIATIMELINE";
	private String screenName;
	private boolean manualRefresh;

	public MediaFeedListAdapter getAdapter() {
		return adapt;
	}

	@Override
	public void onAttach(Activity act) {
		super.onAttach(act);
		context = act;
	}

	@Override
	public void onActivityCreated(Bundle b) {
		super.onActivityCreated(b);
		Log.d("boid", "onActivityCreated()");
		isLoading = false;
		
		GridView grid = getGridView();
		grid.setOnScrollListener(new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				if (totalItemCount > 0
						&& (firstVisibleItem + visibleItemCount) >= totalItemCount)
					performRefresh(true);
				if (firstVisibleItem == 0
						&& context.getActionBar().getTabCount() > 0) {
					if (!PreferenceManager.getDefaultSharedPreferences(context)
							.getBoolean("enable_iconic_tabs", true))
						context.getActionBar()
								.getTabAt(getArguments().getInt("tab_index"))
								.setText(R.string.media_title);
					else
						context.getActionBar()
								.getTabAt(getArguments().getInt("tab_index"))
								.setText("");
				}
			}
		});
		grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			private void viewTweet(long tweetid) {
				context.startActivity(new Intent(context, TweetViewer.class)
						.putExtra("tweet_id", tweetid).addFlags(
								Intent.FLAG_ACTIVITY_CLEAR_TOP));
			}

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long id) {
				final MediaFeedListAdapter.MediaFeedItem tweet = (MediaFeedListAdapter.MediaFeedItem) adapt
						.getItem(position);
				if (tweet.tweet_id != -1) {
					viewTweet(tweet.tweet_id);
				}
			}
		});
		setRetainInstance(true);
		screenName = (String) getArguments().get("screenName");
		manualRefresh = getArguments().getBoolean("manualRefresh", false);
		reloadAdapter(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedIinstnaceState) {
		return inflater.inflate(R.layout.grid_activity, null);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (getView() != null && adapt != null)
			adapt.restoreLastViewed(getGridView());
		if (manualRefresh)
			setEmptyText(context.getString(R.string.manual_refresh_hint));
	}

	@Override
	public void onPause() {
		super.onPause();
		savePosition();
	}

	int pageSkips = 0;

	@Override
	public void performRefresh(final boolean paginate) {
		if (context == null || isLoading == true || adapt == null)
			return;
		isLoading = true;
		context.invalidateOptionsMenu();
		
		if (getView() != null && adapt != null) {
			adapt.setLastViewed(getGridView());
			if (adapt.getCount() == 0)
				setListShown(false);
		}
		if (!paginate)
			pageSkips = 0;
		new Thread(new Runnable() {
			@Override
			public void run() {
				Paging paging = new Paging(50);
				if (paginate)
					paging.setMaxId(adapt.getItemId(adapt.getCount() - 1));
				final Account acc = AccountService.getCurrentAccount();
				if (acc != null) {
					Status[] tweets = null;
					try {
						if (screenName != null) {
							tweets = acc.getClient().getUserMediaTimeline(
									screenName, paging);
						} else {
							tweets = acc.getClient().getHomeTimeline(paging);
						}

						for (final Status p : tweets) {
							if (Utilities.getTweetYFrogTwitpicMedia(p) != null) {
								context.runOnUiThread(new Runnable() {
									@Override
									public void run() {
										MediaFeedItem m = new MediaFeedItem();
										m.imgurl = Utilities
												.getTweetYFrogTwitpicMedia(p);
										m.tweet_id = p.getId();
										m.content = p.getText();
										adapt.add(new MediaFeedItem[] { m },
												MediaTimelineFragment.this);
									}
								});
							}
						}
						
						context.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								adapt.notifyDataSetChanged();
								isLoading = false;
								context.invalidateOptionsMenu();
							}
						});
						
					} catch (final Exception e) {
						e.printStackTrace();
						context.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								setEmptyText(context
										.getString(R.string.error_str));
								Toast.makeText(context, e.getMessage(),
										Toast.LENGTH_SHORT).show();
							}
						});
					}
					if (pageSkips <= 5)
						return;
				}
				context.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						isLoading = false;
						context.invalidateOptionsMenu();
						setListShown(true);
					}
				});
			}
		}).start();
	}

	@Override
	public void reloadAdapter(boolean firstInitialize) {
		if (AccountService.getCurrentAccount() != null) {
			if (adapt != null && !firstInitialize && getView() != null)
				adapt.setLastViewed(getGridView());
			if (screenName != null && !screenName.trim().isEmpty()) {
				adapt = new MediaFeedListAdapter(context, null, AccountService
						.getCurrentAccount().getId());
			} else {
				adapt = AccountService.getMediaFeedAdapter(context,
						MediaTimelineFragment.ID, AccountService
								.getCurrentAccount().getId());
			}
			getGridView().setAdapter(adapt);
			if (getView() != null) {
				if (adapt.getCount() > 0) {
					getView().findViewById(android.R.id.empty).setVisibility(
							View.GONE);
					adapt.restoreLastViewed(getGridView());
					filter();
				} else {
					getView().findViewById(android.R.id.empty).setVisibility(
							View.VISIBLE);
				}
			}
		}
	}

	@Override
	public void savePosition() {
		if (getView() != null && adapt != null)
			adapt.setLastViewed(getGridView());
	}

	@Override
	public void jumpTop() {
		if (getView() != null)
			getGridView().setSelection(0);
	}

	@Override
	public void filter() { }

	Boolean firstSync = false;
	
	@Override
	public void onDisplay() {
		if(!firstSync){
			performRefresh(false);
			firstSync = true;
		}
	}
}
