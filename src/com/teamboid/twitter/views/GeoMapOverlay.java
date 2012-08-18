package com.teamboid.twitter.views;

import java.util.ArrayList;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

/**
 * Used for the overlay items that are shown in the Google Maps view in the
 * TweetViewer.
 * 
 * @author Aidan Follestad
 */
public class GeoMapOverlay extends ItemizedOverlay<OverlayItem> {

	public GeoMapOverlay(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
	}

	public GeoMapOverlay(Drawable defaultMarker, Context context) {
		super(boundCenterBottom(defaultMarker));
		mContext = context;
	}

	private Context mContext;
	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();

	public void addOverlay(OverlayItem overlay) {
		mOverlays.add(overlay);
		populate();
	}

	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}

	@Override
	protected boolean onTap(int index) {
		OverlayItem item = mOverlays.get(index);
		Toast.makeText(mContext, item.getTitle(), Toast.LENGTH_SHORT).show();
		return true;
	}
}
