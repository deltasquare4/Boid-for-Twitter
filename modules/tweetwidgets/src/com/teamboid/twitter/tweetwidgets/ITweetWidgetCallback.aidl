package com.teamboid.twitter.tweetwidgets;

import android.os.Parcel;
import android.widget.RemoteViews;

oneway interface ITweetWidgetCallback{
	void updateWidget(long forTweetId, String forUrl, in RemoteViews remoteView);
	void errorMessage(long forTweetId, String forUrl);
}