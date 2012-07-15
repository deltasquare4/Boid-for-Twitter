package com.teamboid.twitter.columns;

import java.util.ArrayList;

import twitter4j.GeoLocation;
import twitter4j.ResponseList;
import twitter4j.Trends;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.teamboid.twitter.Account;
import com.teamboid.twitter.R;
import com.teamboid.twitter.SearchScreen;
import com.teamboid.twitter.TabsAdapter.BaseSpinnerFragment;
import com.teamboid.twitter.listadapters.TrendsListAdapter;
import com.teamboid.twitter.services.AccountService;

/**
 * Represents the column that displays current trends. 
 * @author Aidan Follestad
 */
public class TrendsFragment extends BaseSpinnerFragment {

	private TrendsListAdapter adapt;
	private Activity context;
	public static final String ID = "COLUMNTYPE:TRENDS";
	private boolean isGettingLocation;
	public GeoLocation location;

	@Override
	public void onAttach(Activity act) {
		super.onAttach(act);
		context = act;
	}

	private boolean filterSelected;

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		context.startActivity(new Intent(context, SearchScreen.class)
		.putExtra("query", (String) adapt.getItem(position))
		.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
	}

	@Override
	public void onStart() {
		super.onStart();
		setRetainInstance(true);
		setEmptyText(getString(R.string.no_trends));
		final ArrayAdapter<String> spinAdapt = new ArrayAdapter<String>(
				context, R.layout.spinner_item);
		String[] toAdd = context.getResources().getStringArray(
				R.array.trend_sources);
		for (String t : toAdd)
			spinAdapt.add(t);
		filterSelected = true;
		getSpinner().setOnItemSelectedListener(
				new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> arg0,
							View arg1, int index, long arg3) {
						if (filterSelected)
							return;
						PreferenceManager
						.getDefaultSharedPreferences(context)
						.edit().putInt("last_trend_source", index)
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
				PreferenceManager.getDefaultSharedPreferences(context)
				.getInt("last_trend_source", 0));
		reloadAdapter(true);
	}

	@Override
	public void performRefresh(final boolean paginate) {
		if (context == null || isLoading || adapt == null
				|| getView() == null || getSpinner() == null)
			return;
		else if (location == null
				&& getSpinner().getSelectedItemPosition() == 2) {
			getLocation();
			return;
		}
		isLoading = true;
		context.invalidateOptionsMenu();
		
		adapt.clear();
		if (getView() != null)
			setListShown(false);
		new Thread(new Runnable() {
			@Override
			public void run() {
				final Account acc = AccountService.getCurrentAccount();
				if (acc != null) {
					try {
						ArrayList<Trends> temp = new ArrayList<Trends>();
						switch (getSpinner().getSelectedItemPosition()) {
						default:
							temp.add(acc.getClient().getDailyTrends()
									.get(0));
							break;
						case 1:
							temp.add(acc.getClient().getWeeklyTrends()
									.get(0));
							break;
						case 2:
							final ResponseList<twitter4j.Location> locs = acc
							.getClient().getAvailableTrends(
									location);
							temp.add(acc.getClient().getLocationTrends(
									locs.get(0).getWoeid()));
							break;
						}
						final Trends[] trends = temp.toArray(new Trends[0]);
						context.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								setEmptyText(context
										.getString(R.string.no_trends));
								adapt.add(trends);
							}
						});
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
				}
				context.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						setListShown(true);
						isLoading = false;
						context.invalidateOptionsMenu();
					}
				});
			}
		}).start();
	}

	@Override
	public void reloadAdapter(boolean firstInitialize) {
		if (context == null && getActivity() != null)
			context = getActivity();
		adapt = AccountService.getTrendsAdapter(context);
		setListAdapter(adapt);
		if (adapt.getCount() == 0)
			performRefresh(false);
	}

	@Override
	public void savePosition() {
	}

	@Override
	public void restorePosition() {
	}

	@Override
	public void jumpTop() {
		if (getView() != null)
			getListView().setSelectionFromTop(0, 0);
	}

	@Override
	public void filter() {
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
}
