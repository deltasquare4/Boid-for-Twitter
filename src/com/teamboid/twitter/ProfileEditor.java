package com.teamboid.twitter;

import android.app.*;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.handlerexploit.prime.RemoteImageView;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;
import com.teamboid.twitterapi.user.User;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * The activity that represents the profile viewer.
 * 
 * @author Aidan Follestad
 */
public class ProfileEditor extends Activity implements
		PopupMenu.OnMenuItemClickListener {

	private int lastTheme;
	private boolean showProgress;
	private Account toSet;
	private int index;
	private File newProfileImg;
	private Uri newProfileUri;
	private File cropResultImg;

	public static final int CROP_RESULT = 400;
	public static final int CAMERA_SELECT_INTENT = 500;
	public static final int GALLERY_SELECT_INTENT = 600;

	public void showProgress(boolean visible) {
		if (showProgress == visible)
			return;
		showProgress = visible;
		setProgressBarIndeterminateVisibility(visible);
		if (showProgress) {
			findViewById(R.id.nameTxt).setEnabled(false);
			findViewById(R.id.urlTxt).setEnabled(false);
			findViewById(R.id.locationTxt).setEnabled(false);
			findViewById(R.id.bioTxt).setEnabled(false);
		} else {
			findViewById(R.id.nameTxt).setEnabled(true);
			findViewById(R.id.urlTxt).setEnabled(true);
			findViewById(R.id.locationTxt).setEnabled(true);
			findViewById(R.id.bioTxt).setEnabled(true);
		}
		invalidateOptionsMenu();
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey("lastTheme")) {
				lastTheme = savedInstanceState.getInt("lastTheme");
				setTheme(lastTheme);
			} else
				setTheme(Utilities.getTheme(getApplicationContext()));
		} else
			setTheme(Utilities.getTheme(getApplicationContext()));
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		setProgressBarIndeterminateVisibility(false);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.profile_editor);
		setTitle("@" + getIntent().getStringExtra("screen_name"));
		loadAccount();
	}

	public void showPopup(View v) {
		PopupMenu popup = new PopupMenu(this, v);
		popup.inflate(R.menu.camera_gallery_choosepopup);
		popup.setOnMenuItemClickListener(this);
		popup.show();
	}

	private void loadAccount() {
		showProgress(true);
		final ArrayList<Account> accs = AccountService.getAccounts();

		/*
		 * Load the account's user object again to make 100% sure the
		 * information is up to date, after the accounts been cached in local
		 * preferences and everything.
		 */
		for (int i = 0; i < accs.size(); i++) {
			if (accs.get(i).getUser().getScreenName()
					.equals(getIntent().getStringExtra("screen_name"))) {
				index = i;
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							toSet = accs.get(index);
							final User user = toSet.getClient()
									.verifyCredentials();
							toSet.setUser(user);
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									displayAccount();
								}
							});
						} catch (final Exception e) {
							e.printStackTrace();
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(
											getApplicationContext(),
											getString(R.string.failed_get_accountinfo),
											Toast.LENGTH_LONG).show();
									/*
									 * Display the last known local account
									 * information if it fails to re-get the
									 * information.
									 */
									displayAccount();
								}
							});
						}
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								showProgress(false);
							}
						});
					}
				}).start();
				break;
			}
		}

	}

	private void displayAccount() {
		((EditText) findViewById(R.id.nameTxt)).setText(toSet.getUser()
				.getName());
		((EditText) findViewById(R.id.urlTxt))
				.setText(toSet.getUser().getUrl());
		((EditText) findViewById(R.id.locationTxt)).setText(toSet.getUser()
				.getLocation());
		((EditText) findViewById(R.id.bioTxt)).setText(toSet.getUser()
				.getDescription());
		((RemoteImageView) findViewById(R.id.profilePic)).setImageURL(toSet
				.getUser().getProfileImageUrl());
		/*
		 * Update the account's information in the account service. This
		 * function also updates the account in the local preferences cache.
		 */
		AccountService.setAccount(this, index, toSet);
	}

	private void commitChanges() {
		showProgress(true);
		final String name = ((EditText) findViewById(R.id.nameTxt)).getText()
				.toString();
		final String url = ((EditText) findViewById(R.id.urlTxt)).getText()
				.toString();
		final String location = ((EditText) findViewById(R.id.locationTxt))
				.getText().toString();
		final String description = ((EditText) findViewById(R.id.bioTxt))
				.getText().toString();

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					toSet.getClient().updateProfileImage(cropResultImg);
				} catch (final Exception e) {
					e.printStackTrace();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							showProgress(false);
							Toast.makeText(getApplicationContext(),
									e.getLocalizedMessage(), Toast.LENGTH_SHORT)
									.show();
						}
					});
					return;
				}
				if (cropResultImg != null) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (cropResultImg.exists())
								cropResultImg.delete();
							cropResultImg = null;
						}
					});
				}

				try {
					final User user = toSet.getClient().updateProfile(name,
							url, location, description);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							toSet.setUser(user);
							displayAccount();
						}
					});
				} catch (final Exception e) {
					e.printStackTrace();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							showProgress(false);
							Toast.makeText(getApplicationContext(),
									e.getLocalizedMessage(), Toast.LENGTH_SHORT)
									.show();
						}
					});
					return;
				}
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						showProgress(false);
						finish();
					}
				});
			}
		}).start();
	}

	private void crop(Uri imageUri) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(imageUri, "image/*");
		if (!Utilities.isIntentAvailable(this, intent)) {
			Toast.makeText(getApplicationContext(), R.string.no_cropapp_str,
					Toast.LENGTH_LONG).show();
			return;
		}
		intent.putExtra("crop", "true");
		intent.putExtra("outputX", 70);
		intent.putExtra("outputY", 70);
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		intent.putExtra("scale", true);
		cropResultImg = new File(Utilities.generateImageFileName(this));
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(cropResultImg));
		startActivityForResult(intent, CROP_RESULT);
	}

	private void captureImage() {
		newProfileImg = null;
		newProfileUri = null;
		cropResultImg = null;
		if (!Utilities.isIntentAvailable(this, MediaStore.ACTION_IMAGE_CAPTURE)) {
			Toast.makeText(getApplicationContext(), R.string.no_camera_app,
					Toast.LENGTH_SHORT).show();
			return;
		}
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		newProfileImg = new File(Utilities.generateImageFileName(this));
		takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
				Uri.fromFile(newProfileImg));
		startActivityForResult(takePictureIntent, CAMERA_SELECT_INTENT);
	}

	private void selectImage() {
		newProfileImg = null;
		newProfileUri = null;
		cropResultImg = null;
		try {
			newProfileImg = Utilities.createImageFile(this);
			Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT, null)
					.setType("image/*")
					.putExtra(MediaStore.EXTRA_OUTPUT,
							Uri.fromFile(newProfileImg))
					.putExtra("outputFormat", Bitmap.CompressFormat.PNG.name());
			startActivityForResult(galleryIntent, GALLERY_SELECT_INTENT);
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(getApplicationContext(), e.getLocalizedMessage(),
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_CANCELED) {
			if (newProfileImg != null) {
				if (newProfileImg.exists())
					newProfileImg.delete();
			}
			if (cropResultImg != null) {
				if (cropResultImg.exists())
					cropResultImg.delete();
			}
			newProfileImg = null;
			newProfileUri = null;
			cropResultImg = null;
			return;
		}
		if (requestCode == GALLERY_SELECT_INTENT
				|| requestCode == CAMERA_SELECT_INTENT) {
			if (ComposerScreen.getFileSize(newProfileImg) == 0) {
				Log.d("e", "Empty File. Using " + intent.getData().toString());
				newProfileImg = null;
				newProfileUri = intent.getData();
			}
			if (newProfileUri != null) {
				crop(newProfileUri);
			} else {
				crop(Uri.fromFile(newProfileImg));
			}
		} else if (requestCode == CROP_RESULT) {
			if (newProfileImg != null && newProfileImg.exists()) {
				newProfileImg.delete();
				newProfileImg = null;
			}
			newProfileUri = null;
			((RemoteImageView) findViewById(R.id.profilePic))
					.setImageBitmap(BitmapFactory.decodeFile(cropResultImg
							.getPath()));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.config_activity_actionbar, menu);
		if (showProgress) {
			menu.findItem(R.id.doneAction).setEnabled(false);
		} else
			menu.findItem(R.id.doneAction).setEnabled(true);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			super.onBackPressed();
			return true;
		case R.id.doneAction:
			commitChanges();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.cameraAction:
			captureImage();
			return true;
		case R.id.galleryAction:
			selectImage();
			return true;
		default:
			return false;
		}
	}
}