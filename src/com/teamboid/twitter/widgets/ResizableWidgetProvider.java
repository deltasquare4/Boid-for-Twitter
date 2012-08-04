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
			RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.resizable_widget);

			Intent intent = new Intent(context, TimelineWidgetViewService.class);
	        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
	        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
	        remoteViews.setRemoteAdapter(R.id.widgetList, intent);
	        remoteViews.setEmptyView(R.id.widgetList, R.id.empty);
			
			intent = new Intent(context, ResizableWidgetProvider.class);
			intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
					0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			remoteViews.setOnClickPendingIntent(R.id.header, pendingIntent);
			
			appWidgetManager.updateAppWidget(widgetId, remoteViews);
		}
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}
} 