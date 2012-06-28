package com.teamboid.twitter;

import java.io.BufferedInputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
	
	public static Bitmap bitmapFromStream(BufferedInputStream bis, float scaleDp, Context c) throws IOException{
		BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inPurgeable = true;
		opt.inInputShareable = true;
		if(scaleDp > 0) {
			int dp = Utilities.convertDpToPx(c, scaleDp);
			opt.inSampleSize = Utilities.calculateInSampleSize(opt, dp, dp);
		}
		final Bitmap bm = BitmapFactory.decodeStream(bis, null, opt);
		bis.close();
		return bm;
	}
}
