package com.teamboid.twitter;

import java.util.ArrayList;

import twitter4j.Status;
import twitter4j.User;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.teamboid.twitter.TabsAdapter.BaseListFragment;

/**
 * The contextual action bar for any lists/columns that display twitter4j.User objects.
 * @author Aidan Follestad
 */
public class UserListCAB {

	public static Activity context;

	public static void clearSelectedItems() {
		if(context instanceof UserListActivity) {
			((UserListActivity)context).getListView().clearChoices();
			ListView list = ((UserListActivity)context).getListView();
			((BaseAdapter)list.getAdapter()).notifyDataSetChanged();
		} else {
			for(int i = 0; i < context.getActionBar().getTabCount(); i++) {
				Fragment frag = context.getFragmentManager().findFragmentByTag("page:" + Integer.toString(i));
				if(frag instanceof BaseListFragment) {
					((BaseListFragment)frag).getListView().clearChoices();
					((BaseAdapter)((BaseListFragment)frag).getListView().getAdapter()).notifyDataSetChanged();
				}
			}
		}
	}
	public static User[] getSelectedUsers() {
		ArrayList<User> toReturn = new ArrayList<User>();
		if(context instanceof UserListActivity) {
			UserListActivity activity = (UserListActivity)context; 
			SparseBooleanArray checkedItems = activity.getListView().getCheckedItemPositions();
			if(checkedItems != null) {
				for(int i = 0; i < checkedItems.size(); i++) {
					if(checkedItems.valueAt(i)) {
						toReturn.add((User)activity.binder.getItem(checkedItems.keyAt(i)));
					}
				}
			}
		} else {
			for(int i = 0; i < context.getActionBar().getTabCount(); i++) {
				Fragment frag = context.getFragmentManager().findFragmentByTag("page:" + Integer.toString(i));
				if(frag instanceof BaseListFragment) {
					User[] toAdd = ((BaseListFragment)frag).getSelectedUsers();
					if(toAdd != null && toAdd.length > 0) {
						for(User u : toAdd) toReturn.add(u);
					}
				}
			}
		}
		return toReturn.toArray(new User[0]);
	}

	public static void updateTitle() {
		User[] selUsers = UserListCAB.getSelectedUsers(); 
		if(selUsers.length == 1) {
			UserListCAB.UserActionMode.setTitle(R.string.one_user_selected);
		} else {
			UserListCAB.UserActionMode.setTitle(context.getString(R.string.x_users_selected).replace("{X}", Integer.toString(selUsers.length)));
		}
	}
	public static void updateMenuItems(User[] selUsers, Menu menu) {
		//		if(selTweets.length > 1) {
		//			boolean allFavorited = true;
		//			for(Status t : selTweets) {
		//				if(!t.isFavorited()) {
		//					allFavorited = false;
		//					break;
		//				}
		//			}
		//			MenuItem fav = menu.findItem(R.id.favoriteAction);
		//			if(allFavorited) {
		//				fav.setTitle(R.string.unfavorite_str);
		//				fav.setIcon(context.getTheme().obtainStyledAttributes(new int[] { R.attr.favoriteIcon }).getDrawable(0));
		//			} else fav.setTitle(R.string.favorite_str);
		//		} else {
		//			final Status status = getSelectedTweets()[0];
		//			if(status.getUser().getId() == AccountService.getCurrentAccount().getId()) {
		//				menu.findItem(R.id.retweetAction).setVisible(false);
		//				menu.findItem(R.id.deleteAction).setVisible(true);
		//			}
		//			MenuItem fav = menu.findItem(R.id.favoriteAction);
		//			if(status.isFavorited()) {
		//				fav.setTitle(R.string.unfavorite_str);
		//				fav.setIcon(context.getTheme().obtainStyledAttributes(new int[] { R.attr.favoriteIcon }).getDrawable(0));
		//			} else fav.setTitle(R.string.favorite_str);
		//		}
	}

	public static void performLongPressAction(ListView list, BaseAdapter adapt, int index) {
		if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("cab", true)) {
			if(list.isItemChecked(index)) {
				list.setItemChecked(index, false);
			} else list.setItemChecked(index, true);
			if(UserListCAB.UserActionMode == null) {
				context.startActionMode(UserListCAB.UserActionModeCallback);
			} else {
				final User[] users = UserListCAB.getSelectedUsers();
				if(users.length == 0) {
					UserListCAB.UserActionMode.finish();
				} else {
					UserListCAB.updateTitle();
					UserListCAB.updateMenuItems(users, UserListCAB.UserActionMode.getMenu());
				}
			}
		} else {
			context.startActivity(new Intent(context, ComposerScreen.class)
			.putExtra("append", "@" + ((User)adapt.getItem(index)).getScreenName() + " ")
			.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
		}
	}

	public static ActionMode UserActionMode;
	public static ActionMode.Callback UserActionModeCallback = new ActionMode.Callback() {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			UserListCAB.UserActionMode = mode;
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.user_cab, menu);
			updateTitle();
			updateMenuItems(UserListCAB.getSelectedUsers(), menu);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			final User[] selUsers = getSelectedUsers();  
			UserListCAB.clearSelectedItems();
			mode.finish();
			switch (item.getItemId()) {
			case R.id.mentionAction:
				String mentionStr = "";
				for(User user : selUsers) {
					mentionStr += "@" + user.getScreenName() + " ";
				}
				context.startActivity(new Intent(context, ComposerScreen.class)
				.putExtra("append", mentionStr).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				return true;
			case R.id.followAction:
				//TODO
				return true;
			case R.id.shareAction:
				String shareStr = "";
				for(int i = 0; i < selUsers.length; i++) {
					String name = selUsers[i].getScreenName();
					if(i > 0) shareStr += "\n";
					shareStr += "@" + name + " (https://twitter.com/" + name + ")";
				}
				context.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareStr), 
						context.getString(R.string.share_str)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				return true;
			default:
				return false;
			}
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			UserListCAB.clearSelectedItems();
			UserListCAB.UserActionMode = null;
		}
	};
}
