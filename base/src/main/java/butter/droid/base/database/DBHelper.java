package butter.droid.base.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import butter.droid.base.database.tables.Downloads;
import timber.log.Timber;

public class DBHelper extends SQLiteOpenHelper {

    private static final String NAME = "butter.db";
    private static final int CURRENT_VERSION = 2;

    public DBHelper(Context context) {
        super(context, NAME, null, CURRENT_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Downloads.createTable(db);
        Timber.d("onCreate version=" + db.getVersion());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Downloads.updateTable(db, oldVersion);
        Timber.d("onUpgrade version=" + db.getVersion());
    }
}