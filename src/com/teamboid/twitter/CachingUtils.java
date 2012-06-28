package com.teamboid.twitter;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * Convenience methods.
 * @author Aidan Follestad
 */
public class CachingUtils {

	public static void clearCache(Context context) {
		File cacheDir = new File(context.getCacheDir().getPath());
		if(cacheDir.listFiles() != null && cacheDir.listFiles().length > 0) {
			for(File fi : cacheDir.listFiles()) fi.delete();
		}
	}
	public static void clearMediaCache(Context context) {
		File cacheDir = new File(context.getExternalCacheDir().getPath());
		if(cacheDir.exists() && cacheDir.listFiles() != null && cacheDir.listFiles().length > 0) {
			for(File fi : cacheDir.listFiles()) fi.delete();
		}
		File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Boid");
		if(storageDir.exists() && storageDir.listFiles() != null && storageDir.listFiles().length > 0) {
			for(File fi : storageDir.listFiles()) fi.delete();
		}
	}
}