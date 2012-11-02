package com.teamboid.twitter;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import com.teamboid.twitter.utilities.Utilities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class ReadabilityActivity extends InAppBrowser {
	public static final String READABILITY_TOKEN = "686795a7595b783307158bdac2bc6e0c6211c8fc";
	public static final String template = "<html><head><style>"+
			"body{font-family:serif}img{max-width:100%}"+
			"</style></head><body>$content</body></html>";
	
	ProgressDialog pd;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupView();
		pd = new ProgressDialog(this);
		pd.setMessage(getString(R.string.please_wait));
		pd.show();
		
		new Thread(new Runnable(){

			@Override
			public void run() {
				try{
					HttpGet get = new HttpGet("https://www.readability.com/api/content/v1/parser?url=" +
										Uri.encode( getIntent().getDataString() ) +
										"&token=" + READABILITY_TOKEN);
					get.setHeader("User-Agent", "Boid for Android v" + Utilities.getVersionName(ReadabilityActivity.this));
					HttpClient client = new DefaultHttpClient();
					HttpResponse r = client.execute(get);
					if(r.getStatusLine().getStatusCode() != 200){
						throw new Exception("Non 200 response");
					}
					final JSONObject response = new JSONObject(EntityUtils.toString(r.getEntity()));
					runOnUiThread(new Runnable(){

						@Override
						public void run() {
							try{
								setTitle(response.getString("title"));
								view.loadData(template.replace("$content", response.getString("content")), "text/html", "ascii");
							} catch(Exception e){
								e.printStackTrace();
							}
						}
						
					});
				} catch(Exception e){
					e.printStackTrace();
					view.loadData(template.replace("$content", "<h1>"+getString(R.string.error_str)+"</h1>"), "text/html", "ascii");
				}
				runOnUiThread(new Runnable(){

					@Override
					public void run() {
						pd.dismiss();
					}
					
				});
			}
			
		}).start();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		startActivity(new Intent(Intent.ACTION_VIEW)
			.setData(getIntent().getData())
			.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
			.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
		return true;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		menu.add(getString(R.string.full)).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		return true;
	}
}
