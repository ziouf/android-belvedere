package fr.marin.cyril.belvedere.async;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import fr.marin.cyril.belvedere.Preferences;
import fr.marin.cyril.belvedere.database.RealmDbHelper;
import fr.marin.cyril.belvedere.dbpedia.QueryManager;
import fr.marin.cyril.belvedere.model.Placemark;
import fr.marin.cyril.belvedere.model.PlacemarkType;

/**
 * Created by cscm6014 on 10/02/2017.
 */

public final class DbpediaDataGetterAsync extends AsyncTask<DbpediaDataGetterAsync.Param, Void, Void> {
    private static final String TAG = DbpediaDataGetterAsync.class.getSimpleName();
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private Context context;
    private OnPostExecuteListener onPostExecuteListener;

    private DbpediaDataGetterAsync() {
    }

    public static DbpediaDataGetterAsync getInstance(Context applicationContext) {
        DbpediaDataGetterAsync async = new DbpediaDataGetterAsync();
        async.context = applicationContext;
        return async;
    }

    @Override
    protected void onPreExecute() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putString(Preferences.LAST_UPDATE_DATE.name(), SIMPLE_DATE_FORMAT.format(new Date())).apply();
    }

    @Override
    protected Void doInBackground(Param... params) {
        for (Param p : params)
            this.doInBackground(p);

        return null;
    }

    private void doInBackground(Param param) {
        final RealmDbHelper realm = RealmDbHelper.getInstance();
        final PlacemarkType type = param.type;
        final String query = this.queryDbPedia(param.query);

        try {
            final Collection<Placemark> items = getPlacemarksFromJSON(query, type);
            Log.i(TAG, "Parsed " + items.size() + " placemarks");

            Log.i(TAG, "Insertion en Base des " + items.size() + " derniers éléments");
            long begin = System.currentTimeMillis();
            realm.saveAll(items);

            Log.i(TAG, "insertion en base réalisée en " + (System.currentTimeMillis() - begin) + "ms");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        realm.close();
        Log.i(TAG, "Update finished, stoping service");
    }

    private final Collection<Placemark> getPlacemarksFromJSON(String json, PlacemarkType type) throws JSONException {
        final Collection<Placemark> placemarks = new ArrayList<>();
        final JSONArray bindings = new JSONObject(json).getJSONObject("results").getJSONArray("bindings");
        for (int i = 0; i < bindings.length(); ++i) {
            JSONObject o = bindings.getJSONObject(i);
            JSONObject id = o.getJSONObject("id");
            JSONObject title = o.getJSONObject("nom");
            JSONObject comment = o.getJSONObject("comment");
            JSONObject elevation = o.getJSONObject("elevation");
            JSONObject latitude = o.getJSONObject("latitude");
            JSONObject longitude = o.getJSONObject("longitude");

            Placemark placemark = new Placemark();
            placemark.setType(type);
            placemark.setId(id.getInt("value"));
            placemark.setTitle(title.getString("value"));
            placemark.setComment(comment.getString("value"));
            placemark.setElevation(elevation.getDouble("value"));
            placemark.setLatitude(latitude.getDouble("value"));
            placemark.setLongitude(longitude.getDouble("value"));

            placemarks.add(placemark);
        }
        return placemarks;
    }

    private final String queryDbPedia(String query) {
        try {
            final String url = QueryManager.buildApiUrl(query, Locale.getDefault().getLanguage());
            Log.i(TAG, "url : " + url);
            final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            Log.i(TAG, "dbPedia response code : " + conn.getResponseCode());

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                final StringBuilder sb = new StringBuilder();
                String buffer;
                while ((buffer = reader.readLine()) != null)
                    sb.append(buffer);

                return sb.toString();
            } finally {
                conn.disconnect();
            }

        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        onPostExecuteListener.onPostExecute();
    }

    public void setOnPostExecuteListener(OnPostExecuteListener onPostExecuteListener) {
        this.onPostExecuteListener = onPostExecuteListener;
    }

    public interface OnPostExecuteListener {
        void onPostExecute();
    }

    public static final class Param {
        private final String query;
        private final PlacemarkType type;

        private Param(String query, PlacemarkType type) {
            this.query = query;
            this.type = type;
        }

        public static Param of(String query, PlacemarkType type) {
            return new Param(query, type);
        }
    }

}
