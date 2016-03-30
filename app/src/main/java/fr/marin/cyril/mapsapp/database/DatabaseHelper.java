package fr.marin.cyril.mapsapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collection;

import fr.marin.cyril.mapsapp.kml.model.Coordinates;
import fr.marin.cyril.mapsapp.kml.model.MapsMarker;
import fr.marin.cyril.mapsapp.tool.MapArea;

/**
 * Created by cscm6014 on 30/03/2016.
 */
public class DatabaseHelper extends SQLiteOpenHelper {


    public DatabaseHelper(Context context) {
        super(context, DatabaseContract.DATABASE_NAME, null, DatabaseContract.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DatabaseContract.MarkerEntry.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DatabaseContract.MarkerEntry.DROP_TABLE);
        onCreate(db);
    }

    public void insert(MapsMarker marker) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_TITLE, marker.getTitle());
        values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_DESCRIPTION, marker.getDescription());
        values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_URL, marker.getUrl());
        values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_LATITUDE, marker.getCoordinates().getLatLng().latitude);
        values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_LONGITUDE, marker.getCoordinates().getLatLng().longitude);
        values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_ALTITUDE, marker.getCoordinates().getElevation());

        db.insert(DatabaseContract.MarkerEntry.TABLE_NAME, null, values);
    }

    public void insertAll(Collection<MapsMarker> markers) {
        for (MapsMarker m : markers) insert(m);
    }

    public Collection<MapsMarker> findInArea(Double top, Double left, Double right, Double bottom) {
        SQLiteDatabase db = this.getReadableDatabase();
        Collection<MapsMarker> markers = new ArrayList<>();

        Boolean distinct = true;
        String[] columns = new String[]{
                DatabaseContract.MarkerEntry.COLUMN_NAME_TITLE,
                DatabaseContract.MarkerEntry.COLUMN_NAME_DESCRIPTION,
                DatabaseContract.MarkerEntry.COLUMN_NAME_URL,
                DatabaseContract.MarkerEntry.COLUMN_NAME_LATITUDE,
                DatabaseContract.MarkerEntry.COLUMN_NAME_LONGITUDE,
                DatabaseContract.MarkerEntry.COLUMN_NAME_ALTITUDE
        };
        String select = DatabaseContract.MarkerEntry.COLUMN_NAME_LATITUDE + " < ?"
                + " AND " + DatabaseContract.MarkerEntry.COLUMN_NAME_LATITUDE + " > ?"
                + " AND " + DatabaseContract.MarkerEntry.COLUMN_NAME_LONGITUDE + " < ?"
                + " AND " + DatabaseContract.MarkerEntry.COLUMN_NAME_LONGITUDE + " > ?";
        String[] args = new String[] { top.toString(), bottom.toString(),
                right.toString(), left.toString() };

        Cursor c = db.query(distinct, DatabaseContract.MarkerEntry.TABLE_NAME, columns, select, args,
                null, null, null, "25");

        c.moveToFirst();
        while (!c.isAfterLast()) {
            MapsMarker m = new MapsMarker();
            m.setTitle(c.getString(0));
            m.setDescription(c.getString(1));
            m.setUrl(c.getString(2));

            LatLng latLng = new LatLng(c.getDouble(3), c.getDouble(4));
            Coordinates coordinates = new Coordinates(latLng);
            coordinates.setElevation(c.getDouble(5));

            m.setCoordinates(coordinates);

            markers.add(m);

            c.moveToNext();
        }

        return markers;
    }

    public Collection<MapsMarker> findInArea(MapArea area) {
        return findInArea(area.getTop(), area.getLeft(), area.getRight(), area.getBottom());
    }

    public Long count() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseContract.MarkerEntry.TABLE_NAME, null);
        c.moveToFirst();
        Long count = c.getLong(0);
        c.close();

        return count;
    }
}
