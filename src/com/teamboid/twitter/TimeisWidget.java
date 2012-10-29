package com.teamboid.twitter;

import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.teamboid.twitter.tweetwidgets.TweetWidget;

/**
 * Shows time.is pages inline
 * 
 * f.e http://2pm_in_Japan
 * @author kennydude
 *
 */
public class TimeisWidget extends TweetWidget {

	@Override
	public void onReceive(Context c, Intent intent) {
		try{
			DefaultHttpClient dfhc = new DefaultHttpClient();
			HttpResponse r = dfhc.execute(new HttpGet(intent.getDataString()));
			if(r.getStatusLine().getStatusCode() == 200){
				String body = EntityUtils.toString(r.getEntity());
				int s = body.indexOf("When the time is");
				String text = body.substring( s, body.indexOf("</li>", s));
				
				// Strip XML tags
				text = text.replaceAll(
						"<([_:A-Za-z][-._:A-Za-z0-9]*(\\s+[_:A-Za-z][-._:A-Za-z0-9]*\\s*=\\s*(\"[^\"]*\"|'[^']*'))*|/[_:A-Za-z][-._:A-Za-z0-9]*)\\s*>",
						""
				);
				
				final RemoteViews rv = new RemoteViews("com.teamboid.twitter", R.layout.timeis);
				rv.setTextViewText(R.id.title, text);
				this.sendRemoteViews(rv, c, intent);
			}
		} catch(Exception e){
			e.printStackTrace();
			this.sendErrorView(c, intent);
		}
	}

}
