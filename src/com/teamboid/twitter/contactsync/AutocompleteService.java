package com.teamboid.twitter.contactsync;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

import org.json.JSONObject;

import android.R;
import android.accounts.Account;
import android.app.Service;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
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
		
		int getNotificationId(){return 38924;}

		@Override
		void postSync(SyncResult syncResult) {
			try {
				FileOutputStream fos = this.mContext.openFileOutput(
						"autocomplete-" + getId() + ".json",
						Context.MODE_PRIVATE);
				fos.write(ja.toString().getBytes());
				fos.close();
			} catch (Exception e) {
				e.printStackTrace();
				Log.d("autocomplete", "Failed to save sync result to disk");
				syncResult.delayUntil = 60 * 60 * 2;
			}
		}
	}
}
