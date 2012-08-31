package com.teamboid.twitter.contactsync;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

import org.json.JSONObject;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.IBinder;
import android.util.Log;

import com.teamboid.twitterapi.user.User;

/**
 * Provides Auto-completion data
 * 
 * @author kennydude
 */
public class AutocompleteService extends Service {
	public static final String AUTHORITY = "com.teamboid.twitter.autocomplete";

	public static void removeItem(User user, Context c, long accId) {
		JSONObject jo = readAutocompleteFile(c, accId);
		if (jo == null)
			return;
		jo.remove(user.getName());
		jo.remove(user.getScreenName());
		saveAutocompleteFile(c, accId, jo);
	}

	public static void addItem(User user, Context c, long accId) {
		JSONObject jo = readAutocompleteFile(c, accId);
		if (jo == null)
			return;
		try {
			jo.put(user.getName(), user.getScreenName());
			jo.put(user.getScreenName(), user.getScreenName());
		} catch (Exception e) {
		}
		saveAutocompleteFile(c, accId, jo);
	}

	public static JSONObject readAutocompleteFile(Context c, long accId) {
		try {
			FileInputStream fis = c.openFileInput("autocomplete-" + accId
					+ ".json");
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			JSONObject ja = new JSONObject(br.readLine());
			br.close();
			fis.close();
			return ja;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static boolean saveAutocompleteFile(Context c, long accId,
			JSONObject ja) {
		try {
			FileOutputStream fos = c.openFileOutput("autocomplete-" + accId
					+ ".json", Context.MODE_PRIVATE);
			fos.write(ja.toString().getBytes());
			fos.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			Log.d("autocomplete", "Failed to save sync result to disk");
		}
		return false;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return getSyncAdapter().getSyncAdapterBinder();
	}

	SyncAdapterImpl instance;

	SyncAdapterImpl getSyncAdapter() {
		if (instance == null)
			instance = new SyncAdapterImpl(this);
		return instance;
	}

	private static class SyncAdapterImpl extends BaseTwitterSync {
		public SyncAdapterImpl(Service contactSyncAdapterService) {
			super(contactSyncAdapterService);
		}

		JSONObject ja;

		@Override
		Integer whatAmI() {
			return com.teamboid.twitter.R.string.autocomplete;
		}

		@Override
		void processUser(User user) {
			try {
				ja.put(user.getScreenName(), user.getScreenName());
				ja.put(user.getName(), user.getScreenName());
			} catch (Exception e) { // Should never happen
				e.printStackTrace();
			}
		}

		@Override
		void preSync() {
			ja = new JSONObject();
		}

		int getNotificationId() {
			return 38924;
		}

		@Override
		void postSync(SyncResult syncResult) {
			if (!saveAutocompleteFile(mContext, getId(), ja)) {
				syncResult.delayUntil = 60 * 60 * 2;
			}
		}

		@Override
		String whatAmIString() {
			return "autocomplete";
		}
	}
}
