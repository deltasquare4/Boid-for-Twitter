package com.teamboid.twitter.utilities;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Convenience methods
 * @author Aidan Follestad
 */
public class NetworkUtils {

	public static boolean haveNetworkConnection(Context context) {
		ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		if(activeNetwork == null) return false;
		return activeNetwork.isConnected();
	}
}
