package butter.droid.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by bkkite on 26/01/17.
 */

public class SyncOfflineContentService extends Service {

    private static SyncOfflineContentAdapter sSyncAdapter = null;
    private static final Object sSyncAdapterLock = new Object();

    @Override
    public void onCreate() {

        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new SyncOfflineContentAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }
}
