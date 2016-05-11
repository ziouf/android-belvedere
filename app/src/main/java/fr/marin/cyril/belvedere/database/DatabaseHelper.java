package fr.marin.cyril.belvedere.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.model.Placemark;
import fr.marin.cyril.belvedere.parser.JsonResponseParser;
import fr.marin.cyril.belvedere.tools.Area;
import fr.marin.cyril.belvedere.tools.Utils;

/**
 * Created by cscm6014 on 30/03/2016.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static DatabaseHelper databaseHelper = null;

    public DatabaseHelper(Context context) {
        super(context, DatabaseContract.DATABASE_NAME, null, DatabaseContract.DATABASE_VERSION);
    }

    public static DatabaseHelper getInstance(Context context) {
        return databaseHelper == null ? new DatabaseHelper(context) : databaseHelper;
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
        if (oldVersion == newVersion) return;

        db.execSQL(DatabaseContract.KmlHashEntry.DROP_INDEX);
        db.execSQL(DatabaseContract.KmlHashEntry.DROP_TABLE);

        db.execSQL(DatabaseContract.MarkerEntry.DROP_INDEX);
        db.execSQL(DatabaseContract.MarkerEntry.DROP_TABLE);

        onCreate(db);
    }

    public void insertOrUpdatePlacemark(Placemark newPlacemark) {
        Placemark oldPlacemark = this.findPlacemarkByLatLng(newPlacemark.getCoordinates().getLatLng());

        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_TITLE, newPlacemark.getTitle());
            values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_URL, newPlacemark.getWiki_uri());
            values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_THUMBNAIL_URL, newPlacemark.getThumbnail_uri());
            values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_THUMBNAIL, newPlacemark.getThumbnailArray());
            values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_ALTITUDE, newPlacemark.getCoordinates().getElevation());

            if (oldPlacemark == null) {
                values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_LATITUDE, newPlacemark.getCoordinates().getLatLng().latitude);
                values.put(DatabaseContract.MarkerEntry.COLUMN_NAME_LONGITUDE, newPlacemark.getCoordinates().getLatLng().longitude);

                db.insert(DatabaseContract.MarkerEntry.TABLE_NAME, null, values);
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
            Log.e("", e.getMessage());
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
                        String title = c.getString(0);
                        String wiki_uri = c.getString(1);
                        String thumbnail_url = c.getString(2);
                        byte[] thumbnail = c.getBlob(3);
                        double lat = c.getDouble(4);
                        double lng = c.getDouble(5);
                        double ele = c.getDouble(6);

                        placemarks.add(new Placemark(title, lat, lng, ele, wiki_uri, thumbnail_url, thumbnail));
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

            Boolean distinct = true;
            String[] columns = new String[]{
                    DatabaseContract.MarkerEntry.COLUMN_NAME_TITLE,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_URL,
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

            try (Cursor c = db.query(distinct, DatabaseContract.MarkerEntry.TABLE_NAME, columns, select, args,
                    null, null, DatabaseContract.MarkerEntry.COLUMN_NAME_ALTITUDE + " DESC", "25")) {

                if (c.getCount() == 0) return markers;
                if (c.moveToFirst()) {
                    while (!c.isAfterLast()) {
                        String title = c.getString(0);
                        String wiki_uri = c.getString(1);
                        String thumbnail_url = c.getString(2);
                        byte[] thumbnail = c.getBlob(3);
                        double lat = c.getDouble(4);
                        double lng = c.getDouble(5);
                        double ele = c.getDouble(6);

                        markers.add(new Placemark(title, lat, lng, ele, wiki_uri, thumbnail_url, thumbnail));
                        c.moveToNext();
                    }
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
                    DatabaseContract.MarkerEntry.COLUMN_NAME_THUMBNAIL_URL,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_THUMBNAIL,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_LATITUDE,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_LONGITUDE,
                    DatabaseContract.MarkerEntry.COLUMN_NAME_ALTITUDE
            };
            String select = DatabaseContract.MarkerEntry.COLUMN_NAME_LATITUDE + " = ? "
                    + " AND " + DatabaseContract.MarkerEntry.COLUMN_NAME_LONGITUDE + " = ? ";
            String[] args = new String[]{"" + latLng.latitude, "" + latLng.longitude};

            try (Cursor c = db.query(distinct, DatabaseContract.MarkerEntry.TABLE_NAME, columns, select, args,
                    null, null, null, null)) {

                if (c.getCount() == 0) return null;

                c.moveToFirst();

                String title = c.getString(0);
                String wiki_uri = c.getString(1);
                String thumbnail_url = c.getString(2);
                byte[] thumbnail = c.getBlob(3);
                double lat = c.getDouble(4);
                double lng = c.getDouble(5);
                double ele = c.getDouble(6);

                return new Placemark(title, lat, lng, ele, wiki_uri, thumbnail_url, thumbnail);
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

    public String findSourceFileHash(String key) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {

            Boolean distinct = true;
            String[] columns = new String[]{
                    DatabaseContract.KmlHashEntry.COLUMN_NAME_VALUE
            };
            String select = DatabaseContract.KmlHashEntry.COLUMN_NAME_KEY + " = ? ";
            String[] args = new String[]{key};

            try (Cursor c = db.query(distinct, DatabaseContract.KmlHashEntry.TABLE_NAME, columns,
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
        String oldValue = this.findSourceFileHash(key);

        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(DatabaseContract.KmlHashEntry.COLUMN_NAME_VALUE, value);

            if (oldValue == null) {
                values.put(DatabaseContract.KmlHashEntry.COLUMN_NAME_KEY, key);

                db.insert(DatabaseContract.KmlHashEntry.TABLE_NAME, null, values);
            } else {
                String where = DatabaseContract.KmlHashEntry.COLUMN_NAME_KEY + " = ?";
                String[] whereArgs = new String[] { key };

                db.update(DatabaseContract.KmlHashEntry.TABLE_NAME, values, where, whereArgs);
            }
        }
    }

    public static class InitDBTask extends AsyncTask<Void, Integer, Void> {
        private static final String TAG = "InitDBTask";
        protected final Context context;
        private final DatabaseHelper databaseHelper;
        private final JsonResponseParser parser;
        private ExecutorService pool;

        public InitDBTask(Context context) {
            this.context = context;
            this.databaseHelper = DatabaseHelper.getInstance(context);
            this.parser = new JsonResponseParser();
        }

        @Override
        protected Void doInBackground(Void... params) {

            // Obtention des fichiers de resource
            TypedArray ta = context.getResources().obtainTypedArray(R.array.sparql_json_array);
            for (int i = 0; i < ta.length(); ++i) {
                publishProgress(i, ta.length());

                int id = ta.getResourceId(i, -1);
                String key = ta.getString(i);
                String hash = Utils.getSHA1FromResource(context, id);

                if (!hash.equals(databaseHelper.findSourceFileHash(key))) {
                    Log.i(TAG, String.format("Importation du fichier : %s (%s)", key, hash));
                    List<Placemark> placemarks = parser.readJsonStream(context.getResources().openRawResource(id));

                    pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 10);
                    for (Placemark p : placemarks) {
                        pool.submit(new DownloadThumbnail(p));
                    }
                    try {
                        pool.shutdown();
                        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                    } catch (InterruptedException ignore) {
                        Log.w(TAG, ignore.getMessage());
                    }

                    databaseHelper.insertOrUpdateKmlHash(key, hash);
                }
            }
            publishProgress(ta.length(), ta.length());
            ta.recycle();

            return null;
        }

        private class DownloadThumbnail implements Runnable {
            private final Placemark placemark;
            private final DatabaseHelper db = DatabaseHelper.getInstance(context);

            public DownloadThumbnail(Placemark placemark) {
                this.placemark = placemark;
            }

            @Override
            public void run() {
                getThumbnail(placemark);
            }

            private void getThumbnail(Placemark placemark) {
                try {
                    URL url = new URL(placemark.getThumbnail_uri());
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    int status = connection.getResponseCode();
                    while (status == HttpURLConnection.HTTP_SEE_OTHER
                            || status == HttpURLConnection.HTTP_MOVED_PERM
                            || status == HttpURLConnection.HTTP_MOVED_TEMP) {
                        url = new URL(connection.getHeaderField("Location"));
                        // fermeture de la connexion actuelle
                        connection.disconnect();
                        connection = (HttpURLConnection) url.openConnection();
                        status = connection.getResponseCode();
                    }

                    try (InputStream is = connection.getInputStream()) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        bitmap.compress(Bitmap.CompressFormat.WEBP, 85, baos);
                        placemark.setThumbnail(baos.toByteArray());
                    } finally {
                        connection.disconnect();
                        db.insertOrUpdatePlacemark(placemark);
                    }

                } catch (IOException ignore) {
                    Log.e("", ignore.getMessage());
                }
            }
        }


    }

}
