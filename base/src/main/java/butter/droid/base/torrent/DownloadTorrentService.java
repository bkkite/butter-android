/*
 * This file is part of Butter.
 *
 * Butter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Butter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Butter. If not, see <http://www.gnu.org/licenses/>.
 */

package butter.droid.base.torrent;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import com.github.bkkite.torrentstream.StreamStatus;
import com.github.bkkite.torrentstream.Torrent;
import com.github.bkkite.torrentstream.TorrentOptions;
import com.github.bkkite.torrentstream.TorrentStream;
import com.github.bkkite.torrentstream.listeners.TorrentListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import butter.droid.base.ButterApplication;
import butter.droid.base.R;
import butter.droid.base.content.preferences.Prefs;
import butter.droid.base.database.tables.Downloads;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.media.models.Movie;
import butter.droid.base.utils.FileUtils;
import butter.droid.base.utils.PrefUtils;
import timber.log.Timber;

public class DownloadTorrentService extends Service implements TorrentListener {

    public static final Integer NOTIFICATION_ID = 4534534;

    private static String WAKE_LOCK = "DownloadTorrentService_WakeLock";

    private static DownloadTorrentService sThis;

    private List<Media.Torrent> mDownloadTorrents;
    private Media.Torrent mMediaTorrent;

    private TorrentStream mTorrentStream;
    private Torrent mCurrentTorrent;
    private StreamStatus mStreamStatus;
    private String lastStatus;

    private boolean mShowProgress = false;

    private IBinder mBinder = new ServiceBinder();

    private PowerManager.WakeLock mWakeLock;
    private Timer mStartDownloading;

    public class ServiceBinder extends Binder {
        public DownloadTorrentService getService() {
            return DownloadTorrentService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sThis = this;

        TorrentOptions options = new TorrentOptions.Builder()
                .removeFilesAfterStop(true)
                .maxConnections(PrefUtils.get(this, Prefs.LIBTORRENT_CONNECTION_LIMIT, 200))
                .maxDownloadSpeed(PrefUtils.get(this, Prefs.LIBTORRENT_DOWNLOAD_LIMIT, 0))
                .maxUploadSpeed(PrefUtils.get(this, Prefs.LIBTORRENT_UPLOAD_LIMIT, 0))
                .saveLocation(ButterApplication.getOffLineFileDir())
                .build();

        mTorrentStream = TorrentStream.init(options);

        mDownloadTorrents = new ArrayList<>();

        Timber.d("onCreate");
        lastStatus = "onCreate";
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDownloadTorrents = null;
        Timber.d("onDestroy");
        lastStatus = "onDestroy";
        if (mWakeLock != null && mWakeLock.isHeld())
            mWakeLock.release();

        if (mStartDownloading != null) {
            mStartDownloading.cancel();
            mStartDownloading = null;
        }
    }

    private void loadListToDownload()
    {
        ArrayList<Media> mediaToSync = new ArrayList<>();
        Downloads.getMediaToSync(this, mediaToSync);

        for (Media media: mediaToSync)
        {
            Movie movie = (Movie) media;
            final Set<String> keys = movie.torrents.keySet();

            for (String key: keys)
            {
                final Media.Torrent torrent = movie.torrents.get(key);
                addTorrent(torrent);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("onStartCommand");

        NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.notify(NOTIFICATION_ID, addNotificationStartService());

        loadListToDownload();

        restartDownloading();

        lastStatus = "onStartCommand";

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Timber.d("onBind");
        lastStatus = "onBind";
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Timber.d("onRebind");
        lastStatus = "onRebind";
    }

    private Notification addNotificationStartService()
    {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notif_logo)
                .setContentTitle(getString(R.string.app_name) + " - " + lastStatus)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(Notification.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS);

        if (mStreamStatus != null && mShowProgress)
        {
            int progress = (int) mStreamStatus.progress;
            builder.setProgress(100, progress, false);
        }

        Notification notification = builder.build();
        NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notifManager.notify(NOTIFICATION_ID, notification);

        return notification;
    }

    private boolean isTorrentToDownload(final Media.Torrent torrent)
    {
        for (Media.Torrent torrent_item: mDownloadTorrents) {
            if (torrent.hash.equals(torrent_item.hash))
                return true;
        }

        return false;
    }

    private void popTorrentToDownload()
    {
        try {

            Iterator<Media.Torrent> iterator = mDownloadTorrents.iterator();
            mMediaTorrent = iterator.next();

            iterator.remove();

            Timber.d("pop torrent: " + mMediaTorrent.url);
            lastStatus = "pop torrent: " + mMediaTorrent.url;
        }
        catch (NoSuchElementException e)
        {
            mMediaTorrent = null;
        }
        finally {
            downloadTorrent(mMediaTorrent.url, mMediaTorrent.hash);
        }
    }

    public void addTorrent(@NonNull final Media.Torrent torrent) {
        Timber.d("addTorrent");
        lastStatus = "addTorrent";

        if (isTorrentToDownload(torrent) == false)
            mDownloadTorrents.add(torrent);

        restartDownloading();
    }

    public void delTorrent(@NonNull final Media.Torrent torrent) {
        Timber.d("delTorrent");
        lastStatus = "delTorrent";

        if (mStartDownloading != null)
            mStartDownloading.cancel();

        if (isTorrentToDownload(torrent) == true) {

            if (mTorrentStream.isStreaming() == true){

                if (mMediaTorrent != null && mMediaTorrent.hash.equals(torrent.hash) == true) {
                    mTorrentStream.stopStream();
                    FileUtils.deleteMagnetDownloadedPathVideoFiles(this, torrent.hash);

                    popTorrentToDownload();
                }
                else
                    mDownloadTorrents.remove(torrent);
            }
            else
                mDownloadTorrents.remove(torrent);
        }

        restartDownloading();
    }

    private void downloadTorrent(@NonNull final String torrentUrl, @NonNull final String hash) {
        Timber.d("downloadTorrent");
        lastStatus = "downloadTorrent";

        if (mTorrentStream.isStreaming()) return;

        Timber.d("Starting downloading");
        lastStatus = "Starting downloading";

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if(mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
        }
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK);
        mWakeLock.acquire();

        TorrentOptions options = new TorrentOptions.Builder()
                .removeFilesAfterStop(true)
                .maxConnections(PrefUtils.get(this, Prefs.LIBTORRENT_CONNECTION_LIMIT, 200))
                .maxDownloadSpeed(PrefUtils.get(this, Prefs.LIBTORRENT_DOWNLOAD_LIMIT, 0))
                .maxUploadSpeed(PrefUtils.get(this, Prefs.LIBTORRENT_UPLOAD_LIMIT, 0))
                .saveLocation(FileUtils.getMagnetDownloadedPathVideoFile(this, hash))
                .build();

        mTorrentStream.setOptions(options);

        mShowProgress = false;
        mTorrentStream.addListener(this);
        mTorrentStream.startStream(torrentUrl);
    }

    public void stopDownloading() {

        mTorrentStream.removeListener(this);

        if (mWakeLock != null && mWakeLock.isHeld())
            mWakeLock.release();

        if(!mTorrentStream.isStreaming())
            return;

        mTorrentStream.stopStream();
        mShowProgress = false;
        mStreamStatus = null;

        Timber.d("Stopped torrent and removed files if possible");
        lastStatus = "Stopped torrent and removed files if possible";
    }

    public boolean isDownloading() {
        return mTorrentStream.isStreaming();
    }

    public static void bindHere(Context context, ServiceConnection serviceConnection) {
        Intent torrentServiceIntent = new Intent(context, DownloadTorrentService.class);
        context.bindService(torrentServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public static void start(Context context) {
        Intent torrentServiceIntent = new Intent(context, DownloadTorrentService.class);
        context.startService(torrentServiceIntent);
    }

    protected static void stop() {
        sThis.stopDownloading();
    }

    @Override
    public void onStreamPrepared(Torrent torrent) {
        mCurrentTorrent = torrent;
        mCurrentTorrent.startDownload();

        Timber.d("onStreamPrepared");
        lastStatus = "onStreamPrepared";
    }

    @Override
    public void onStreamStarted(Torrent torrent) {

        Timber.d("onStreamStarted");
        lastStatus = "onStreamStarted";
    }

    @Override
    public void onStreamError(Torrent torrent, Exception e) {

        Timber.d("onStreamError");
        lastStatus = "onStreamError";

        if (mMediaTorrent != null)
            FileUtils.deleteMagnetDownloadedPathVideoFiles(this, mMediaTorrent.hash);

        if (isDownloading() == true)
            stopDownloading();
    }

    @Override
    public void onStreamReady(Torrent torrent) {
        mCurrentTorrent = torrent;
        mShowProgress = true;

        Timber.d("onStreamReady");
        lastStatus = "onStreamReady";
    }

    @Override
    public void onStreamProgress(Torrent torrent, StreamStatus streamStatus) {

        mStreamStatus = streamStatus;

        NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.notify(NOTIFICATION_ID, addNotificationStartService());

        if (mStreamStatus.finished == true)
        {
            Downloads.setMovieSync(this, mMediaTorrent.hash);
            stopDownloading();
        }
    }

    @Override
    public void onStreamStopped() {

        Timber.d("onStreamStopped");
        lastStatus = "onStreamStopped";

    }

    private void restartDownloading()
    {
        if(mStartDownloading != null)
            mStartDownloading.cancel();

        mStartDownloading = new Timer();
        mStartDownloading.scheduleAtFixedRate(new StartDownloading(), 30000, 30000);
    }

    private class StartDownloading extends TimerTask {
        @Override
        public void run()
        {
            Timber.d("StartDownloading");
            lastStatus = "PendingDownloading: " + mDownloadTorrents.size();

            if (mDownloadTorrents.size() > 0 && isDownloading() == false)
                popTorrentToDownload();
        }
    };
}
