package fr.marin.cyril.belvedere.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
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
 * Created by CSCM6014 on 09/09/2016.
 */
public class UpdateDataService extends IntentService {
    private static final String TAG = UpdateDataService.class.getSimpleName();
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public UpdateDataService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "HandleIntent");
        RealmDbHelper realm = RealmDbHelper.getInstance();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        sharedPreferences.edit().putString(Preferences.LAST_UPDATE_DATE.name(), SIMPLE_DATE_FORMAT.format(new Date())).apply();

        Log.i(TAG, "Querying dbpedia.org");
        final String peaks_query_result = this.queryDbPedia(QueryManager.PEAKS_QUERY);
        final String mounts_query_result = this.queryDbPedia(QueryManager.MOUNTAINS_QUERY);

        Log.i(TAG, "Parsing results");
        try {
            final Collection<Placemark> peaks = getPlacemarksFromJSON(peaks_query_result, PlacemarkType.PEAK);
            final Collection<Placemark> mounts = getPlacemarksFromJSON(mounts_query_result, PlacemarkType.MOUNTAIN);
            Log.i(TAG, "Parsed " + peaks.size() + " placemarks");

            Log.i(TAG, "Insertion en Base des " + peaks.size() + " derniers éléments");
            long begin = System.currentTimeMillis();
            realm.saveAll(peaks);
            realm.saveAll(mounts);
            Log.i(TAG, "insertion en base réalisée en " + (System.currentTimeMillis() - begin) + "ms");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        realm.close();
        Log.i(TAG, "Update finished, stoping service");
        this.stopSelf();
    }

    private Collection<Placemark> getPlacemarksFromJSON(String json, PlacemarkType type) throws JSONException {
        Collection<Placemark> placemarks = new ArrayList<>();
        JSONArray bindings = new JSONObject(json).getJSONObject("results").getJSONArray("bindings");
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

    private String queryDbPedia(String query) {
        try {
            final String url = QueryManager.buildApiUrl(query);
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
}
