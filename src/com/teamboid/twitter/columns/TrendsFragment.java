package com.teamboid.twitter.columns;

import com.teamboid.twitterapi.status.GeoLocation;
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
import com.teamboid.twitterapi.trend.Trend;
import com.teamboid.twitterapi.trend.TrendLocation;
import com.teamboid.twitterapi.trend.Trends;

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
	public TrendLocation[] places;
	private int selectedIndex;

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

	private void resetSpinner(boolean loading) {
		if(getSpinner() == null) return;
		final ArrayAdapter<String> spinAdapt = new ArrayAdapter<String>(context, R.layout.spinner_item);
		spinAdapt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		if(loading) {
			filterSelected = true;
			spinAdapt.add(context.getString(R.string.loading_str));
			getSpinner().setAdapter(spinAdapt);
			return;
		} else filterSelected = false;
		
		String[] toAdd = context.getResources().getStringArray(R.array.trend_sources);
		for (String t : toAdd) spinAdapt.add(t);
		filterSelected = true;
		
		if(!deviceHasLocation()){ // probably won't happen in production, but it's to cover us
			spinAdapt.remove(spinAdapt.getItem(3));
		} else if(places != null) {
			spinAdapt.remove(spinAdapt.getItem(3));
			for(TrendLocation loc : places) {
				try{
					spinAdapt.add(context.getString(R.string.local_trend_with_place).replace("{place}", loc.getName()));
				} catch(Exception e){}
			}
			getSpinner().setAdapter(spinAdapt);
			getSpinner().setSelection(3);
		} else {
			int sourceIndex = PreferenceManager.getDefaultSharedPreferences(context).getInt("last_trend_source", 0);
			getSpinner().setAdapter(spinAdapt);
			getSpinner().setSelection(sourceIndex);
		}
		filterSelected = false;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		setRetainInstance(true);
		setEmptyText(getString(R.string.no_trends));
		resetSpinner(false);
		getSpinner().setOnItemSelectedListener(
				new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> arg0, View arg1, int index, long arg3) {
						if (filterSelected) return;
						if(index > 3) index = 3;
						selectedIndex = index;
						PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("last_trend_source", index).apply();
						performRefresh(false);
					}
					@Override
					public void onNothingSelected(AdapterView<?> arg0) { }
				});
		reloadAdapter(true);
	}

	@Override
	public void performRefresh(final boolean paginate) {
		if (context == null || isLoading || adapt == null || getSpinner() == null) return;
		else if (location == null && selectedIndex == 3) {
			getLocation();
			return;
		}
		isLoading = true;
		context.invalidateOptionsMenu();
		adapt.clear();
		if (getView() != null) setListShown(false);
		new Thread(new Runnable() {
			@Override
			public void run() {
				final Account acc = AccountService.getCurrentAccount();
				if (acc != null) {
					try {
						switch (selectedIndex) {
						case 0:
							final Trend[] trends_global = acc.getClient().getTrendsGlobal();
                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setEmptyText(context.getString(R.string.no_trends));
                                    adapt.add(trends_global);
                                }
                            });
							break;
						case 1:
                            final Trends trends_daily = acc.getClient().getTrendsDaily()[0];
                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setEmptyText(context.getString(R.string.no_trends));
                                    adapt.add(trends_daily);
                                }
                            });
							break;
						case 2:
                            final Trends trends_weekly = acc.getClient().getTrendsWeekly()[0];
                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setEmptyText(context.getString(R.string.no_trends));
                                    adapt.add(trends_weekly);
                                }
                            });
							break;
						default:
							if(getSpinner().getSelectedItem().toString().equals(
									context.getResources().getStringArray(R.array.trend_sources)[3]) || places == null) {
								context.runOnUiThread(new Runnable() {
									@Override
									public void run() { resetSpinner(true); }
								});
								places = new TrendLocation[4];
								TrendLocation[] temp = acc.getClient().getTrendsAvailable(location);
								int count = 0;
								for(int i = 0; i < temp.length; i++) {
									if(count == 4) break;
									places[i] = temp[i];
									count++;
								}
								context.runOnUiThread(new Runnable() {
									@Override
									public void run() { resetSpinner(false); }
								});
							} else {
								final Trend[] trends_local = acc.getClient().getLocationTrends(
										places[selectedIndex - 3].getWoeId());
								context.runOnUiThread(new Runnable() {
									@Override
									public void run() {
										setEmptyText(context.getString(R.string.no_trends));
										adapt.add(trends_local);
									}
								});
								break;
							}
						}
					} catch (final Exception e) {
						e.printStackTrace();
						context.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								setEmptyText(context.getString(R.string.error_str));
								Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
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
	
	public boolean deviceHasLocation(){
		try{
			LocationManager locationManager = (LocationManager) context
					.getSystemService(Context.LOCATION_SERVICE);
			return locationManager.getProviders(true).size() > 0;
		} catch(Exception e){
			return false;
		}
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
