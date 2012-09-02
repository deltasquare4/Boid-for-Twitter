package com.teamboid.twitter.utilities;

import java.util.Calendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class NightModeUtils {
	
	/**
	 * Is it nightime yet? I'm tired!
	 * 
	 * @param c
	 * @return
	 */
	public static boolean isNightMode(Context c) {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		if (prefs.getBoolean("night_mode", false)) {
			int nowHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
			int startHour = Integer.parseInt(prefs.getString("night_mode_time",
					"20"));
			int endHour = Integer.parseInt(prefs.getString(
					"night_mode_endtime", "7"));
			if ((nowHour < endHour) || (nowHour > startHour)) {
				return true;
			}
		}
		return false;
	}
}