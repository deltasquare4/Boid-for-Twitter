package com.teamboid.twitter.utilities;

import java.util.Calendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

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
		Calendar time = parseTimePreference(prefs.getString("night_mode",
				"00:00"));
		if (now.after(time)) { // We have passed the begining of the night time
			time = parseTimePreference(prefs.getString("night_mode_endtime",
					"00:00"));
			if (now.before(time)) { // We are actually inside night time now
				return true;
			}
		}

		return false; // Did not meet all conditions
	}
}