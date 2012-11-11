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
import com.teamboid.twitter.TabsAdapter.BaseLocationSpinnerFragment;
import com.teamboid.twitter.TimelineScreen;
import com.teamboid.twitter.TweetViewer;
import com.teamboid.twitter.TabsAdapter.BaseSpinnerFragment;
import com.teamboid.twitter.listadapters.MessageConvoAdapter.DMConversation;
import com.teamboid.twitter.listadapters.SearchFeedListAdapter;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;
import com.teamboid.twitterapi.client.Paging;
import com.teamboid.twitterapi.search.GeoCode;
import com.teamboid.twitterapi.search.SearchQuery;
import com.teamboid.twitterapi.search.SearchResult;
import com.teamboid.twitterapi.search.Tweet;
import com.teamboid.twitterapi.status.GeoLocation;
import com.teamboid.twitterapi.status.Status;
import com.teamboid.twitterapi.user.User;

/**
 * Represents the column that gets your current location and makes a search
 * query to get Tweets sent by people that are close to your current location.
 * 
 * @author Aidan Follestad
 */
public class NearbyFragment extends BaseLocationSpinnerFragment<Tweet> {
	public static final String ID = "COLUMNTYPE:NEARBY";
	private int selectedIndex;

	private boolean filterSelected;

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Tweet tweet = (Tweet) getListAdapter().getItem(position);
		getActivity().startActivity(new Intent(getActivity(), TweetViewer.class)
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
		getListView().setOnItemLongClickListener(
				new AdapterView.OnItemLongClickListener() {
					@Override
					public boolean onItemLongClick(AdapterView<?> arg0,
							View arg1, int index, long id) {
						Tweet item = (Tweet) getListAdapter().getItem(index);
						getActivity().startActivity(new Intent(getActivity(),
								ComposerScreen.class)
								.putExtra("reply_to", item.getId())
								.putExtra("reply_to_name", item.getFromUser())
								.putExtra("append", Utilities.getAllMentions(item))
								.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
						return false;
					}
				});
	}
	
	public void resetSpiner(){
		final ArrayAdapter<String> spinAdapt = new ArrayAdapter<String>(
				getActivity(), R.layout.spinner_item);
		spinAdapt
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinAdapt.addAll(getActivity().getResources().getStringArray(
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
						PreferenceManager.getDefaultSharedPreferences(getActivity())
								.edit().putInt("last_nearby_range", index)
								.apply();
						performRefresh();
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					}
				});
		getSpinner().setAdapter(spinAdapt);
		filterSelected = false;
		getSpinner().setSelection(
				PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt(
						"last_nearby_range", 0));
		setRetainInstance(true);
	}

	@Override public Status[] getSelectedStatuses() {	return null; }
	@Override public User[] getSelectedUsers() { return null; }
	@Override public Tweet[] getSelectedTweets() { return null; }
	@Override public DMConversation[] getSelectedMessages() { return null; }

	@Override
	public void setupAdapter() {
		setListAdapter(AccountService.getNearbyAdapter(getActivity()));
	}

	@Override
	public Tweet[] fetch(long maxId, long sinceId) {
		try{
			if(getGeoLocation() == null){
				getLocation();
			}
			resetSpiner();
			
			String[] radiuses = getActivity().getResources().getStringArray(
					R.array.nearby_distances);
			String[] values = getActivity().getResources().getStringArray(
					R.array.nearby_distance_values);
			final String nearbyRadius = radiuses[selectedIndex];
			final int nearbyValue = Integer.parseInt(values[selectedIndex]);
			
			GeoCode.DistanceUnit unit = GeoCode.DistanceUnit.MI;
			if (nearbyRadius.endsWith("km"))
				unit = GeoCode.DistanceUnit.KM;
			GeoCode geo = GeoCode.create(location, nearbyValue, unit);
			Paging paging = new Paging(50);
			if (maxId != -1)
				paging.setMaxId(maxId);
			final SearchQuery query = SearchQuery.create(null, paging)
					.setGeoCode(geo);
			final Account acc = AccountService.getCurrentAccount();
			
			return acc.getClient().search(query).getResults();
			
		} catch(Exception e){
			e.printStackTrace();
			showError(e.getMessage());
			return null;
		}
	}

	@Override
	public String getColumnName() {
		return ID;
	}
}
