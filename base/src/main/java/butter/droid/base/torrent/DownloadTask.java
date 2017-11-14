package butter.droid.base.torrent;


import com.github.bkkite.torrentstream.StreamStatus;
import com.github.bkkite.torrentstream.Torrent;
import com.github.bkkite.torrentstream.TorrentStream;
import com.github.bkkite.torrentstream.listeners.TorrentListener;

/**
 * Created by bkkite on 25/05/17.
 */

public class DownloadTask implements TorrentListener {

    private TorrentStream mTorrentStream;
    private Torrent mCurrentTorrent;
    private StreamStatus mStreamStatus;

    @Override
    public void onStreamPrepared(Torrent torrent) {

    }

    @Override
    public void onStreamStarted(Torrent torrent) {

    }

    @Override
    public void onStreamError(Torrent torrent, Exception e) {

    }

    @Override
    public void onStreamReady(Torrent torrent) {

    }

    @Override
    public void onStreamProgress(Torrent torrent, StreamStatus status) {

    }

    @Override
    public void onStreamStopped() {

    }
}
