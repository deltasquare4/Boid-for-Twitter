package com.teamboid.twitter.utilities;

import com.teamboid.twitter.AccountManager;
import com.teamboid.twitter.services.AccountService;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * Defines common things all Boid activites should require to function correctly
 * @author kennydude
 *
 */
public class BoidActivity {
	public static abstract class OnAction{
		public abstract void done();
	} public static class NullOnAction extends OnAction{
		public void done(){}
	}
	
	public OnAction AccountsReady = new NullOnAction();
	
	public BoidActivity(Context c){
		this.mContext = c;
	}
	Context mContext;
	
	public ServiceConnection accConn = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {}
		@Override
		public void onServiceDisconnected(ComponentName name) {}
	};
	
	public BroadcastReceiver acBC = new BroadcastReceiver(){

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			Log.d("Boid", "Accounts are ready");
			AccountsReady.done();
		}
		
	};
	
	/**
	 * Call this when the Activity is inside `onCreate()`
	 */
	public void onCreate(){
		// Register for when service is ready
		IntentFilter filter = new IntentFilter();
		filter.addAction(AccountManager.END_LOAD);
		mContext.registerReceiver(acBC, filter);
		
		// Creates a dependency on the Account Service sticking around
		Intent intent = new Intent(mContext, AccountService.class);
		mContext.startService(intent);
		mContext.bindService(intent, accConn, Context. BIND_AUTO_CREATE);
	}
	
	/**
	 * Call when the activity is inside `onDestroy()`
	 */
	public void onDestroy(){
		mContext.unbindService(accConn);
		mContext.unregisterReceiver(acBC);
	}
}
