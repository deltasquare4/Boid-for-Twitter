package com.teamboid.twitter.views;

import android.content.Context;
import me.kennydude.awesomeprefs.PreferenceCategory;
import me.kennydude.awesomeprefs.PreferenceFragment;

/**
 * "Accounts" header for Settings screen with an external button widget for adding new accounts
 * @author kennydude
 *
 */
public class AccountPreferenceCategory extends PreferenceCategory {

	public AccountPreferenceCategory(Context c, PreferenceFragment f) {
		super(c, f);
	}

}
