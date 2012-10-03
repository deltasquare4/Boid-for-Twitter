package com.teamboid.twitter.boxes;

import com.teamboid.twitter.R;

import android.app.Activity;
import android.view.ViewStub;

/**
 * I am a box that deals with the timeline Sidebar controls
 * @author kennydude
 *
 */
public class TimelineSidebarBox {
	public static void setup(Activity a){
		// Phase 1: Check if we are a tablet/large-screen
		if(a.findViewById(R.id.sidebar) != null){
			// Phase 2: Load
			
			ViewStub vs = (ViewStub) a.findViewById(R.id.sidebar);
			vs.inflate();
			
		}
	}
}
