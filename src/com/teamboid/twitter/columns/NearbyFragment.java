package com.teamboid.twitter.columns;

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
import com.teamboid.twitterapi.client.Paging;
import com.teamboid.twitterapi.search.GeoCode;
import com.teamboid.twitterapi.search.SearchQuery;
import com.teamboid.twitterapi.search.SearchResult;
import com.teamboid.twitterapi.search.Tweet;
import com.teamboid.twitterapi.status.GeoLocation;

/**
 * Represents the column that gets your current location and makes a search
 * query to get Tweets sent by people that are close to your current location.
 * 
 * @author Aidan Follestad
 */
public class NearbyFragment extends BaseSpinnerFragment {

	private SearchFeedListAdapter adapt;
	private TimelineScreen context;
	public static final String ID = "COLUMNTYPE:NEARBY";
	public GeoLocation location;
	private int selectedIndex;
	private boolean isGettingLocation;

	@Override
	public void onAttach(Activity act) {
		super.onAttach(act);
		context = (TimelineScreen) act;
	}

	private boolean filterSelected;

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Tweet tweet = (Tweet) adapt.getItem(position);
		context.startActivity(new Intent(context, TweetViewer.class)
				.putExtra("tweet_id", id)
				.putExtra("user_name", tweet.getFromUser())
				.putExtra("user_id", tweet.getFromUserId())
				.putExtra("screen_name", tweet.getFromUser())
				.putExtra("content", tweet.getText())
				.putExtra("timer", tweet.getCreatedAt().getTime())
				.putExtra("via", tweet.getSource())
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
	}

	@Override
	public void onStart() {
		super.onStart();
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
					performRefresh(true);
				}
				if (firstVisibleItem == 0
						&& context.getActionBar().getTabCount() > 0) {
					if (!PreferenceManager.getDefaultSharedPreferences(context)
							.getBoolean("enable_iconic_tabs", true))
						context.getActionBar()
								.getTabAt(getArguments().getInt("tab_index"))
								.setText(R.string.nearby_str);
					else {
						context.getActionBar()
								.getTabAt(getArguments().getInt("tab_index"))
								.setText("");
					}
				}
			}
		});
		getListView().setOnItemLongClickListener(
				new AdapterView.OnItemLongClickListener() {
					@Override
					public boolean onItemLongClick(AdapterView<?> arg0,
							View arg1, int index, long id) {
						Tweet item = (Tweet) adapt.getItem(index);
						context.startActivity(new Intent(context,
								ComposerScreen.class)
								.putExtra("reply_to", item.getId())
								.putExtra("reply_to_name", item.getFromUser())
								.putExtra(
										"append",
										Utilities.getAllMentions(item,
												(int) AccountService
														.getCurrentAccount()
														.getId()))
								.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
						return false;
					}
				});
		final ArrayAdapter<String> spinAdapt = new ArrayAdapter<String>(
				context, R.layout.spinner_item);
		spinAdapt
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinAdapt.addAll(context.getResources().getStringArray(
				R.array.nearby_distances));
		filterSelected = true;
		getSpinner().setOnItemSelectedListener(
				new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> arg0, View arg1,
							int index, long arg3) {
						if (filterSelected)
							return;
						selectedIndex = index;
						PreferenceManager.getDefaultSharedPreferences(context)
								.edit().putInt("last_nearby_range", index)
								.apply();
						performRefresh(false);
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
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
	public void onResume() {
		super.onResume();
		if (adapt.getCount() == 0)
			performRefresh(false);
	}

	private void getLocation() {
		if (isGettingLocation)
			return;
		isGettingLocation = true;
		final LocationManager locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		LocationListener locationListener = new LocationListener() {
			@Override
			public void onLocationChanged(Location loc) {
				locationManager.removeUpdates(this);
				isGettingLocation = false;
				location = new GeoLocation(loc.getLatitude(),
						loc.getLongitude());
				performRefresh(false);
			}

			@Override
			public void onStatusChanged(String provider, int status,
					Bundle extras) {
			}

			@Override
			public void onProviderEnabled(String provider) {
			}

			@Override
			public void onProviderDisabled(String provider) {
			}
		};
		locationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
	}

	@Override
	public void performRefresh(final boolean paginate) {
		if (context == null || isLoading || adapt == null
				&& getSpinner() != null)
			return;
		else if (location == null) {
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
		String[] radiuses = context.getResources().getStringArray(
				R.array.nearby_distances);
		String[] values = context.getResources().getStringArray(
				R.array.nearby_distance_values);
		final String nearbyRadius = radiuses[selectedIndex];
		final int nearbyValue = Integer.parseInt(values[selectedIndex]);
		new Thread(new Runnable() {
			@Override
			public void run() {
				GeoCode.DistanceUnit unit = GeoCode.DistanceUnit.MI;
				if (nearbyRadius.endsWith("km"))
					unit = GeoCode.DistanceUnit.KM;
				GeoCode geo = GeoCode.create(location, nearbyValue, unit);
				Paging paging = new Paging(50);
				if (paginate)
					paging.setMaxId(adapt.getItemId(adapt.getCount() - 1));
				final SearchQuery query = SearchQuery.create(null, paging)
						.setGeoCode(geo);
				final Account acc = AccountService.getCurrentAccount();
				if (acc != null) {
					try {
						final SearchResult feed = acc.getClient().search(query);
						context.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								if (getView() != null) {
									setEmptyText(context
											.getString(R.string.no_results));
								}
								int beforeLast = adapt.getCount() - 1;
								int addedCount = adapt.add(feed.getResults());
								if (addedCount > 0 || beforeLast > 0) {
									if (getView() != null) {
										if (paginate && addedCount > 0) {
											getListView()
													.smoothScrollToPosition(
															beforeLast + 1);
										} else
											adapt.restoreLastViewed(getListView());
									}
									if (!PreferenceManager
											.getDefaultSharedPreferences(
													context).getBoolean(
													"enable_iconic_tabs", true)) {
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
									} else {
										context.getActionBar()
												.getTabAt(
														getArguments().getInt(
																"tab_index"))
												.setText(
														Integer.toString(addedCount));
									}
								}
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
				}
				context.runOnUiThread(new Runnable() {
					@Override
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
		if (AccountService.getCurrentAccount() != null) {
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
	public void savePosition() {
		if (getView() != null && adapt != null)
			adapt.setLastViewed(getListView());
	}

	@Override
	public void restorePosition() {
		if (getView() != null && adapt != null)
			adapt.restoreLastViewed(getListView());
	}

	@Override
	public void jumpTop() {
		if (getView() != null)
			getListView().setSelectionFromTop(0, 0);
	}

	@Override
	public void filter() {
	}
}
