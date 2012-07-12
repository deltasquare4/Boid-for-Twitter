package com.teamboid.twitter;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

/**
 * Better UX for Font Size ;)
 * @author kennydude
 *
 */
public class FontSizePreference extends ListPreference {

	private int mClickedDialogEntryIndex;
	public class FontItem{
		public FontItem(String key, String value){ this.key = key; this.value = value; }
		
		public String key;
		public String value;
	}
	
	@Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        
        if (positiveResult && mClickedDialogEntryIndex >= 0 && getEntryValues() != null) {
            String value = getEntryValues()[mClickedDialogEntryIndex].toString();
            if (callChangeListener(value)) {
                setValue(value);
            }
        }
    }

	public FontSizePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
    protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);
        
        mClickedDialogEntryIndex = getValueIndex();
        final List<FontItem> data = new ArrayList<FontItem>();
        int i = 0;
        while(i < getEntries().length){
        	data.add(new FontItem( getEntries()[i].toString(), getEntryValues()[i].toString() ));
        	i++;
        }
        
        // Purely here is the UX gained :') That's all
        ListAdapter adapter = new ArrayAdapter<FontItem>(getContext(), 0, data){
        	@Override
        	public View getView (int position, View convertView, ViewGroup parent){
        		FontItem ft = data.get(position);
        		TextView tv = new TextView(getContext());
        		tv.setText(ft.key);
        		tv.setTextSize( Float.parseFloat( ft.value ) );
        		tv.setPadding(10, 10, 10, 10);
        		return tv;
        	}
        };
        
        mClickedDialogEntryIndex = getValueIndex();
        builder.setSingleChoiceItems(adapter, mClickedDialogEntryIndex, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mClickedDialogEntryIndex = which;

                /*
                 * Clicking on an item simulates the positive button
                 * click, and dismisses the dialog.
                 */
                FontSizePreference.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                dialog.dismiss();
                
			}
		});
	}

	private int getValueIndex() {
		return findIndexOfValue(getValue());
	}
}
