package com.teamboid.twitter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class InAppBrowser extends Activity {

	private WebView view;
	
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		view = new WebView(this);
		setContentView(view);
		view.getSettings().setJavaScriptEnabled(true);
		view.getSettings().setAppCacheEnabled(false);
		view.setWebViewClient(new WebViewClient() {
			@Override
		    public boolean shouldOverrideUrlLoading(WebView view, String url) { return false; }
			@Override
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
				Toast.makeText(InAppBrowser.this, "Oh no! " + description, Toast.LENGTH_SHORT).show();
			}
		});
		view.loadUrl(getIntent().getStringExtra("url"));
	}
	
	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		view.loadUrl(intent.getStringExtra("url"));
	}
}
