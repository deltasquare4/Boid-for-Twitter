package com.teamboid.twitter;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

/**
 * The activity that represents the mute keywords manager.
 * @author Aidan Follestad
 */

public class MutingManager extends ListActivity {

	private ArrayAdapter<String> adapt;
	private int lastTheme;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		if(savedInstanceState != null) {
	    	if(savedInstanceState.containsKey("lastTheme")) {
	    		lastTheme = savedInstanceState.getInt("lastTheme");
	    		setTheme(lastTheme);
	    	} else setTheme(Utilities.getTheme(getApplicationContext()));
	    }  else setTheme(Utilities.getTheme(getApplicationContext()));
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.muting_manager);
		adapt = new ArrayAdapter<String>(this, R.layout.trends_list_item);
		setListAdapter(adapt);
		final EditText input = (EditText)findViewById(R.id.keywordInput);
		input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if(actionId == EditorInfo.IME_ACTION_GO) {
					String term = input.getText().toString().trim();
					if(term.length() == 0) return true;
					else if(addKeyword(term)) input.setText("");
				}
				return false;
			}
		});
		getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int index, long id) {
				final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				String prefName = Long.toString(AccountService.getCurrentAccount().getId()) + "_muting";
				ArrayList<String> cols = Utilities.jsonToArray(getApplicationContext(), prefs.getString(prefName, ""));
				cols.remove(index);
				prefs.edit().putString(prefName, Utilities.arrayToJson(getApplicationContext(), cols)).commit();
				Vibrator v = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
				v.vibrate(100);
				loadKeywords();
				return false;
			}
		});
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if(lastTheme == 0) lastTheme = Utilities.getTheme(getApplicationContext());
		else if(lastTheme != Utilities.getTheme(getApplicationContext())) {
			lastTheme = Utilities.getTheme(getApplicationContext());
			recreate();
			return;
		}
		loadKeywords();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if(isFinishing()) {
			setResult(600, null);
			finish();
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt("lastTheme", lastTheme);
		super.onSaveInstanceState(outState);
	}
	
	private void loadKeywords() {
		adapt.clear();
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String prefName = Long.toString(AccountService.getCurrentAccount().getId()) + "_muting";
		ArrayList<String> cols = Utilities.jsonToArray(this, prefs.getString(prefName, ""));
		if(cols.size() > 0) {
			for(String c : cols) adapt.add(c);
			adapt.notifyDataSetChanged();
		}
	}
	private boolean addKeyword(String term) {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String prefName = Long.toString(AccountService.getCurrentAccount().getId()) + "_muting";
		ArrayList<String> cols = Utilities.jsonToArray(this, prefs.getString(prefName, ""));
		if(!cols.contains(term)) {
			cols.add(term);
			adapt.add(term);
			adapt.notifyDataSetChanged();
			prefs.edit().putString(prefName, Utilities.arrayToJson(this, cols)).commit();
			Vibrator v = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
			v.vibrate(100);
			return true;
		}
		return false;
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
}