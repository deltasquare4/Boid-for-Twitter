package com.teamboid.twitter;

import java.io.InputStream;
import java.util.HashMap;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import twitter4j.StatusUpdate;
import twitter4j.auth.OAuthAuthorization;
import twitter4j.auth.OAuthToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.internal.http.HttpClient;
import twitter4j.internal.http.HttpClientImpl;
import twitter4j.internal.http.HttpParameter;
import twitter4j.internal.http.HttpRequest;
import twitter4j.internal.http.HttpResponse;
import twitter4j.internal.http.RequestMethod;
import twitter4j.media.ImageUpload;
import twitter4j.media.ImageUploadFactory;

/**
 * Handles media uploads
 * @author kennydude
 *
 */
public class MediaUtilities {

	/**
	 * Get all we can use right now
	 * @param include_setup if you include ones the user hasn't setup yet
	 * @return
	 */
	public static HashMap<String, MediaService> getMediaServices(boolean include_setup, Context c){
		HashMap<String, MediaService> services = new HashMap<String, MediaService>();
		services.put("twitter", new TwitterMediaService());
		services.put("yfrog", new yFrogUpload());
		services.put("imgly", new imgLyUpload());
		services.put("posterous", new posterousUpload());
		services.put("twitgoo", new twitGooUpload());
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		MediaService ms = new imgurUpload(
				prefs.getString("imgur-token",""),
				prefs.getString("imgur-secret", ""),
				prefs.getString("imgur-url", ""));
		if(ms.isConfigured(prefs) || include_setup == true) {
			services.put("imgur", ms);
		}
		return services;
	}
	public interface MediaConfigured{
		public void configured();
		public void startActivity(Intent activity);
		public Uri getCallback();
	}

	public abstract static class MediaService{
		public MediaConfigured configured;
		public int name = 0;
		public boolean needs_config = false;

		void callConfigured(){
			if(configured != null) configured.configured();
		}

		abstract public StatusUpdate attachMedia(InputStream input, StatusUpdate update, SendTweetTask stt) throws Exception;
		public StatusUpdate appendToUpdate(String url, SendTweetTask stt){
			String contents = stt.contents;
			if(!contents.endsWith(" ")) // Space out by 1 if possible
				contents = contents + " ";
			contents = contents + url;
			return new StatusUpdate(contents);
		}

		public void configure(Activity c, Intent i){}
		public void configure(Activity c){}
		public boolean isConfigured(SharedPreferences x){return true;}
	}

	public static class TwitterMediaService extends MediaService{
		public TwitterMediaService(){
			name = R.string.upload_twitter;
		}
		@Override
		public StatusUpdate attachMedia(InputStream input, StatusUpdate update,
				SendTweetTask stt) {
			update.media("BOIDUPLOAD", input);
			return update;
		}	
	}

	// Twitter4J Services {
	public abstract static class Twitter4JMediaService extends MediaService{
		public String twitter4jname = "";
		@Override
		public StatusUpdate attachMedia(InputStream input, StatusUpdate update,
				SendTweetTask stt) throws Exception {
			ConfigurationBuilder cb = AccountService.getConfiguration(stt.from.getToken(), stt.from.getSecret());
			cb.setMediaProvider(twitter4jname);
			if(SendTweetTask.MEDIA_API_KEYS.containsKey(twitter4jname)) {
				cb.setMediaProviderAPIKey(SendTweetTask.MEDIA_API_KEYS.get(twitter4jname));
			}

			ImageUpload up = new ImageUploadFactory(cb.build()).getInstance();

			return appendToUpdate(up.upload("BOIDUPLOAD.jpg", input), stt);
		}
	}
	public static class yFrogUpload extends Twitter4JMediaService{
		public yFrogUpload(){
			twitter4jname = "yfrog";
			name = R.string.upload_yfrog;	
		}
	}
	public static class imgLyUpload extends Twitter4JMediaService{
		public imgLyUpload(){
			twitter4jname = "imgly";
			name = R.string.upload_imgly;	
		}
	}
	public static class posterousUpload extends Twitter4JMediaService{
		public posterousUpload(){
			twitter4jname = "posterous";
			name = R.string.upload_posterous;	
		}
	}
	public static class twitGooUpload extends Twitter4JMediaService{
		public twitGooUpload(){
			twitter4jname = "twitgoo";
			name = R.string.upload_twitgoo;	
		}
	}
	public static class lockerzUpload extends Twitter4JMediaService{
		public lockerzUpload(){
			twitter4jname = "lockerz";
			name = R.string.upload_lockerz;	
		}
	}
	// } End of Twitter4J Services

	public static class imgurUpload extends MediaService{

		static final String KEY = "ee7a6c73396f12b50ea7890b05fc6e4404fd5ffa2";
		static final String SECRET = "2f0470bdae5e2238a112c2edd317c5cf";
		static final String AUTH_URL = "https://api.imgur.com/oauth/authorize";

		@Override
		public boolean isConfigured(SharedPreferences prefs){
			return (prefs.contains("imgur-token"));
		}

		String auth_token;
		String auth_secret;
		String url;

		public imgurUpload(String token, String secret, String url){
			auth_token = token;
			auth_secret = secret;
			name = R.string.upload_imgur;
			needs_config = true;
			this.url = url;
		}

		@Override
		public StatusUpdate attachMedia(InputStream input, StatusUpdate update, SendTweetTask stt) throws Exception {
			oauth = getAuth(true);
			HttpClient cl = new HttpClientImpl();
			HttpRequest get = new HttpRequest(RequestMethod.POST, "http://api.imgur.com/2/account/images.json",
					new HttpParameter[]{
					new HttpParameter("image", "BOIDUPLOAD", input),
					new HttpParameter("type", "file"),
					new HttpParameter("title", stt.contents),
					new HttpParameter("caption", "Uploaded via Boid for Android http://boidapp.com")
			}, oauth, new HashMap<String,String>());
			HttpResponse r = cl.request(get);
			if(r.getStatusCode() != 200) throw new Exception("imgur did not repond with 200 ¬_¬");
			JSONObject jo = r.asJSONObject();
			String url = jo.getJSONObject("images").getJSONObject("links").getString("imgur_page");
			return appendToUpdate(url, stt);
		}

		// twitter4j :)
		public OAuthAuthorization getAuth(boolean auth){
			ConfigurationBuilder cb = new ConfigurationBuilder();
			cb.setOAuthConsumerKey(KEY).setOAuthConsumerSecret(SECRET).setOAuthRequestTokenURL("https://api.imgur.com/oauth/request_token");
			cb.setOAuthAuthorizationURL(AUTH_URL).setOAuthAccessTokenURL("https://api.imgur.com/oauth/access_token");
			OAuthAuthorization oauth = new OAuthAuthorization(cb.build());
			if(auth == true){
				//Note: Another twitter4j hack
				Log.d("auth", auth_token + " " + auth_secret);
				oauth.setAccessToken(new RequestToken(auth_token, auth_secret));
			}
			return oauth;
		}
		static OAuthAuthorization oauth;

		public void configureView(final Context c, final View v){
			TextView t = (TextView)v.findViewById(R.id.summary);
			if(auth_token.equals("")) t.setText(R.string.not_logged_in);
			else t.setText(c.getString(R.string.logged_in_as).replace("{user}", url));
			Button b = (Button)v.findViewById(R.id.action);
			if(auth_token.equals("")){
				b.setText(R.string.login);
				b.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View button) {
						button.setEnabled(false);
						((Button)button).setText(R.string.loading_str);
						new Thread(new Runnable(){
							@Override
							public void run() {
								try {
									oauth = getAuth(false);
									RequestToken r = oauth.getOAuthRequestToken(configured.getCallback().toString());
									configured.startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(AUTH_URL + "?oauth_token=" + r.getToken())));
								} catch(Exception e) {
									//TODO: Error message
									e.printStackTrace();
								}
							}
						}).start();
					}
				});
			} else{
				b.setText(R.string.logout);
				b.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View w) {
						PreferenceManager.getDefaultSharedPreferences(c).edit().remove("imgur-token").remove("imgur-secret").remove("imgur-url").commit();
						configureView(c, v);
					}
				});
			}
		}

		/**
		 * Callback
		 */
		public void configure(final Activity c, final Intent i){
			Log.d("imgur", "Finish config");
			final ProgressDialog d = new ProgressDialog(c);
			d.setMessage(c.getString(R.string.loading_str));
			d.show();
			new Thread(new Runnable(){

				@Override
				public void run() {
					try{
						// **notice** officially in twitter4j, this is private. libs has a custom build
						OAuthToken a = oauth.getOAuthAccessToken(i.getData().getQueryParameter("oauth_verifier"));

						// Now we get our URL for account
						HttpClient cl = new HttpClientImpl();
						HttpRequest get = new HttpRequest(RequestMethod.GET, "http://api.imgur.com/2/account.json", new HttpParameter[0], oauth, new HashMap<String,String>());
						HttpResponse r = cl.request(get);
						if(r.getStatusCode() != 200) throw new Exception("Non 200 response! ¬_¬");
						JSONObject ob = r.asJSONObject();
						String url = ob.getJSONObject("account").getString("url");

						PreferenceManager.getDefaultSharedPreferences(c).edit().putString("imgur-token", a.getToken()).putString("imgur-secret", a.getTokenSecret()).putString("imgur-url", url).commit();
						Log.d("imgur", "DONE! :D");

						callConfigured();
					} catch(Exception e){
						e.printStackTrace(); // TODO: Error MSG
					}

					d.dismiss();
				}

			}).start();
		}

		public void configure(Activity c){
			AlertDialog.Builder ab = new AlertDialog.Builder(c);
			ab.setTitle(R.string.upload_imgur);
			View v = LayoutInflater.from(c).inflate(R.layout.oauth_media_panel, null);
			configureView(c,v);
			TextView t = (TextView)v.findViewById(R.id.notes);
			t.setText(R.string.imgur_note);
			ab.setView(v);
			ab.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			ab.show();
		}
	}
}
