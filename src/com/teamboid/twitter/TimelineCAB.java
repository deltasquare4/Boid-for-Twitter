package com.teamboid.twitter;

import java.util.ArrayList;

import twitter4j.Status;
import twitter4j.TwitterException;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.teamboid.twitter.TabsAdapter.BaseListFragment;
import com.teamboid.twitter.TabsAdapter.TimelineFragment;

public class TimelineCAB {

	public static TimelineScreen context;

	public static void clearSelectedItems() {
		for(int i = 0; i < context.getActionBar().getTabCount(); i++) {
			Fragment frag = context.getFragmentManager().findFragmentByTag("page:" + Integer.toString(i));
			if(frag instanceof BaseListFragment) {
				((BaseListFragment)frag).getListView().clearChoices();
				((BaseAdapter)((BaseListFragment)frag).getListView().getAdapter()).notifyDataSetChanged();
			}
		}
	}
	public static Status[] getSelectedTweets() {
		ArrayList<Status> toReturn = new ArrayList<Status>();
		for(int i = 0; i < context.getActionBar().getTabCount(); i++) {
			Fragment frag = context.getFragmentManager().findFragmentByTag("page:" + Integer.toString(i));
			if(frag instanceof BaseListFragment) {
				Status[] toAdd = ((BaseListFragment)frag).getSelectedStatuses();
				if(toAdd != null && toAdd.length > 0) {
					for(Status s : toAdd) toReturn.add(s);
				}
			}
		}
		return toReturn.toArray(new Status[0]);
	}

	public static void updateTitle(Status[] selTweets) {
		if(TimelineCAB.getSelectedTweets().length == 1) {
			TimelineCAB.TimelineActionMode.setTitle(R.string.one_tweet_selected);
		} else {
			TimelineCAB.TimelineActionMode.setTitle(context.getString(R.string.x_tweets_Selected).replace("{X}", Integer.toString(selTweets.length)));
		}
	}
	public static void updateMenuItems(Status[] selTweets, Menu menu) {
		if(selTweets.length > 1) {
			boolean allFavorited = true;
			for(Status t : selTweets) {
				if(!t.isFavorited()) {
					allFavorited = false;
					break;
				}
			}
			MenuItem fav = menu.findItem(R.id.favoriteAction);
			if(allFavorited) {
				fav.setTitle(R.string.unfavorite_str);
				fav.setIcon(context.getTheme().obtainStyledAttributes(new int[] { R.attr.favoriteIcon }).getDrawable(0));
			} else fav.setTitle(R.string.favorite_str);
		} else {
			final Status status = getSelectedTweets()[0];
			if(status.getUser().getId() == AccountService.getCurrentAccount().getId()) {
				menu.findItem(R.id.retweetAction).setVisible(false);
				menu.findItem(R.id.deleteAction).setVisible(true);
			}
			MenuItem fav = menu.findItem(R.id.favoriteAction);
			if(status.isFavorited()) {
				fav.setTitle(R.string.unfavorite_str);
				fav.setIcon(context.getTheme().obtainStyledAttributes(new int[] { R.attr.favoriteIcon }).getDrawable(0));
			} else fav.setTitle(R.string.favorite_str);
		}
	}

	public static void performLongPressAction(ListView list, BaseAdapter adapt, int index) {
		if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("cab", true)) {
			int beforeChecked = list.getCheckedItemCount();
			if(list.isItemChecked(index)) {
				list.setItemChecked(index, false);
			} else list.setItemChecked(index, true);
			if(TimelineCAB.TimelineActionMode == null) {
				 context.startActionMode(TimelineCAB.TimelineActionModeCallback);
			} else {
				final Status[] tweets = TimelineCAB.getSelectedTweets();
				if(tweets.length == 0) {
					TimelineCAB.TimelineActionMode.finish();
				} else {
					if(beforeChecked == 1 && list.getCheckedItemCount() > 1) {
						TimelineCAB.TimelineActionMode.getMenu().clear();
						TimelineCAB.TimelineActionMode.getMenuInflater().inflate(R.menu.multi_tweet_cab, TimelineCAB.TimelineActionMode.getMenu());
					} else if(beforeChecked > 1 && list.getCheckedItemCount() == 1) {
						TimelineCAB.TimelineActionMode.getMenu().clear();
						TimelineCAB.TimelineActionMode.getMenuInflater().inflate(R.menu.single_tweet_cab, TimelineCAB.TimelineActionMode.getMenu());
					}
					TimelineCAB.updateTitle(tweets);
					TimelineCAB.updateMenuItems(tweets, TimelineCAB.TimelineActionMode.getMenu());
				}
			}
		} else {
			Status item = (Status)adapt.getItem(index);
			context.startActivity(new Intent(context, ComposerScreen.class)
			.putExtra("reply_to", item.getId())
			.putExtra("reply_to_name",item.getUser().getScreenName())
			.putExtra("append",Utilities.getAllMentions(item))
			.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
		}
	}
	
	public static ActionMode TimelineActionMode;
	public static ActionMode.Callback TimelineActionModeCallback = new ActionMode.Callback() {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			TimelineCAB.TimelineActionMode = mode;
			MenuInflater inflater = mode.getMenuInflater();
			Status[] selTweets = TimelineCAB.getSelectedTweets();
			if(selTweets.length > 1) inflater.inflate(R.menu.multi_tweet_cab, menu);
			else inflater.inflate(R.menu.single_tweet_cab, menu);
			updateTitle(selTweets);
			updateMenuItems(selTweets, menu);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			final Status[] selTweets = getSelectedTweets();  
			TimelineCAB.clearSelectedItems();
			mode.finish();
			switch (item.getItemId()) {
			case R.id.replyAction:
				context.startActivity(new Intent(context, ComposerScreen.class)
				.putExtra("reply_to", selTweets[0].getId())
				.putExtra("reply_to_name", selTweets[0].getUser().getScreenName())
				.putExtra("append", Utilities.getAllMentions(selTweets[0].getUser().getScreenName(), selTweets[0].getText()))
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				return true;
			case R.id.favoriteAction:
				for(final Status tweet : selTweets) {
					if(tweet.isFavorited()) {
						new Thread(new Runnable() {
							public void run() {
								//TODO Update the favorite indicator in the corresponding column
								try { AccountService.getCurrentAccount().getClient().destroyFavorite(tweet.getId()); }
								catch(TwitterException e) {
									e.printStackTrace();
									context.runOnUiThread(new Runnable() {
										public void run() { 
											Toast.makeText(context, context.getString(R.string.failed_unfavorite).replace("{user}", tweet.getUser().getScreenName()), Toast.LENGTH_LONG).show();
										}
									});
								}
							}
						}).start();
					} else {
						new Thread(new Runnable() {
							public void run() {
								//TODO Update the favorite indicator in the corresponding column
								try { AccountService.getCurrentAccount().getClient().createFavorite(tweet.getId()); }
								catch(TwitterException e) {
									e.printStackTrace();
									context.runOnUiThread(new Runnable() {
										public void run() { 
											Toast.makeText(context, context.getString(R.string.failed_favorite).replace("{user}", tweet.getUser().getScreenName()), Toast.LENGTH_LONG).show();
										}
									});
								}
							}
						}).start();
					}
				}
				return true;
			case R.id.retweetAction:
				for(final Status tweet : selTweets) {
					new Thread(new Runnable() {
						public void run() {
							try { 
								final Status result = AccountService.getCurrentAccount().getClient().retweetStatus(tweet.getId());
								context.runOnUiThread(new Runnable() {
									public void run() { 
										AccountService.getFeedAdapter(context, TimelineFragment.ID, AccountService.getCurrentAccount().getId()).add(new Status[] { result });
									}
								});
							}
							catch(TwitterException e) {
								e.printStackTrace();
								context.runOnUiThread(new Runnable() {
									public void run() { 
										Toast.makeText(context, context.getString(R.string.failed_retweet).replace("{user}", tweet.getUser().getScreenName()), Toast.LENGTH_LONG).show();
									}
								});
							}
						}
					}).start();
				}
				return true;
			case R.id.shareAction:
				String text = selTweets[0].getText() + "\n\n(via @" + selTweets[0].getUser().getScreenName() + ", http://twitter.com/" + selTweets[0].getUser().getScreenName() + "/status/" + Long.toString(selTweets[0].getId()) + ")";
				context.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), 
						context.getString(R.string.share_str)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				return true;
			case R.id.copyAction:
				ClipboardManager clipboard = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
				clipboard.setPrimaryClip(ClipData.newPlainText("Boid_Tweet", selTweets[0].getText()));
				Toast.makeText(context, context.getString(R.string.copied_str).replace("{user}", selTweets[0].getUser().getScreenName()), Toast.LENGTH_SHORT).show();
				return true;
			default:
				return false;
			}
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			TimelineCAB.clearSelectedItems();
			TimelineCAB.TimelineActionMode = null;
		}
	};
}
