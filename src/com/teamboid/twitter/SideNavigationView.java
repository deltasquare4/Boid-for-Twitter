package com.teamboid.twitter;

import twitter4j.Status;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;

/**
 * 
 * @author e.shishkin
 * 
 */
public class SideNavigationView extends LinearLayout {
	
	private static final String LOG_TAG = SideNavigationView.class.getSimpleName();
	private LinearLayout navigationMenu;
	private ListView listView;
	private View outsideView;

	private ISideNavigationCallback callback;

	private FeedListAdapter adapter;

	public SideNavigationView(Context context) {
		super(context);
		load();
	}

	public SideNavigationView(Context context, AttributeSet attrs) {
		super(context, attrs);
		load();
	}

	private void load() {
		if (isInEditMode()) {
			return;
		}
		inflateLayout();
		initUi();
	}

	private void inflateLayout() {
		try {
			LayoutInflater.from(getContext()).inflate(R.layout.side_navigation, this, true);
		} catch (Exception e) {
			Log.w(LOG_TAG, e);
		}
	}

	private void initUi() {
		navigationMenu = (LinearLayout) findViewById(R.id.side_navigation_menu);
		listView = (ListView) findViewById(R.id.side_navigation_listview);
		outsideView = (View) findViewById(R.id.side_navigation_outside_view);
		outsideView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				hideMenu();
			}
		});
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if(callback != null) {
					callback.onSideNavigationItemClick((Status)adapter.getItem(position));
				}
				hideMenu();
			}
		});
	}

	public void setMenuClickCallback(ISideNavigationCallback callback) {
		this.callback = callback;
	}

	public void setMenuItems(Activity mContext, Status[] items) {
		adapter = new FeedListAdapter(mContext, null);
		adapter.addInverted(items);
		listView.setAdapter(adapter);
	}

	public void setBackgroundResource(int resource) {
		listView.setBackgroundResource(resource);
	}

	public void showMenu() {
		outsideView.setVisibility(View.VISIBLE);
		outsideView.startAnimation(AnimationUtils.loadAnimation(getContext(),
				R.anim.side_navigation_fade_in));
		navigationMenu.setVisibility(View.VISIBLE);
		navigationMenu.startAnimation(AnimationUtils.loadAnimation(getContext(),
				R.anim.side_navigation_in_from_left));
	}
	public void moveToNormal(){
		outsideView.setX(0);
		navigationMenu.setX(0);
	}
	
	public void moveTo(float x){
		navigationMenu.setVisibility(View.VISIBLE);
		navigationMenu.setX( (x - navigationMenu.getWidth() ) );
		
		outsideView.setVisibility(View.VISIBLE);
		outsideView.setX( (x - outsideView.getWidth() ) );
	}

	public void hideMenu() {
		outsideView.setVisibility(View.GONE);
		outsideView.startAnimation(AnimationUtils.loadAnimation(getContext(),
				R.anim.side_navigation_fade_out));
		navigationMenu.setVisibility(View.GONE);
		navigationMenu.startAnimation(AnimationUtils.loadAnimation(getContext(),
				R.anim.side_navigation_out_to_left));
	}

	public boolean isShown() {
		return (outsideView.getVisibility() == View.VISIBLE);
	}
	
	public void toggleMenu() {
		if(outsideView.getVisibility() == View.GONE) {
			showMenu();
		} else hideMenu();
	}
}
