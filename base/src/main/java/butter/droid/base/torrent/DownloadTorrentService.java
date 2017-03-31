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

import com.github.sv244.torrentstream.StreamStatus;
import com.github.sv244.torrentstream.Torrent;
import com.github.sv244.torrentstream.TorrentOptions;
import com.github.sv244.torrentstream.TorrentStream;
import com.github.sv244.torrentstream.listeners.TorrentListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;

import butter.droid.base.ButterApplication;
import butter.droid.base.R;
import butter.droid.base.content.preferences.Prefs;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.utils.FileUtils;
import butter.droid.base.utils.PrefUtils;
import timber.log.Timber;

public class DownloadTorrentService extends Service implements TorrentListener {

    public static final Integer NOTIFICATION_ID = 4534534;

    private static String WAKE_LOCK = "DownloadTorrentService_WakeLock";

    private static DownloadTorrentService sThis;

    private List<Media.Torrent> mDownloadTorrents;

    private TorrentStream mTorrentStream;
    private Torrent mCurrentTorrent;
    private StreamStatus mStreamStatus;

    private boolean mIsReady = false, mStopped = false;

    private IBinder mBinder = new ServiceBinder();
    private List<TorrentListener> mListener = new ArrayList<>();

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

        TorrentOptions options = new TorrentOptions();
        options.setRemoveFilesAfterStop(true);
        options.setMaxConnections(PrefUtils.get(this, Prefs.LIBTORRENT_CONNECTION_LIMIT, 200));
        options.setMaxDownloadSpeed(PrefUtils.get(this, Prefs.LIBTORRENT_DOWNLOAD_LIMIT, 0));
        options.setMaxUploadSpeed(PrefUtils.get(this, Prefs.LIBTORRENT_UPLOAD_LIMIT, 0));
        options.setSaveLocation(ButterApplication.getOffLineFileDir());
        mTorrentStream = TorrentStream.init(options);

        mDownloadTorrents = new ArrayList<>();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDownloadTorrents = null;
        Timber.d("onDestroy");
        if (mWakeLock != null && mWakeLock.isHeld())
            mWakeLock.release();

        if (mStartDownloading != null)
            mStartDownloading.cancel();

        NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.cancel(NOTIFICATION_ID);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("onStartCommand");

        NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.notify(NOTIFICATION_ID, addNotificationStartService());

        if(mStartDownloading == null) {
            mStartDownloading = new Timer();
            mStartDownloading.scheduleAtFixedRate(new StartDownloading(), 5000, 5000);
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Timber.d("onBind");

        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Timber.d("onRebind");

    }

    private Notification addNotificationStartService()
    {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notif_logo)
                .setContentTitle(getString(R.string.app_name) + " - " + getString(R.string.offlineServiceRunning))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(Notification.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE);

        if(mStreamStatus != null && mIsReady) {
            String downloadSpeed;
            DecimalFormat df = new DecimalFormat("#############0.00");
            if (mStreamStatus.downloadSpeed / 1024 < 1000) {
                downloadSpeed = df.format(mStreamStatus.downloadSpeed / 1024) + " KB/s";
            } else {
                downloadSpeed = df.format(mStreamStatus.downloadSpeed / (1024 * 1024)) + " MB/s";
            }
            String progress = df.format(mStreamStatus.progress);
            builder.setContentText(mCurrentTorrent.getVideoFile().getName() + " " + progress + "%, ↓" + downloadSpeed);
        }

        Notification notification = builder.build();

        NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notifManager.notify(NOTIFICATION_ID, notification);

        return notification;
    }

    private boolean isTorrentToDownload(final Media.Torrent torrent)
    {
        for (Media.Torrent torrent_item: mDownloadTorrents) {
            if (torrent.hash.equals(torrent.hash))
                return true;
        }

        return false;
    }

    public void addTorrent(@NonNull final Media.Torrent torrent) {
        Timber.d("addTorrent");

        if (isTorrentToDownload(torrent) == false)
            mDownloadTorrents.add(torrent);
    }

    public void delTorrent(@NonNull final Media.Torrent torrent) {
        Timber.d("delTorrent");

        if (mStartDownloading != null)
            mStartDownloading.cancel();

        if (isTorrentToDownload(torrent) == true) {

            if (mTorrentStream.isStreaming() == true) {
                String saveLocation = FileUtils.getMagnetDownloadedPathVideoFile(this, torrent.hash);

                if (mCurrentTorrent.getSaveLocation().getAbsolutePath().startsWith(saveLocation)) {
                    mTorrentStream.stopStream();
                    FileUtils.deleteMagnetDownloadedPathVideoFiles(this, torrent.hash);

                    try {
                        Media.Torrent nextTorrent = mDownloadTorrents.iterator().next();
                        downloadTorrent(nextTorrent.url, nextTorrent.hash);
                    }
                    catch (NoSuchElementException e)
                    {
                    }
                }
            }

            mDownloadTorrents.remove(torrent);
        }

        if(mStartDownloading == null) {
            mStartDownloading = new Timer();
            mStartDownloading.scheduleAtFixedRate(new StartDownloading(), 5000, 5000);
        }
    }

    private void downloadTorrent(@NonNull final String torrentUrl, @NonNull final String hash) {
        Timber.d("downloadTorrent");
        mStopped = false;

        if (mTorrentStream.isStreaming()) return;

        Timber.d("Starting downloading");

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if(mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
        }
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK);
        mWakeLock.acquire();

        TorrentOptions options = mTorrentStream.getOptions();
        options.setRemoveFilesAfterStop(true);
        options.setMaxConnections(PrefUtils.get(this, Prefs.LIBTORRENT_CONNECTION_LIMIT, 200));
        options.setMaxDownloadSpeed(PrefUtils.get(this, Prefs.LIBTORRENT_DOWNLOAD_LIMIT, 0));
        options.setMaxUploadSpeed(PrefUtils.get(this, Prefs.LIBTORRENT_UPLOAD_LIMIT, 0));
        options.setSaveLocation(FileUtils.getMagnetDownloadedPathVideoFile(this, hash));
        mTorrentStream.setOptions(options);

        mIsReady = false;
        mTorrentStream.addListener(this);
        mTorrentStream.startStream(torrentUrl);
    }

    public void stopDownloading() {
        mStopped = true;
        mTorrentStream.removeListener(this);

        if (mWakeLock != null && mWakeLock.isHeld())
            mWakeLock.release();

        if(!mTorrentStream.isStreaming())
            return;

        mTorrentStream.stopStream();
        mIsReady = false;

        Timber.d("Stopped torrent and removed files if possible");
    }

    public boolean isDownloading() {
        return mTorrentStream.isStreaming();
    }

    public boolean isReady() {
        return mIsReady;
    }

    public boolean checkStopped() {
        if(mStopped) {
            mStopped = false;
            return true;
        }
        return false;
    }

    public void addListener(@NonNull TorrentListener listener) {
        mListener.add(listener);
    }

    public void removeListener(@NonNull TorrentListener listener) {
        mListener.remove(listener);
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

    public Torrent getCurrentTorrent() {
        return mCurrentTorrent;
    }

    public String getCurrentTorrentUrl() {
        return mTorrentStream.getCurrentTorrentUrl();
    }

    @Override
    public void onStreamPrepared(Torrent torrent) {
        mCurrentTorrent = torrent;

        for(TorrentListener listener : mListener) {
            listener.onStreamPrepared(torrent);
        }
    }

    @Override
    public void onStreamStarted(Torrent torrent) {
        for(TorrentListener listener : mListener) {
            listener.onStreamStarted(torrent);
        }
    }

    @Override
    public void onStreamError(Torrent torrent, Exception e) {
        for(TorrentListener listener : mListener) {
            listener.onStreamError(torrent, e);
        }
    }

    @Override
    public void onStreamReady(Torrent torrent) {
        mCurrentTorrent = torrent;
        mIsReady = true;

        for(TorrentListener listener : mListener) {
            listener.onStreamReady(torrent);
        }
    }

    @Override
    public void onStreamProgress(Torrent torrent, StreamStatus streamStatus) {
        for(TorrentListener listener : mListener) {
            if (null != listener) {
                listener.onStreamProgress(torrent, streamStatus);
            }
        }

        {
            mStreamStatus = streamStatus;

            NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notifManager.notify(NOTIFICATION_ID, addNotificationStartService());
        }
    }

    @Override
    public void onStreamStopped() {
        for(TorrentListener listener : mListener) {
            if (listener != null) {
                listener.onStreamStopped();
            }
        }
    }

    private class StartDownloading extends TimerTask {
        @Override
        public void run()
        {
            if (mDownloadTorrents.size() > 0 && isDownloading() == false)
            {
                Media.Torrent torrent = mDownloadTorrents.iterator().next();
                downloadTorrent(torrent.url, torrent.hash);
            }
        }
    };
}
