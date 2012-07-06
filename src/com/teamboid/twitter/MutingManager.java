package com.teamboid.twitter;

import android.app.ListActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The activity that represents the muting rules manager.
 * @author Aidan Follestad
 */
public class MutingManager extends ListActivity {

	private MutingListAdapter adapt;
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
		adapt = new MutingListAdapter(this);
		setListAdapter(adapt);
		final EditText input = (EditText)findViewById(R.id.keywordInput);
		final Spinner typeSpin = (Spinner)findViewById(R.id.keywordType);
		input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if(actionId == EditorInfo.IME_ACTION_GO) {
					String term = input.getText().toString().trim().replace("@", "%40");
					final String[] types = getResources().getStringArray(R.array.muting_types);
					if(term.length() == 0) return true;
					else {
						switch(typeSpin.getSelectedItemPosition()) {
						default:
							if(adapt.add(term)) input.setText("");
							break;
						case 1:
							if(!term.startsWith("%40")) term = ("%40" + term);
							if(adapt.add(term + "@" + types[1])) input.setText("");
							break;
						case 2:
							if(adapt.add(term + "@" + types[2])) input.setText("");
							break;
						}
					}
				}
				return false;
			}
		});
		ArrayAdapter<CharSequence> typeAdapt = ArrayAdapter.createFromResource(this,
		        R.array.muting_types, R.layout.spinner_item_small);
		typeAdapt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		typeSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int index, long id) {
				switch(index) {
				default:
					input.setHint(R.string.muting_hint_keyword);
					break;
				case 1:
					input.setHint(R.string.muting_hint_user);
					break;
				case 2:
					input.setHint(R.string.muting_hint_app);
					break;
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) { }
		});
		typeSpin.setAdapter(typeAdapt);
		final ListView listView = getListView();
		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int index, long id) {
				Toast.makeText(MutingManager.this, R.string.swipe_to_delete_items, Toast.LENGTH_LONG).show();
				return false;
			}
		});
		SwipeDismissListViewTouchListener touchListener =
				new SwipeDismissListViewTouchListener(listView,
						new SwipeDismissListViewTouchListener.OnDismissCallback() {
							@Override
							public void onDismiss(ListView listView, int[] reverseSortedPositions) {
								adapt.remove(reverseSortedPositions);
							}
				});
		listView.setOnTouchListener(touchListener);
		listView.setOnScrollListener(touchListener.makeScrollListener());
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if(!prefs.contains("enable_muting")) prefs.edit().putBoolean("enable_muting", true).apply();
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
		adapt.notifyDataSetChanged();
	}
	
	@Override
	public void onBackPressed() {
		setResult(600);
		finish();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt("lastTheme", lastTheme);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mutingmanager_actionbar, menu);
		Switch s = new Switch(this);
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		s.setChecked(prefs.getBoolean("enable_muting", true));
		s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				prefs.edit().putBoolean("enable_muting", isChecked).apply();
			}
		});
		menu.findItem(R.id.toggleBtn).setActionView(s);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			setResult(600);
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}