package com.teamboid.twitter.columns;

import twitter4j.GeoLocation;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Tweet;
import twitter4j.TwitterException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.teamboid.twitter.Account;
import com.teamboid.twitter.ComposerScreen;
import com.teamboid.twitter.R;
import com.teamboid.twitter.TimelineScreen;
import com.teamboid.twitter.TweetViewer;
import com.teamboid.twitter.TabsAdapter.BaseSpinnerFragment;
import com.teamboid.twitter.listadapters.SearchFeedListAdapter;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;

/**
 * Represents the column that gets your current location and makes a search
 * query to get Tweets sent by people that are close to your current location.
 * 
 * @author Aidan Follestad
 */
public class NearbyFragment extends BaseSpinnerFragment
{

	private SearchFeedListAdapter adapt;
	private TimelineScreen context;
	public static final String ID = "COLUMNTYPE:NEARBY";
	public GeoLocation location;
	private String radius;
	private boolean isGettingLocation;

	@Override
	public void onAttach(Activity act)
	{
		super.onAttach(act);
		context = (TimelineScreen) act;
	}

	private boolean filterSelected;

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);
		Tweet tweet = (Tweet) adapt.getItem(position);
		context.startActivity(new Intent(context, TweetViewer.class)
				.putExtra("tweet_id", id)
				.putExtra("user_name", tweet.getFromUserName())
				.putExtra("user_id", tweet.getFromUserId())
				.putExtra("screen_name", tweet.getFromUser())
				.putExtra("content", tweet.getText())
				.putExtra("timer", tweet.getCreatedAt().getTime())
				.putExtra("via", tweet.getSource())
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
	}

	@Override
	public void onStart()
	{
		super.onStart();
		getListView().setOnScrollListener(new AbsListView.OnScrollListener()
		{
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState)
			{
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount)
			{
				if (totalItemCount > 0
						&& (firstVisibleItem + visibleItemCount) >= totalItemCount
						&& totalItemCount > visibleItemCount)
				{
					performRefresh(true);
				}
				if (firstVisibleItem == 0
						&& context.getActionBar().getTabCount() > 0)
				{
					if (!PreferenceManager.getDefaultSharedPreferences(context)
							.getBoolean("enable_iconic_tabs", true))
						context.getActionBar()
								.getTabAt(getArguments().getInt("tab_index"))
								.setText(R.string.nearby_str);
					else
						context.getActionBar()
								.getTabAt(getArguments().getInt("tab_index"))
								.setText("");
				}
			}
		});
		getListView().setOnItemLongClickListener(
				new AdapterView.OnItemLongClickListener()
				{
					@Override
					public boolean onItemLongClick(AdapterView<?> arg0,
							View arg1, int index, long id)
					{
						Tweet item = (Tweet) adapt.getItem(index);
						context.startActivity(new Intent(context,
								ComposerScreen.class)
								.putExtra("reply_to", item.getId())
								.putExtra("reply_to_name", item.getFromUser())
								.putExtra("append",
										Utilities.getAllMentions(item))
								.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
						return false;
					}
				});
		final ArrayAdapter<String> spinAdapt = new ArrayAdapter<String>(
				context, R.layout.spinner_item);
		spinAdapt.addAll(context.getResources().getStringArray(
				R.array.nearby_distances));
		filterSelected = true;
		getSpinner().setOnItemSelectedListener(
				new AdapterView.OnItemSelectedListener()
				{
					@Override
					public void onItemSelected(AdapterView<?> arg0, View arg1,
							int index, long arg3)
					{
						if (filterSelected)
							return;
						PreferenceManager.getDefaultSharedPreferences(context)
								.edit().putInt("last_nearby_range", index)
								.apply();
						radius = getSpinner().getSelectedItem().toString();
						radius = radius.substring(radius.indexOf(" ") + 1);
						performRefresh(false);
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0)
					{
					}
				});
		getSpinner().setAdapter(spinAdapt);
		filterSelected = false;
		getSpinner().setSelection(
				PreferenceManager.getDefaultSharedPreferences(context).getInt(
						"last_nearby_range", 0));
		setRetainInstance(true);
		reloadAdapter(true);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		if (adapt.getCount() == 0)
			performRefresh(false);
	}

	private void getLocation()
	{
		if (isGettingLocation)
			return;
		isGettingLocation = true;
		final LocationManager locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		LocationListener locationListener = new LocationListener()
		{
			@Override
			public void onLocationChanged(Location loc)
			{
				locationManager.removeUpdates(this);
				isGettingLocation = false;
				location = new GeoLocation(loc.getLatitude(),
						loc.getLongitude());
				performRefresh(false);
			}

			@Override
			public void onStatusChanged(String provider, int status,
					Bundle extras)
			{
			}

			@Override
			public void onProviderEnabled(String provider)
			{
			}

			@Override
			public void onProviderDisabled(String provider)
			{
			}
		};
		locationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
	}

	@Override
	public void performRefresh(final boolean paginate)
	{
		if (context == null || isLoading || adapt == null && getView() != null)
			return;
		else if (location == null)
		{
			getLocation();
			return;
		}
		isLoading = true;
		if (!paginate)
			adapt.clear();
		if (getView() != null)
			adapt.setLastViewed(getListView());
		if (adapt.getCount() == 0)
			setListShown(false);
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				Query q = new Query("geocode:"
						+ Double.toString(location.getLatitude()) + ","
						+ Double.toString(location.getLongitude()) + ","
						+ radius);
				if (paginate)
					q.setMaxId(adapt.getItemId(adapt.getCount() - 1));
				final Account acc = AccountService.getCurrentAccount();
				if (acc != null)
				{
					try
					{
						final QueryResult feed = acc.getClient().search(q);
						context.runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								if (getView() != null)
									setEmptyText(context
											.getString(R.string.no_results));
								int beforeLast = adapt.getCount() - 1;
								int addedCount = adapt.add(feed.getTweets()
										.toArray(new Tweet[0]));
								if (addedCount > 0 || beforeLast > 0)
								{
									if (getView() != null)
									{
										if (paginate && addedCount > 0)
											getListView()
													.smoothScrollToPosition(
															beforeLast + 1);
										else
											adapt.restoreLastViewed(getListView());
									}
									if (!PreferenceManager
											.getDefaultSharedPreferences(
													context).getBoolean(
													"enable_iconic_tabs", true))
									{
										context.getActionBar()
												.getTabAt(
														getArguments().getInt(
																"tab_index"))
												.setText(
														context.getString(R.string.nearby_str)
																+ " ("
																+ Integer
																		.toString(addedCount)
																+ ")");
									}
									else
										context.getActionBar()
												.getTabAt(
														getArguments().getInt(
																"tab_index"))
												.setText(
														Integer.toString(addedCount));
								}
							}
						});
					}
					catch (final TwitterException e)
					{
						e.printStackTrace();
						context.runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								setEmptyText(context
										.getString(R.string.error_str));
								Toast.makeText(context, e.getErrorMessage(),
										Toast.LENGTH_SHORT).show();
							}
						});
					}
				}
				context.runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						setListShown(true);
						isLoading = false;
					}
				});
			}
		}).start();
	}

	@Override
	public void reloadAdapter(boolean firstInitialize)
	{
		if (AccountService.getCurrentAccount() != null)
		{
			if (adapt != null && !firstInitialize && getView() != null)
				adapt.setLastViewed(getListView());
			adapt = AccountService.getNearbyAdapter(context);
			if (getView() != null)
				adapt.list = getListView();
			setListAdapter(adapt);
			if (adapt.getCount() == 0)
				performRefresh(false);
			else
				adapt.restoreLastViewed(getListView());
		}
	}

	@Override
	public void savePosition()
	{
		if (getView() != null && adapt != null)
			adapt.setLastViewed(getListView());
	}

	@Override
	public void restorePosition()
	{
		if (getView() != null && adapt != null)
			adapt.restoreLastViewed(getListView());
	}

	@Override
	public void jumpTop()
	{
		if (getView() != null)
			getListView().setSelectionFromTop(0, 0);
	}

	@Override
	public void filter()
	{
	}
}
