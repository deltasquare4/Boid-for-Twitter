package com.teamboid.twitter.compat;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
public class Api15 {

	public static void setUserVisibleHint(Fragment fragment, boolean b) {
		fragment.setUserVisibleHint(b);
	}

}
