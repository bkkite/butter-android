package butter.droid.base.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import butter.droid.base.database.tables.Downloads;
import butter.droid.base.database.tables.Tables;

public class DBProvider extends ContentProvider {

    public static final String CONTENT_AUTHORITY = "droid.pc.butter";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    private static final int DOWNLOADS = 200;
    private static final int DOWNLOADS_ID = 201;

    private DBHelper mHelper;
    private UriMatcher mUriMatcher = buildUriMatcher();

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

        matcher.addURI(CONTENT_AUTHORITY, Tables.DOWNLOADS, DOWNLOADS);
        matcher.addURI(CONTENT_AUTHORITY, Tables.DOWNLOADS + "/#", DOWNLOADS_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mHelper = new DBHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        switch (mUriMatcher.match(uri)) {
            case DOWNLOADS:
                return Downloads.CONTENT_TYPE;
            case DOWNLOADS_ID:
                return Downloads.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor cursor = null;

        switch (mUriMatcher.match(uri)) {
            case DOWNLOADS:
                cursor = db.query(Tables.DOWNLOADS, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case DOWNLOADS_ID:
                cursor = db.query(Tables.DOWNLOADS, projection, selectionDownloadWithId(selection, uri.getLastPathSegment()), selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new UnsupportedOperationException("Wrong uri: " + uri);
        }

        if (cursor != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }

        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mHelper.getWritableDatabase();
        Uri insertUri = null;

        switch (mUriMatcher.match(uri)) {
            case DOWNLOADS:
                long downloadId = db.insert(Tables.DOWNLOADS, null, values);
                insertUri = Downloads.buildUri(Long.toString(downloadId));
                break;
            default:
                throw new UnsupportedOperationException("Wrong uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return insertUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mHelper.getWritableDatabase();
        int itemDeletedCount = 0;

        switch (mUriMatcher.match(uri)) {
            case DOWNLOADS:
                itemDeletedCount = db.delete(Tables.DOWNLOADS, selection, selectionArgs);
                break;
            case DOWNLOADS_ID:
                itemDeletedCount = db.delete(Tables.DOWNLOADS, selectionDownloadWithId(selection, uri.getLastPathSegment()), selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Wrong uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return itemDeletedCount;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mHelper.getWritableDatabase();
        int itemUpdatedCount = 0;

        switch (mUriMatcher.match(uri)) {
            case DOWNLOADS:
                itemUpdatedCount = db.update(Tables.DOWNLOADS, values, selection, selectionArgs);
                break;
            case DOWNLOADS_ID:
                itemUpdatedCount = db.update(Tables.DOWNLOADS, values, selectionDownloadWithId(selection, uri.getLastPathSegment()), selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Wrong uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return itemUpdatedCount;
    }

    private String selectionDownloadWithId(String selection, String id) {
        if (TextUtils.isEmpty(selection)) {
            selection = Downloads._VIDEOID + "=" + id;
        } else {
            selection += " AND " + BaseColumns._ID + "=" + id;
        }

        return selection;
    }

}