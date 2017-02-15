package fr.marin.cyril.belvedere.async;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import fr.marin.cyril.belvedere.Preferences;
import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.database.RealmDbHelper;
import fr.marin.cyril.belvedere.dbpedia.QueryManager;
import fr.marin.cyril.belvedere.model.Placemark;
import fr.marin.cyril.belvedere.model.PlacemarkType;

/**
 * Created by cscm6014 on 10/02/2017.
 */

public final class DbpediaDataGetterAsync extends AsyncTask<DbpediaDataGetterAsync.Param, Void, Void> {
    private static final String TAG = DbpediaDataGetterAsync.class.getSimpleName();
    private static final int BATCH_SIZE = 250;
    private RealmDbHelper realm;
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
    protected Void doInBackground(Param... params) {
        realm = RealmDbHelper.getInstance();

        long begin = System.currentTimeMillis();

        for (Param p : params)
            this.doInBackground(p);

        Log.i(TAG, "Parsing + Insertion en base réalisé en " + (System.currentTimeMillis() - begin) + "ms");

        realm.close();
        return null;
    }

    private void doInBackground(Param param) {
        final PlacemarkType type = param.type;
        final String json = this.queryDbPedia(param.query);

        try {
            final JSONArray bindings = new JSONObject(json).getJSONObject("results").getJSONArray("bindings");
            Log.i(TAG, "Parsed " + bindings.length() + " placemarks");

            Collection<Placemark> placemarks = new ArrayList<>();

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

                if (placemarks.size() > BATCH_SIZE) {
                    realm.saveAll(placemarks);
                    placemarks = new ArrayList<>();
                }
            }

            realm.saveAll(placemarks);

        } catch (JSONException e) {
            Log.e(TAG, "Erreur de parsing JSON", e);
        }
    }

    private String queryDbPedia(String query) {
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
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (sharedPreferences.contains(Preferences.LAST_UPDATE_DATE.name()))
            Toast.makeText(context, R.string.toast_update_database_finished, Toast.LENGTH_LONG).show();
        else
            Toast.makeText(context, R.string.toast_init_database_finished, Toast.LENGTH_LONG).show();

        sharedPreferences.edit()
                .putLong(Preferences.LAST_UPDATE_DATE.name(), new Date().getTime())
                .apply();

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
