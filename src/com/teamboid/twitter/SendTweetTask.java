package com.teamboid.twitter;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.HashMap;

import com.teamboid.twitterapi.media.ExternalMediaService;
import com.teamboid.twitterapi.media.MediaServices;
import com.teamboid.twitterapi.status.GeoLocation;
import com.teamboid.twitterapi.status.Status;
import com.teamboid.twitterapi.status.StatusUpdate;
import com.teamboid.twitterapi.status.entity.media.MediaEntity;

import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.model.Token;

import com.teamboid.twitter.services.AccountService;

import com.teamboid.twitter.utilities.MediaUtilities;
import com.teamboid.twitter.utilities.TwitlongerHelper;
import com.teamboid.twitter.utilities.TwitlongerHelper.TwitlongerPostResponse;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Defines a send tweet task.
 * 
 * Basically a Tweet we want to send with all it's bits and bobs
 * and we can do this on a background service
 * @author kennydude
 *
 */
public class SendTweetTask {
	
	public static HashMap<String, String> MEDIA_API_KEYS = new HashMap<String, String>();
	static {
		MEDIA_API_KEYS.put("plixi", "10b6f8fd-c373-44cb-bd35-bbeb61a199f3");
		MEDIA_API_KEYS.put("twitpic", "10c80e3453f27d3b34cb0bba320a17b3");
		MEDIA_API_KEYS.put("yfrog", "e1f3d4d1625ec410a79b573dbbfe0570");
	}
		
	public static class Result{
		
		public static final int TWITLONGER_ERROR = -102;
		public static final int WAITING = -101;
		public static final int MEDIAIO_ERROR = -90;		
		public boolean sent = false;
		public int errorCode = WAITING;
		
		public Bundle toBundle(){
			Bundle out = new Bundle();
			out.putBoolean("sent", sent);
			out.putInt("errorCode", errorCode);
			return out;
		}
	
		public static Result fromBundle(Bundle in){
			Result r = new Result();
			r.errorCode = in.getInt("errorCode");
			r.sent = in.getBoolean("sent");
			return r;
		}
	}
	public Result result = new Result();

	public String contents;
	public Boolean twtlonger = false;
	public GeoLocation location;
	public String attachedImage;
	public Uri attachedImageUri;
	public long in_reply_to = 0;
	public String replyToName;
	public Account from;
	public String mediaService;
	
	public Status tweet;
	public boolean isGalleryImage = false;

	public String placeId;
	public boolean hasMedia(){
		return attachedImageUri != null || attachedImage != null;
	}
	
	/**
	 * Actually attempt to send tweet. You should thread this
	 * @return Result
	 */
	public SendTweetTask.Result sendTweet(Context context){
		TwitlongerHelper helper = null;
		TwitlongerPostResponse response = null;
		if(twtlonger){
			helper = TwitlongerHelper.create("boidforandroid", "nJRJRiNn3VGiCWZl", from.getUser().getScreenName());
			response = null;
			try { response = helper.post(contents, in_reply_to, replyToName); }
			catch(Exception e){
				e.printStackTrace();
				result.sent = false;
				result.errorCode = Result.TWITLONGER_ERROR;
				return result;
			}
			contents = response.getContent();
		}
		
		StatusUpdate update = StatusUpdate.create(contents);
		
		if(this.hasMedia()){
			try{
				String prefValue = mediaService;
				InputStream input;
				if(attachedImageUri != null){
					input = context.getContentResolver().openAssetFileDescriptor(attachedImageUri, "r").createInputStream();
				} else{
					input = new FileInputStream(new File(attachedImage));
				}
				Log.d("up", "Uploading with " + prefValue);
				
				MediaServices.setupServices();
				ExternalMediaService ems = MediaServices.services.get(prefValue.toLowerCase());
				ems.setAPIKey( MEDIA_API_KEYS.get( prefValue.toLowerCase() ) );
				ems.setAttribution("Uploaded via Boid for Android. Download for free -- http://boidapp.com");
				
				SharedPreferences sp;
				switch(ems.getNeededAuthorization()){ // We need to send whatever auth it requires
				case MAIL_AND_PASSWORD:
					sp = PreferenceManager.getDefaultSharedPreferences(context);
					ems.setMailAndPassword(
							sp.getString(prefValue + "-username", ""),
							sp.getString(prefValue + "-password", "")
					);
					break;
				case OAUTH:
					sp = PreferenceManager.getDefaultSharedPreferences(context);
					Token token = new Token(sp.getString(prefValue + "-token", ""), sp.getString(prefValue + "-secret", ""));
					ems.setAuthorized(MediaUtilities.buildAuthService(prefValue), token);
					break;
				default:
					break;
				}
				
				MediaEntity me = ems.uploadFile(update, from.getClient(), input);
				if(!prefValue.equals("twitter")){ // Only twitter doesn't respond the same
					if(contents.length() > 1){
						contents = contents + ( contents.charAt(contents.length()-1) == ' ' ? "" : " " ) + me.getExpandedUrl();
					} else{
						contents = me.getExpandedUrl();
					}
					update = StatusUpdate.create(contents);
				}
				
			} catch(Exception e){
				e.printStackTrace();
				result.errorCode = Result.MEDIAIO_ERROR;
				result.sent = false;
				return result;
			}
		}
		
		if(location != null) update.setLocation(location);
		if(in_reply_to > 0) update.setInReplyToStatusId(in_reply_to);
		
		try{
			tweet = from.getClient().updateStatus(update);
			if(twtlonger){
				helper.callback(tweet.getId(), response);
			}
			//AccountService.getFeedAdapter(context, TimelineFragment.ID).add(new Status[] { tweet });
			result.sent = true;
		} catch(Exception e){
			e.printStackTrace();
			result.sent = false;
			result.errorCode = Result.TWITLONGER_ERROR;
		}
		
		return result;
	}
	
	/**
	 * Put it in a Bundle to store later
	 * @return
	 */
	public Bundle toBundle(){
		Bundle r = new Bundle();
		r.putString("contents", contents);
		r.putBoolean("twtlonger", twtlonger);
		r.putLong("in_reply_to", in_reply_to);
		r.putString("replyName", replyToName);
		if(attachedImage != null)
			r.putSerializable("file", attachedImage);
		if(attachedImageUri != null)
			r.putString("fileU", attachedImageUri.toString());
		
		if(location != null){
			r.putDouble("lat", location.getLatitude());
			r.putDouble("long", location.getLongitude());
		}
		r.putLong("from", from.getId());
		if(mediaService != null)
			r.putString("mediaService", mediaService);
		r.putBoolean("isGallery", isGalleryImage);
		
		r.putBundle("result", result.toBundle());
		return r;
	}
	
	public JSONObject toJSONObject() throws JSONException{
		JSONObject r = new JSONObject();
		r.put("contents", contents);
		r.put("twtlonger", twtlonger);
		r.put("in_reply_to", in_reply_to);
		r.put("replyName", replyToName);
		if(attachedImage != null)
			r.put("file", attachedImage);
		if(attachedImageUri != null)
			r.put("fileU", attachedImageUri.toString());
		if(location != null){
			r.put("lat", location.getLatitude());
			r.put("lon", location.getLongitude());
		}
		r.put("from", from.getId());
		r.put("res-code", result.errorCode);
		r.put("res-sent", result.sent);
		r.put("isGallery", isGalleryImage);
		if(mediaService != null)
			r.put("mediaService", mediaService);
		
		return r;
	}
	public static SendTweetTask fromJSONObject(JSONObject in) throws JSONException{
		SendTweetTask r = new SendTweetTask();
		r.contents = in.getString("contents");
		r.twtlonger = in.getBoolean("twtlonger");
		r.in_reply_to = in.getLong("in_reply_to");
		if(in.has("replyName"))
			r.replyToName = in.getString("replyName");
		r.from = AccountService.getAccount(in.getLong("from"));
		
		if(in.has("file"))
			r.attachedImage = in.getString("file");
		if(in.has("lat") && in.has("lon"))
			r.location = new GeoLocation( in.getLong("lat"), in.getLong("lon") );
		if(in.has("fileU"))
			r.attachedImageUri = Uri.parse(in.getString("fileU"));
		if(in.has("mediaService"))
			r.mediaService = in.getString("mediaService");
		
		r.result.errorCode = in.getInt("res-code");
		r.result.sent = in.getBoolean("res-sent");
		r.isGalleryImage = in.getBoolean("isGallery");
		return r;
	}
	
	public static SendTweetTask fromBundle(Bundle in){
		SendTweetTask r = new SendTweetTask();
		r.contents = in.getString("contents");
		r.twtlonger = in.getBoolean("twtlonger");
		r.in_reply_to = in.getLong("in_reply_to");
		r.replyToName = in.getString("replyName");
		r.attachedImage = in.getString("file");
		if(in.containsKey("lat") && in.containsKey("long"))
			r.location = new GeoLocation(in.getDouble("lat"), in.getDouble("long"));
		
		Log.d("x", in.getLong("from") + "");
		r.from = AccountService.getAccount(in.getLong("from"));
		if( in.containsKey("fileU") )
			r.attachedImageUri = Uri.parse(in.getString("fileU"));
		if(in.containsKey("mediaService"))
			r.mediaService = in.getString("mediaService");
		r.isGalleryImage = in.getBoolean("isGallery");
		
		r.result = Result.fromBundle(in.getBundle("result"));
		return r;
	}
}
