package fr.marin.cyril.belvedere.dbpedia;

import android.net.Uri;

/**
 * Created by cyril on 08/05/16.
 * <p>
 * see:
 * - http://developer.android.com/training/basics/network-ops/connecting.html
 * - http://fr.dbpedia.org/sparql
 * - http://git.cm-cloud.fr/MARIN/belvedere/snippets/16
 * <p>
 * Selection de tous les sommets de France, groupés par Region
 * SELECT ?regionLabel ?nom ?altitude ?latitude ?longitude ?thumbnail ?wiki
 * WHERE {
 * ?peak rdf:type dbpedia-owl:Mountain .
 * ?peak rdf:type dbpedia-owl:NaturalPlace .
 * ?peak dbpedia-owl:region ?region .
 * ?region dbpedia-owl:country <http://fr.dbpedia.org/resource/France> .
 * ?peak dbpedia-owl:region ?region .
 * ?region rdfs:label ?regionLabel .
 * ?peak rdfs:label ?nom .
 * ?peak prop-fr:altitude ?altitude .
 * ?peak prop-fr:latitude ?latitude .
 * ?peak prop-fr:longitude ?longitude .
 * ?peak dbpedia-owl:thumbnail ?thumbnail .
 * ?peak foaf:isPrimaryTopicOf ?wiki .
 * FILTER(LANGMATCHES(LANG(?nom), "fr") && LANGMATCHES(LANG(?regionLabel), "fr"))
 * }
 * GROUP BY ?region
 * ORDER BY DESC(?region)
 */
public class QueryManager {
    private static final String SEP = " ";
    private static final String API_URL = "http://dbpedia.org/sparql";
    private static final String RDF_PREFIX = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>";
    private static final String GEO_PREFIX = "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>";
    private static final String DBO_PREFIX = "PREFIX dbo: <http://dbpedia.org/ontology/>";
    private static final String FOAF_PREFIX = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>";
    public static final String PEAKS_QUERY =
            RDF_PREFIX + SEP + DBO_PREFIX + SEP + GEO_PREFIX + SEP + FOAF_PREFIX + SEP +
                    "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" + SEP +
                    "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>" + SEP +
                    "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>" + SEP +
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
                    "FILTER(LANGMATCHES(LANG(?nom), \"fr\") && LANGMATCHES(LANG(?comment), \"fr\"))" + SEP +
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
                    "FILTER(LANGMATCHES(LANG(?nom), \"fr\") && LANGMATCHES(LANG(?comment), \"fr\"))" + SEP +
                    "}";

    public static String buildApiUrl(String query) {
        return API_URL + "?default-graph-uri=" +
                "&query=" + Uri.encode(query) +
                "&format=application%2Fsparql-results%2Bjson" +
                "&timeout=0";
    }

}
