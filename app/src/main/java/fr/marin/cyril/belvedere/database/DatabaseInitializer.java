package fr.marin.cyril.belvedere.database;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.model.Placemark;
import fr.marin.cyril.belvedere.parser.DbPediaJsonResponseParser;
import fr.marin.cyril.belvedere.tools.ConnectivityStatus;
import fr.marin.cyril.belvedere.tools.Utils;

/**
 * Created by Cyril on 18/05/2016.
 */
public class DatabaseInitializer extends AsyncTask<Void, Integer, Void> {
    private static final String TAG = "InitDBTask";

    private final Context context;
    private final DatabaseHelper db;
    private final DbPediaJsonResponseParser parser;
    private final ConnectivityStatus connectivityStatus;

    public DatabaseInitializer(Context context) {
        this.context = context;
        this.db = DatabaseHelper.getInstance(context);
        this.parser = new DbPediaJsonResponseParser();
        this.connectivityStatus = new ConnectivityStatus(context);
    }

    @Override
    protected Void doInBackground(Void... params) {
        // Obtention des fichiers de resource
        final TypedArray ta = context.getResources().obtainTypedArray(R.array.sparql_json_array);

        for (int i = 0; i < ta.length(); ++i) {
            publishProgress(i, ta.length());

            final int id = ta.getResourceId(i, -1);
            final String key = ta.getString(i);
            final String hash = Utils.getSHA1FromResource(context, id);

            if (!hash.equals(db.findSourceFileHash(key))) {
                Log.i(TAG, String.format("Importation du fichier : %s (%s)", key, hash));
                final List<Placemark> placemarks = parser.readJsonStream(context.getResources().openRawResource(id));

                if (connectivityStatus.canDownloadFromWeb()) {
                    final ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 10);
                    for (Placemark p : placemarks) {
                        pool.submit(new DownloadThumbnail(p));
                    }
                    try {
                        pool.shutdown();
                        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                    } catch (InterruptedException ignore) {
                        Log.w(TAG, ignore.getMessage());
                    }
                }

                db.insertAllPlacemark(placemarks);
                db.insertOrUpdateKmlHash(key, hash);
            }
        }
        publishProgress(ta.length(), ta.length());

        ta.recycle();
        return null;
    }

    private class DownloadThumbnail implements Runnable {
        private final Placemark placemark;

        public DownloadThumbnail(Placemark placemark) {
            this.placemark = placemark;
        }

        @Override
        public void run() {
            getThumbnail(placemark);
        }

        private void getThumbnail(final Placemark placemark) {
            try {
                final HttpURLConnection connection = getRedirectedConnection(new URL(placemark.getThumbnail_uri()));
                try (InputStream is = connection.getInputStream()) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 50, baos);
                    placemark.setThumbnail(baos.toByteArray());
                } finally {
                    connection.disconnect();
                }
            } catch (IOException ignore) {
                Log.e(TAG, ignore.getMessage());
            }
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
}
