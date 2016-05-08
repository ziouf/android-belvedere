package fr.marin.cyril.belvedere.sparql;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import fr.marin.cyril.belvedere.sparql.model.PeakInfo;
import fr.marin.cyril.belvedere.sparql.parser.JsonResponseParser;

/**
 * Created by cyril on 08/05/16.
 *
 * see:
 * - http://developer.android.com/training/basics/network-ops/connecting.html
 * - http://fr.dbpedia.org/sparql
 * - http://git.cm-cloud.fr/MARIN/belvedere/snippets/16
 */
public class DbPediaQueryManager extends AsyncTask<String, Void, List<PeakInfo>> {
    private static final String TAG = "DbPediaQueryManager";

    private static final String API_URL = "http://fr.dbpedia.org/sparql";
    private static final String RDF_PREFIX = "rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>";

    private static final String ELEVATION_QUERY =
            "PREFIX " + RDF_PREFIX + " " +
                "SELECT ?altitude WHERE { " +
                "?peak rdf:type dbpedia-owl:Mountain . " +
                "?peak rdf:type dbpedia-owl:NaturalPlace . " +
                "?peak prop-fr:nom \"%s\"@fr . " +
                "?peak prop-fr:altitude ?altitude } LIMIT 1";

    private String buildApiUrl(String query, String peakName) {
        return Uri.encode(API_URL + "?default-graph-uri=" +
                "&query=" + String.format(query, peakName) +
                "&format=application%2Fsparql-results%2Bjson" +
                "&timeout=0");
    }

    private JsonResponseParser parser = new JsonResponseParser();

    private InputStream queryDbPedia(String peakName) {
        try {
            URL url = new URL(buildApiUrl(ELEVATION_QUERY, peakName));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            try {
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                // Starts the query
                conn.connect();
                int response = conn.getResponseCode();
                if (response != HttpURLConnection.HTTP_OK) return null;

                return conn.getInputStream();
            } finally {
                conn.disconnect();
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }

    @Override
    protected List<PeakInfo> doInBackground(String... params) {
        return parser.readJsonStream(queryDbPedia(params[0]));
    }

}
