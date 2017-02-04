package fr.marin.cyril.belvedere.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import fr.marin.cyril.belvedere.database.RealmDbHelper;
import fr.marin.cyril.belvedere.dbpedia.JsonResponseParser;
import fr.marin.cyril.belvedere.dbpedia.QueryManager;
import fr.marin.cyril.belvedere.model.Placemark;
import fr.marin.cyril.belvedere.model.PlacemarkType;

/**
 * Created by CSCM6014 on 09/09/2016.
 */
public class UpdateDataService extends IntentService {
    private static final String TAG = "UpdateDataService";
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");

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
        sharedPreferences.edit().putString("last_bdd_update", SIMPLE_DATE_FORMAT.format(new Date())).apply();

        Log.i(TAG, "Querying dbpedia.org");
        final String peaks_query_result = this.queryDbPedia(QueryManager.PEAKS_QUERY);
        final String mountains_query_result = this.queryDbPedia(QueryManager.MOUNTAINS_QUERY);

        Log.i(TAG, "Parsing results");
        final JsonResponseParser parser = new JsonResponseParser();
        final Collection<Placemark> peaks = parser.readJsonString(peaks_query_result, PlacemarkType.PEAK, Placemark.class);
        final Collection<Placemark> mounts = parser.readJsonString(mountains_query_result, PlacemarkType.MOUNTAIN, Placemark.class);
        Log.i(TAG, "Parsed " + peaks.size() + " placemarks");

        Log.i(TAG, "Insertion en Base des " + peaks.size() + " derniers éléments");
        long begin = System.currentTimeMillis();
        realm.saveAll(peaks);
        realm.saveAll(mounts);
        Log.i(TAG, "insertion en base réalisée en " + (System.currentTimeMillis() - begin) + "ms");

        realm.close();
        Log.i(TAG, "Update finished, stoping service");
        this.stopSelf();
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
