package com.teamboid.twitter.columns;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.teamboid.twitter.R;
import com.teamboid.twitter.TabsAdapter.BaseListFragment;

/**
 * A base column extended by the {@link PaddedProfileTimelineFragment} and {@link ProfileAboutFragment}, uses padding in its layout to compensate for the profile screen header.
 * @author Aidan Follestad
 */
public abstract class ProfilePaddedFragment extends BaseListFragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.profile_content, container, false);
	}
}