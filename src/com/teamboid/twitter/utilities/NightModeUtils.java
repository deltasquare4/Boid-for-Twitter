package com.teamboid.twitter.utilities;

import java.util.Calendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class NightModeUtils {
	public static Calendar parseTimePreference(String input) {
		Calendar c = Calendar.getInstance();
		String[] parts = input.split(":");
		c.set(Calendar.HOUR, Integer.parseInt(parts[0]));
		c.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
		return c;
	}

	/**
	 * Is it nightime yet? I'm tired!
	 * 
	 * @param c
	 * @return
	 */
	public static boolean isNightMode(Context c) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(c);
		if (prefs.getBoolean("night_mode", false) == false)
			return false; // SLEEP IS FOR THE WEAK!

		Calendar now = Calendar.getInstance();
		Calendar start = parseTimePreference(prefs.getString("night_mode",
				"00:00"));
		Calendar end = parseTimePreference(prefs.getString("night_mode_endtime",
				"00:00"));
		if(now.get(Calendar.HOUR) < start.get(Calendar.HOUR) && end.get(Calendar.HOUR) > now.get(Calendar.HOUR)){
			start.roll(Calendar.DAY_OF_YEAR, -1);
		}
		
		Log.d("night", start.toString() + " < " + now.toString() + " < " + end.toString());
		if (now.after(start)) { // We have passed the begining of the night time
			if (now.before(end)) { // We are actually inside night time now
				return true;
			}
		}

		return false; // Did not meet all conditions
	}
}