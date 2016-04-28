package fr.marin.cyril.mapsapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

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
    private Context context;

    public DatabaseHelper(Context context) {
        super(context, DatabaseContract.DATABASE_NAME, null, DatabaseContract.DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Settings table
        db.execSQL(DatabaseContract.KmlEntry.CREATE_TABLE);

        // Create Markers table
        db.execSQL(DatabaseContract.MarkerEntry.CREATE_TABLE);
        // Create Markers indexes
        db.execSQL(DatabaseContract.MarkerEntry.CREATE_INDEX_LAT_LNG);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            db.execSQL(DatabaseContract.KmlEntry.DROP_TABLE);
            db.execSQL(DatabaseContract.MarkerEntry.DROP_TABLE);
            onCreate(db);
        }
    }

    public void initDataIfNeeded() {
        HashMap<String, Integer> kmlfiles = new HashMap<>();
        // Ajouter ci dessous la liste des fichiers de ressource
        kmlfiles.put("sommets_des_alpes_francaises", R.raw.sommets_des_alpes_francaises);// "963f9abb35f78339886eb3ebd43bcbae95a0a129")
        kmlfiles.put("sommets_des_pyrenees", R.raw.sommets_des_pyrenees); // "25fa96ca2f869004315750b429635f7f13c8c8da")
        kmlfiles.put("sommets_du_massif_central", R.raw.sommets_du_massif_central);// "a02a91fe25a99453870d8109dbd1c252634b6047")
        kmlfiles.put("sommets_du_massif_des_vosges", R.raw.sommets_du_massif_des_vosges);// "31a2a76ad41497d164fd13156b6007875c379fd1")

        for (String key : kmlfiles.keySet()) {
            String savedHash = this.findKml(key);
            String hash = Utils.getSHA1FromInputStream(context.getResources().openRawResource(kmlfiles.get(key)));
            if (!hash.equals(savedHash)) {
                this.insertAll(new KmlParser().parse(context.getResources().openRawResource(kmlfiles.get(key))));
                this.insertKml(key, hash);
            }
        }
    }

    public void insert(Placemark marker) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_TITLE, marker.getTitle());
        values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_URL, marker.getUrl());
        values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_LATITUDE, marker.getCoordinates().getLatLng().latitude);
        values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_LONGITUDE, marker.getCoordinates().getLatLng().longitude);
        values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_ALTITUDE, marker.getCoordinates().getElevation());

        db.insert(DatabaseContract.MarkerEntry.TABLE_NAME, null, values);
    }

    public void insertAll(Collection<Placemark> markers) {
        for (Placemark m : markers) insert(m);
    }

    public Collection<Placemark> findInArea(Double top, Double left, Double right, Double bottom) {
        SQLiteDatabase db = this.getReadableDatabase();
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
        String[] args = new String[] { top.toString(), bottom.toString(),
                right.toString(), left.toString() };

        Cursor c = db.query(distinct, DatabaseContract.MarkerEntry.TABLE_NAME, columns, select, args,
                null, null, DatabaseContract.MarkerEntry.COLUMN_NAME_ALTITUDE + " DESC", "25");

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

        c.close();
        return markers;
    }

    public Collection<Placemark> findInArea(Area area) {
        return findInArea(area.getTop(), area.getLeft(), area.getRight(), area.getBottom());
    }

    public Placemark findByLatLng(LatLng latLng) {
        SQLiteDatabase db = this.getReadableDatabase();

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
        String[] args = new String[] { ""+latLng.latitude, ""+latLng.longitude };

        Cursor c = db.query(distinct, DatabaseContract.MarkerEntry.TABLE_NAME, columns, select, args,
                null, null, null, null);

        c.moveToFirst();
        Placemark marker = new Placemark();
        marker.setTitle(c.getString(0));
        marker.setUrl(c.getString(1));
        Coordinates coordinates = new Coordinates();
        coordinates.setLatLng(new LatLng(c.getDouble(2), c.getDouble(3)));
        coordinates.setElevation(c.getDouble(4));
        marker.setCoordinates(coordinates);

        c.close();
        return marker;
    }

    public Long count() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseContract.MarkerEntry.TABLE_NAME, null);
        c.moveToFirst();
        Long count = c.getLong(0);
        c.close();

        return count;
    }

    public String findKml(String key) {
        SQLiteDatabase db = this.getReadableDatabase();

        Boolean distinct = true;
        String[] columns = new String[]{
                DatabaseContract.KmlEntry.COLUMN_NAME_VALUE
        };
        String select = DatabaseContract.KmlEntry.COLUMN_NAME_KEY + " = ? ";
        String[] args = new String[]{key};

        Cursor c = db.query(distinct, DatabaseContract.KmlEntry.TABLE_NAME, columns, select, args, null, null, null, null);

        if (c.getCount() == 0) return null;

        c.moveToFirst();
        String value = c.getString(0);
        c.close();

        return value;
    }

    public void insertKml(String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseContract.KmlEntry.COLUMN_NAME_KEY, key);
        values.put(DatabaseContract.KmlEntry.COLUMN_NAME_VALUE, value);

        db.insert(DatabaseContract.KmlEntry.TABLE_NAME, null, values);
    }
}
