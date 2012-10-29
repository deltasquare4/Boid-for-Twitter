package com.teamboid.twitter.views;

import com.handlerexploit.prime.RemoteImageView;
import com.teamboid.twitter.R;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;
import me.kennydude.awesomeprefs.HeaderPreference;
import me.kennydude.awesomeprefs.PreferenceFragment;

public class AccountHeaderPreference extends HeaderPreference {

	public AccountHeaderPreference(Context c, PreferenceFragment f) {
		super(c, f);
	}
	
	public String url;
	
	public View getIconView(){
		RemoteImageView riv = new RemoteImageView(getContext());
		
		DisplayMetrics outMetrics = new DisplayMetrics();
		getFragment().getActivity().getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
		float dp = outMetrics.density;
		riv.setMinimumWidth((int)(64*dp));
		//((int) (64 * dp));
		
		riv.setImageResource(R.drawable.sillouette);
		if(url != null){
			riv.setImageURL(url);
		}
		return riv;
	}

}
