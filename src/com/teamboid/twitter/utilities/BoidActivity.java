package com.teamboid.twitter.utilities;

import com.teamboid.twitter.services.AccountService;

import com.teamboid.twitter.R;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
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
	
	void callAccountsReady(){
		ready=true;
		if(pd != null) pd.dismiss();
		mContext.setTitle(oldTitle);
		AccountsReady.done();
		AccountsReady = new NullOnAction(); // reset
	}
	
	public OnAction AccountsReady = new NullOnAction();
	
	public BoidActivity(Activity c){
		this.mContext = c;
	}
	Activity mContext;
	public boolean firstLoad;
	
	public ServiceConnection accConn = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			if(AccountService.getAccounts().size() > 0){
				Log.d("Boid", "Accounts are ready");
				callAccountsReady();
			}
		}
		@Override
		public void onServiceDisconnected(ComponentName name) {}
	};
	
	public BroadcastReceiver acBC = new BroadcastReceiver(){

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			Log.d("Boid", "Accounts are ready");
			firstLoad = arg1.getBooleanExtra("last_account_count", false);
			callAccountsReady();
		}
		
	};
	
	int lastTheme;
	ProgressDialog pd;
	boolean ready = false;
	CharSequence oldTitle;
	
	/**
	 * Call this when the Activity is inside `onCreate()`
	 */
	public void onCreate(Bundle savedInstanceState){
		// Theme
		if (savedInstanceState != null
				&& savedInstanceState.containsKey("lastTheme")) {
			lastTheme = savedInstanceState.getInt("lastTheme");
			mContext.setTheme(lastTheme);
		} else
			mContext.setTheme(Utilities.getTheme(mContext));
		
		try{
			Log.d("boid", AccountService.getAccounts().size() + "");
			if(AccountService.getAccounts().size() > 0){
				callAccountsReady();
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		
		if(!ready){
			pd = new ProgressDialog(mContext);
			pd.setMessage(mContext.getString(R.string.loading_str));
			pd.show();
		}
		
		// Register for when service is ready
		IntentFilter filter = new IntentFilter();
		filter.addAction(AccountService.END_LOAD);
		mContext.registerReceiver(acBC, filter);
		
		// Creates a dependency on the Account Service sticking around
		Intent intent = new Intent(mContext, AccountService.class);
		mContext.startService(intent);
		mContext.bindService(intent, accConn, Context. BIND_AUTO_CREATE);
	}
	
	public void onSaveInstanceState(Bundle out){
		out.putInt("lastTheme", lastTheme);
	}
	
	/**
	 * Call when the activity is inside `onDestroy()`
	 */
	public void onDestroy(){
		mContext.unbindService(accConn);
		mContext.unregisterReceiver(acBC);
	}
}
