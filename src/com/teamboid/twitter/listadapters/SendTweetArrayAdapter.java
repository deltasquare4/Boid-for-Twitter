package com.teamboid.twitter.listadapters;

import java.util.List;

import com.handlerexploit.prime.RemoteImageView;
import com.teamboid.twitter.ComposerScreen;
import com.teamboid.twitter.R;
import com.teamboid.twitter.SendTweetTask;
import com.teamboid.twitter.SendTweetTask.Result;
import com.teamboid.twitter.services.SendTweetService;
import com.teamboid.twitter.utilities.Utilities;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * @author kennydude
 */
public class SendTweetArrayAdapter extends ArrayAdapter<SendTweetTask> {

	public SendTweetArrayAdapter(Context context, int resource,
			int textViewResourceId, List<SendTweetTask> objects) {
		super(context, resource, textViewResourceId, objects);
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		if(convertView == null){
			convertView = ((LayoutInflater)this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.send_tweet, null);
		}
		final SendTweetTask stt = this.getItem(position);
		TextView tv = (TextView)convertView.findViewById(R.id.feedItemText);
		tv.setText(stt.contents);
		tv = (TextView)convertView.findViewById(R.id.feedItemUserName);
		tv.setText(stt.from.getUser().getScreenName());
		convertView.findViewById(R.id.feedItemMediaIndicator).setVisibility(stt.hasMedia() ? View.VISIBLE : View.GONE);
		
		RemoteImageView profilePic = (RemoteImageView)convertView.findViewById(R.id.feedItemProfilePic);
		profilePic.setImageURL( Utilities.getUserImage(stt.from.getUser().getScreenName(), getContext(), stt.from.getUser()) );
		convertView.findViewById(R.id.progressBar).setVisibility(stt.result.errorCode == Result.WAITING ? View.VISIBLE : View.GONE);
		ImageButton btn = (ImageButton)convertView.findViewById(R.id.delete);
		btn.setVisibility(stt.result.errorCode == Result.WAITING ? View.GONE : View.VISIBLE);
		btn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				SendTweetService.removeTweet(stt);
			}		
		});
		btn = (ImageButton)convertView.findViewById(R.id.resend);
		btn.setVisibility(stt.result.errorCode == Result.WAITING ? View.GONE : View.VISIBLE);
		btn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(getContext(), SendTweetService.class);
				intent.setAction(SendTweetService.NETWORK_AVAIL);
				//intent.putExtra("tweet", position);
				getContext().startService(intent);
			}
		});
		btn = (ImageButton)convertView.findViewById(R.id.edit);
		btn.setVisibility(stt.result.errorCode == Result.WAITING ? View.GONE : View.VISIBLE);
		btn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(getContext(), ComposerScreen.class);
				intent.putExtra("stt", stt.toBundle());
				getContext().startActivity(intent);
				
				SendTweetService.removeTweet(stt);
			}			
		});
		return convertView;
	}
}
