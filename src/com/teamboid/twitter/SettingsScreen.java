package com.teamboid.twitter;

import com.handlerexploit.prime.ImageManager;
import com.teamboid.twitter.contactsync.AndroidAccountHelper;
import com.teamboid.twitter.notifications.NotificationService;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;
import com.teamboid.twitter.views.AccountHeaderPreference;
import com.teamboid.twitterapi.media.MediaServices;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import me.kennydude.awesomeprefs.Preference;
import me.kennydude.awesomeprefs.Preference.OnPreferenceChangeListener;

import me.kennydude.awesomeprefs.ListPreference;
import me.kennydude.awesomeprefs.Preference.OnPreferenceClickListener;
import me.kennydude.awesomeprefs.PreferenceActivity;
import me.kennydude.awesomeprefs.PreferenceCategory;
import me.kennydude.awesomeprefs.PreferenceFragment;
import me.kennydude.awesomeprefs.SwitchPreference;

import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.SearchRecentSuggestions;
import android.util.Log;
import android.view.MenuItem;
import android.widget.NumberPicker;
import android.widget.TextView;

/**
 * The settings screen, displays fragments that contain preferences.
 * @author Aidan Follestad
 */
@SuppressWarnings("rawtypes")
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
		
		addHeaders(R.xml.pref_headers);
	}
		
	@Override
	public void finishAddingPreferences(){
		addAccounts();
	}
	
	public void addAccounts(){
		for(Preference p : this.getHeaderFragment().findPreferencesByClass("account")){
			p.remove();
		}
		
		if(AccountService.getAccounts().size() > 0){
			PreferenceCategory accHeader = (PreferenceCategory) getHeaderFragment().findPreference("accounts");
			
			for(Account acc : AccountService.getAccounts()){
				AccountHeaderPreference hp = new AccountHeaderPreference(this	, getHeaderFragment());
				hp.setTitle(acc.getUser().getScreenName());
				hp.url = Utilities.getUserImage(acc.getUser().getScreenName(), this, acc.getUser());
				hp.Class = "account";
				
				Bundle extras = new Bundle();
				extras.putString("accountName", acc.getUser().getScreenName());
				extras.putLong("accountId", acc.getId());
				hp.setFragment(AccountFragment.class, extras);
				
				accHeader.addPreference(hp);
			}
		}
		
		getHeaderFragment().findPreference("addNewAccount").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference pref) {
				AccountService.startAuth(SettingsScreen.this);
				return false;
			}
			
		});
	}

	/* Depracted
	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.pref_headers, target);
	}
	*/

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
			super.onBackPressed();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == AccountService.AUTH_CODE){
			if(resultCode == RESULT_OK){
				addAccounts();
			}
		}
	}
	
	public static class AccountFragment extends PreferenceFragment {

		@Override
		public void onDestroy() {
			super.onDestroy();
		}

		long accountId;
		SharedPreferences sp;
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			addPreferencesFromResource(R.xml.prefs_accounts);
			
			getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
			getActivity().setTitle(getArguments().getString("accName"));
			accountId = this.getArguments().getLong("accountId");
			sp = PreferenceManager.getDefaultSharedPreferences(getActivity());

			setKey("c2dm", accountId);
			setKey("c2dm_period", accountId);
			setKey("c2dm_mention", accountId);
			setKey("c2dm_dm", accountId);
			setKey("c2dm_vibrate", accountId);
			setKey("c2dm_ringtone", accountId);
			setKey("contactsync_on", accountId);
			
			findPreference("delete").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference pref) {
					final Account acc = AccountService.getAccount(accountId);
					AlertDialog.Builder ab = new AlertDialog.Builder(
							getActivity())
							.setTitle(R.string.remove_account)
							.setMessage(
									getString(R.string.confirm_remove_account)
											.replace("{account}",
													acc.getUser().getScreenName()))
							.setPositiveButton(R.string.yes_str,
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog,
												int which) {
											dialog.dismiss();
											AccountService.removeAccount(
													getActivity(), acc);
											getActivity().finish();
										}
									})
							.setNegativeButton(R.string.no_str,
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog,
												int which) {
											dialog.dismiss();
										}
									});
					ab.create().show();
					return true;
				}
			});

			SwitchPreference syncPref = ((SwitchPreference) findPreference(accountId
					+ "_contactsync_on"));
			syncPref.setChecked(ContentResolver.getSyncAutomatically(
					AndroidAccountHelper.getAccount(getActivity(),
							AccountService.getAccount(accountId)),
					ContactsContract.AUTHORITY));
			syncPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(final Preference preference,
						Object newValue) {
					AndroidAccountHelper.setServiceSync(ContactsContract.AUTHORITY, (Boolean)newValue,
							AndroidAccountHelper.getAccount(getActivity(), AccountService.getAccount(accountId)));
					return true;
				}
			});
			
			SwitchPreference c2dmPref = ((SwitchPreference) findPreference(accountId + "_c2dm"));
			c2dmPref.setChecked(ContentResolver.getSyncAutomatically(
					AndroidAccountHelper.getAccount(getActivity(),
							AccountService.getAccount(accountId)),
					NotificationService.AUTHORITY));
			c2dmPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(
						final Preference preference, Object newValue) {
					
					Log.d("boid", "notification switched");
					AndroidAccountHelper.setServiceSync(NotificationService.AUTHORITY, (Boolean)newValue,
							AndroidAccountHelper.getAccount(getActivity(), AccountService.getAccount(accountId)));
					
					return true;
				}
			});
			
			ListPreference c2dmPeriod = (ListPreference) findPreference(accountId + "_c2dm_period");
			c2dmPeriod.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					ContentResolver.addPeriodicSync(
							AndroidAccountHelper.getAccount(getActivity(), AccountService.getAccount(accountId)), 
							NotificationService.AUTHORITY,
							new Bundle(),
							Integer.parseInt((String)newValue) * 60);
					preference.setSummary(getString(R.string.notification_every).replace("{x}", (String)newValue));
					return true;
				}
			});
			c2dmPeriod.setSummary(getString(R.string.notification_every).replace("{x}", c2dmPeriod.getValue() ));
		}

		void setKey(String key, long accountId) {
			Preference p = findPreference("{user}_" + key);
			if(p == null) return;
			p.setKey(accountId + "_" + key);
		}
	}

	public static class HelpFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.help_category);
			findPreference("version").setSummary(Utilities.getVersionName(getActivity()));
			findPreference("build").setSummary(Utilities.getVersionCode(getActivity()));
			findPreference("share_boid").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, getString(R.string.share_boid_content)), getString(R.string.share_boid_title)));
					return false;
				}
			});
			findPreference("boidapp").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					startActivity(new Intent(getActivity(), ProfileScreen.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("screen_name", "boidapp"));
					return false;
				}
			});
			findPreference("donate").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					startActivity(new Intent(getActivity(), DonateActivity.class));
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
			findPreference("enable_ssl").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					for(Account acc : AccountService.getAccounts()) {
						acc.setClient(acc.getClient().setSslEnabled((Boolean)newValue));
						AccountService.setAccount(getActivity(), acc);
					}
					return true;
				}
			});
			final Preference clearHistory = findPreference("clear_search_history");
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
			findPreference("delete_media_cache").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference pref) {
					final ProgressDialog pd = new ProgressDialog(getActivity());
					pd.setMessage(getActivity().getString(R.string.please_wait));
					pd.show();
					
					new Thread(new Runnable(){
						public void run(){
							try{
								ImageManager.getInstance(getActivity()).mDiskLruCache.delete();
							} catch(Exception e){
								e.printStackTrace();
							}
							
							getActivity().runOnUiThread(new Runnable(){

								@Override
								public void run() {
									pd.dismiss();
									// TODO: Toast/Crouton something
								}
								
							});
						}
					}).start();
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
	            	MediaServices.setupServices();
	                uploadService.setSummary(MediaServices.services.get(pref).getServiceName());
	            } catch(Exception e) { e.printStackTrace(); }
	    }
        
	    public static int SELECT_MEDIA;
	
	    @Override
	    public void onActivityResult(int requestCode, int resultCode, Intent data){
	            if(requestCode == SELECT_MEDIA && resultCode == RESULT_OK){
	                    PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit().putString("upload_service", data.getStringExtra("service").toLowerCase()).commit();
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
			findPreference("boid_theme").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
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
			picker.setValue(Integer.parseInt(prefs.getString("font_size", "15")));
			diag.show();
		}
	}

    public static class NightModeFragment extends PreferenceFragment {
		
		public void displayThemeText(String theme) {
			final Preference pref = findPreference("night_mode_theme");
			if(theme.equals("0")) {
				pref.setSummary(getString(R.string.night_mode_switches_to).replace("{theme}", "dark"));
			} else if(theme.equals("1")) {
				pref.setSummary(getString(R.string.night_mode_switches_to).replace("{theme}", "light"));
			} else if(theme.equals("2")) {
				pref.setSummary(getString(R.string.night_mode_switches_to).replace("{theme}", "dark and light"));
			} else if(theme.equals("3")) {
				pref.setSummary(getString(R.string.night_mode_switches_to).replace("{theme}", "pure black"));
			}
		}
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.nightmode_category);
			findPreference("night_mode_time").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					ListPreference pref = (ListPreference)findPreference("night_mode_time");
					int index = pref.findIndexOfValue((String)newValue);
					pref.setSummary(getString(R.string.night_mode_enabled_at).replace("{time}", pref.getEntries()[index].toString()));
					return true;
				}
			});
			findPreference("night_mode_endtime").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					ListPreference pref = (ListPreference)findPreference("night_mode_endtime");
					int index = pref.findIndexOfValue((String)newValue);
					pref.setSummary(getString(R.string.night_mode_disabled_at).replace("{time}", pref.getEntries()[index].toString()));
					return true;
				}
			});
			findPreference("night_mode_theme").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					displayThemeText((String)newValue);
					return true;
				}
			});
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
			if(prefs.contains("night_mode_time")) {
				ListPreference pref = (ListPreference)findPreference("night_mode_time");
				int index = pref.findIndexOfValue(pref.getValue());
				if(index == -1) index = 7;
				pref.setSummary(getString(R.string.night_mode_enabled_at).replace("{time}", pref.getEntries()[index].toString()));
			}
			if(prefs.contains("night_mode_endtime")) {
				ListPreference pref = (ListPreference)findPreference("night_mode_endtime");
				int index = pref.findIndexOfValue(pref.getValue());
				if(index == -1) index = 6;
				pref.setSummary(getString(R.string.night_mode_disabled_at).replace("{time}", pref.getEntries()[index].toString()));
			}
			if(prefs.contains("night_mode_theme")) {
				displayThemeText(prefs.getString("night_mode_theme", "0"));
			}
		}
	}
}