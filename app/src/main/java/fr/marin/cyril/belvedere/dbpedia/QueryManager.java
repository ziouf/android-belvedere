package fr.marin.cyril.belvedere.dbpedia;

import android.net.Uri;

import java.util.Locale;

/**
 * Created by cyril on 08/05/16.
 * <p>
 * see:
 * - http://developer.android.com/training/basics/network-ops/connecting.html
 * - http://fr.dbpedia.org/sparql
 * - http://git.cm-cloud.fr/MARIN/belvedere/snippets/16
 * <p>
 */
public class QueryManager {
    private static final String lang = Locale.getDefault().getLanguage();
    private static final String SEP = " ";
    private static final String RDF_PREFIX = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>";
    private static final String GEO_PREFIX = "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>";
    private static final String DBO_PREFIX = "PREFIX dbo: <http://dbpedia.org/ontology/>";
    private static final String FOAF_PREFIX = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>";
    public static final String PEAKS_QUERY =
            RDF_PREFIX + SEP + DBO_PREFIX + SEP + GEO_PREFIX + SEP + FOAF_PREFIX + SEP +
                    "SELECT ?id ?nom ?elevation ?latitude ?longitude ?wiki_url ?comment" + SEP +
                    "WHERE {" + SEP +
                    "?peak rdf:type dbo:Mountain ." + SEP +
                    "?peak dbo:wikiPageID ?id ." + SEP +
                    "?peak rdfs:label ?nom ." + SEP +
                    "?peak dbo:elevation ?elevation ." + SEP +
                    "?peak geo:lat ?latitude ." + SEP +
                    "?peak geo:long ?longitude ." + SEP +
                    "?peak foaf:isPrimaryTopicOf ?wiki_url ." + SEP +
                    "?peak rdfs:comment ?comment ." + SEP +
                    "FILTER(LANGMATCHES(LANG(?nom), \"" + lang + "\") && LANGMATCHES(LANG(?comment), \"" + lang + "\"))" + SEP +
                    "}";
    public static final String MOUNTAINS_QUERY =
            RDF_PREFIX + SEP + DBO_PREFIX + SEP + GEO_PREFIX + SEP + FOAF_PREFIX + SEP +
                    "SELECT DISTINCT ?id ?nom ?comment ?elevation ?latitude ?longitude ?wiki_url" + SEP +
                    "WHERE {" + SEP +
                    "?peak rdf:type dbo:Mountain ." + SEP +
                    "?peak dbo:wikiPageID ?id ." + SEP +
                    "?peak dbo:mountainRange ?massif ." + SEP +
                    "?massif rdfs:label ?nom ." + SEP +
                    "?massif rdfs:comment ?comment ." + SEP +
                    "?massif dbo:elevation ?elevation ." + SEP +
                    "?massif geo:lat ?latitude ." + SEP +
                    "?massif geo:long ?longitude ." + SEP +
                    "?massif foaf:isPrimaryTopicOf ?wiki_url ." + SEP +
                    "FILTER(LANGMATCHES(LANG(?nom), \"" + lang + "\") && LANGMATCHES(LANG(?comment), \"" + lang + "\"))" + SEP +
                    "}";

    public static String buildApiUrl(String query) {
        return new Uri.Builder()
                .scheme("http")
                .authority("dbpedia.org")
                .appendPath("sparql")
                .appendQueryParameter("default-graph-uri", "")
                .appendQueryParameter("format", "application/sparql-results+json")
                .appendQueryParameter("timeout", "0")
                .appendQueryParameter("query", query)
                .build()
                .toString();
    }

}
