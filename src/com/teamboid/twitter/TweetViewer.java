package com.teamboid.twitter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.teamboid.twitterapi.experimentalapis.RelatedResults;
import com.teamboid.twitterapi.status.GeoLocation;
import com.teamboid.twitterapi.status.Place;
import com.teamboid.twitterapi.status.Status;
import com.teamboid.twitterapi.status.entity.url.UrlEntity;
import com.teamboid.twitterapi.utilities.Utils;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.OverlayItem;
import com.handlerexploit.prime.ImageManager;
import com.handlerexploit.prime.RemoteImageView;
import com.teamboid.twitter.cab.TimelineCAB;
import com.teamboid.twitter.columns.TimelineFragment;
import com.teamboid.twitter.listadapters.FeedListAdapter;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.tweetwidgets.TweetWidgetHostHelper;
import com.teamboid.twitter.tweetwidgets.TweetWidgetHostHelper.IFoundWidget;
import com.teamboid.twitter.utilities.BoidActivity;
import com.teamboid.twitter.utilities.TwitlongerHelper;
import com.teamboid.twitter.utilities.Utilities;
import com.teamboid.twitter.views.BetterMapView;
import com.teamboid.twitter.views.GeoMapOverlay;
import com.teamboid.twitter.views.GlowableRelativeLayout;
import com.teamboid.twitter.views.PolygonOverlay;
import com.teamboid.twitter.views.SideNavigationLayout;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The activity that represents the tweet viewer, shown when you click a tweet
 * to view more details about it.
 * 
 * @author Aidan Follestad
 */
public class TweetViewer extends MapActivity {

	private long statusId;
	private boolean isFavorited;
	private Status status;
	private int lastTheme;
	private String mediaUrl;
	private boolean hasConvo;
	BoidActivity boid;

	private FeedListAdapter binder;

	public void showProgress(boolean show) {
		findViewById(R.id.horizontalProgress).setVisibility(
				(show == true) ? View.VISIBLE : View.GONE);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey("lastTheme")) {
				lastTheme = savedInstanceState.getInt("lastTheme");
				setTheme(lastTheme);
			} else
				setTheme(Utilities.getTheme(getApplicationContext()));
		} else
			setTheme(Utilities.getTheme(getApplicationContext()));
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tweet_view);
		if (getIntent().getExtras() != null
				&& getIntent().getExtras().containsKey("account")) {
			long accId = getIntent().getIntExtra("account", 0);
			AccountService.selectedAccount = (long) accId;
			PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext())
					.edit().putLong("last_sel_account", (long) accId).commit();
		}
		getActionBar().setDisplayHomeAsUpEnabled(true);
		binder = new FeedListAdapter(this, null, AccountService
				.getCurrentAccount().getId());
		ListView list = ((ListView) findViewById(android.R.id.list));
		list.setAdapter(binder);
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
					long arg3) {
				startActivity(new Intent(getApplicationContext(),
						TweetViewer.class).putExtra("sr_tweet",
						Utils.serializeObject((Status) binder.getItem(pos)))
						.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
			}
		});
		
		boid = new BoidActivity(this);
		boid.AccountsReady = new BoidActivity.OnAction() {
			
			@Override
			public void done() {
				finishCreate();
			}
		};
		boid.onCreate(savedInstanceState);
	}
	public void finishCreate(){
		
		if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
			try {
				statusId = Long.parseLong(getIntent().getData()
						.getPathSegments().get(2));
				loadTweet();
			} catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(this, R.string.error_str, Toast.LENGTH_SHORT)
						.show();
				finish();
			}
		} else if (getIntent().hasExtra("sr_tweet")) {
			displayTweet((Status) Utils.deserializeObject(getIntent()
					.getStringExtra("sr_tweet")));
		} else {
			preloadTweet();
			loadTweet();
		}
	}

	private TweetWidgetHostHelper twhh = new TweetWidgetHostHelper();

	@Override
	public void onBackPressed() {
		SideNavigationLayout sideNav = (SideNavigationLayout) findViewById(R.id.slide);
		if(sideNav == null) { super.onBackPressed(); return; }
		
		if (sideNav.isShowingNavigationView()) {
			sideNav.showContentView();
		} else
			super.onBackPressed();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (lastTheme == 0)
			lastTheme = Utilities.getTheme(getApplicationContext());
		else if (lastTheme != Utilities.getTheme(getApplicationContext())) {
			lastTheme = Utilities.getTheme(getApplicationContext());
			recreate();
		}
		twhh.load(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		twhh.stop(this);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt("lastTheme", lastTheme);
		super.onSaveInstanceState(outState);
	}

	private void preloadTweet() {
		statusId = getIntent().getLongExtra("tweet_id", 0l);
		if (!getIntent().hasExtra("screen_name"))
			return;
		final String screenName = getIntent().getStringExtra("screen_name");
		setTitle(getString(R.string.tweet_str) + " (@" + screenName + ")");
		RelativeLayout toReturn = (RelativeLayout) findViewById(R.id.tweetDisplay);
		RemoteImageView profilePic = (RemoteImageView) toReturn
				.findViewById(R.id.tweetProfilePic);
		View.OnClickListener profileOpen = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getApplicationContext(),
						ProfileScreen.class)
						.putExtra("screen_name", screenName).addFlags(
								Intent.FLAG_ACTIVITY_CLEAR_TOP));
			}
		};
		profilePic.setImageResource(R.drawable.sillouette);
		profilePic.setImageURL(Utilities.getUserImage(screenName, this));
		profilePic.setOnClickListener(profileOpen);
		final TextView userName = (TextView) toReturn
				.findViewById(R.id.tweetUserName);
		userName.setText(getIntent().getStringExtra("user_name"));
		userName.setOnClickListener(profileOpen);
		final TextView screen = (TextView) toReturn
				.findViewById(R.id.tweetScreenName);
		screen.setOnClickListener(profileOpen);
		screen.setText("@" + screenName);
		TextView contents = (TextView) toReturn
				.findViewById(R.id.tweetContents);
		contents.setText(Utilities.twitterifyText(this, getIntent()
				.getStringExtra("content"), null, null, true, null));
		contents.setMovementMethod(LinkMovementMethod.getInstance());
		((TextView) toReturn.findViewById(R.id.tweetTimer)).setText(Utilities
				.friendlyTimeLong(new Date(getIntent()
						.getLongExtra("timer", 0l)))
				+ " via " + Html.fromHtml(getIntent().getStringExtra("via")));
		isFavorited = getIntent().getBooleanExtra("isFavorited", false);
	}

	private void loadTweet() {
		showProgress(true);
		if (statusId == 0) {
			finish();
			return;
		}
		new Thread(new Runnable() {
			public void run() {
				try {
					final Status tweet;
					if (status == null) {
						tweet = AccountService.getCurrentAccount().getClient()
								.showStatus(statusId);
					} else
						tweet = status;
					runOnUiThread(new Runnable() {
						public void run() {
							displayTweet(tweet);
							loadConversation(tweet);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
					runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(getApplicationContext(),
									R.string.failed_load_tweet,
									Toast.LENGTH_LONG).show();
							showProgress(false);
						}
					});
					return;
				}
				runOnUiThread(new Runnable() {
					public void run() {
						showProgress(false);
					}
				});
			}
		}).start();
	}

	private void loadConversation(final Status tweet) {
		new Thread(new Runnable() {
			public void run() {
				try {
					if (tweet.getInReplyToStatusId() > 0) {
						RelatedResults res = AccountService.getCurrentAccount()
								.getClient().getRelatedResults(tweet.getId());
						final ArrayList<Status> toAdd = new ArrayList<Status>();
						for (Status s : res.getTweetsWithConversation())
							toAdd.add(s);
						boolean found = false;
						for (Status stat : toAdd) {
							if (stat.getId() == tweet.getInReplyToStatusId()) {
								found = true;
								break;
							}
						}
						if (!found) {
							final Status repliedTo = AccountService
									.getCurrentAccount().getClient()
									.showStatus(tweet.getInReplyToStatusId());
							toAdd.add(repliedTo);
						}
						toAdd.add(tweet);
						for (Status s : res.getTweetsWithReply())
							toAdd.add(s);
						if (toAdd.size() > 0) {
							runOnUiThread(new Runnable() {
								public void run() {
									hasConvo = true;
									invalidateOptionsMenu();
									binder.add(toAdd.toArray(new Status[0]));
									binder.notifyDataSetChanged();
									if(findViewById(R.id.glowstone) != null){
										((GlowableRelativeLayout) findViewById(R.id.glowstone))
												.glow();
									}
								}
							});
						}
					} else {
						final SideNavigationLayout sideNav = (SideNavigationLayout) findViewById(R.id.slide);
						if(sideNav == null){
							findViewById(android.R.id.list).setVisibility(View.GONE);
						} else{
							sideNav.enabled = false;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(getApplicationContext(),
									R.string.failed_load_tweet,
									Toast.LENGTH_LONG).show();
						}
					});
				}
				runOnUiThread(new Runnable() {
					public void run() {
						showProgress(false);
					}
				});
			}
		}).start();
	}

	private void displayTweet(Status tweet) {
		status = tweet;
		statusId = status.getId();
		isFavorited = status.isFavorited();
		if (status.isRetweet())
			status = status.getRetweetedStatus();
		RemoteImageView profilePic = (RemoteImageView) findViewById(R.id.tweetProfilePic);
		profilePic.setImageResource(R.drawable.sillouette);
		profilePic.setImageURL(Utilities.getUserImage(status.getUser()
				.getScreenName(), this));
		View.OnClickListener profileOpen = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getApplicationContext(),
						ProfileScreen.class).putExtra("screen_name",
						status.getUser().getScreenName()).addFlags(
						Intent.FLAG_ACTIVITY_CLEAR_TOP));
			}
		};
		profilePic.setOnClickListener(profileOpen);
		TextView userName = (TextView) findViewById(R.id.tweetUserName);
		userName.setText(status.getUser().getName());
		userName.setOnClickListener(profileOpen);
		TextView screenName = (TextView) findViewById(R.id.tweetScreenName);
		screenName.setText("@" + status.getUser().getScreenName());
		screenName.setOnClickListener(profileOpen);
		TextView contents = (TextView) findViewById(R.id.tweetContents);
		contents.setText(Utilities.twitterifyText(this, status.getText(),
				status.getUrlEntities(), status.getMediaEntities(), true, null));
		contents.setMovementMethod(LinkMovementMethod.getInstance());
		((TextView) findViewById(R.id.tweetTimer)).setText(Utilities
				.friendlyTimeLong(status.getCreatedAt())
				+ " via "
				+ Html.fromHtml(status.getSource()));
		invalidateOptionsMenu();
		expandTwitlonger(contents);
		expandTwtmore(contents);
		displayLocation();
		displayMedia();
		if (tweetWidgetsLoaded == false) {
			tweetWidgetsLoaded = true;
			if (status.getUrlEntities() != null) {
				for (UrlEntity ue : status.getUrlEntities()) {
					fetchWidgetForUrl(ue.getExpandedUrl().toString());
				}
			}
		}
		loadConversation(tweet);
	}

	boolean tweetWidgetsLoaded = false;

	List<String> widgetPos = new ArrayList<String>();

	/**
	 * Fetches a Widget for a URL. If there is not widget for it, we just ignore
	 * it
	 * 
	 * @param url
	 */
	public void fetchWidgetForUrl(final String url) {
		final LinearLayout widgets = (LinearLayout) findViewById(R.id.widgets);
		final String widgetName = twhh.getWidgetName(url, this);
		if (widgetName == null)
			return;
		final TextView loading = new TextView(this);
		loading.setText(getString(R.string.load_widget).replace("{widget}",
				widgetName));
		loading.setPadding(5, 5, 5, 5);
		loading.setGravity(Gravity.CENTER_HORIZONTAL);
		widgets.addView(loading, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
		widgetPos.add(url);
		twhh.findWidget(status.getId(), url, this, new IFoundWidget() {
			@Override
			public void displayWidget(View v) {
				try {
					widgets.removeViewAt(widgetPos.indexOf(url));
					widgets.addView(v, widgetPos.indexOf(url),
							new LinearLayout.LayoutParams(
									LinearLayout.LayoutParams.MATCH_PARENT,
									LinearLayout.LayoutParams.WRAP_CONTENT));
				} catch (Exception e) {
					Log.d("tv", "TweetWidget failed:");
					e.printStackTrace();
				}
			}

			@Override
			public void displayError() {
				loading.setText(getString(R.string.error_in_widget).replace(
						"{widget}", widgetName));
			}

		});
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.tweetviewer_actionbar, menu);
		if (status != null) {
			if (status.getUser().getId() == AccountService.getCurrentAccount()
					.getId()) {
				menu.findItem(R.id.retweetAction).setVisible(false);
				menu.findItem(R.id.deleteAction).setVisible(true);
			} else if(status.getUser().isProtected()){
				menu.findItem(R.id.retweetAction).setVisible(false);
			}
			
			// And here we have another api :)
			PackageManager pm = this.getPackageManager();
			Intent intent = new Intent("com.teamboid.twitter.tweet_actions");
			intent.setData( Uri.parse( "http://twitter.com/" + status.getUser().getScreenName() + "/status/" + status.getId() ));
			intent.putExtra("contents", status.getText());
			intent.putExtra("byUser", status.getUser().getScreenName());
			intent.putExtra("id", status.getId());
			
			List<ResolveInfo> ri = pm.queryIntentActivities(intent, 0);
			for(ResolveInfo r : ri){
				menu.add(r.loadLabel(pm)).setIntent(intent)
					.setIcon(r.loadIcon(pm)).setShowAsAction( MenuItem.SHOW_AS_ACTION_NEVER );
			}
		}
		MenuItem fav = menu.findItem(R.id.favoriteAction);
		if (isFavorited) {
			fav.setTitle(R.string.unfavorite_str);
			fav.setIcon(getTheme().obtainStyledAttributes(
					new int[] { R.attr.favoriteIcon }).getDrawable(0));
		} else
			fav.setTitle(R.string.favorite_str);
		if (hasConvo) {
			MenuItem convo = menu.findItem(R.id.viewConvoAction);
			convo.setVisible(true);
		}
		return true;
	}

	private void retweet(final MenuItem item, final String screenName) {
		new Thread(new Runnable() {
			public void run() {
				try {
					final Status result = AccountService.getCurrentAccount()
							.getClient().retweetStatus(statusId);
					runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(
									getApplicationContext(),
									getString(R.string.retweeted_status)
											.replace("{user}", screenName),
									Toast.LENGTH_LONG).show();
							AccountService.getFeedAdapter(TweetViewer.this,
									TimelineFragment.ID,
									AccountService.getCurrentAccount().getId())
									.add(new Status[] { result });
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
					runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(getApplicationContext(),
									R.string.failed_retweet, Toast.LENGTH_LONG)
									.show();
						}
					});
				}
				runOnUiThread(new Runnable() {
					public void run() {
						item.setEnabled(true);
						showProgress(false);
					}
				});
			}
		}).start();
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		boid.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		String content = null;
		String replyToName = null;
		if (status != null) {
			content = status.getText();
			replyToName = status.getUser().getScreenName();
		} else {
			content = getIntent().getStringExtra("content");
			replyToName = getIntent().getStringExtra("screen_name");
		}
		final String screenName = replyToName;
		switch (item.getItemId()) {
		case android.R.id.home:
			super.onBackPressed();
			return true;
		case R.id.replyAction:
			startActivity(new Intent(this, ComposerScreen.class)
					.putExtra("reply_to", status)
					.putExtra("reply_to_name", replyToName)
					.putExtra("append",
							Utilities.getAllMentions(replyToName, content))
					.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
			return true;
		case R.id.favoriteAction:
			if (statusId == 0)
				return false;
			showProgress(true);
			item.setEnabled(false);
			if (isFavorited) {
				item.setIcon(getTheme().obtainStyledAttributes(
						new int[] { R.attr.unfavoriteIcon }).getDrawable(0));
				new Thread(new Runnable() {
					public void run() {
						try {
							final Status unfavorited = AccountService
									.getCurrentAccount().getClient()
									.destroyFavorite(statusId);
							runOnUiThread(new Runnable() {
								public void run() {
									isFavorited = false;
									unfavorited.setFavorited(false);
									TimelineCAB.reinsertStatus(unfavorited);
								}
							});
						} catch (Exception e) {
							e.printStackTrace();
							runOnUiThread(new Runnable() {
								public void run() {
									Toast.makeText(
											getApplicationContext(),
											getString(
													R.string.failed_unfavorite)
													.replace("{user}",
															screenName),
											Toast.LENGTH_LONG).show();
									item.setIcon(getTheme()
											.obtainStyledAttributes(
													new int[] { R.attr.favoriteIcon })
											.getDrawable(0));
								}
							});
						}
						runOnUiThread(new Runnable() {
							public void run() {
								item.setEnabled(true);
								showProgress(false);
							}
						});
					}
				}).start();
			} else {
				item.setIcon(getTheme().obtainStyledAttributes(
						new int[] { R.attr.favoriteIcon }).getDrawable(0));
				new Thread(new Runnable() {
					public void run() {
						try {
							final Status favorited = AccountService
									.getCurrentAccount().getClient()
									.createFavorite(statusId);
							runOnUiThread(new Runnable() {
								public void run() {
									isFavorited = true;
									favorited.setFavorited(true);
									TimelineCAB.reinsertStatus(favorited);
								}
							});
						} catch (Exception e) {
							e.printStackTrace();
							runOnUiThread(new Runnable() {
								public void run() {
									Toast.makeText(
											getApplicationContext(),
											getString(R.string.failed_favorite)
													.replace("{user}",
															screenName),
											Toast.LENGTH_LONG).show();
									item.setIcon(getTheme()
											.obtainStyledAttributes(
													new int[] { R.attr.unfavoriteIcon })
											.getDrawable(0));
								}
							});
						}
						runOnUiThread(new Runnable() {
							public void run() {
								item.setEnabled(true);
								showProgress(false);
							}
						});
					}
				}).start();
			}
			return true;
		case R.id.retweetSubItem:
			if (AccountService.getAccounts().size() > 1) {
				AlertDialog.Builder diag = new AlertDialog.Builder(
						TweetViewer.this);
				diag.setTitle(R.string.retweet_str);
				diag.setMessage(getString(R.string.confirm_retweet).replace(
						"{account}",
						AccountService.getCurrentAccount().getUser()
								.getScreenName()));
				diag.setPositiveButton(R.string.yes_str,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
								item.setEnabled(false);
								showProgress(true);
								retweet(item, screenName);
							}
						});
				diag.setNegativeButton(R.string.no_str,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						});
				diag.create().show();
			} else
				retweet(item, replyToName);
			return true;
		case R.id.quoteSubItem:
			startActivity(new Intent(this, ComposerScreen.class).putExtra(
					"text", "RT @" + replyToName + ": " + content).addFlags(
					Intent.FLAG_ACTIVITY_CLEAR_TOP));
			return true;
		case R.id.shareAction:
			String text = status.getText() + "\n\n(via @" + replyToName
					+ ", http://twitter.com/" + replyToName + "/status/"
					+ Long.toString(status.getId());
			startActivity(Intent.createChooser(
					new Intent(Intent.ACTION_SEND).setType("text/plain")
							.putExtra(Intent.EXTRA_TEXT, text),
					getString(R.string.share_str)).addFlags(
					Intent.FLAG_ACTIVITY_CLEAR_TOP));
			return true;
		case R.id.deleteAction:
			showProgress(true);
			item.setEnabled(false);
			new Thread(new Runnable() {
				public void run() {
					try {
						final Status removed = AccountService
								.getCurrentAccount().getClient()
								.destroyStatus(statusId);
						runOnUiThread(new Runnable() {
							public void run() {
								TimelineCAB.removeStatus(removed);
								Toast.makeText(getApplicationContext(),
										R.string.successfully_deleted_status,
										Toast.LENGTH_LONG).show();
								finish();
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
						runOnUiThread(new Runnable() {
							public void run() {
								Toast.makeText(getApplicationContext(),
										R.string.failed_delete_status,
										Toast.LENGTH_LONG).show();
								showProgress(false);
								item.setEnabled(true);
							}
						});
					}
				}
			}).start();
			setResult(RESULT_OK);
			finish();
			return true;
		case R.id.copyAction:
			ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setPrimaryClip(ClipData.newPlainText("Boid_Tweet",
					content));
			Toast.makeText(
					getApplicationContext(),
					getString(R.string.copied_str).replace("{user}",
							replyToName), Toast.LENGTH_SHORT).show();
			return true;
		case R.id.viewConvoAction:
			SideNavigationLayout sideNav = (SideNavigationLayout) findViewById(R.id.slide);
			if (!sideNav.isShowingNavigationView()) {
				sideNav.showNavigationView();
			} else
				sideNav.showContentView();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void expandTwitlonger(final TextView txt) {
		if (status.getUrlEntities() == null
				|| status.getUrlEntities().length == 0)
			return;
		String url = null;
		for (int i = 0; i < status.getUrlEntities().length; i++) {
			UrlEntity e = status.getUrlEntities()[i];
			if (e.getDisplayUrl().contains("tl.gd")) {
				url = "http://" + e.getDisplayUrl();
				break;
			}
		}
		if (url == null)
			return;
		final String id = url.substring(url.lastIndexOf("/") + 1, url.length())
				.replace("/", "");
		Toast.makeText(this, R.string.expanding_twitlonger, Toast.LENGTH_SHORT)
				.show();
		new Thread(new Runnable() {
			public void run() {
				final TwitlongerHelper helper = TwitlongerHelper.create(
						"boidforandroid", "nJRJRiNn3VGiCWZl", AccountService
								.getCurrentAccount().getUser().getScreenName());
				try {
					final String content = helper.readPost(id);
					txt.post(new Runnable() {
						public void run() {
							txt.setText(Utilities.twitterifyText(
									getApplicationContext(), content,
									status.getUrlEntities(),
									status.getMediaEntities(), true, null));
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
					txt.post(new Runnable() {
						public void run() {
							Toast.makeText(getApplicationContext(),
									R.string.failed_expand_twitlonger,
									Toast.LENGTH_LONG).show();
						}
					});
				}
			}
		}).start();
	}

	private void expandTwtmore(final TextView txt) {
		if (status.getUrlEntities() == null
				|| status.getUrlEntities().length == 0)
			return;
		String url = null;
		for (int i = 0; i < status.getUrlEntities().length; i++) {
			UrlEntity e = status.getUrlEntities()[i];
			if (e.getDisplayUrl().contains("tm.to"))
				url = "http://" + e.getDisplayUrl();
		}
		if (url == null)
			return;
		Toast.makeText(this, R.string.expanding_twtmore, Toast.LENGTH_SHORT)
				.show();
		final String getUrl = (url + "/content");
		new Thread(new Runnable() {
			public void run() {
				try {
					HttpClient httpclient = new DefaultHttpClient();
					HttpGet httpget = new HttpGet(getUrl);
					HttpResponse response = httpclient.execute(httpget);
					final String responseStr = EntityUtils.toString(
							response.getEntity(), "UTF-8");
					txt.post(new Runnable() {
						public void run() {
							txt.setText(Utilities.twitterifyText(
									getApplicationContext(), responseStr,
									status.getUrlEntities(),
									status.getMediaEntities(), true, null));
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
					txt.post(new Runnable() {
						public void run() {
							Toast.makeText(getApplicationContext(),
									R.string.failed_expand_twtmore,
									Toast.LENGTH_LONG).show();
						}
					});
				}
			}
		}).start();
	}

	private Place place;

	private void displayLocation() {
		if (status.getGeoLocation() == null && status.getPlace() == null)
			return;
		final GeoLocation point = status.getGeoLocation();
		if (place == null)
			place = status.getPlace();
		if (place != null && place.getGeometryCoordinates() == null) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						place = AccountService.getCurrentAccount().getClient()
								.getPlaceDetails(place.getId());
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								displayLocation();
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			}).start();
			return;
		}
		View m = null;
		if (findViewById(R.id.mapView) != null) {
			m = ((ViewStub) findViewById(R.id.mapView)).inflate();
		} else
			m = findViewById(R.id.mapViewImported);
		m.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent geo = new Intent(Intent.ACTION_VIEW);
				if (point != null) {
					geo.setData(Uri.parse("geo:" + point.getLatitude() + ","
							+ point.getLongitude()));
					if (Utilities.isIntentAvailable(TweetViewer.this, geo)) {
						startActivity(geo);
					} else {
						geo.setData(Uri.parse("https://maps.google.com/maps?q="
								+ point.getLatitude() + ","
								+ point.getLongitude()));
						startActivity(geo);
					}
				} else {
					geo.setData(Uri.parse("geo:0,0?q=" + place.getFullName()));
					if (Utilities.isIntentAvailable(TweetViewer.this, geo)) {
						startActivity(geo);
					} else {
						geo.setData(Uri.parse("https://maps.google.com/maps?q="
								+ place.getFullName()));
						startActivity(geo);
					}
				}
			}

		});
		BetterMapView miniMap = (BetterMapView) m.findViewById(R.id.miniMap);
		if (place != null && place.getGeometryCoordinates() != null
				&& place.getGeometryCoordinates().length >= 1) {
			PolygonOverlay overlay = new PolygonOverlay(
					place.getGeometryCoordinates()[0]);
			miniMap.getOverlays().add(overlay);
			List<GeoPoint> items = new ArrayList<GeoPoint>();
			for (GeoLocation p : place.getGeometryCoordinates()[0]) {
				items.add(BetterMapView.fromTwitter(p));
			}
			miniMap.setMapBoundsToPois(items, 0, 0);
			Log.d("fe", "Geometry");
		}
		if (point != null) {
			GeoPoint gp = BetterMapView.fromTwitter(point);
			Drawable drawable = getResources().getDrawable(
					R.drawable.locate_dark);
			GeoMapOverlay itemizedoverlay = new GeoMapOverlay(drawable, this);
			OverlayItem overlayitem = new OverlayItem(gp, "", null);
			itemizedoverlay.addOverlay(overlayitem);
			miniMap.getOverlays().add(itemizedoverlay);
			if (place != null && place.getGeometryCoordinates() != null
					&& place.getGeometryCoordinates().length >= 1) {
				miniMap.getController().animateTo(gp);
			}
			miniMap.invalidate();
		}
		TextView dets = (TextView) m.findViewById(R.id.miniMapDetails);
		if (place != null)
			dets.setText(place.getFullName());
		else
			dets.setText(point.getLatitude() + ", " + point.getLongitude());
	}

	private void displayMedia() {
		if (!PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).getBoolean("enable_media_download",
				true))
			return;
		mediaUrl = Utilities.getTweetYFrogTwitpicMedia(status);
		if (mediaUrl != null) {
			final ImageView imageView = (ImageView) findViewById(R.id.tweetMedia);
			imageView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					ImageManager download = ImageManager
							.getInstance(getApplicationContext());
					download.get(mediaUrl,
							new ImageManager.OnImageReceivedListener() {
								@Override
								public void onImageReceived(String source,
										Bitmap bitmap) {
									try {
										String file = Utilities
												.generateImageFileName(TweetViewer.this);
										if (bitmap
												.compress(CompressFormat.PNG,
														100,
														new FileOutputStream(
																file))) {
											startActivity(new Intent(
													Intent.ACTION_VIEW)
													.setDataAndType(Uri
															.fromFile(new File(
																	file)),
															"image/*"));
										}
									} catch (FileNotFoundException e) {
										e.printStackTrace();
										Toast.makeText(getApplicationContext(),
												e.getLocalizedMessage(),
												Toast.LENGTH_LONG).show();
									}
								}
							});
				}
			});
			final ProgressBar progress = (ProgressBar) findViewById(R.id.tweetMediaProgress);
			findViewById(R.id.tweetMediaFrame).setVisibility(View.VISIBLE);
			progress.setVisibility(View.VISIBLE);
			ImageManager download = ImageManager
					.getInstance(getApplicationContext());
			download.get(mediaUrl, new ImageManager.OnImageReceivedListener() {
				@Override
				public void onImageReceived(String source, Bitmap bitmap) {
					imageView.setVisibility(View.VISIBLE);
					progress.setVisibility(View.GONE);
					imageView.setImageBitmap(bitmap);
				}
			});
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}