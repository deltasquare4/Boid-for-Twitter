package com.teamboid.twitter;

import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.services.AccountService.VerifyAccountResult;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
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

		setContentView(R.layout.login_handler);
		WebView view = (WebView)findViewById(R.id.webView);
		view.getSettings().setJavaScriptEnabled(true);
		view.getSettings().setAppCacheEnabled(false);
		view.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, final String url) {
				if(url.startsWith("boid://")) {
					new Thread(new Runnable(){

						@Override
						public void run() {
							final VerifyAccountResult var = AccountService.verifyAccount(LoginHandler.this,
									Uri.parse(url).getQueryParameter("oauth_verifier"));
							runOnUiThread(new Runnable(){
								@Override
								public void run() {
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

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favIcon) {
				view.setVisibility(View.GONE);
				findViewById(R.id.webProgress).setVisibility(View.VISIBLE);
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				view.setVisibility(View.VISIBLE);
				findViewById(R.id.webProgress).setVisibility(View.GONE);
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
