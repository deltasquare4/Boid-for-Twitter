package com.teamboid.twitter.widgets;

import com.teamboid.twitter.R;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

public class ResizableWidgetProvider extends AppWidgetProvider {

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		ComponentName thisWidget = new ComponentName(context, ResizableWidgetProvider.class);
		int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
		
		for(int widgetId : allWidgetIds) {
			appWidgetManager.updateAppWidget(widgetId, TimelineWidgetViewService
                    .createWidgetView(context, widgetId, appWidgetIds));
		}

		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}
} 