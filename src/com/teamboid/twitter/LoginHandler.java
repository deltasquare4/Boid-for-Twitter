package com.teamboid.twitter;

import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.services.AccountService.VerifyAccountResult;
import com.teamboid.twitter.utilities.BoidActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

@SuppressLint("SetJavaScriptEnabled")
public class LoginHandler extends Activity {
	
	@Override
	public void onDestroy(){
		super.onDestroy();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		WebView view = new WebView(this);
		setContentView(view);
		view.getSettings().setJavaScriptEnabled(true);
		view.getSettings().setAppCacheEnabled(false);
		view.setWebViewClient(new WebViewClient() {
			@Override
		    public boolean shouldOverrideUrlLoading(WebView view, final String url) {
				if(url.startsWith("boid://")) {
					final ProgressDialog pd = new ProgressDialog(LoginHandler.this);
					pd.setMessage(getString(R.string.please_wait));
					pd.show();
					new Thread(new Runnable(){

						@Override
						public void run() {
							final VerifyAccountResult var = AccountService.verifyAccount(LoginHandler.this,
									Uri.parse(url).getQueryParameter("oauth_verifier"));
							runOnUiThread(new Runnable(){

								@Override
								public void run() {
									pd.dismiss();
									switch(var){
									case OK:
										setResult(RESULT_OK);
										break;
									default:
										setResult(var.code);
									}
									finish();
								}
								
							});
						}
						
					}).start();
					return true;
				} else return false;
		    }
			
			@Override
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
				Toast.makeText(LoginHandler.this, "Oh no! " + description, Toast.LENGTH_SHORT).show();
			}
		});
		
		view.loadUrl(getIntent().getStringExtra("url"));
	}
	
	@Override
	public void onResume() {
		super.onResume();
		//((WebView)findViewById(R.id.webView)).loadUrl(getIntent().getStringExtra("url"));
	}
}
