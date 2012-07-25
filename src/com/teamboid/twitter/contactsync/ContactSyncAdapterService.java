package com.teamboid.twitter.contactsync;

import java.util.ArrayList;

import com.teamboid.twitter.services.AccountService;

import com.teamboid.twitterapi.client.Paging;

import android.accounts.Account;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import com.teamboid.twitterapi.relationship.IDs;
import com.teamboid.twitterapi.user.User;

public class ContactSyncAdapterService extends Service {

    @Override
    public IBinder onBind(Intent arg0) {
        return getSyncAdapter().getSyncAdapterBinder();
    }

    SyncAdapterImpl instance;

    SyncAdapterImpl getSyncAdapter() {
        if (instance == null) instance = new SyncAdapterImpl(this);
        return instance;
    }

    private static class SyncAdapterImpl extends AbstractThreadedSyncAdapter {
        Context mContext;
        Account account;

        public SyncAdapterImpl(Context context) {
            super(context, true);
            mContext = context;
        }

        public void addContact(User user) {
            ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();

            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI);
            builder.withValue(RawContacts.ACCOUNT_NAME, account.name);
            builder.withValue(RawContacts.ACCOUNT_TYPE, account.type);
            builder.withValue(RawContacts.SYNC1, user.getScreenName());
            operationList.add(builder.build());

            builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder.withValueBackReference(ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID, 0);
            builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
            builder.withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, user.getName());
            operationList.add(builder.build());

            builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
            builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
            builder.withValue(ContactsContract.Data.MIMETYPE, "vnd.android.cursor.item/vnd.com.teamboid.twitter.account");
            builder.withValue(ContactsContract.Data.DATA1, user.getScreenName());
            builder.withValue(ContactsContract.Data.DATA2, "Twitter Profile");
            builder.withValue(ContactsContract.Data.DATA3, "View profile");
            operationList.add(builder.build());

            try {
                mContext.getContentResolver().applyBatch(ContactsContract.AUTHORITY, operationList);
            } catch (Exception e) {
                Log.d("sync", "Couldn't add " + user.getScreenName());
            }
        }

        com.teamboid.twitter.Account getAccount() {
            for (com.teamboid.twitter.Account acc : AccountService.getAccounts()) {
                if (acc.getUser().getScreenName() == account.name) {
                    return acc;
                }
            }
            return null;
        }

        String getWhatToSync() { // TODO: Actually make this return something the user wants
            return "following";
        }

        int getTotalNumber() {
            try {
                com.teamboid.twitter.Account acc = getAccount();
                String type = getWhatToSync();
                if (type.equals("following")) {
                    return (int) acc.getUser().getFriendsCount();
                } else if (type.equals("followers")) {
                    return (int) acc.getUser().getFollowersCount();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
            return -1;
        }

        User[] getTimeline(Paging paging) {
            try {
                com.teamboid.twitter.Account acc = getAccount();
                String type = getWhatToSync();
                if (type.equals("following")) {
                    IDs ids = acc.getClient().getFriends(acc.getUser().getId(), -2l);
                    return acc.getClient().lookupUsers(ids.getIds());
                } else if (type.equals("followers")) {
                    IDs ids = acc.getClient().getFollowers(acc.getUser().getId(), -2l);
                    return acc.getClient().lookupUsers(ids.getIds());
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            return null;
        }

        @Override
        public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
            this.account = account;
            // Here we can actually sync
            int total = getTotalNumber();
            int got = 0;
            Paging p = new Paging(25);
            while (got > total) {
                User[] users = getTimeline(p);
                for (User user : users) {
                    addContact(user);
                }
                p.setPage(p.getPage() + 1);
            }
        }
    }
}
