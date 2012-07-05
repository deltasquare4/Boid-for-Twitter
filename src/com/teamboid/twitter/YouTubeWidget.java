package com.teamboid.twitter;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import com.handlerexploit.prime.utils.ImageManager;
import com.teamboid.twitter.R;
import com.teamboid.twitter.tweetwidgets.TweetWidget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * @author kennydude
 */

public class YouTubeWidget extends TweetWidget {

	public void onReceive(Context cntxt, Intent intent) {
		Log.d("yt", "Found me @ " + intent.getData().toString() + " " + intent.getData().getHost());
		String vidid = "";
		if(intent.getData().getHost().contains("youtube.com")){
			vidid = intent.getData().getQueryParameter("v");
		} else if(intent.getData().getHost().contains("youtu.be")){
			vidid = intent.getData().getPath().substring(1);
		}
		Log.d("yt", "VideoID: " + vidid);
		String vidtitle = "";
		try{
			DefaultHttpClient dfhc = new DefaultHttpClient();
			HttpResponse r = dfhc.execute(new HttpGet("http://gdata.youtube.com/feeds/api/videos/" + vidid + "?v=2&alt=json&&fields=title"));
			if(r.getStatusLine().getStatusCode() == 200){
				JSONObject jo = new JSONObject(EntityUtils.toString(r.getEntity(), "UTF-8")).getJSONObject("entry");
				vidtitle = jo.getJSONObject("title").getString("$t");
			} else{
				Log.d("yt", "E downloading title");
				sendErrorView(cntxt, intent);
				return;
			}
		} catch(Exception e){
			e.printStackTrace();
			this.sendErrorView(cntxt, intent);
			return;
		}
		final RemoteViews rv = new RemoteViews("com.teamboid.twitter", R.layout.youtube_widget);
		rv.setTextViewText(R.id.title, vidtitle);
		Intent send = new Intent(Intent.ACTION_VIEW);
		send.setData(Uri.parse("http://youtube.com/watch?v=" + vidid));
		PendingIntent pendingIntent = PendingIntent.getActivity(cntxt, 0, send, 0);
		rv.setOnClickPendingIntent(R.id.title, pendingIntent);
		ImageManager imageManager = ImageManager.getInstance(cntxt);
		rv.setBitmap(R.id.thumb, "setImageBitmap", 
				imageManager.get("http://i.ytimg.com/vi/" + vidid + "/mqdefault.jpg")); 
		sendRemoteViews(rv, cntxt, intent);
	}
}
