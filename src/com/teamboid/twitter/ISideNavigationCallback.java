package com.teamboid.twitter;

import twitter4j.Status;

/**
 * 
 * @author e.shishkin
 *
 */
public interface ISideNavigationCallback {

	/**
	 * Validation clicking on side navigation item.
	 * @param itemId id of selected item
	 */
	public void onSideNavigationItemClick(Status tweet);

}
