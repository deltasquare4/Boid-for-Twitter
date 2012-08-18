package com.teamboid.twitter.contactsync;

import java.util.LinkedList;
import java.util.Queue;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitterapi.client.Authorizer;
import com.teamboid.twitterapi.client.Twitter;
import com.teamboid.twitterapi.relationship.IDs;
import com.teamboid.twitterapi.user.User;
import com.teamboid.twitterapi.utilities.Utils;

public abstract class BaseTwitterSync extends AbstractThreadedSyncAdapter {
	public Context mContext;
	public Account account;

	String getWhatToSync() { // TODO: Actually make this return something the
								// user wants
		return "following";
	}

	// Notes:
	// This works by having a queue `idQueue` which contains up to 1000 ids
	// which is how twitter works, but when it empties we grab more if needed
	// and we drain them out into batches of 100 to query Twitter with
	Queue<Long> idQueue = new LinkedList<Long>();
	long cursor = -1;

	User[] getTimeline() {
		try {
			Twitter client = getTwitter();
			String type = getWhatToSync();

			if (idQueue.isEmpty()) { // If we have no more IDs Left in the queue
				IDs ids = null;
				if (type.equals("following")) {
					ids = client.getFriends(getId(), cursor);
				} else if (type.equals("followers")) {
					ids = client.getFollowers(getId(), cursor);
				} else {
					Log.d("contactsync",
							"Righto, someone is hacking our app. Let's just let it crash");
				}

				cursor = ids.getNextCursor();
				for (Long id : ids.getIds()) {
					idQueue.add(id);
				}
				// Now the queue is stocked up with up to 5000 IDs (as twitter
				// says).
			}

			// Off-load up to 100 ids from the queue
			Long[] ids = new Long[idQueue.size() >= 100 ? 100 : idQueue.size()];
			for (int i = 0; i < ids.length; i++) {
				ids[i] = idQueue.remove();
			}

			// Now fetch information about them
			return client.lookupUsers(ids);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	int getTotalNumber() {
		try {
			Twitter client = getTwitter();
			String type = getWhatToSync();

			if (type.equals("following")) {
				return (int) client.verifyCredentials().getFriendsCount();
			} else if (type.equals("followers")) {
				return (int) client.verifyCredentials().getFollowersCount();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
		return -1;
	}

	public BaseTwitterSync(Context context) {
		super(context, true);
		mContext = context;
	}

	long _id = -1;

	Long getId() {
		if (_id != -1)
			return _id;

		android.accounts.AccountManager am = android.accounts.AccountManager
				.get(mContext);
		_id = Long.parseLong(am.getUserData(account, "accId"));
		return _id;
	}

	Twitter getTwitter() {
		SharedPreferences sp = mContext.getSharedPreferences("profiles-v2",
				Context.MODE_PRIVATE);
		String s = sp.getString(getId() + "", null);
		if (s == null)
			return null;

		com.teamboid.twitter.Account toAdd = (com.teamboid.twitter.Account) Utils
				.deserializeObject(s);
		Log.d("contactsync", "Hello " + toAdd.getId());
		return Authorizer.create(AccountService.CONSUMER_KEY,
				AccountService.CONSUMER_SECRET, AccountService.CALLBACK_URL)
				.getAuthorizedInstance(toAdd.getToken(), toAdd.getSecret());
	}
}
