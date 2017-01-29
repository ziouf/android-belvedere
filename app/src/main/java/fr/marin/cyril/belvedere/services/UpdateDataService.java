package fr.marin.cyril.belvedere.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;

import fr.marin.cyril.belvedere.database.RealmDbHelper;
import fr.marin.cyril.belvedere.dbpedia.JsonResponseParser;
import fr.marin.cyril.belvedere.dbpedia.QueryManager;
import fr.marin.cyril.belvedere.model.Placemark;
import io.realm.Realm;

/**
 * Created by CSCM6014 on 09/09/2016.
 */
public class UpdateDataService extends IntentService {
    private static final String TAG = "UpdateDataService";

    private int max = 0;
    private Realm realm;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public UpdateDataService() {
        super(TAG);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "HandleIntent");
        this.realm = Realm.getDefaultInstance();

        Log.i(TAG, "Querying dbpedia.org");
        final String result = this.queryDbPedia(QueryManager.FRENCH_PEAKS_QUERY);

        Log.i(TAG, "Parsing results");
        final JsonResponseParser parser = new JsonResponseParser();
        final Collection<Placemark> placemarks = parser.readJsonString(result);
        this.max = placemarks.size();
        Log.i(TAG, "Parsed " + placemarks.size() + " placemarks");

        Log.i(TAG, "Insertion en Base des " + placemarks.size() + " derniers éléments");
        long begin = System.currentTimeMillis();
        RealmDbHelper.saveAll(realm, placemarks);
        Log.i(TAG, "insertion en base réalisée en " + (System.currentTimeMillis() - begin) + "ms");

        this.realm.close();
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
