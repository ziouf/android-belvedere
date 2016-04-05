package fr.marin.cyril.mapsapp.database;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * Created by cscm6014 on 30/03/2016.
 */
final class DatabaseContract {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "database.db";

    public static final String TEXT_TYPE = " TEXT";
    public static final String NUMBER_TYPE = " REAL";
    public static final String UNIQUE = " UNIQUE";
    public static final String COMMA_SEP = ",";

    public DatabaseContract() {
    }

    public static abstract class MarkerEntry implements BaseColumns {
        public static final String TABLE_NAME = "marker";

        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_URL = "url";
        public static final String COLUMN_NAME_LATITUDE = "latitude";
        public static final String COLUMN_NAME_LONGITUDE = "longitude";
        public static final String COLUMN_NAME_ALTITUDE = "altitude";

        public static final String CREATE_TABLE =
                "CREATE TABLE " + MarkerEntry.TABLE_NAME + " (" +
                        MarkerEntry._ID + " INTEGER PRIMARY KEY," +
                        MarkerEntry.COLUMN_NAME_TITLE + TEXT_TYPE + UNIQUE + COMMA_SEP +
                        MarkerEntry.COLUMN_NAME_URL + TEXT_TYPE + UNIQUE + COMMA_SEP +
                        MarkerEntry.COLUMN_NAME_LATITUDE + NUMBER_TYPE + COMMA_SEP +
                        MarkerEntry.COLUMN_NAME_LONGITUDE + NUMBER_TYPE + COMMA_SEP +
                        MarkerEntry.COLUMN_NAME_ALTITUDE + NUMBER_TYPE +
                        " )";

        public static final String DROP_TABLE =
                "DROP TABLE IF EXISTS " + MarkerEntry.TABLE_NAME;
    }

}
