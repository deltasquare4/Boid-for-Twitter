package com.teamboid.twitter.contactsync;

import java.io.FileOutputStream;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

public class ContactViewNotifyService extends IntentService {

	public ContactViewNotifyService() {
		super("Cv");
	}

	@Override
	protected void onHandleIntent(final Intent intent) {
		/*
		 * Cursor c = this.getContentResolver().query(intent.getData(), new
		 * String[]{ContactsContract.Data.DATA1}, ContactsContract.Data.MIMETYPE
		 * + "='vnd.android.cursor.item/vnd.com.teamboid.twitter.account'",
		 * null, null);
		 * 
		 * c.moveToNext(); String uname = c.getString(0); Log.d("cv", uname);
		 */
		Log.d("cv", intent.getDataString());
	}

	private static void saveBitmapToRawContact(Context context,
			long rawContactId, byte[] photo) throws Exception {
		Uri rawContactUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI,
				rawContactId);
		Uri outputFileUri = Uri.withAppendedPath(rawContactUri,
				RawContacts.DisplayPhoto.CONTENT_DIRECTORY);
		AssetFileDescriptor descriptor = context.getContentResolver()
				.openAssetFileDescriptor(outputFileUri, "rw");
		FileOutputStream stream = descriptor.createOutputStream();
		try {
			stream.write(photo);
		} finally {
			stream.close();
			descriptor.close();
		}
	}
}
