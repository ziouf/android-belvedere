package fr.marin.cyril.belvedere.sparql;

import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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
    private static final String SEP = " ";

    private static final String API_URL = "http://fr.dbpedia.org/sparql";

    private static final String RDF_PREFIX = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>";
    private static final String DBPEDIA_OWL_PREFIX = "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>";
    private static final String PROP_FR_PREFIX = "PREFIX prop-fr: <http://fr.dbpedia.org/property/>";
    private static final String FOAF_PREFIX = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>";

    private static final String FRENCH_PEAKS_QUERY =
            RDF_PREFIX + SEP + DBPEDIA_OWL_PREFIX + SEP + PROP_FR_PREFIX + SEP + FOAF_PREFIX + SEP +
                    "SELECT ?nom ?altitude ?latitude ?longitude ?thumbnail ?wiki" + SEP +
                    "WHERE {" +
                    "?peak dbpedia-owl:region ?region ." + SEP +
                    "?region dbpedia-owl:country <http://fr.dbpedia.org/resource/France> ." + SEP +
                    "?peak rdf:type dbpedia-owl:Mountain ." + SEP +
                    "?peak rdf:type dbpedia-owl:NaturalPlace ." + SEP +
                    "?peak prop-fr:nom ?nom ." + SEP +
                    "?peak prop-fr:altitude ?altitude ." + SEP +
                    "?peak prop-fr:latitude ?latitude ." + SEP +
                    "?peak prop-fr:longitude ?longitude ." + SEP +
                    "?peak dbpedia-owl:thumbnail ?thumbnail ." + SEP +
                    "?peak foaf:isPrimaryTopicOf ?wiki" +
                    "}";

    private static final String ELEVATION_QUERY =
            RDF_PREFIX + " " +
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

}