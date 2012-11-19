package com.teamboid.twitter.columns;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

/**
 * TIMELINE CACHING
 * 
 * *throws a big party*
 * 
 * @author kennydude
 *
 */
public class ColumnCacheManager {
	static File getCacheFile( Context c, String column ){
		File r = new File( c.getCacheDir(), "columnCache." + column + ".cache" );
		try{ new File(r.getParent()).mkdirs(); } catch(Exception e){ e.printStackTrace(); } 
		try{ r.createNewFile(); } catch(Exception e){ e.printStackTrace(); }
		return r;
	}
	
	/**
	 * Save cache to disk
	 * 
	 * Column should be something like <userid>.mentions
	 * 
	 * @param column
	 * @param contents
	 */
	public static void saveCache( Context c, String column, List<Serializable> contents ){
		try{
			FileOutputStream fos = new FileOutputStream(getCacheFile(c, column));
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			Log.d("cache", "saving to " + getCacheFile(c, column).getAbsolutePath());
			for(Serializable object : contents){
				oos.writeObject(object);
			}
			oos.close();
			fos.close();
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * BE PREPARED FOR NULL
	 */
	public static List<Serializable> getCache(Context c, String column){
		try{
			List<Serializable> contents = new ArrayList<Serializable>();
			Log.d("cache", "reading from " + getCacheFile(c, column).getAbsolutePath());
			FileInputStream fis = new FileInputStream(getCacheFile(c, column));
			if(fis.available() == 0){ fis.close(); return null; }
			
			ObjectInputStream ooi = new ObjectInputStream(fis);
			while(true){
				try{
					contents.add((Serializable) ooi.readObject());
				} catch(EOFException e){
					break;
				}
			}
			fis.close();
			ooi.close();
			return contents;
		} catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
}
