package com.teamboid.twitter;

import java.util.ArrayList;
import java.util.List;

import com.teamboid.twitter.listadapters.MessageConvoAdapter;
import com.teamboid.twitter.listadapters.MessageItemAdapter;
import com.teamboid.twitter.listadapters.MessageConvoAdapter.DMConversation;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Extractor;
import com.teamboid.twitter.utilities.Utilities;

import android.app.ListActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;
import com.teamboid.twitterapi.client.Twitter;
import com.teamboid.twitterapi.dm.DirectMessage;

/**
 * The activity that represents the message conversation viewer screen.
 *
 * @author Aidan Follestad
 */
public class ConversationScreen extends ListActivity {

    private int lastTheme;
    private boolean showProgress;
    private MessageItemAdapter adapt;
    private boolean isLoading;
    private boolean shownLinksMessage;

    public void showProgress(boolean visible) {
        if (showProgress == visible) return;
        showProgress = visible;
        setProgressBarIndeterminateVisibility(visible);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("lastTheme")) {
                lastTheme = savedInstanceState.getInt("lastTheme");
                setTheme(lastTheme);
            } else setTheme(Utilities.getTheme(getApplicationContext()));
            if (savedInstanceState.containsKey("showProgress")) showProgress(true);
        } else setTheme(Utilities.getTheme(getApplicationContext()));
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.conversation_screen);
        setProgressBarIndeterminateVisibility(false);
        setTitle("@" + getIntent().getStringExtra("screen_name"));
        final EditText content = (EditText) findViewById(R.id.messageInput);
        content.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                getLengthIndicator();
                invalidateOptionsMenu();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        adapt = new MessageItemAdapter(this);
        setListAdapter(adapt);
        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("notification")) {
            reloadMessages();
        } else loadCachedMessages();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (lastTheme == 0) lastTheme = Utilities.getTheme(getApplicationContext());
        else if (lastTheme != Utilities.getTheme(getApplicationContext())) {
            lastTheme = Utilities.getTheme(getApplicationContext());
            recreate();
        }
    }

    private void loadCachedMessages() {
        DMConversation convo = AccountService.getMessageConvoAdapter(this,
                AccountService.getCurrentAccount().getId()).find(getIntent().getStringExtra("screen_name"));
        if (convo != null) adapt.setConversation(convo);
    }

    public void reloadMessages() {
        if (isLoading) return;
        isLoading = true;
        showProgress(true);
        final String screenName = getIntent().getStringExtra("screen_name");
        new Thread(new Runnable() {
            public void run() {
                final Account acc = AccountService.getCurrentAccount();
                if (acc != null) {
                    try {
                        final ArrayList<DirectMessage> messages = new ArrayList<DirectMessage>();
                        DirectMessage[] recv = acc.getClient().getDirectMessages(null);
                        if(recv != null && recv.length > 0) {
                            for(DirectMessage msg : recv) messages.add(msg);
                        }
                        DirectMessage[] sent = acc.getClient().getSentDirectMessages(null);
                        if(sent != null && sent.length > 0) {
                            for(DirectMessage msg : sent) messages.add(msg);
                        }
                        runOnUiThread(new Runnable() {
                            public void run() {
                                MessageConvoAdapter list = AccountService.getMessageConvoAdapter(
                                        ConversationScreen.this, AccountService.getCurrentAccount().getId());
                                list.add(messages.toArray(new DirectMessage[0]));
                                for (int i = 0; i < list.getCount(); i++) {
                                    if (((DMConversation) list.getItem(i)).getToScreenName().equals(screenName)) {
                                        adapt.setConversation((DMConversation) list.getItem(i));
                                        break;
                                    }
                                }
                            }
                        });
                    } catch (final Exception e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        showProgress(false);
                        isLoading = false;
                    }
                });
            }
        }).start();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("lastTheme", lastTheme);
        if (showProgress) {
            showProgress(false);
            outState.putBoolean("showProgress", true);
        }
        super.onSaveInstanceState(outState);
    }

    private int lengthIndic;

    private int getLengthIndicator() {
        int shortLength = AccountService.configShortURLLength;
        String text = ((EditText) findViewById(R.id.messageInput)).getText().toString();
        int toReturn = (140 - text.length());
        List<String> urls = new Extractor().extractURLs(text);
        for (String u : urls) {
            if (!shownLinksMessage) {
                shownLinksMessage = true;
                Toast.makeText(getApplicationContext(), getString(R.string.links_shortened), Toast.LENGTH_SHORT).show();
            }
            toReturn += (u.length() - shortLength);
        }
        lengthIndic = toReturn;
        if (toReturn > 140) {
            toReturn = 140;
            lengthIndic = 140;
        }
        invalidateOptionsMenu();
        return toReturn;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.conversation_actionbar, menu);
        if (showProgress) {
            final MenuItem refreshAction = menu.findItem(R.id.refreshAction);
            refreshAction.setEnabled(false);
        }
        final EditText content = (EditText) findViewById(R.id.messageInput);
        MenuItem sendAct = menu.findItem(R.id.sendAction);
        if (lengthIndic < 0 || content.getText().toString().trim().length() == 0) {
            if (content.getText().toString().trim().length() == 0) lengthIndic = 140;
            sendAct.setEnabled(false);
        } else sendAct.setEnabled(true);
        sendAct.setTitle(getString(R.string.send_str) + " (" + Integer.toString(lengthIndic) + ")");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //startActivity(new Intent(this, TimelineScreen.class));
                //finish();

                super.onBackPressed(); //Back button should go back, not restart a new activity
                return true;
            case R.id.refreshAction:
                reloadMessages();
                return true;
            case R.id.sendAction:
                showProgress(true);
                item.setEnabled(true);
                final EditText content = (EditText) findViewById(R.id.messageInput);
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Twitter cl = AccountService.getCurrentAccount().getClient();
                            final DirectMessage sentMsg = cl.createDirectMessage(
                                    getIntent().getStringExtra("screen_name"),
                                    content.getText().toString().trim());
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    MessageConvoAdapter list = (MessageConvoAdapter)AccountService.getMessageConvoAdapter(
                                            ConversationScreen.this, AccountService.getCurrentAccount().getId());
                                    list.add(new DirectMessage[] { sentMsg });
                                    adapt.add(sentMsg);
                                    content.setText("");
                                }
                            });
                        } catch (final Exception e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(getApplicationContext(), R.string.failed_send_message, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        runOnUiThread(new Runnable() {
                            public void run() {
                                showProgress(false);
                                item.setEnabled(true);
                            }
                        });
                    }
                }).start();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}