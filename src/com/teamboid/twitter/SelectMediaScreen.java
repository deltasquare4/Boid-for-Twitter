package com.teamboid.twitter;

import java.util.Map.Entry;

import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.teamboid.twitter.utilities.MediaUtilities;
import com.teamboid.twitter.utilities.Utilities;
import com.teamboid.twitterapi.media.ExternalMediaService;
import com.teamboid.twitterapi.media.ExternalMediaService.AuthorizationNeeded;
import com.teamboid.twitterapi.media.MediaServices;

public class SelectMediaScreen extends PreferenceActivity {

	public class MediaPreference extends Preference {
		public boolean checked;
		public boolean needsConfig;
		public ExternalMediaService m;
		public OnPreferenceClickListener callback;

		public MediaPreference(Context context, ExternalMediaService m,
				final String key) {
			super(context);
			try {
				setTitle(m.getServiceName());
			} catch (Exception e) {
				e.printStackTrace();
			}
			needsConfig = (m.getNeededAuthorization() != AuthorizationNeeded.NONE);
			this.m = m;

			Uri.parse("boid://finishconfig/" + key);
		}

		boolean isLoggedIn() {
			SharedPreferences sp = getSharedPreferences();
			switch (m.getNeededAuthorization()) {
			default:
				return false;
			case MAIL_AND_PASSWORD:
				return !sp.getString(getKey() + "-username", "").equals("");
			case OAUTH:
				return !sp.getString(getKey() + "-token", "").equals("");
			}
		}

		@Override
		public View getView(View convertView, ViewGroup parent) {
			if (convertView == null)
				convertView = LayoutInflater.from(getContext()).inflate(
						R.layout.media_service, null);
			RadioButton r = (RadioButton) convertView.findViewById(R.id.radio);
			boolean configured = isLoggedIn();
			if (needsConfig)
				r.setEnabled(configured);
			if (!needsConfig || (needsConfig && configured))
				r.setText(this.getTitle());
			else
				r.setText(getContext().getResources()
						.getString(R.string.service_needs_config)
						.replace("{service}", this.getTitle()));
			r.setChecked(checked);
			r.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
					callback.onPreferenceClick(MediaPreference.this);
				}
			});
			ImageButton c = (ImageButton) convertView.findViewById(R.id.button);
			c.setVisibility(needsConfig ? View.VISIBLE : View.GONE);

			c.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					AlertDialog.Builder ab = new AlertDialog.Builder(
							SelectMediaScreen.this);
					ab.setTitle(m.getServiceName());
					View v = LayoutInflater.from(SelectMediaScreen.this)
							.inflate(R.layout.oauth_media_panel, null);

					TextView t = (TextView) v.findViewById(R.id.notes);
					t.setText(R.string.imgur_note);
					ab.setView(v);
					ab.setNegativeButton(android.R.string.cancel,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							});
					AlertDialog dlg = ab.create();
					configureView(dlg, v);
					dlg.show();
				}

				public void configureView(final DialogInterface dlg,
						final View v) {
					final SharedPreferences sp = getSharedPreferences();
					boolean isLoggedIn = isLoggedIn();
					String user = sp.getString(getKey() + "-url", "");

					TextView t = (TextView) v.findViewById(R.id.summary);
					if (!isLoggedIn)
						t.setText(R.string.not_logged_in);
					else
						t.setText(getString(R.string.logged_in_as).replace(
								"{user}", user));
					Button b = (Button) v.findViewById(R.id.action);

					if (!isLoggedIn) {
						b.setText(R.string.login);
						b.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View button) {
								requestAuth(dlg);
							}
						});
					} else {
						b.setText(R.string.logout);
						b.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View w) {
								sp.edit().remove(getKey() + "-token")
										.remove(getKey() + "-secret")
										.remove(getKey() + "-username")
										.remove(getKey() + "-password")
										.remove(getKey() + "-url").commit();
								configureView(dlg, v);
							}
						});
					}

				}

				public void requestAuth(final DialogInterface parent) {
					switch (m.getNeededAuthorization()) {
					default:
						break;
					case OAUTH:
						startOAuth();
						break;
					case MAIL_AND_PASSWORD:
						AlertDialog.Builder ab = new AlertDialog.Builder(
								SelectMediaScreen.this);

						ab.setTitle(getString(R.string.login_to_service)
								.replace("{service}", m.getServiceName()));
						LayoutInflater lf = LayoutInflater
								.from(SelectMediaScreen.this);
						final View v = lf.inflate(
								R.layout.user_and_pwd_login_panel, null);
						ab.setView(v);

						ab.setNegativeButton(android.R.string.cancel,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										dialog.dismiss();
									}
								});
						ab.setPositiveButton(R.string.login,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(
											final DialogInterface dialog,
											int which) {
										// Let's now save the data if we can
										final String email = ((EditText) v
												.findViewById(R.id.email))
												.getText().toString().trim();
										final String password = ((EditText) v
												.findViewById(R.id.password))
												.getText().toString();

										final ProgressDialog pd = new ProgressDialog(
												SelectMediaScreen.this);
										pd.setMessage(getText(R.string.please_wait));
										pd.show();

										new Thread(new Runnable() {

											@Override
											public void run() {
												try {
													m.setMailAndPassword(email,
															password);
													String username = m
															.getUserName();
													if (username == null)
														throw new Exception(
																"Username is null. This is illegal. Call the cops");

													SharedPreferences sp = PreferenceManager
															.getDefaultSharedPreferences(SelectMediaScreen.this);
													sp.edit()
															.putString(
																	getKey()
																			+ "-username",
																	email)
															.putString(
																	getKey()
																			+ "-password",
																	password)
															.putString(
																	getKey()
																			+ "-url",
																	username)
															.commit();

													pd.dismiss();
													setupPreferences();
													dialog.dismiss();
													parent.dismiss();
												} catch (Exception e) {
													e.printStackTrace();
													runOnUiThread(new Runnable() {

														@Override
														public void run() {
															pd.dismiss();
															Toast.makeText(
																	SelectMediaScreen.this,
																	getText(R.string.error_str),
																	Toast.LENGTH_SHORT)
																	.show();
														}

													});
												}
											}

										}).start();
									}

								});

						ab.show();
						break;
					}
				}

				void startOAuth() {
					final ProgressDialog pd = new ProgressDialog(
							SelectMediaScreen.this);
					pd.setMessage(getText(R.string.please_wait));
					pd.show();

					new Thread(new Runnable() {

						@Override
						public void run() {
							try {
								OAuthService auth = MediaUtilities
										.buildAuthService(getKey(),
												"boid://finishconfig/"
														+ getKey());
								requestToken = auth.getRequestToken();
								final String url = auth
										.getAuthorizationUrl(requestToken);

								runOnUiThread(new Runnable() {

									@Override
									public void run() {
										pd.dismiss();

										Intent i = new Intent(
												Intent.ACTION_VIEW);
										i.setData(Uri.parse(url));
										startActivity(i);
									}

								});
							} catch (Exception e) {
								e.printStackTrace();
								runOnUiThread(new Runnable() {

									@Override
									public void run() {
										pd.dismiss();
										Toast.makeText(SelectMediaScreen.this,
												getText(R.string.error_str),
												Toast.LENGTH_SHORT).show();
									}

								});
							}
						}

					}).start();
				}
			});
			return convertView;
		}
	}

	private int lastTheme;
	private static Token requestToken;

	@Override
	public void onResume() {
		super.onResume();
		if (getIntent().getData() != null)
			onNewIntent(getIntent());
	}

	@Override
	public void onNewIntent(final Intent intent) {
		Log.d("n", "NEW INTENT");
		if (intent.getData() != null) {
			if (!intent.getData().getScheme().equals("boid"))
				return;
			Log.d("i", "new intent");
			final String key = intent.getData().getPathSegments().get(0);
			Log.d("i", key);

			final ProgressDialog pd = new ProgressDialog(SelectMediaScreen.this);
			pd.setMessage(getText(R.string.please_wait));
			pd.show();

			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						OAuthService auth = MediaUtilities
								.buildAuthService(key);
						Token accessToken = auth.getAccessToken(requestToken,
								new Verifier(intent.getData()
										.getQueryParameter("oauth_verifier")));

						SharedPreferences sp = PreferenceManager
								.getDefaultSharedPreferences(SelectMediaScreen.this);

						MediaServices.setupServices();
						ExternalMediaService ems = MediaServices
								.getService(key);
						ems.setAuthorized(auth, accessToken);
						String user = ems.getUserName();

						sp.edit()
								.putString(key + "-token",
										accessToken.getToken())
								.putString(key + "-secret",
										accessToken.getSecret())
								.putString(key + "-url", user).commit();
						setupPreferences();
						pd.dismiss();
					} catch (Exception e) {
						e.printStackTrace();
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								pd.dismiss();
								Toast.makeText(SelectMediaScreen.this,
										getText(R.string.error_str),
										Toast.LENGTH_SHORT).show();
							}

						});
					}
				}

			}).start();

		}
	}

	private void setupPreferences() {
		String pref = PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).getString("upload_service", "twitter");
		MediaServices.setupServices();
		// TODO this function is deprecated and needs to be replaced.
		getPreferenceScreen().removeAll();

		for (final Entry<String, ExternalMediaService> entry : MediaServices.services
				.entrySet()) {
			MediaPreference m = new MediaPreference(this, entry.getValue(),
					entry.getKey());
			m.setKey(entry.getKey());
			m.setPersistent(false);
			m.checked = pref.equals(entry.getKey());
			m.callback = new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference pref) {
					setResult(RESULT_OK, new Intent().putExtra("service", entry.getKey()));
					finish();
					return true;
				}
			};
			// TODO this function is deprecated and needs to be replaced.
			getPreferenceScreen().addPreference(m);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey("lastTheme")) {
				lastTheme = savedInstanceState.getInt("lastTheme");
				setTheme(lastTheme);
			} else
				setTheme(Utilities.getTheme(getApplicationContext()));
		} else
			setTheme(Utilities.getTheme(getApplicationContext()));
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		// TODO this function is deprecated and needs to be replaced.
		addPreferencesFromResource(R.xml.select_media);
		setupPreferences();
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
}