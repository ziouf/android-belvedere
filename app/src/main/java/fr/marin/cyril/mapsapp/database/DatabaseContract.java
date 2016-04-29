package fr.marin.cyril.mapsapp.database;

import android.provider.BaseColumns;

/**
 * Created by cscm6014 on 30/03/2016.
 */
final class DatabaseContract {
    public static final int DATABASE_VERSION = 6;
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
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        COLUMN_NAME_TITLE + TEXT_TYPE + UNIQUE + COMMA_SEP +
                        COLUMN_NAME_URL + TEXT_TYPE + UNIQUE + COMMA_SEP +
                        COLUMN_NAME_LATITUDE + NUMBER_TYPE + COMMA_SEP +
                        COLUMN_NAME_LONGITUDE + NUMBER_TYPE + COMMA_SEP +
                        COLUMN_NAME_ALTITUDE + NUMBER_TYPE +
                        " )";

        public static final String DROP_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;

        public static final String CREATE_INDEX_LAT_LNG =
                "CREATE INDEX IF NOT EXISTS index_" + TABLE_NAME + "_lat_lng ON "
                        + TABLE_NAME + "(" + COLUMN_NAME_LATITUDE + COMMA_SEP + COLUMN_NAME_LONGITUDE + ")";

    }

    public static abstract class KmlHashEntry implements BaseColumns {
        public static final String TABLE_NAME = "kmlHash";

        public static final String COLUMN_NAME_KEY = "key";
        public static final String COLUMN_NAME_VALUE = "value";

        public static final String CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        COLUMN_NAME_KEY + TEXT_TYPE + UNIQUE + COMMA_SEP +
                        COLUMN_NAME_VALUE + TEXT_TYPE + UNIQUE + ")";

        public static final String DROP_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;

        public static final String CREATE_INDEX_KEY =
                "CREATE INDEX IF NOT EXISTS index_" + TABLE_NAME + "_key ON "
                        + TABLE_NAME + "(" + COLUMN_NAME_KEY + ")";

    }

}
