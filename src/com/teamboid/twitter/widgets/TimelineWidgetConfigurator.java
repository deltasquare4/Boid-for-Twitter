package com.teamboid.twitter.widgets;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.handlerexploit.prime.ImageManager;
import com.teamboid.twitter.*;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;
import com.teamboid.twitter.views.TimePreference;
import com.teamboid.twitterapi.media.MediaServices;
import net.robotmedia.billing.BillingController;
import net.robotmedia.billing.BillingRequest;
import net.robotmedia.billing.helper.AbstractBillingObserver;
import net.robotmedia.billing.model.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aidan Follestad
 */
public class TimelineWidgetConfigurator extends PreferenceActivity {

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private int lastTheme;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey("lastTheme")) {
            lastTheme = savedInstanceState.getInt("lastTheme");
            setTheme(lastTheme);
        } else setTheme(Utilities.getTheme(getApplicationContext()));
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mAppWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        addPreferencesFromResource(R.xml.timeline_widget_config);
    }

    @Override
    public void onBuildHeaders(List<Header> target) { }

    @Override
    public void onResume() {
        super.onResume();
        if (lastTheme == 0) lastTheme = Utilities.getTheme(getApplicationContext());
        else if (lastTheme != Utilities.getTheme(getApplicationContext())) {
            lastTheme = Utilities.getTheme(getApplicationContext());
            recreate();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("lastTheme", lastTheme);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.config_activity_actionbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            case R.id.doneAction:
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
                appWidgetManager.updateAppWidget(mAppWidgetId,
                        TimelineWidgetViewService.createWidgetView(getApplicationContext(), mAppWidgetId, null));
                setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
