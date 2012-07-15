package com.teamboid.twitter;

import java.util.List;

import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.MediaUtilities;
import com.teamboid.twitter.utilities.Utilities;


import net.robotmedia.billing.BillingController;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.provider.SearchRecentSuggestions;
import android.view.MenuItem;
import android.widget.NumberPicker;
import android.widget.TextView;

/**
 * The settings screen, displays fragments that contain preferences.
 * @author Aidan Follestad
 */
public class SettingsScreen extends PreferenceActivity  {

	private int lastTheme;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if(savedInstanceState != null && savedInstanceState.containsKey("lastTheme")) {
			lastTheme = savedInstanceState.getInt("lastTheme");
			setTheme(lastTheme);
		} else setTheme(Utilities.getTheme(getApplicationContext()));
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.pref_headers, target);
	}

	@Override
	public void onResume() {
		super.onResume();
		if(lastTheme == 0) lastTheme = Utilities.getTheme(getApplicationContext());
		else if(lastTheme != Utilities.getTheme(getApplicationContext())) {
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
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			startActivity(new Intent(this, TimelineScreen.class));
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public static class HelpFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.help_category);
			((Preference)findPreference("version")).setSummary(Utilities.getVersionName(getActivity()));
			((Preference)findPreference("build")).setSummary(Utilities.getVersionCode(getActivity()));
			((Preference)findPreference("share_boid")).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, getString(R.string.share_boid_content)), getString(R.string.share_boid_title)));
					return false;
				}
			});
			((Preference)findPreference("boidapp")).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					startActivity(new Intent(getActivity(), ProfileScreen.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("screen_name", "boidapp"));
					return false;
				}
			});
			((Preference)findPreference("donate")).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					BillingController.requestPurchase(getActivity(), "com.teamboid.twitter.donate", true);
					return false;
				}
			});

		}
	}
	public static class GeneralFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.general_category);
			((SwitchPreference)findPreference("enable_ssl")).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					int index = 0;
					for(Account acc : AccountService.getAccounts()) {
						ConfigurationBuilder cb = new ConfigurationBuilder().setOAuthConsumerKey("5LvP1d0cOmkQleJlbKICtg")
								.setOAuthConsumerSecret("j44kDQMIDuZZEvvCHy046HSurt8avLuGeip2QnOpHKI")
								.setOAuthAccessToken(acc.getToken()).setOAuthAccessTokenSecret(acc.getSecret())
								.setUseSSL((Boolean)newValue).setJSONStoreEnabled(true);
						final Twitter toAdd = new TwitterFactory(cb.build()).getInstance();
						acc.setClient(toAdd);
						AccountService.setAccount(index, acc);
						index++;
					}
					return true;
				}
			});
			final Preference clearHistory = (Preference)findPreference("clear_search_history");
			clearHistory.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					clearHistory.setEnabled(false);
					SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getActivity(),
							SearchSuggestionsProvider.AUTHORITY, SearchSuggestionsProvider.MODE);
					suggestions.clearHistory();
					return false;
				}
			});
		}
	}
	public static class ComposerFragment extends PreferenceFragment {
		private void setMediaName(){
			final Preference uploadService = (Preference)findPreference("upload_service");
			String pref = uploadService.getSharedPreferences().getString("upload_service", "twitter");
			try{
				uploadService.setSummary(MediaUtilities.getMediaServices(true, getActivity()).get(pref).name);
			} catch(Exception e) { e.printStackTrace(); }
		}
		public static int SELECT_MEDIA;

		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data){
			if(requestCode == SELECT_MEDIA && resultCode == RESULT_OK){
				PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit().putString("upload_service", data.getStringExtra("service")).commit();
				setMediaName();
			}
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.composer_category);
			final Preference uploadService = (Preference)findPreference("upload_service"); 
			setMediaName();
			uploadService.setOnPreferenceClickListener(new OnPreferenceClickListener(){
				@Override
				public boolean onPreferenceClick(final Preference arg0) {
					Intent i = new Intent(getActivity(), SelectMediaScreen.class);
					startActivityForResult(i, SELECT_MEDIA);
					return true;
				}
			});
			uploadService.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					uploadService.setSummary(newValue.toString());
					return true;
				}
			});
		}
	}
	public static class AppearanceFragment extends PreferenceFragment {
		
		@Override		
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.appearance_category);
			((ListPreference)findPreference("boid_theme")).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					getActivity().recreate();
					return true;
				}
			});
			findPreference("font_size").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					showFontDialog();
					return false;
				}
			});
		}
		
		private void showFontDialog() {
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
			Dialog diag = new Dialog(getActivity());
			diag.setContentView(R.layout.font_size_dialog);
			diag.setCancelable(true);
			diag.setTitle(R.string.font_size);
			final TextView display = (TextView)diag.findViewById(R.id.fontSizeDialogExample);
			final NumberPicker picker = (NumberPicker)diag.findViewById(R.id.fontSizePicker);
			picker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
				@Override
				public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
					display.setText(getString(R.string.boid_is_awesome) + " " + newVal + "pt");
					display.setTextSize(newVal);
					prefs.edit().putString("font_size", Integer.toString(newVal)).apply();
				}
			});
			picker.setMinValue(11);
			picker.setMaxValue(26);
			picker.setValue(Integer.parseInt(prefs.getString("font_size", "16")));
			diag.show();
		}
	}
	public static class NotificationsFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.notifications_category);
			((SwitchPreference)findPreference("c2dm")).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					if((Boolean)newValue == true){
						// Register!
						Intent registrationIntent = new Intent("com.google.android.c2dm.intent.REGISTER");
						registrationIntent.putExtra("app", PendingIntent.getBroadcast(getActivity(), 0, new Intent(getActivity(), PushReceiver.class), 0)); // boilerplate
						registrationIntent.putExtra("sender", PushReceiver.SENDER_EMAIL);
						getActivity().startService(registrationIntent);
						// TODO: Maybe add some kind of progress?
					} else{
						// TODO: Deregister
					}
					return true;
				}
			});
		}
	}
}