package com.teamboid.columns;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.teamboid.twitter.Account;
import com.teamboid.twitter.AccountService;
import com.teamboid.twitter.MediaFeedListAdapter;
import com.teamboid.twitter.ProfileScreen;
import com.teamboid.twitter.R;
import com.teamboid.twitter.TweetViewer;
import com.teamboid.twitter.Utilities;
import com.teamboid.twitter.MediaFeedListAdapter.MediaFeedItem;
import com.teamboid.twitter.TabsAdapter.BaseGridFragment;

public class MediaTimelineFragment extends BaseGridFragment {

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
			public void onScrollStateChanged(AbsListView view,
					int scrollState) {
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				if (totalItemCount > 0
						&& (firstVisibleItem + visibleItemCount) >= totalItemCount)
					performRefresh(true);
				if (firstVisibleItem == 0
						&& context.getActionBar().getTabCount() > 0) {
					if (!PreferenceManager.getDefaultSharedPreferences(
							context).getBoolean("enable_iconic_tabs", true))
						context.getActionBar()
						.getTabAt(
								getArguments().getInt("tab_index"))
								.setText(R.string.media_title);
					else
						context.getActionBar()
						.getTabAt(
								getArguments().getInt("tab_index"))
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
				} else {
					final ProgressDialog pd = new ProgressDialog(context);
					pd.setMessage(context.getString(R.string.loading_str));
					pd.show();
					new Thread(new Runnable() {

						@Override
						public void run() {
							try {
								HttpClient httpclient = new DefaultHttpClient();

								String url = "http://api.twicsy.com/pic/"
										+ Uri.encode(tweet.twicsy_id)
										+ "?max=1";
								HttpGet g = new HttpGet(url);
								HttpResponse r = httpclient.execute(g);
								if (r.getStatusLine().getStatusCode() == 200) {
									final long tweetId = Long
											.parseLong(new JSONObject(
													EntityUtils.toString(r
															.getEntity()))
											.getJSONArray("results")
											.getJSONObject(0)
											.getString(
													"twitterStatusId"));
									context.runOnUiThread(new Runnable() {
										@Override
										public void run() {
											viewTweet(tweetId);
										}
									});
								} else {
									throw new Exception("Non 200 response");
								}
							} catch (Exception e) {
								e.printStackTrace();
								Toast.makeText(context, R.string.error_str,
										Toast.LENGTH_SHORT).show();
							}
							pd.dismiss();

						}

					}).start();
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

	private int pageSkips;

	@Override
	public void performRefresh(final boolean paginate) {
		if (context == null || isLoading || adapt == null)
			return;
		isLoading = true;
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
				Paging paging = new Paging(1, 50);
				if (paginate) paging.setMaxId(adapt.getItemId(adapt.getCount() - 1));
				final Account acc = AccountService.getCurrentAccount();
				if (acc != null) {
					try {
						if (screenName != null) {
							// Powered by Twicsy
							try {
								HttpClient httpclient = new DefaultHttpClient();
								String url = "http://api.twicsy.com/user/"
										+ Uri.encode(screenName);
								if (paging.getMaxId() > 0)
									url += "/skip/" + paging.getMaxId();
								HttpGet g = new HttpGet(url);
								HttpResponse r = httpclient.execute(g);
								if (r.getStatusLine().getStatusCode() == 200) {
									JSONObject jo = new JSONObject(EntityUtils.toString(r.getEntity()));
									JSONArray results = jo.getJSONArray("results");
									int i = 0;
									while (i < results.length()) {
										final JSONObject result = results.getJSONObject(i);
										i++;
										context.runOnUiThread(new Runnable() {

											@Override
											public void run() {
												MediaFeedItem m = new MediaFeedItem();
												try {
													m.imgurl = result
															.getString("thumb");
													m.twicsy_id = result
															.getString("id");
													adapt.add(
															new MediaFeedItem[] { m },
															MediaTimelineFragment.this);
													// WARN: Should be
													// safer. Persuming
													// ProfileScreen
													if (adapt.getCount() == 1) {
														((ProfileScreen) context)
														.setupMediaView();
													}
												} catch (JSONException e) {
													e.printStackTrace();
												}
											}

										});
									}
								} else throw new Exception("non-200 response code");
							} catch (Exception e) {
								e.printStackTrace();
								context.runOnUiThread(new Runnable() {
									@Override
									public void run() {
										setEmptyText(context
												.getString(R.string.error_str));
									}
								});
							}
						} else {
							ResponseList<Status> temp = acc.getClient().getHomeTimeline(paging);
							for (final Status p : temp) {
								if (Utilities.getTweetYFrogTwitpicMedia(p) != null) {
									context.runOnUiThread(new Runnable() {
										@Override
										public void run() {
											MediaFeedItem m = new MediaFeedItem();
											m.imgurl = Utilities
													.getTweetYFrogTwitpicMedia(p);
											m.tweet_id = p.getId();
											adapt.add(
													new MediaFeedItem[] { m },
													MediaTimelineFragment.this);
										}
									});
								}
							}
						}
					} catch (final TwitterException e) {
						e.printStackTrace();
						context.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								setEmptyText(context
										.getString(R.string.error_str));
								Toast.makeText(context,
										e.getErrorMessage(),
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
				if (((ProfileScreen) context).mediaAdapter == null) {
					((ProfileScreen) context).mediaAdapter = new MediaFeedListAdapter(
							context, null, AccountService
							.getCurrentAccount().getId());
				}
				adapt = ((ProfileScreen) context).mediaAdapter;
			} else {
				adapt = AccountService.getMediaFeedAdapter(context,
						MediaTimelineFragment.ID, AccountService
						.getCurrentAccount().getId());
			}
			getGridView().setAdapter(adapt);
			if (getView() != null) {
				if (adapt.getCount() > 0) {
					getView().findViewById(android.R.id.empty)
					.setVisibility(View.GONE);
					adapt.restoreLastViewed(getGridView());
					filter();
				} else {
					getView().findViewById(android.R.id.empty)
					.setVisibility(View.VISIBLE);
					if (!manualRefresh)
						performRefresh(false);
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
	public void filter() {
	}
}
