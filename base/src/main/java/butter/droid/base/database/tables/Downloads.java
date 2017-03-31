package butter.droid.base.database.tables;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.ArrayList;

import butter.droid.base.database.DBProvider;
import butter.droid.base.providers.media.MediaProvider;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.media.models.Movie;
import butter.droid.base.utils.FileUtils;

public class Downloads implements BaseColumns {

    private static final String _TYPE = "_type";
    public static final String _VIDEOID = "_videoid";
    private static final String _IMDB = "_imdb";
    private static final String _TITLE = "_title";
    private static final String _YEAR = "_year";
    private static final String _GENRE = "_genre";
    private static final String _RATING = "_rating";
    private static final String _POSTER_URL = "_poster_url";
    private static final String _HEADER_URL = "_header_url";
    private static final String _SYPNOPSIS = "_sypnopsis";
    private static final String _STATE = "_state";
    private static final String _SYNC = "_sync";
    private static final String _SIZE = "_size";
    private static final String _SEASON = "_season";
    private static final String _EPISODE = "_episode";
    private static final String _TORRENT_MAGNET = "_torrent_magnet";
    private static final String _TORRENT_QUALITY = "_torrent_quality";
    private static final String _TORRENT_HASH = "_torrent_hash";
    private static final String _DIRECTORY = "_directory";

    public static final String WATCHED = "watched";
    public static final String NOT_WATCHED = "NotWatched";

    private static final int NOT_SYNC = 0;
    private static final int SYNC = 1;

    private static final String NAME = Tables.DOWNLOADS;
    private static final Uri CONTENT_URI = DBProvider.BASE_CONTENT_URI.buildUpon().appendPath(NAME).build();

    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.butter." + NAME;
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.butter." + NAME;

    private static final String QUERY_CREATE = "CREATE TABLE " + NAME + " ("
            + _ID + " INTEGER PRIMARY KEY, "
            + _TYPE + " TEXT, "
            + _VIDEOID + " TEXT, "
            + _IMDB + " TEXT, "
            + _TITLE + " TEXT, "
            + _YEAR + " INTEGER, "
            + _GENRE + " TEXT, "
            + _RATING + " TEXT, "
            + _POSTER_URL + " TEXT, "
            + _HEADER_URL + " TEXT, "
            + _SYPNOPSIS + " TEXT, "
            + _STATE + " TEXT, "
            + _SIZE + " INTEGER, "
            + _SEASON + " INTEGER, "
            + _EPISODE + " INTEGER, "
            + _TORRENT_MAGNET + " TEXT, "
            + _TORRENT_QUALITY + " TEXT, "
            + _TORRENT_HASH + " TEXT, "
            + _DIRECTORY + " TEXT"
            + ")";

    private static final String VERSION_1_TO_2 = "ALTER TABLE " + NAME + " ADD COLUMN " + _SYNC + " INTEGER DEFAULT 1";

    public static void createTable(SQLiteDatabase db)
    {
        db.execSQL(Downloads.QUERY_CREATE);
        db.execSQL(Downloads.VERSION_1_TO_2);
    }

    public static void updateTable(SQLiteDatabase db, int oldVersion)
    {
        if (oldVersion < 2) {
            db.execSQL(Downloads.VERSION_1_TO_2);
        }
    }

    public static Uri getContentUri() {
        return CONTENT_URI;
    }

    public static Uri buildUri(final String id) {
        return CONTENT_URI.buildUpon().appendPath(id).build();
    }

    public static void getList(Context context, ArrayList<Media> currentList, final String state, MediaProvider mediaProvider) throws IllegalArgumentException
    {
        Cursor cursor;
        String[] projection = {_TYPE, _VIDEOID, _IMDB, _TITLE, _YEAR, _GENRE, _RATING, _POSTER_URL, _HEADER_URL, _SYPNOPSIS, _TORRENT_MAGNET, _TORRENT_QUALITY, _TORRENT_HASH, _DIRECTORY, _STATE};
        String selection = null;

        if (state != null) {
            selection = Downloads._STATE + "='" + state+"'";
        }

        cursor = context.getContentResolver().query(CONTENT_URI, projection, selection, null, null);

        cursorToList(currentList, cursor, mediaProvider);
    }

    public static void getMediaToSync(Context context, ArrayList<Media> mediaList) throws IllegalArgumentException
    {
        Cursor cursor;
        String[] projection = {_TYPE, _VIDEOID, _IMDB, _TITLE, _YEAR, _GENRE, _RATING, _POSTER_URL, _HEADER_URL, _SYPNOPSIS, _TORRENT_MAGNET, _TORRENT_QUALITY, _TORRENT_HASH, _DIRECTORY, _STATE};

        String selection = Downloads._SYNC + "=" + NOT_SYNC;

        cursor = context.getContentResolver().query(CONTENT_URI, projection, selection, null, null);

        cursorToList(mediaList, cursor, null);
    }

    public static boolean isInDataBase(Context context, final Movie info)
    {
        if (info.videoId != null) {
            String[] projection = {_VIDEOID};
            String selection = Downloads._VIDEOID + "='" + info.videoId+"'";
            Cursor cursor = context.getContentResolver().query(CONTENT_URI, projection, selection, null, null);

            boolean isInDB = (cursor.getCount() > 0 ? true : false);
            cursor.close();

            return isInDB;
        }
        else
            return false;
    }

    public static boolean isInDataBaseSync(Context context, final Movie info)
    {
        if (info.videoId != null) {
            String[] projection = {_VIDEOID};
            String selection1 = Downloads._VIDEOID + "='" + info.videoId+"'";
            String selection2 = Downloads._SYNC + "=" + SYNC;
            String selection = selection1 + " AND " + selection2;
            Cursor cursor = context.getContentResolver().query(CONTENT_URI, projection, selection, null, null);

            boolean isInDB = (cursor.getCount() > 0 ? true : false);
            cursor.close();

            return isInDB;
        }
        else
            return false;
    }

    public static Uri insertMovie(Context context, Movie info, String quality) {
        return context.getContentResolver().insert(CONTENT_URI, buildValuesMovie(context, info, NOT_WATCHED, quality));
    }

    public static int setMovieOffline(Context context, Movie info) {
        return context.getContentResolver().update(buildUri(info.videoId), buildSetSyncValueMovie(NOT_SYNC), null, null);
    }

    public static int setMovieWatched(Context context, Movie info) {
        return context.getContentResolver().update(buildUri(info.videoId), buildStateValueMovie(WATCHED), null, null);
    }

    public static int setMovieNotWatched(Context context, Movie info) {
        return context.getContentResolver().update(buildUri(info.videoId), buildStateValueMovie(NOT_WATCHED), null, null);
    }

    public static int setMovieSync(Context context, Movie info) {
        return context.getContentResolver().update(buildUri(info.videoId), buildSetSyncValueMovie(SYNC), null, null);
    }

    public static int deleteMovie(Context context, Movie info) {
        return context.getContentResolver().delete(buildUri(info.videoId), null, null);
    }

    private static boolean isInResults(ArrayList<Media> results, Cursor cursor)
    {
        String videoId = cursor.getString(cursor.getColumnIndexOrThrow(Downloads._VIDEOID));

        for (Media item : results) {

            if (item.videoId.equals(videoId)) return true;
        }
        return false;
    }

    private static void cursorToList(ArrayList<Media> currentList, Cursor cursor, MediaProvider mediaProvider) throws IllegalArgumentException
    {
        boolean isEmpty;

        isEmpty = (cursor.getCount() == 0);
        cursor.moveToFirst();

        while (!isEmpty)
        {
            if (!isInResults(currentList, cursor)) {
                Movie movie = new Movie(mediaProvider, null);

                movie.videoId = cursor.getString(cursor.getColumnIndexOrThrow(Downloads._VIDEOID));
                movie.imdbId = cursor.getString(cursor.getColumnIndexOrThrow(Downloads._IMDB));
                movie.title = cursor.getString(cursor.getColumnIndexOrThrow(Downloads._TITLE));
                movie.year = cursor.getString(cursor.getColumnIndexOrThrow(Downloads._YEAR));
                movie.rating = cursor.getString(cursor.getColumnIndexOrThrow(Downloads._RATING));
                movie.genre = cursor.getString(cursor.getColumnIndexOrThrow(Downloads._GENRE));
                movie.image = cursor.getString(cursor.getColumnIndexOrThrow(Downloads._POSTER_URL));
                movie.headerImage = cursor.getString(cursor.getColumnIndexOrThrow(Downloads._HEADER_URL));
                movie.trailer = "";
                movie.runtime = "";
                movie.synopsis = cursor.getString(cursor.getColumnIndexOrThrow(Downloads._SYPNOPSIS));
                movie.fullImage = cursor.getString(cursor.getColumnIndexOrThrow(Downloads._HEADER_URL));

                movie.torrents.clear();

                if (!cursor.getString(cursor.getColumnIndexOrThrow(Downloads._TORRENT_MAGNET)).isEmpty()) {
                    Media.Torrent torrent = new Media.Torrent();
                    torrent.url = cursor.getString(cursor.getColumnIndexOrThrow(Downloads._TORRENT_MAGNET));
                    torrent.peers = 0;
                    torrent.seeds = 0;
                    torrent.hash = cursor.getString(cursor.getColumnIndexOrThrow(Downloads._TORRENT_HASH));
                    torrent.isDownloaded = true;

                    movie.torrents.put(cursor.getString(cursor.getColumnIndexOrThrow(Downloads._TORRENT_QUALITY)), torrent);
                }

                currentList.add(movie);
            }

            isEmpty = !cursor.moveToNext();
        }

        cursor.close();
    }

    private static ContentValues buildValuesMovie(Context context, Movie info, String state, String quality) {

        ContentValues values = new ContentValues();

        values.put(_TYPE, info.type);
        values.put(_VIDEOID, info.videoId);
        values.put(_IMDB, info.imdbId);
        values.put(_TITLE, info.title);
        values.put(_YEAR, info.year);
        values.put(_GENRE, info.genre);
        values.put(_RATING, info.rating);
        values.put(_POSTER_URL, info.image);
        values.put(_HEADER_URL, info.headerImage);
        values.put(_SYPNOPSIS, info.synopsis);
        values.put(_STATE, state);
        values.put(_SYNC, NOT_SYNC);
        values.put(_SIZE, "");
        values.put(_SEASON, "");
        values.put(_EPISODE, "");

        {
            Media.Torrent torrent = info.torrents.get(quality);
            values.put(_TORRENT_MAGNET, torrent.url);
            values.put(_TORRENT_QUALITY, quality);
            values.put(_TORRENT_HASH, torrent.hash);
            values.put(_DIRECTORY, FileUtils.getMagnetDownloadedPathVideoFile(context, torrent.hash));
        }

        return values;
    }

    private static ContentValues buildStateValueMovie(String state) {

        ContentValues values = new ContentValues();

        values.put(_STATE, state);

        return values;
    }

    private static ContentValues buildSetSyncValueMovie(int Sync) {

        ContentValues values = new ContentValues();

        values.put(_SYNC, Sync);

        return values;
    }
}