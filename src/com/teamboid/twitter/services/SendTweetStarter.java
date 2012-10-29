package com.teamboid.twitter.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This allows us to "queue" tweets until network is available.
 * 
 * @author kennydude
 */
public class SendTweetStarter extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent arg1) {
		
		Log.d("network", "Boid: The network is back! :D");
		Intent start = new Intent(context, SendTweetService.class);
		start.setAction(SendTweetService.NETWORK_AVAIL);
		context.startService(start);
	
	}

}
