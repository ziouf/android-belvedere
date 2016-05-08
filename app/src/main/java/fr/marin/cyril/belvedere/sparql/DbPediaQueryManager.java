package fr.marin.cyril.belvedere.sparql;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import fr.marin.cyril.belvedere.sparql.model.PeakInfo;

/**
 * Created by cyril on 08/05/16.
 *
 * see:
 * - http://developer.android.com/training/basics/network-ops/connecting.html
 * - http://fr.dbpedia.org/sparql
 * - http://git.cm-cloud.fr/MARIN/belvedere/snippets/16
 */
public class DbPediaQueryManager {
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

    public PeakInfo getElevation() {

        return null;
    }

    private String downloadUrl(String myurl) {
        try {
            URL url = new URL(myurl);
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

                try (InputStreamReader isr = new InputStreamReader(conn.getInputStream(), "UTF-8")) {
                    try (BufferedReader reader = new BufferedReader(isr)) {
                        StringBuilder sb = new StringBuilder();

                        while (reader.ready())
                            sb.append(reader.readLine());

                        return sb.toString();
                    }
                }
            } finally {
                conn.disconnect();
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }

    private class SparqlQueryTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }

        @Override
        protected String doInBackground(String... params) {
            return downloadUrl(params[0]);
        }
    }
}
