package com.teamboid.twitter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class LoginHandler extends Activity {

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		WebView view = new WebView(this);
		setContentView(view);
		view.getSettings().setJavaScriptEnabled(true);
		view.getSettings().setAppCacheEnabled(false);
		view.setWebViewClient(new WebViewClient() {
			@Override
		    public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if(url.startsWith("boid://")) {
					setResult(RESULT_OK, new Intent().putExtra("oauth_verifier", 
							Uri.parse(url).getQueryParameter("oauth_verifier")));
					finish();
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
