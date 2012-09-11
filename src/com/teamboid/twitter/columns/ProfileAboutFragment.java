package com.teamboid.twitter.columns;

import java.net.URLEncoder;

import com.teamboid.twitterapi.search.Tweet;
import com.teamboid.twitterapi.status.Status;
import com.teamboid.twitterapi.user.User;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.teamboid.twitter.Account;
import com.teamboid.twitter.ProfileScreen;
import com.teamboid.twitter.R;
import com.teamboid.twitter.TweetListActivity;
import com.teamboid.twitter.UserListActivity;
import com.teamboid.twitter.listadapters.ProfileAboutAdapter;
import com.teamboid.twitter.listadapters.MessageConvoAdapter.DMConversation;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;

/**
 * Represents the column that displays details about a profile, it's padded to compesensate for the header in the profile screen.
 * @author Aidan Follestad
 */
public class ProfileAboutFragment extends ProfilePaddedFragment {

	private Activity context;
	private ProfileAboutAdapter adapt;
	private String screenName;

	public ProfileAboutAdapter getAdapter() {
		return adapt;
	}

	@Override
	public void onAttach(Activity act) {
		super.onAttach(act);
		context = act;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		BasicNameValuePair pair = null;
		if(AccountService.getCurrentAccount().getUser().getScreenName().equals(screenName)) {
			pair = adapt.values.get(position);
		} else {
			if(position == 0) return;
			pair = adapt.values.get(position - 1);
		}
		if (pair.getName().equals(context.getString(R.string.website_str))) {
			String url = pair.getValue();
			try {
				context.startActivity(new Intent(Intent.ACTION_VIEW)
				.setData(Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(pair.getName().equals(context.getString(R.string.tweets_str))) {
			context.getActionBar().setSelectedNavigationItem(0);
		} else if(pair.getName().equals(context.getString(R.string.location_str))) {
			String loc = null;
			try { loc = URLEncoder.encode(pair.getValue(), "UTF-8"); }
			catch (Exception e) { e.printStackTrace(); }
			Intent geo = new Intent(Intent.ACTION_VIEW);
			geo.setData(Uri.parse("geo:0,0?q=" + loc));
			if (Utilities.isIntentAvailable(context, geo)) startActivity(geo);
			else {
				geo.setData(Uri.parse("https://maps.google.com/maps?q=" + loc));
				startActivity(geo);
			}
		} else if (pair.getName().equals(context.getString(R.string.followers_str))) {
			Intent intent = new Intent(context, UserListActivity.class)
			.putExtra("mode", UserListActivity.FOLLOWERS_LIST)
			.putExtra("user", adapt.user.getId())
			.putExtra("username", adapt.user.getScreenName());
			context.startActivity(intent);
		} else if (pair.getName().equals(context.getString(R.string.friends_str))) {
			Intent intent = new Intent(context, UserListActivity.class)
			.putExtra("mode", UserListActivity.FOLLOWING_LIST)
			.putExtra("user", adapt.user.getId())
			.putExtra("username", adapt.user.getScreenName());
			context.startActivity(intent);
		} else if (pair.getName().equals(context.getString(R.string.favorites_str))) {
			Intent intent = new Intent(context, TweetListActivity.class)
			.putExtra("mode", TweetListActivity.USER_FAVORITES)
			.putExtra("username", adapt.user.getScreenName());
			context.startActivity(intent);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		setRetainInstance(true);
		screenName = getArguments().getString("query");
		reloadAdapter(true);
        getListView().setDivider(null);
	}

	@Override
	public void performRefresh(final boolean paginate) {
		if (context == null || isLoading || adapt == null) return;
		isLoading = true;
		context.invalidateOptionsMenu();
		if (adapt.getCount() == 0 && getView() != null) setListShown(false);
		new Thread(new Runnable() {
			@Override
			public void run() {
				final Account acc = AccountService.getCurrentAccount();
				if (acc != null) {
					try {
						User temp = null;
						if (screenName.equals(acc.getUser().getScreenName())) {
							temp = acc.getClient().verifyCredentials();
							AccountService.setAccount(context, temp);
						} else temp = acc.getClient().showUser(screenName);
						final User user = temp;
						context.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								((ProfileScreen)context).user = user;
								((ProfileScreen)context).setupViews();
								((ProfileScreen)context).loadFollowingInfo();
								((ProfileScreen)context).invalidateOptionsMenu();
								adapt.setUser(user);
							}
						});
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
						if (getView() != null) setListShown(true);
						isLoading = false;
						context.invalidateOptionsMenu();
					}
				});
			}
		}).start();
	}

	@Override
	public void reloadAdapter(boolean firstInitialize) {
		if (AccountService.getCurrentAccount() != null) {
			adapt = new ProfileAboutAdapter(context);
			setListAdapter(adapt);
			if (adapt.getCount() == 0) performRefresh(false);
		}
	}

	@Override
	public void savePosition() { }

	@Override
	public void restorePosition() { }

	@Override
	public void jumpTop() {
		if (getView() != null) getListView().setSelectionFromTop(0, 0);
	}

	@Override
	public void filter() { }

	@Override
	public Status[] getSelectedStatuses() { return null; }

	@Override
	public User[] getSelectedUsers() { return null; }

	@Override
	public Tweet[] getSelectedTweets() { return null; }

	@Override
	public DMConversation[] getSelectedMessages() { return null; }
}
