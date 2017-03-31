package butter.droid.sync;

/**
 * Created by bkkite on 26/01/17.
 */

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.Set;

import butter.droid.R;
import butter.droid.base.database.DBProvider;
import butter.droid.base.database.tables.Downloads;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.media.models.Movie;
import butter.droid.base.torrent.DownloadTorrentService;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class SyncOfflineContentAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "SyncDataAdapter";
    private static final String i_CONTENT_AUTHORITY = DBProvider.CONTENT_AUTHORITY;
    private Context mContext;

    private DownloadTorrentService mService;

    public SyncOfflineContentAdapter(Context context, boolean autoInitialize) {

        super(context, autoInitialize, true);
        mContext = context;
    }

    public SyncOfflineContentAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        mContext = context;
    }

    @Override
    public void onPerformSync(
            Account account,
            Bundle extras,
            String authority,
            ContentProviderClient provider,
            SyncResult syncResult)
    {
        DownloadTorrentService.bindHere(mContext, mServiceConnection);

        ArrayList<Media> mediaToSync = new ArrayList<>();
        Downloads.getMediaToSync(mContext, mediaToSync);

        for (Media media: mediaToSync)
        {
            Movie movie = (Movie) media;
            final Set<String> keys = movie.torrents.keySet();

            for (String key: keys)
            {
                final Media.Torrent torrent = movie.torrents.get(key);
                mService.addTorrent(torrent);
            }
        }

        if (mService != null) {
            mContext.unbindService(mServiceConnection);
            mService = null;
        }
    }

    @Override
    public void onSyncCanceled() {
        super.onSyncCanceled();

        if (mService != null) {
            mContext.unbindService(mServiceConnection);
            mService = null;
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((DownloadTorrentService.ServiceBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    static public void syncDataOfflineContent(Context context)
    {
        Account account = getAppAccount(context);

        if (account != null) {

            if (ContentResolver.isSyncActive(account, i_CONTENT_AUTHORITY) == true)
                ContentResolver.cancelSync(account, i_CONTENT_AUTHORITY);

            Bundle bundle = new Bundle();
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

            ContentResolver.requestSync(account, i_CONTENT_AUTHORITY, bundle);
        }
    }

    static private Account getAppAccount(Context context)
    {
        try {
            Account[] accounts = AccountManager.get(context).getAccounts();

            for (Account account : accounts) {
               if (account.type.equals(context.getString(R.string.app_name_authority)))
                   return account;
            }

            return null;
        }
        catch(SecurityException e)
        {
            return null;
        }
    }

}
