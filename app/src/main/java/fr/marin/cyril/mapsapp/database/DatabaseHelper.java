package fr.marin.cyril.mapsapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collection;

import fr.marin.cyril.mapsapp.R;
import fr.marin.cyril.mapsapp.kml.model.Coordinates;
import fr.marin.cyril.mapsapp.kml.model.Placemark;
import fr.marin.cyril.mapsapp.kml.parser.KmlParser;
import fr.marin.cyril.mapsapp.tools.Area;
import fr.marin.cyril.mapsapp.tools.Utils;

/**
 * Created by cscm6014 on 30/03/2016.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private final Context context;

    public DatabaseHelper(Context context) {
        super(context, DatabaseContract.DATABASE_NAME, null, DatabaseContract.DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create KML table
        db.execSQL(DatabaseContract.KmlHashEntry.CREATE_TABLE);
        // Create KML indexes
        db.execSQL(DatabaseContract.KmlHashEntry.CREATE_INDEX_KEY);

        // Create Markers table
        db.execSQL(DatabaseContract.MarkerEntry.CREATE_TABLE);
        // Create Markers indexes
        db.execSQL(DatabaseContract.MarkerEntry.CREATE_INDEX_LAT_LNG);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            db.execSQL(DatabaseContract.KmlHashEntry.DROP_TABLE);
            db.execSQL(DatabaseContract.MarkerEntry.DROP_TABLE);
            onCreate(db);
        }
    }

    public void initDataIfNeeded() {
        // Obtention des fichiers de resource
        TypedArray ta = context.getResources().obtainTypedArray(R.array.kml_array);
        for (int i = 0; i < ta.length(); ++i) {
            int id = ta.getResourceId(i, -1);
            String key = ta.getString(i);
            String hash = Utils.getSHA1FromResource(context, id);

            if (!hash.equals(this.findKmlHash(key))) {
                Log.i("KML FILES", String.format("insertion du fichier : %s (%s)", key, hash));
                this.insertAllPlacemark(new KmlParser(context).parse(id));
                this.insertKmlHash(key, hash);
            }
        }
        ta.recycle();
    }

    public void insertPlacemark(Placemark marker) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {

            ContentValues values = new ContentValues();
            values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_TITLE, marker.getTitle());
            values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_URL, marker.getUrl());
            values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_LATITUDE, marker.getCoordinates().getLatLng().latitude);
            values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_LONGITUDE, marker.getCoordinates().getLatLng().longitude);
            values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_ALTITUDE, marker.getCoordinates().getElevation());

            db.insert(DatabaseContract.MarkerEntry.TABLE_NAME, null, values);
        }
    }

    public void insertAllPlacemark(Collection<Placemark> markers) {
        for (Placemark m : markers) insertPlacemark(m);
    }

    public Collection<Placemark> findPlacemarkInArea(Double top, Double left, Double right, Double bottom) {
        try (SQLiteDatabase db = this.getReadableDatabase()) {
            Collection<Placemark> markers = new ArrayList<>();

            Boolean distinct = true;
            String[] columns = new String[]{
                    DatabaseContract.MarkerEntry.COLUMN_NAME_TITLE,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_URL,
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

            try (Cursor c = db.query(distinct, DatabaseContract.MarkerEntry.TABLE_NAME, columns, select, args,
                    null, null, DatabaseContract.MarkerEntry.COLUMN_NAME_ALTITUDE + " DESC", "25")) {

                c.moveToFirst();
                while (!c.isAfterLast()) {
                    Placemark m = new Placemark();
                    m.setTitle(c.getString(0));
                    m.setUrl(c.getString(1));

                    LatLng latLng = new LatLng(c.getDouble(2), c.getDouble(3));
                    Coordinates coordinates = new Coordinates(latLng);
                    coordinates.setElevation(c.getDouble(4));

                    m.setCoordinates(coordinates);

                    markers.add(m);

                    c.moveToNext();
                }

                return markers;
            }
        }
    }

    public Collection<Placemark> findPlacemarkInArea(Area area) {
        return findPlacemarkInArea(area.getTop(), area.getLeft(), area.getRight(), area.getBottom());
    }

    public Placemark findPlacemarkByLatLng(LatLng latLng) {
        try (SQLiteDatabase db = this.getReadableDatabase()) {

            Boolean distinct = true;
            String[] columns = new String[]{
                    DatabaseContract.MarkerEntry.COLUMN_NAME_TITLE,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_URL,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_LATITUDE,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_LONGITUDE,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_ALTITUDE
            };
            String select = DatabaseContract.MarkerEntry.COLUMN_NAME_LATITUDE + " = ? "
                    + " AND " + DatabaseContract.MarkerEntry.COLUMN_NAME_LONGITUDE + " = ? ";
            String[] args = new String[]{"" + latLng.latitude, "" + latLng.longitude};

            try (Cursor c = db.query(distinct, DatabaseContract.MarkerEntry.TABLE_NAME, columns, select, args,
                    null, null, null, null)) {

                c.moveToFirst();
                Placemark marker = new Placemark();
                marker.setTitle(c.getString(0));
                marker.setUrl(c.getString(1));
                Coordinates coordinates = new Coordinates();
                coordinates.setLatLng(new LatLng(c.getDouble(2), c.getDouble(3)));
                coordinates.setElevation(c.getDouble(4));
                marker.setCoordinates(coordinates);

                return marker;
            }
        }
    }

    public Long countPlacemark() {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseContract.MarkerEntry.TABLE_NAME, null)) {
                c.moveToFirst();
                return c.getLong(0);
            }
        }
    }

    public String findKmlHash(String key) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {

            Boolean distinct = true;
            String[] columns = new String[]{
                    DatabaseContract.KmlHashEntry.COLUMN_NAME_VALUE
            };
            String select = DatabaseContract.KmlHashEntry.COLUMN_NAME_KEY + " = ? ";
            String[] args = new String[]{key};

            try (Cursor c = db.query(distinct, DatabaseContract.KmlHashEntry.TABLE_NAME, columns, select, args, null, null, null, null)) {
                if (c.getCount() == 0)
                    return null;
                else {
                    c.moveToFirst();
                    return c.getString(0);
                }
            }
        }
    }

    public void insertKmlHash(String key, String value) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {

            ContentValues values = new ContentValues();
            values.put(DatabaseContract.KmlHashEntry.COLUMN_NAME_KEY, key);
            values.put(DatabaseContract.KmlHashEntry.COLUMN_NAME_VALUE, value);

            db.insert(DatabaseContract.KmlHashEntry.TABLE_NAME, null, values);
        }
    }
}
