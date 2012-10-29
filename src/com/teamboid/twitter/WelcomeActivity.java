package com.teamboid.twitter;

import com.teamboid.twitter.services.AccountService;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Welcome to Boid!
 * @author kennydude
 *
 */
public class WelcomeActivity extends Activity {
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == AccountService.AUTH_CODE){
			if(resultCode == RESULT_OK){
				finish(); // Yay!
			} else{
				findViewById(R.id.error).setVisibility(View.VISIBLE);
			}
		}
	}
	
	@Override
	public void onCreate(Bundle sis){
		super.onCreate(sis);
		setContentView(R.layout.activity_welcome);
		
		Button welcome = (Button)findViewById(R.id.login);
		welcome.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				AccountService.startAuth(WelcomeActivity.this);
			}
			
		});
	}
}
