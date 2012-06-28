package com.teamboid.twitter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This allows us to "queue" tweets until network is available.
 * @author kennydude
 *
 */
public class SendTweetStarter extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent arg1) {
		if(NetworkUtils.haveNetworkConnection(context)){
			Log.d("network", "Boid: The network is back! :D");
			Intent start = new Intent(context, SendTweetService.class);
			start.setAction(SendTweetService.NETWORK_AVAIL);
			context.startService(start);
		}
	}

}
