package com.teamboid.twitter;

import java.util.Map.Entry;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.RadioButton;

import com.teamboid.twitter.utilities.MediaUtilities;
import com.teamboid.twitter.utilities.MediaUtilities.MediaService;

public class SelectMediaScreen extends PreferenceActivity
{
	public class MediaPreference extends Preference
	{
		public boolean checked;
		public boolean needsConfig;
		public MediaService m;
		public OnPreferenceClickListener callback;

		public MediaPreference(Context context, MediaService m, final String key)
		{
			super(context);
			try
			{
				setTitle(context.getResources().getString(m.name));
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			needsConfig = m.needs_config;
			this.m = m;
			m.configured = new MediaUtilities.MediaConfigured()
			{

				@Override
				public void startActivity(Intent activity)
				{
					SelectMediaScreen.this.startActivity(activity);
				}

				@Override
				public Uri getCallback()
				{
					return Uri.parse("boid://finishconfig/" + key);
				}

				@Override
				public void configured()
				{
					// TODO this function is deprecated and needs to be
					// replaced.
					getPreferenceScreen().removeAll();
					setupPreferences();
				}
			};
		}

		@Override
		public View getView(View convertView, ViewGroup parent)
		{
			if (convertView == null)
				convertView = LayoutInflater.from(getContext()).inflate(
						R.layout.media_service, null);
			RadioButton r = (RadioButton) convertView.findViewById(R.id.radio);
			r.setEnabled(m.isConfigured(PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext())));
			if (!needsConfig)
				r.setText(this.getTitle());
			else
				r.setText(getContext().getResources()
						.getString(R.string.service_needs_config)
						.replace("{service}", this.getTitle()));
			r.setChecked(checked);
			r.setOnCheckedChangeListener(new OnCheckedChangeListener()
			{
				@Override
				public void onCheckedChanged(CompoundButton arg0, boolean arg1)
				{
					callback.onPreferenceClick(MediaPreference.this);
				}
			});
			ImageButton c = (ImageButton) convertView.findViewById(R.id.button);
			c.setVisibility(needsConfig ? View.VISIBLE : View.GONE);
			c.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View arg0)
				{
					m.configure(SelectMediaScreen.this);
				}
			});
			return convertView;
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();
		if (getIntent().getData() != null)
			onNewIntent(getIntent());
	}

	@Override
	public void onNewIntent(Intent intent)
	{
		Log.d("n", "NEW INTENT");
		if (intent.getData() != null)
		{
			if (!intent.getData().getScheme().equals("boid"))
				return;
			Log.d("i", "new intent");
			String key = intent.getData().getPathSegments().get(0);
			Log.d("i", key);
			MediaUtilities.getMediaServices(true, this).get(key)
					.configure(this, intent);
		}
	}

	private void setupPreferences()
	{
		String pref = PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).getString("upload_service", "twitter");
		for (final Entry<String, MediaService> entry : MediaUtilities
				.getMediaServices(true, this).entrySet())
		{
			MediaPreference m = new MediaPreference(this, entry.getValue(),
					entry.getKey());
			m.setKey(entry.getKey());
			m.setPersistent(false);
			m.checked = pref.equals(entry.getKey());
			m.callback = new OnPreferenceClickListener()
			{

				@Override
				public boolean onPreferenceClick(Preference pref)
				{
					setResult(RESULT_OK,
							new Intent().putExtra("service", entry.getKey()));
					finish();
					return true;
				}

			};
			// TODO this function is deprecated and needs to be replaced.
			getPreferenceScreen().addPreference(m);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		// TODO this function is deprecated and needs to be replaced.
		addPreferencesFromResource(R.xml.select_media);
		setupPreferences();
	}
}