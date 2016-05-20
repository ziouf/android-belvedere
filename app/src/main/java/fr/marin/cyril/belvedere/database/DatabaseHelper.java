package fr.marin.cyril.belvedere.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collection;

import fr.marin.cyril.belvedere.model.Area;
import fr.marin.cyril.belvedere.model.Placemark;

/**
 * Created by Cyril on 30/03/2016.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static DatabaseHelper databaseHelper = null;

    public DatabaseHelper(Context context) {
        super(context, DatabaseContract.DATABASE_NAME, null, DatabaseContract.DATABASE_VERSION);
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        return databaseHelper == null ? new DatabaseHelper(context.getApplicationContext()) : databaseHelper;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create KML Hash table + index
        db.execSQL(DatabaseContract.KmlHashEntry.CREATE_TABLE);
        db.execSQL(DatabaseContract.KmlHashEntry.CREATE_INDEX);

        // Create Markers table + index
        db.execSQL(DatabaseContract.MarkerEntry.CREATE_TABLE);
        db.execSQL(DatabaseContract.MarkerEntry.CREATE_INDEX);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DatabaseContract.KmlHashEntry.DROP_INDEX);
        db.execSQL(DatabaseContract.KmlHashEntry.DROP_TABLE);

        db.execSQL(DatabaseContract.MarkerEntry.DROP_INDEX);
        db.execSQL(DatabaseContract.MarkerEntry.DROP_TABLE);

        onCreate(db);
    }

    public void insertOrUpdatePlacemark(Placemark newPlacemark) {
        final Placemark oldPlacemark = this.findPlacemarkByLatLng(newPlacemark.getCoordinates().getLatLng());

        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_TITLE, newPlacemark.getTitle());
            values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_URL, newPlacemark.getWiki_uri());
            values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_COMMENT, newPlacemark.getComment());
            values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_THUMBNAIL_URL, newPlacemark.getThumbnail_uri());
            values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_THUMBNAIL, newPlacemark.getThumbnailArray());
            values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_ALTITUDE, newPlacemark.getCoordinates().getElevation());

            if (oldPlacemark == null) {
                values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_LATITUDE, newPlacemark.getCoordinates().getLatLng().latitude);
                values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_LONGITUDE, newPlacemark.getCoordinates().getLatLng().longitude);

                db.insertOrThrow(DatabaseContract.MarkerEntry.TABLE_NAME, null, values);
            } else {
                String where = DatabaseContract.MarkerEntry.COLUMN_NAME_LATITUDE + " = ?"
                        + " AND " + DatabaseContract.MarkerEntry.COLUMN_NAME_LONGITUDE + " = ?";
                String[] whereArgs = new String[]{
                        "" + newPlacemark.getCoordinates().getLatLng().latitude,
                        "" + newPlacemark.getCoordinates().getLatLng().longitude
                };

                db.update(DatabaseContract.MarkerEntry.TABLE_NAME, values, where, whereArgs);
            }
        } catch (SQLiteConstraintException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void insertAllPlacemark(Collection<Placemark> markers) {
        for (Placemark m : markers) insertOrUpdatePlacemark(m);
    }

    public Collection<Placemark> findAllPlacemarks() {
        try (SQLiteDatabase db = this.getReadableDatabase()) {
            Collection<Placemark> placemarks = new ArrayList<>();

            String[] columns = new String[]{
                    DatabaseContract.MarkerEntry.COLUMN_NAME_TITLE,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_URL,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_COMMENT,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_THUMBNAIL_URL,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_THUMBNAIL,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_LATITUDE,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_LONGITUDE,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_ALTITUDE
            };

            try (Cursor c = db.query(DatabaseContract.MarkerEntry.TABLE_NAME, columns, null, null, null, null, null)) {
                if (c.getCount() == 0) return placemarks;
                if (c.moveToFirst()) {
                    while (!c.isAfterLast()) {
                        placemarks.add(getPlacemarkFromCursor(c));
                        c.moveToNext();
                    }
                }
            }
            return placemarks;
        }
    }

    public Collection<Placemark> findPlacemarkInArea(Double top, Double left, Double right, Double bottom) {
        try (SQLiteDatabase db = this.getReadableDatabase()) {
            Collection<Placemark> markers = new ArrayList<>();

            String[] columns = new String[]{
                    DatabaseContract.MarkerEntry.COLUMN_NAME_TITLE,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_URL,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_COMMENT,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_THUMBNAIL_URL,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_THUMBNAIL,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_LATITUDE,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_LONGITUDE,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_ALTITUDE
            };
            String select = DatabaseContract.MarkerEntry.COLUMN_NAME_LATITUDE + " < ?"
                    + " AND " + DatabaseContract.MarkerEntry.COLUMN_NAME_LATITUDE + " > ?"
                    + " AND " + DatabaseContract.MarkerEntry.COLUMN_NAME_LONGITUDE + " < ?"
                    + " AND " + DatabaseContract.MarkerEntry.COLUMN_NAME_LONGITUDE + " > ?";
            String[] args = new String[]{top.toString(), bottom.toString(),
                    right.toString(), left.toString()};

            try (Cursor c = db.query(DatabaseContract.MarkerEntry.TABLE_NAME, columns, select, args,
                    null, null, DatabaseContract.MarkerEntry.COLUMN_NAME_ALTITUDE + " DESC", "25")) {

                if (c.getCount() == 0) return markers;
                if (c.moveToFirst()) {
                    while (!c.isAfterLast()) {
                        markers.add(getPlacemarkFromCursor(c));
                        c.moveToNext();
                    }
                }
                return markers;
            }
        }
    }

    public Collection<Placemark> findPlacemarkInArea(Area area) {
        if (area == null) return null;
        return findPlacemarkInArea(area.getTop(), area.getLeft(), area.getRight(), area.getBottom());
    }

    public Placemark findPlacemarkByLatLng(LatLng latLng) {
        try (SQLiteDatabase db = this.getReadableDatabase()) {

            String[] columns = new String[]{
                    DatabaseContract.MarkerEntry.COLUMN_NAME_TITLE,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_URL,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_COMMENT,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_THUMBNAIL_URL,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_THUMBNAIL,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_LATITUDE,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_LONGITUDE,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_ALTITUDE
            };
            String select = DatabaseContract.MarkerEntry.COLUMN_NAME_LATITUDE + " = ? "
                    + " AND " + DatabaseContract.MarkerEntry.COLUMN_NAME_LONGITUDE + " = ? ";
            String[] args = new String[]{"" + latLng.latitude, "" + latLng.longitude};

            try (Cursor c = db.query(DatabaseContract.MarkerEntry.TABLE_NAME, columns, select, args,
                    null, null, null, null)) {

                if (c.getCount() == 0) return null;
                if (c.moveToFirst()) {
                    return getPlacemarkFromCursor(c);
                }
                return null;
            }
        }
    }

    public Long countPlacemark() {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseContract.MarkerEntry.TABLE_NAME, null)) {
                if (c.getCount() > 0) {
                    c.moveToFirst();
                    return c.getLong(0);
                }
                return 0L;
            }
        }
    }

    private Placemark getPlacemarkFromCursor(Cursor c) {
        String title = c.getString(0);
        String wiki_uri = c.getString(1);
        String comment = c.getString(2);
        String thumbnail_url = c.getString(3);
        byte[] thumbnail = c.getBlob(4);
        double lat = c.getDouble(5);
        double lng = c.getDouble(6);
        double ele = c.getDouble(7);

        return new Placemark(title, comment, lat, lng, ele, wiki_uri, thumbnail_url, thumbnail);
    }

    public String findSourceFileHash(String key) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            String[] columns = new String[]{
                    DatabaseContract.KmlHashEntry.COLUMN_NAME_VALUE
            };
            String select = DatabaseContract.KmlHashEntry.COLUMN_NAME_KEY + " = ? ";
            String[] args = new String[]{key};

            try (Cursor c = db.query(DatabaseContract.KmlHashEntry.TABLE_NAME, columns,
                    select, args, null, null, null, null)) {
                if (c.getCount() == 0)
                    return null;
                else {
                    c.moveToFirst();
                    return c.getString(0);
                }
            }
        }
    }

    public void insertOrUpdateKmlHash(String key, String value) {
        final String oldValue = this.findSourceFileHash(key);

        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(DatabaseContract.KmlHashEntry.COLUMN_NAME_VALUE, value);

            if (oldValue == null) {
                values.put(DatabaseContract.KmlHashEntry.COLUMN_NAME_KEY, key);

                db.insertOrThrow(DatabaseContract.KmlHashEntry.TABLE_NAME, null, values);
            } else {
                String where = DatabaseContract.KmlHashEntry.COLUMN_NAME_KEY + " = ?";
                String[] whereArgs = new String[] { key };

                db.update(DatabaseContract.KmlHashEntry.TABLE_NAME, values, where, whereArgs);
            }
        }
    }

}
