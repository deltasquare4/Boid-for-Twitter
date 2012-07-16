package com.teamboid.twitter;

import java.util.List;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import net.robotmedia.billing.BillingController;

import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.MediaUtilities;
import com.teamboid.twitter.utilities.Utilities;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.internal.http.HttpResponse;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.Toast;

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
			//startActivity(new Intent(this, TimelineScreen.class));
			//finish();
			
			super.onBackPressed(); //Back button should go back, not restart a new activity
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
		ProgressDialog pd;
		BroadcastReceiver pupdater;
		
		@Override
		public void onDestroy (){
			getActivity().unregisterReceiver(pupdater);
		}
		
		boolean realChange = false;
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			pd = new ProgressDialog(getActivity());
			pd.setMessage(getText(R.string.push_wait));
			pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			
			pupdater = new BroadcastReceiver(){

				@Override
				public void onReceive(Context arg0, Intent arg1) {
					pd.setProgress(arg1.getIntExtra("progress", 1000));
					if(arg1.getIntExtra("progress", 0) == 1000){
						pd.dismiss();
						if(arg1.getBooleanExtra("error", false) == true){
							Toast.makeText(getActivity(), R.string.push_error, Toast.LENGTH_SHORT).show();
						} else{
							Toast.makeText(getActivity(), R.string.push_registered, Toast.LENGTH_SHORT).show();
							findPreference("c2dm").getSharedPreferences().edit().putBoolean("c2dm", true).commit();
							realChange = true;
							((SwitchPreference)findPreference("c2dm")).setChecked(true);
							realChange = false;
						}
					}
				}
				
			};
			IntentFilter i = new IntentFilter();
			i.addAction("com.teamboid.twitter.PUSH_PROGRESS");
			getActivity().registerReceiver(pupdater, i);
			
			addPreferencesFromResource(R.xml.notifications_category);
			((SwitchPreference)findPreference("c2dm")).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					if(realChange == true) return true;
					
					if((Boolean)newValue == true){
						// Register!
						Intent registrationIntent = new Intent("com.google.android.c2dm.intent.REGISTER");
						registrationIntent.putExtra("app", PendingIntent.getBroadcast(getActivity(), 0, new Intent(getActivity(), PushReceiver.class), 0)); // boilerplate
						registrationIntent.putExtra("sender", PushReceiver.SENDER_EMAIL);
						getActivity().startService(registrationIntent);
						
						pd.setProgress(0);
						pd.show();
					} else{
						// Unregister
						pd.setProgress(0);
						pd.show();
						
						new Thread(new Runnable(){

							@Override
							public void run() {
								try{
									DefaultHttpClient dhc = new DefaultHttpClient();
									HttpGet get = new HttpGet(PushReceiver.SERVER + "/remove/" + AccountService.getCurrentAccount().getId());
									org.apache.http.HttpResponse r = dhc.execute(get);
									if(r.getStatusLine().getStatusCode() == 200 ){
										getActivity().runOnUiThread(new Runnable(){

											@Override
											public void run() {
												pd.dismiss();
												
												// Update Switch
												findPreference("c2dm").getSharedPreferences().edit().putBoolean("c2dm", false).commit();
												realChange = true;
												((SwitchPreference)findPreference("c2dm")).setChecked(false);
												realChange = false;
												
												Toast.makeText(getActivity(), R.string.push_updated, Toast.LENGTH_SHORT).show();
											}
											
										});
									} else{
										throw new Exception("NON 200 RESPONSE ;__;");
									}
								} catch(Exception e){
									e.printStackTrace();
									getActivity().runOnUiThread( new Runnable(){
										@Override
										public void run(){
											Toast.makeText(getActivity(), R.string.push_error, Toast.LENGTH_SHORT).show();
										}
									});
								}
							}
							
						});
					}
					return false;
				}
			});
			
			findPreference("c2dm_mentions").setOnPreferenceChangeListener( new RemotePushSettingChange( "replies" ) );
			findPreference("c2dm_messages").setOnPreferenceChangeListener( new RemotePushSettingChange( "dm" ) );
		}
		
		public class RemotePushSettingChange implements Preference.OnPreferenceChangeListener{
			String remote_setting;
			
			public RemotePushSettingChange(String remote_setting){
				this.remote_setting = remote_setting;
			}
			
			@Override
			public boolean onPreferenceChange(final Preference preference,
					final Object newValue) {
				
				pd.setProgress(0);
				pd.show();
				
				new Thread(new Runnable(){

					@Override
					public void run() {
						try{
							DefaultHttpClient dhc = new DefaultHttpClient();
							HttpGet get = new HttpGet(PushReceiver.SERVER +
													"/edit/" +
													AccountService.getCurrentAccount().getId() + "/" +
													remote_setting + "/" + ( (Boolean)newValue ? "on" : "off" ) );
							org.apache.http.HttpResponse r = dhc.execute(get);
							if(r.getStatusLine().getStatusCode() == 200 ){
								getActivity().runOnUiThread(new Runnable(){

									@Override
									public void run() {
										pd.dismiss();
										
										Toast.makeText(getActivity(), R.string.push_updated, Toast.LENGTH_SHORT).show();
									}
									
								});
							} else{
								throw new Exception("NON 200 RESPONSE ;__;");
							}
						} catch(Exception e){
							e.printStackTrace();
							getActivity().runOnUiThread( new Runnable(){
								@Override
								public void run(){
									Toast.makeText(getActivity(), R.string.push_error, Toast.LENGTH_SHORT).show();
									((SwitchPreference)preference).setChecked( !(Boolean)newValue );
								}
							});
						}
					}
					
				});
				
				return true;
			}
			
		}
	}
}