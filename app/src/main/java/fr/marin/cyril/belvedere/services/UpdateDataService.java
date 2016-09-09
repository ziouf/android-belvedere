package fr.marin.cyril.belvedere.services;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import fr.marin.cyril.belvedere.database.DatabaseHelper;
import fr.marin.cyril.belvedere.datasources.DbPediaQueryManager;
import fr.marin.cyril.belvedere.model.Placemark;
import fr.marin.cyril.belvedere.parser.DbPediaJsonResponseParser;

/**
 * Created by CSCM6014 on 09/09/2016.
 */
public class UpdateDataService extends IntentService {
    private static final String TAG = "UpdateDataService";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public UpdateDataService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "HandleIntent");

        Log.i(TAG, "Querying dbpedia.org");
        final String result = this.queryDbPedia(DbPediaQueryManager.PEAKS_QUERY);

        Log.i(TAG, "Parsing results");
        final DbPediaJsonResponseParser parser = new DbPediaJsonResponseParser();
        final List<Placemark> placemarks = parser.readJsonString(result);

        Log.i(TAG, "Parsed " + placemarks.size() + " placemarks");
        for (Placemark p : placemarks) {
            try {
                final HttpURLConnection connection = getRedirectedConnection(new URL(p.getThumbnail_uri()));
                try (InputStream is = connection.getInputStream()) {
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
                    final Bitmap bitmap = BitmapFactory.decodeStream(is);
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 45, baos);
                    p.setThumbnail(baos.toByteArray());
                } finally {
                    connection.disconnect();
                }
            } catch (IOException ignore) {
                Log.e(TAG, ignore.getMessage());
            }

            DatabaseHelper.getInstance(getApplicationContext()).insertOrUpdatePlacemark(p);
        }

        Log.i(TAG, "Update finished, stoping service");
        this.stopSelf();
    }

    private String queryDbPedia(String query) {
        try {
            String url = DbPediaQueryManager.buildApiUrl(query);
            Log.i(TAG, "url : " + url);
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                Log.i(TAG, "dbPedia response code : " + conn.getResponseCode());
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

    private HttpURLConnection getRedirectedConnection(URL url) throws IOException {
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
        return connection;
    }
}
