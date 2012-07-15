package com.teamboid.twitter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class LoginHandler extends Activity
{

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_handler);
		WebView view = (WebView) findViewById(R.id.webView);
		view.getSettings().setJavaScriptEnabled(true);
		view.getSettings().setAppCacheEnabled(false);
		view.setWebViewClient(new WebViewClient()
		{
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url)
			{
				if (url.startsWith("boid://"))
				{
					setResult(RESULT_OK, new Intent().putExtra("oauth_verifier", 
							Uri.parse(url).getQueryParameter("oauth_verifier")));
					finish();
					return true;
				}
				else
					return false;
			}
		});
	}

	@Override
	public void onResume()
	{
		super.onResume();
		((WebView) findViewById(R.id.webView)).loadUrl(getIntent()
				.getStringExtra("url"));
	}
}
