package com.teamboid.twitter.utilities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.*;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;

import com.teamboid.twitterapi.search.Tweet;
import com.teamboid.twitterapi.status.Status;
import com.teamboid.twitterapi.status.entity.media.MediaEntity;
import com.teamboid.twitterapi.status.entity.media.MediaSize;
import com.teamboid.twitterapi.status.entity.mention.MentionEntity;
import com.teamboid.twitterapi.status.entity.url.UrlEntity;
import com.teamboid.twitterapi.user.User;

import com.teamboid.twitter.ComposerScreen;
import com.teamboid.twitter.InAppBrowser;
import com.teamboid.twitter.ProfileScreen;
import com.teamboid.twitter.R;
import com.teamboid.twitter.SearchScreen;
import com.teamboid.twitter.listadapters.FeedListAdapter;
import com.teamboid.twitter.listadapters.MediaFeedListAdapter;
import com.teamboid.twitter.listadapters.MessageConvoAdapter;
import com.teamboid.twitter.listadapters.SearchFeedListAdapter;
import com.teamboid.twitter.listadapters.MessageConvoAdapter.DMConversation;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.views.NoUnderlineClickableSpan;
import com.teamboid.twitter.views.TimePreference;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Convenience methods.
 * @author Aidan Follestad
 */
public class Utilities {

	public static String[] getMuteFilters(Context context) {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String prefName = Long.toString(AccountService.getCurrentAccount().getId()) + "_muting";
		return Utilities.jsonToArray(prefs.getString(prefName, "")).toArray(new String[0]);
	}
	
	public static void recreateFeedAdapter(Activity context, FeedListAdapter adapt) {
		Status[] before = null;
		if(adapt.getCount() > 0) before = adapt.toArray();
		adapt = new FeedListAdapter(context, adapt.ID, adapt.account);
		if(before != null) adapt.add(before);
		int index = 0;
		for(FeedListAdapter a : AccountService.feedAdapters) {
			if(a.ID.equals(adapt.ID) && a.account == adapt.account) {
				AccountService.feedAdapters.set(index, adapt);
			}
			index++;
		}
	}
	public static void recreateMessageAdapter(Activity context, MessageConvoAdapter adapt) {
		DMConversation[] before = null;
		if(adapt.getCount() > 0) before = adapt.toArray();
		adapt = new MessageConvoAdapter(context, adapt.account);
		if(before != null) adapt.add(before);
		int index = 0;
		for(MessageConvoAdapter a : AccountService.messageAdapters) {
			if(a.account == adapt.account) {
				AccountService.messageAdapters.set(index, adapt);
			}
			index++;
		}
	}
	public static void recreateSearchAdapter(Activity context, SearchFeedListAdapter adapt) {
		Tweet[] before = null;
		if(adapt.getCount() > 0) {
            before = adapt.toArray();
        }
		adapt = new SearchFeedListAdapter(context, adapt.ID, adapt.account);
		if(before != null) {
            adapt.add(before);
        }
		int index = 0;
		if(AccountService.searchFeedAdapters != null) {
			for(SearchFeedListAdapter a : AccountService.searchFeedAdapters) {
				if(a.ID.equals(adapt.ID) && a.account == adapt.account) {
					AccountService.searchFeedAdapters.set(index, adapt);
				}
				index++;
			}
		}
	}
	public static void recreateMediaFeedAdapter(Activity context, MediaFeedListAdapter adapt) {
		MediaFeedListAdapter.MediaFeedItem[] before = null;
		if(adapt.getCount() > 0) before = adapt.toArray();
		adapt = new MediaFeedListAdapter(context, adapt.ID, adapt.account);
		if(before != null) adapt.add(before, null);
		int index = 0;
		for(MediaFeedListAdapter a : AccountService.mediaAdapters) {
			if(a.ID.equals(adapt.ID) && a.account == adapt.account) {
				AccountService.mediaAdapters.set(index, adapt);
			}
			index++;
		}
	}	

	public static String getVersionName(Context context) {
		try { return context.getPackageManager().getPackageInfo(context.getPackageName(), 0 ).versionName; }
		catch (NameNotFoundException e) { return "Unknown"; }
	}
	public static String getVersionCode(Context context) {
		try { return Integer.toString(context.getPackageManager().getPackageInfo(context.getPackageName(), 0 ).versionCode); }
		catch (NameNotFoundException e) { return "Unknown"; }
	}
	
	public static int getTheme(Context context) {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String curTheme = null;
		if(prefs.getBoolean("night_mode", false)) {
			int nowHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
			int startHour = Integer.parseInt(prefs.getString("night_mode_time", "20"));
			int endHour = Integer.parseInt(prefs.getString("night_mode_endtime", "7"));
			if((nowHour < endHour) || (nowHour > startHour)) {
				curTheme = prefs.getString("night_mode_theme", "3");
			}
		}
		if(curTheme == null) curTheme = prefs.getString("boid_theme", "0");
		switch(Integer.parseInt(curTheme)) {
		default:
			prefs.edit().putString("boid_theme", "0").commit();
			return R.style.Boid_DarkTheme;
		case 1:
			return R.style.Boid_LightTheme;
		case 2:
			return R.style.Boid_DarkLightTheme;
		case 3:
			return R.style.Boid_PureBlackTheme;
		}
	}

	public static String getMedia(UrlEntity[] urls){
		String found = null;
		for(int i = 0; i < urls.length; i++) {
			UrlEntity urlEntity = urls[i];
			if(urlEntity == null || urlEntity.getUrl() == null) continue;
			String curEntity = urlEntity.getUrl().toString();
			if(urlEntity.getExpandedUrl() != null) curEntity = urlEntity.getExpandedUrl().toString();
			if(curEntity.endsWith(".jpg") || curEntity.endsWith(".jpeg") || curEntity.endsWith(".png") || curEntity.endsWith(".bmp") || curEntity.endsWith(".gif") || curEntity.endsWith(".bmp")) {
				found = curEntity;
				break;
			} else if(curEntity.contains("yfrog.com/")) {
				found = curEntity + ":medium";
				break;
			} else if(curEntity.contains("twitpic.com/")) {
				if(curEntity.contains("/show/")) found = curEntity;
				else found = curEntity.replace("twitpic.com/", "twitpic.com/show/full/");
				break;
			} else if(curEntity.contains("instagr.am/p/")) {
				found = "http://instagr.am/p/" + curEntity.replace("https://", "http://").substring(20);
				if(!found.endsWith("/")) found += "/";
				found += "media";
				break;
			} else if(curEntity.contains("img.ly/")) {
				if(curEntity.contains("/show/")) found = curEntity;
				else found = "http://img.ly/show/full/" + curEntity.replace("https://", "http://").substring(14);
				break;
			} else if(curEntity.contains("lockerz.com/") || curEntity.contains("plixi.com/")) {
				found = "http://api.plixi.com/api/tpapi.svc/imagefromurl?url=" + curEntity + "&size=big";
				break;
			} else if(curEntity.contains("imgur.com")){
				found = curEntity.replace("imgur.com", "i.imgur.com") + ".jpg";
				break;
			}
		}
		return found;
	}
	
	public static String getTweetYFrogTwitpicMedia(final Status tweet) {
		if(tweet.getMediaEntities() != null && tweet.getMediaEntities().length > 0) {
			return tweet.getMediaEntities()[0].getMediaUrl(MediaSize.LARGE).toString();
		} else if(tweet.getUrlEntities() != null && tweet.getUrlEntities().length > 0) {
			return getMedia( tweet.getUrlEntities() );
		} else return null;
	}
	public static String getTweetYFrogTwitpicMedia(final Tweet tweet) {
		if(tweet.getMediaEntities() != null && tweet.getMediaEntities().length > 0) {
			return tweet.getMediaEntities()[0].getMediaUrl(MediaSize.LARGE).toString();
		} else if(tweet.getUrlEntities() != null && tweet.getUrlEntities().length > 0) {
			return getMedia(tweet.getUrlEntities());
		} else return null;
	}
	public static boolean tweetContainsVideo(final Status tweet) {
		if(tweet.getUrlEntities() == null) return false;
		
		for(int i = 0; i < tweet.getUrlEntities().length; i++) {
			UrlEntity urlEntity = tweet.getUrlEntities()[i];
			if(urlEntity == null || urlEntity.getUrl() == null) continue;
			String curEntity = urlEntity.getUrl().toString();
			if(urlEntity.getExpandedUrl() != null) curEntity = urlEntity.getExpandedUrl().toString();
			if(curEntity.contains("youtube.com/watch?v=") || curEntity.contains("youtu.be")) {
				return true;
			}
		}
		return false;
	}
	public static boolean tweetContainsVideo(final Tweet tweet) {
		if(tweet.getUrlEntities() == null) return false;
		for(int i = 0; i < tweet.getUrlEntities().length; i++) {
			UrlEntity urlEntity = tweet.getUrlEntities()[i];
			if(urlEntity == null || urlEntity.getUrl() == null) continue;
			String curEntity = urlEntity.getUrl().toString();
			if(urlEntity.getExpandedUrl() != null) curEntity = urlEntity.getExpandedUrl().toString();
			if(curEntity.contains("youtube.com/watch?v=") || curEntity.contains("youtu.be")) {
				return true;
			}
		}
		return false;
	}

	public static Spannable twitterifyText(final Context context, Status status) {
		return twitterifyText(context, status.getText(), status.getUrlEntities(), status.getMediaEntities(), false);
	}
    public static Spannable twitterifyText(final Context context, Tweet status) {
        return twitterifyText(context, status.getText(), status.getUrlEntities(), status.getMediaEntities(), false);
    }
	public static Spannable twitterifyText(final Context context, String text, final UrlEntity[] urls, final MediaEntity[] pics, final boolean expand) {
		if(urls != null) {
			for(UrlEntity url : urls) {
				if(expand && url.getExpandedUrl() != null) {
					text = text.replace(url.getUrl().toString(), url.getExpandedUrl().toString());
				} else if(url.getDisplayUrl() != null) text = text.replace(url.getUrl().toString(), url.getDisplayUrl());
			}
		}
		if(pics != null) {
			for(MediaEntity p : pics) {
				text = text.replace(p.getUrl().toString(), p.getDisplayUrl());
			}
		}
		Spannable rtSpan = new SpannableString(text);
		Extractor ext = new Extractor();
		final List<Extractor.Entity> entities = ext.extractEntitiesWithIndices(text);
		final Hashtable<String, String> realLinks = getRealLinks(entities, urls, pics);
		for(final Extractor.Entity e : entities) {
			if(e.getType() == Extractor.Entity.Type.HASHTAG) {
				final String hashText = "#" + e.getValue();
				rtSpan.setSpan(new NoUnderlineClickableSpan() {
					@Override
					public void onClick(View widget) {
						if(ComposerScreen.class.isInstance( widget.getContext()) ){
							((ComposerScreen)widget.getContext()).appendText(hashText);
						}else{
							context.startActivity(new Intent(context, SearchScreen.class)
							.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
							.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
							.putExtra("query", hashText));
						}
					}
				}, e.getStart(), e.getEnd(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			} else if(e.getType() == Extractor.Entity.Type.MENTION) {
				final String screenName = e.getValue();
				rtSpan.setSpan(new NoUnderlineClickableSpan() {
					@Override
					public void onClick(View widget) {
						if(ComposerScreen.class.isInstance( widget.getContext()) ){ return; }
						context.startActivity(new Intent(context, ProfileScreen.class)
						.putExtra("screen_name", screenName)
						.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
						.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
					}
				}, e.getStart(), e.getEnd(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			} else if(e.getType() == Extractor.Entity.Type.URL) {
				rtSpan.setSpan(new NoUnderlineClickableSpan() {
					@Override
					public void onClick(View widget) {
						if(ComposerScreen.class.isInstance( widget.getContext()) ){return;}
						String url = null;
						if(realLinks.contains(e.getValue())) url = realLinks.get(e.getValue());
						else url = e.getValue();
						if(!url.startsWith("http://") && !url.startsWith("https://")) url = ("http://" + url);
						context.startActivity(new Intent(Intent.ACTION_VIEW)
							.setData(Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
							.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
					}
				}, e.getStart(), e.getEnd(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			} else if(e.getType() == Extractor.Entity.Type.SEARCH) {
				rtSpan.setSpan(new NoUnderlineClickableSpan() {
					@Override
					public void onClick(View widget) {
						try { 
							if(ComposerScreen.class.isInstance( widget.getContext()) ){return;}
							context.startActivity(new Intent(context, InAppBrowser.class)
							.putExtra("url", context.getString(R.string.google_url) +
									URLEncoder.encode(e.getValue(), "UTF-8"))
							.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
							.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
						}  catch (UnsupportedEncodingException e) { e.printStackTrace(); }
					}
				}, e.getStart(), e.getEnd(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
		return rtSpan;
	}

	private static Hashtable<String, String> getRealLinks(List<Extractor.Entity> entities, UrlEntity[] urls, MediaEntity[] pics) {
		Hashtable<String, String> toReturn = new Hashtable<String, String>();
		for(Extractor.Entity e : entities) {
			if(e.getType() != Extractor.Entity.Type.URL) continue;
			e.setExpandedURL("http://" + e.getValue());
			if(urls != null) {
				for(UrlEntity url : urls) {
					if(url.getUrl() == null) continue;
					String text = url.getUrl().toString();
					if(url.getDisplayUrl() != null) text = url.getDisplayUrl();
					if(text.equals(e.getValue()) || text.equals(e.getValue() + "�")) {
						if(url.getExpandedUrl() != null) {
                            e.setExpandedURL(url.getExpandedUrl().toString());
                        } else {
                            e.setExpandedURL(url.getUrl().toString());
                        }
						break;
					}
				}
			}
			if(e.getExpandedURL() == null) {
				if(pics != null) {
					for(MediaEntity p : pics) {
						if(p.getDisplayUrl().equals(e.getValue()) || p.getDisplayUrl().equals(e.getValue() + "�")) {
							if(p.getExpandedUrl() != null) {
                                e.setExpandedURL(p.getExpandedUrl().toString());
                            } else {
                                e.setExpandedURL(p.getUrl().toString());
                            }
							break;
						}
					}
				}
			}
			toReturn.put(e.getValue(), e.getExpandedURL());
		}
		return toReturn;
	}

	private static String convertMonth(int month, boolean isShort) {
		String toReturn = "";
		switch(month) {
		case Calendar.JANUARY:
			toReturn = "January";
			if(isShort) toReturn = "Jan";
			break;
		case Calendar.FEBRUARY:
			toReturn = "February";
			if(isShort) toReturn = "Feb";
			break;
		case Calendar.MARCH:
			toReturn = "March";
			if(isShort) toReturn = "Mar";
			break;
		case Calendar.APRIL:
			toReturn = "April";
			if(isShort) toReturn = "Apr";
			break;
		case Calendar.MAY:
			toReturn = "May";
			break;
		case Calendar.JUNE:
			toReturn = "June";
			if(isShort) toReturn = "Jun";
			break;
		case Calendar.JULY:
			toReturn = "July";
			if(isShort) toReturn = "Jul";
			break;
		case Calendar.AUGUST:
			toReturn = "August";
			if(isShort) toReturn = "Aug";
			break;
		case Calendar.SEPTEMBER:
			toReturn = "September";
			if(isShort) toReturn = "Sep";
			break;
		case Calendar.OCTOBER:
			toReturn = "October";
			if(isShort) toReturn = "Oct";
			break;
		case Calendar.NOVEMBER:
			toReturn = "November";
			if(isShort) toReturn = "Nov";
			break;
		case Calendar.DECEMBER:
			toReturn = "December";
			if(isShort) toReturn = "Dec";
			break;
		}
		return toReturn;
	}
	public static String friendlyTimeShort(Calendar createdAt) {
        Date now = new Date();
        long diff = now.getTime() - createdAt.getTime().getTime();
        if(diff <= 60000) {
                long seconds = (diff / 6000);
                if(seconds < 5) return "now";
                return Long.toString(seconds) + "s";
        } else if(diff <= 3600000) {
                return Long.toString(diff / 60000) + "m";
        } else if(diff <= 86400000) {
                return Long.toString(diff / 3600000) + "h";
        } else if(diff <= 604800000) {
                return Long.toString(diff / 86400000) + "d"; 
        } else return Long.toString(diff / 604800000) + "w";
	}
	public static String friendlyTimeLong(Date time) {
	    Calendar timeCal = Calendar.getInstance();
		timeCal.setTime(time);
		return friendlyTimeLong(timeCal);
	}

	public static String friendlyTimeLong(Calendar time) {
        Calendar now = Calendar.getInstance();
        String am_pm = "AM";
        if(time.get(Calendar.AM_PM) == Calendar.PM) am_pm = "PM";
        String day = Integer.toString(time.get(Calendar.DAY_OF_MONTH));
        if(day.length() == 1) day = ("0" + day);
        String minute = Integer.toString(time.get(Calendar.MINUTE));
        int hour = time.get(Calendar.HOUR);
        if(hour == 0) hour = 12;
        if(minute.length() == 1) minute = ("0" + minute);
        if(now.get(Calendar.YEAR) == time.get(Calendar.YEAR)) {
            if(now.get(Calendar.MONTH) == time.get(Calendar.MONTH)) {
                if(now.get(Calendar.WEEK_OF_MONTH) == time.get(Calendar.WEEK_OF_MONTH)) {
                    return hour + ":" + minute + am_pm + " " + convertMonth(time.get(Calendar.MONTH), false) + " " + day;
                } else {
                    return hour + ":" + minute + am_pm + " " + convertMonth(time.get(Calendar.MONTH), false) + " " + day;
                }
            } else {
                return hour + ":" + minute + am_pm + " " + convertMonth(time.get(Calendar.MONTH), false) + " " + day;
            }
        } else {
            String year = Integer.toString(time.get(Calendar.YEAR));
            if(now.get(Calendar.YEAR) < time.get(Calendar.YEAR)) year = year.substring(1, 3);
            return hour + ":" + minute + am_pm + " " + convertMonth(time.get(Calendar.MONTH), false) + " " + day + ", " + year;
        }
    }
    public static String friendlyTimeHourMinute(Calendar time) {
    	int hour = time.get(Calendar.HOUR);
    	String minute = Integer.toString(time.get(Calendar.MINUTE));
    	if(minute.length() == 1) minute = ("0" + minute);
    	return hour + ":" + minute;
    }

	public static String generateImageFileName(Context context) {
		String timeStamp =  new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String imageFileName = "IMG_" + timeStamp + ".jpg";
		File storageDir = context.getExternalCacheDir();
		storageDir.mkdirs();
		File fi = new File(storageDir, imageFileName);
		return fi.getAbsolutePath();
	}

	public static File createImageFile(Context context) throws IOException {
		File fi = new File(generateImageFileName(context));
		fi.createNewFile();
		return fi;
	}

	public static String arrayToJson(ArrayList<String> values) {
		JSONArray a = new JSONArray();
		for (int i = 0; i < values.size(); i++) a.put(values.get(i));
		return a.toString();
	}

	public static ArrayList<String> jsonToArray(String json) {
		if(json == null || json.trim().length() == 0) return new ArrayList<String>();
		ArrayList<String> urls = new ArrayList<String>();
		if (json != null) {
			try {
				JSONArray a = new JSONArray(json);
				for (int i = 0; i < a.length(); i++) {
					String url = a.optString(i);
					urls.add(url);
				}
			} catch (JSONException e) { return new ArrayList<String>();  }
		}
		return urls;
	}

	public static int convertDpToPx(Context mContext, float dp) {
		final float scale = mContext.getResources().getDisplayMetrics().density;
		return (int)(dp * scale + 0.5f);
	}

	public static String getAllMentions(Tweet tweet) {
		return getAllMentions(tweet.getFromUser(), tweet.getMentionEntities());
	}
	public static String getAllMentions(Status tweet) {
		return getAllMentions(tweet.getUser().getScreenName(), tweet.getMentionEntities());
	}
	public static String getAllMentions(String initScreenname, String tweetText) {
		String toReturn = "@" + initScreenname;
		Extractor extract = new Extractor();
		List<String> mentions = extract.extractMentionedScreennames(tweetText);
		if(mentions != null) {
			int index = 0;
			for(String mention : mentions) {
				if(index == 0 && mention.equals(AccountService.getCurrentAccount().getUser().getScreenName())) {
                    continue;
                } else if(mention.equals(initScreenname)) continue;
				toReturn += " @" + mention;
			}
		}
		return toReturn;
	}
	public static String getAllMentions(String initScreenname, MentionEntity[] mentions) {
		String toReturn = "@" + initScreenname;
		if(mentions != null) {
			int index = 0;
			for(MentionEntity mention : mentions) {
				if(index == 0 && mention.getScreenName().equals(AccountService.getCurrentAccount().getUser().getScreenName())) continue;
				toReturn += " @" + mention.getScreenName();
			}
		}
		return toReturn;
	}

	public static Bitmap getRoundedImage(Bitmap bitmap, float roundPx) {
		if(bitmap == null) return bitmap;
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		final RectF rectF = new RectF(rect);
		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);
		return output;
	}

	public static boolean isIntentAvailable(Context context, String action) {
		final Intent intent = new Intent(action);
		return isIntentAvailable(context, intent);
	}
	public static boolean isIntentAvailable(Context context, Intent intent){
		final PackageManager packageManager = context.getPackageManager();
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	public static String getUserImage(String screenname, Context mContext){
		return getUserImage(screenname, mContext, null);
	}

	public static String getUserImage(String screenname, Context mContext, User user){
		String url = "https://api.twitter.com/1/users/profile_image?screen_name=" + screenname;
		if(user != null){ // Allows us to have auto-updating cache
			url += "&v=" + Uri.encode(user.getProfileImageUrl().toString());
		}
		int size = convertDpToPx(mContext, 50);
		if( size >= 73 ) {
			url += "&size=bigger";
		} else if( size >= 48 ) {
			url += "&size=normal";
		} else url += "&size=mini";
		return url;
	}
}
