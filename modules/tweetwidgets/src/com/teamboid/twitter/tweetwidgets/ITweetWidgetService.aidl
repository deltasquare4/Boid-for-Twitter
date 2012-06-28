package com.teamboid.twitter.tweetwidgets;

import android.os.Parcel;
import android.widget.RemoteViews;
import com.teamboid.twitter.tweetwidgets.ITweetWidgetCallback;

interface ITweetWidgetService{
	void updateWidget(long forTweetId, String forUrl, in RemoteViews remoteView);
	void updateCallback(ITweetWidgetCallback callback);
	void sendError(long forTweetId, String forUrl);
}