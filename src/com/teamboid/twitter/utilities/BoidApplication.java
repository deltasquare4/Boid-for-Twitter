package com.teamboid.twitter.utilities;

import android.app.Application;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Application class. Handles low-memory
 * 
 * Uses parts of
 * https://github.com/cyrilmottier/GreenDroid/blob/master/GreenDroid/src/greendroid/app/GDApplication.java
 * @author kennydude
 *
 */
public class BoidApplication extends Application {
	public BoidApplication(){
		mLowMemoryListeners = new ArrayList<WeakReference<OnLowMemoryListener>>();
	}
	
	/**
     * Used for receiving low memory system notification. You should definitely
     * use it in order to clear caches and not important data every time the
     * system needs memory.
     * 
     * @author Cyril Mottier
     * @see registerOnLowMemoryListener(OnLowMemoryListener)
     * @see unregisterOnLowMemoryListener(OnLowMemoryListener)
     */
    public static interface OnLowMemoryListener {
        
        /**
         * Callback to be invoked when the system needs memory.
         */
        public void onLowMemoryReceived();
    }
    
    private ArrayList<WeakReference<OnLowMemoryListener>> mLowMemoryListeners;
    
    /**
     * Add a new listener to registered {@link OnLowMemoryListener}.
     * 
     * @param listener The listener to unregister
     * @see OnLowMemoryListener
     */
    public void registerOnLowMemoryListener(OnLowMemoryListener listener) {
        if (listener != null) {
            mLowMemoryListeners.add(new WeakReference<OnLowMemoryListener>(listener));
        }
    }

    /**
     * Remove a previously registered listener
     * 
     * @param listener The listener to unregister
     * @see OnLowMemoryListener
     */
    public void unregisterOnLowMemoryListener(OnLowMemoryListener listener) {
        if (listener != null) {
            int i = 0;
            while (i < mLowMemoryListeners.size()) {
                final OnLowMemoryListener l = mLowMemoryListeners.get(i).get();
                if (l == null || l == listener) {
                    mLowMemoryListeners.remove(i);
                } else {
                    i++;
                }
            }
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.d("boid", "low memory");
        int i = 0;
        while (i < mLowMemoryListeners.size()) {
            final OnLowMemoryListener listener = mLowMemoryListeners.get(i).get();
            if (listener == null) {
                mLowMemoryListeners.remove(i);
            } else {
                listener.onLowMemoryReceived();
                i++;
            }
        }
    }
}
