package butter.droid.fragments;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.text.Layout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Switch;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import butter.droid.R;
import butter.droid.activities.TrailerPlayerActivity;
import butter.droid.activities.VideoPlayerActivity;
import butter.droid.base.content.preferences.DefaultPlayer;
import butter.droid.base.content.preferences.DefaultQuality;
import butter.droid.base.content.preferences.Prefs;
import butter.droid.base.database.tables.Downloads;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.media.models.Movie;
import butter.droid.base.providers.subs.SubsProvider;
import butter.droid.base.torrent.DownloadTorrentService;
import butter.droid.base.torrent.Magnet;
import butter.droid.base.torrent.StreamInfo;
import butter.droid.base.torrent.TorrentHealth;
import butter.droid.base.utils.FileUtils;
import butter.droid.base.utils.LocaleUtils;
import butter.droid.base.utils.PixelUtils;
import butter.droid.base.utils.PrefUtils;
import butter.droid.base.utils.SortUtils;
import butter.droid.base.utils.StringUtils;
import butter.droid.base.utils.ThreadUtils;
import butter.droid.base.utils.VersionUtils;
import butter.droid.base.youtube.YouTubeData;
import butter.droid.fragments.base.BaseDetailFragment;
import butter.droid.fragments.dialog.ChooserOptionDialogFragment;
import butter.droid.fragments.dialog.OptionDeleteMovieDialogFragment;
import butter.droid.fragments.dialog.SynopsisDialogFragment;
import butter.droid.widget.OptionSelector;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MovieDetailFragment extends BaseDetailFragment {

    private static Movie sMovie;
    private String mSelectedSubtitleLanguage, mSelectedQuality;
    private Boolean mAttached = false, mDownloadSync = false;
    private Magnet mMagnet;

    private DownloadTorrentService mService;

    @Bind(R.id.play_button)
    ImageButton mPlayButton;
    @Bind(R.id.title)
    TextView mTitle;
    @Bind(R.id.health)
    ImageView mHealth;
    @Bind(R.id.meta)
    TextView mMeta;
    @Bind(R.id.synopsis)
    TextView mSynopsis;
    @Bind(R.id.read_more)
    Button mReadMore;
    @Bind(R.id.watch_trailer)
    Button mWatchTrailer;
    @Bind(R.id.magnet)
    ImageButton mOpenMagnet;
    @Bind(R.id.offline)
    Switch mOffline;
    @Bind(R.id.rating)
    RatingBar mRating;
    @Bind(R.id.subtitles)
    OptionSelector mSubtitles;
    @Bind(R.id.quality)
    OptionSelector mQuality;
    @Nullable
    @Bind(R.id.cover_image)
    ImageView mCoverImage;

    public static MovieDetailFragment newInstance(Movie movie) {
        sMovie = movie;
        return new MovieDetailFragment();
    }

    @Override
    public void onCreate(
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRoot = inflater.inflate(R.layout.fragment_moviedetail, container, false);
        if (VersionUtils.isJellyBean() && container != null) {
            mRoot.setMinimumHeight(container.getMinimumHeight());
        }
        ButterKnife.bind(this, mRoot);

        if(sMovie != null) {

            if (!VersionUtils.isJellyBean()) {
                mPlayButton.setBackgroundDrawable(PixelUtils.changeDrawableColor(mPlayButton.getContext(), R.drawable.play_button_circle, sMovie.color));
            } else {
                mPlayButton.setBackground(PixelUtils.changeDrawableColor(mPlayButton.getContext(), R.drawable.play_button_circle, sMovie.color));
            }

            mTitle.setText(sMovie.title);
            if (!sMovie.rating.equals("-1")) {
                Double rating = Double.parseDouble(sMovie.rating);
                mRating.setProgress(rating.intValue());
                mRating.setContentDescription("Rating: " + rating.intValue() + " out of 10");
                mRating.setVisibility(View.VISIBLE);
            } else {
                mRating.setVisibility(View.GONE);
            }

            String metaDataStr = sMovie.year;
            if (!TextUtils.isEmpty(sMovie.runtime)) {
                metaDataStr += " • ";
                metaDataStr += sMovie.runtime + " " + getString(R.string.minutes);
            }

            if (!TextUtils.isEmpty(sMovie.genre)) {
                metaDataStr += " • ";
                metaDataStr += sMovie.genre;
            }

            mMeta.setText(metaDataStr);

            if (!TextUtils.isEmpty(sMovie.synopsis)) {
                mSynopsis.setText(sMovie.synopsis);
                mSynopsis.post(new Runnable() {
                    @Override
                    public void run() {
                        boolean ellipsized = false;
                        Layout layout = mSynopsis.getLayout();
                        if (layout == null) return;
                        int lines = layout.getLineCount();
                        if (lines > 0) {
                            int ellipsisCount = layout.getEllipsisCount(lines - 1);
                            if (ellipsisCount > 0) {
                                ellipsized = true;
                            }
                        }
                        mReadMore.setVisibility(ellipsized ? View.VISIBLE : View.GONE);
                    }
                });
            } else {
                mSynopsis.setClickable(false);
                mReadMore.setVisibility(View.GONE);
            }

            mWatchTrailer.setVisibility(sMovie.trailer == null || sMovie.trailer.isEmpty() ? View.GONE : View.VISIBLE);

            mSubtitles.setFragmentManager(getFragmentManager());
            mQuality.setFragmentManager(getFragmentManager());
            mSubtitles.setTitle(R.string.subtitles);
            mQuality.setTitle(R.string.quality);

            mSubtitles.setText(R.string.loading_subs);
            mSubtitles.setClickable(false);

            if (sMovie.getSubsProvider() != null) {
                sMovie.getSubsProvider().getList(sMovie, new SubsProvider.Callback() {
                    @Override
                    public void onSuccess(Map<String, String> subtitles) {
                        if (!mAttached) return;

                        if (subtitles == null) {
                            ThreadUtils.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mSubtitles.setText(R.string.no_subs_available);
                                }
                            });
                            return;
                        }

                        sMovie.subtitles = subtitles;

                        String[] languages = subtitles.keySet().toArray(new String[subtitles.size()]);
                        Arrays.sort(languages);
                        final String[] adapterLanguages = new String[languages.length + 1];
                        adapterLanguages[0] = "no-subs";
                        System.arraycopy(languages, 0, adapterLanguages, 1, languages.length);

                        String[] readableNames = new String[adapterLanguages.length];
                        for (int i = 0; i < readableNames.length; i++) {
                            String language = adapterLanguages[i];
                            if (language.equals("no-subs")) {
                                readableNames[i] = getString(R.string.no_subs);
                            } else {
                                Locale locale = LocaleUtils.toLocale(language);
                                readableNames[i] = locale.getDisplayName(locale);
                            }
                        }

                        mSubtitles.setListener(new OptionSelector.SelectorListener() {
                            @Override
                            public void onSelectionChanged(int position, String value) {
                                onSubtitleLanguageSelected(adapterLanguages[position]);
                            }
                        });
                        mSubtitles.setData(readableNames);
                        ThreadUtils.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mSubtitles.setClickable(true);
                            }
                        });

                        String defaultSubtitle = PrefUtils.get(mSubtitles.getContext(), Prefs.SUBTITLE_DEFAULT, null);
                        if (subtitles.containsKey(defaultSubtitle)) {
                            onSubtitleLanguageSelected(defaultSubtitle);
                            mSubtitles.setDefault(Arrays.asList(adapterLanguages).indexOf(defaultSubtitle));
                        } else {
                            onSubtitleLanguageSelected("no-subs");
                            mSubtitles.setDefault(Arrays.asList(adapterLanguages).indexOf("no-subs"));
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        mSubtitles.setData(new String[0]);
                        mSubtitles.setClickable(true);
                    }
                });
            } else {
                mSubtitles.setClickable(false);
                mSubtitles.setText(R.string.no_subs_available);
            }

            if (sMovie.torrents.size() > 0) {
                final String[] qualities = sMovie.torrents.keySet().toArray(new String[sMovie.torrents.size()]);
                SortUtils.sortQualities(qualities);

                mQuality.setData(qualities);
                mQuality.setListener(new OptionSelector.SelectorListener() {
                    @Override
                    public void onSelectionChanged(int position, String value) {
                        mSelectedQuality = value;
                        renderHealth();
                        updateMagnet();
                    }
                });

                String quality = DefaultQuality.get(mActivity, Arrays.asList(qualities));
                int qualityIndex = Arrays.asList(qualities).indexOf(quality);
                mSelectedQuality = quality;
                mQuality.setText(mSelectedQuality);
                mQuality.setDefault(qualityIndex);

                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mOffline.setChecked(Downloads.isMovieInDataBase(getContext(), sMovie));
                    }
                });

                mOffline.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            setOfflineContent(isChecked);
                    }
                });

                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        mDownloadSync = prv_mDownloadSync();
                        if (mDownloadSync)
                            setHasOptionsMenu(true);
                    }
                });

                renderHealth();
                updateMagnet();
            }

            if (mCoverImage != null) {
                Picasso.with(mCoverImage.getContext()).load(sMovie.image).into(mCoverImage);
            }
        }

        return mRoot;
    }

    private boolean prv_mDownloadSync()
    {
        if (Downloads.isTorrentMovieInDataBaseSync(getContext(), sMovie, sMovie.getHash(mSelectedQuality)) == true)
            return true;

        final ArrayList<String> video_files = FileUtils.getMagnetDownloadedVideoFiles(mActivity, sMovie.torrents.get(mSelectedQuality).hash);

        if (video_files.isEmpty() == false)
            return true;

        return false;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mAttached = true;
        if (activity instanceof FragmentListener) {
            mCallback = (FragmentListener) activity;
            DownloadTorrentService.bindHere(getContext(), mServiceConnection);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mAttached = false;

        if (mService != null) {
            getContext().unbindService(mServiceConnection);
            mService = null;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){

        if (mDownloadSync)
            inflater.inflate(R.menu.fragment_movie_detail_downloaded, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDownloadSync)
        {
            switch (item.getItemId())
            {
                case R.id.action_not_watched: return notWatchedMovie();
                case R.id.action_watched: return watchedMovie();
                default: return super.onOptionsItemSelected(item);
            }
        }
        else
            return false;
    }

    private void renderHealth() {
        if(mHealth.getVisibility() == View.GONE) {
            mHealth.setVisibility(View.VISIBLE);
        }

        TorrentHealth health = TorrentHealth.calculate(sMovie.torrents.get(mSelectedQuality).seeds, sMovie.torrents.get(mSelectedQuality).peers);
        mHealth.setImageResource(health.getImageResource());
    }

    private void updateMagnet() {
        if(mMagnet == null) {
            mMagnet = new Magnet(mActivity, sMovie.torrents.get(mSelectedQuality).url);
        }
        mMagnet.setUrl(sMovie.torrents.get(mSelectedQuality).url);

        if(!mMagnet.canOpen()) {
            mOpenMagnet.setVisibility(View.GONE);
        } else {
            mOpenMagnet.setVisibility(View.VISIBLE);
        }
    }

    private boolean downloadMagnet()
    {
        try {
            if (Downloads.isMovieInDataBase(getContext(), sMovie) == false)
                Downloads.insertMovie(getContext(), sMovie, mSelectedQuality);
        }
        catch (UnsupportedOperationException e){
            e.printStackTrace();
        }
        finally {
            mMagnet.open(mActivity);
            return true;
        }
    }

    private boolean setOfflineContent(boolean offline)
    {
        try {
            if (offline)
            {
                if (Downloads.isMovieInDataBase(getContext(), sMovie) == false)
                    Downloads.insertMovie(getContext(), sMovie, mSelectedQuality);
                else
                    Downloads.setMovieOffline(getContext(), sMovie);
            }
            else
            {
                if (Downloads.isMovieInDataBase(getContext(), sMovie))
                    return deleteMovie();
            }

            return true;
        }
        catch (UnsupportedOperationException e){
            e.printStackTrace();
            return false;
        }
    }

    private boolean notWatchedMovie(){
        int numUpdated = Downloads.setMovieNotWatched(getContext(), sMovie);

        if (numUpdated > 0)
            return true;
        else
            return false;
    }

    private boolean watchedMovie(){
        int numUpdated = Downloads.setMovieWatched(getContext(), sMovie);

        if (numUpdated > 0)
            return true;
        else
            return false;
    }

    private boolean deleteMovie()
    {
        OptionDeleteMovieDialogFragment.show(getFragmentManager(), new OptionDeleteMovieDialogFragment.Listener() {
            @Override
            public void onSelectionPositive(boolean delete_files) {

                int numDeleted = Downloads.deleteMovie(getContext(), sMovie);

                if (numDeleted > 0) {
                    getFragmentManager().popBackStack();
                }

                if (mService != null) {
                    Media.Torrent torrent = sMovie.getTorrent(mSelectedQuality);
                    if (torrent != null)
                        mService.delTorrent(torrent);
                }

                if (delete_files)
                    FileUtils.deleteMagnetDownloadedPathVideoFiles(getContext(), sMovie.getHash(mSelectedQuality));
            }

            @Override
            public void onSelectionNegative() {}
        });

        return true;
    }

    @OnClick(R.id.read_more)
    public void openReadMore(View v) {
        if (getFragmentManager().findFragmentByTag("overlay_fragment") != null)
            return;
        SynopsisDialogFragment synopsisDialogFragment = new SynopsisDialogFragment();
        Bundle b = new Bundle();
        b.putString("text", sMovie.synopsis);
        synopsisDialogFragment.setArguments(b);
        synopsisDialogFragment.show(getFragmentManager(), "overlay_fragment");
    }

    @OnClick(R.id.watch_trailer)
    public void openTrailer(View v) {
        if (!YouTubeData.isYouTubeUrl(sMovie.trailer)) {
            VideoPlayerActivity.startActivity(mActivity, new StreamInfo(sMovie, null, null, null, null, sMovie.trailer));
        } else {
            TrailerPlayerActivity.startActivity(mActivity, sMovie.trailer, sMovie);
        }
    }

    @OnClick(R.id.play_button)
    public void play() {
        if (mDownloadSync)
        {
            final ArrayList<String> video_files = FileUtils.getMagnetDownloadedVideoFiles(mActivity, sMovie.torrents.get(mSelectedQuality).hash);

            ChooserOptionDialogFragment.show(mActivity, getChildFragmentManager(), R.string.select_video, video_files, android.R.string.yes, android.R.string.no, new ChooserOptionDialogFragment.Listener() {
                @Override
                public void onItemSelected(int position) {
                    DefaultPlayer.startOffLine(sMovie, video_files.get(position));
                }

                @Override
                public void onSelectionNegative() {}
            });
        }
        else
        {
            String streamUrl = sMovie.torrents.get(mSelectedQuality).url;
            StreamInfo streamInfo = new StreamInfo(sMovie, streamUrl, mSelectedSubtitleLanguage, mSelectedQuality);
            mCallback.playStream(streamInfo);
        }
    }

    @OnClick(R.id.magnet)
    public void openMagnet() {
        downloadMagnet();
    }

    @OnClick(R.id.health)
    public void clickHealth() {
        int seeds = sMovie.torrents.get(mSelectedQuality).seeds;
        int peers = sMovie.torrents.get(mSelectedQuality).peers;
        TorrentHealth health = TorrentHealth.calculate(seeds, peers);

        final Snackbar snackbar = Snackbar.make(mRoot, getString(R.string.health_info, getString(health.getStringResource()), seeds, peers), Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.close, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackbar.dismiss();
            }
        });
        snackbar.show();
    }

    private void onSubtitleLanguageSelected(String language) {
        mSelectedSubtitleLanguage = language;
        if (!language.equals("no-subs")) {
            final Locale locale = LocaleUtils.toLocale(language);
            ThreadUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSubtitles.setText(StringUtils.uppercaseFirst(locale.getDisplayName(locale)));
                }
            });
        } else {
            ThreadUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSubtitles.setText(R.string.no_subs);
                }
            });
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

}
