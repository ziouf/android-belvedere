package fr.marin.cyril.belvedere.datasource;

import android.net.Uri;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

/**
 * Created by cyril on 21/10/17.
 */

public class WikiDataQueryManager {
    private static final String COUNT_ALL_MOUNTAINS_QUERY = "SELECT (COUNT(DISTINCT ?m) as ?count) " +
            "WHERE " +
            "{" +
            "  ?m wdt:P31 wd:Q8502;" +
            "  p:P625 [" +
            "    psv:P625 [" +
            "      wikibase:geoLatitude ?lat ;" +
            "      wikibase:geoLongitude ?lon ;" +
            "      wikibase:geoGlobe ?globe ;" +
            "    ];" +
            "  ]" +
            "  FILTER ( ?globe = wd:Q2 )" +
            "}";

    private static final String COUNT_FR_MOUTAINS_QUERY = "SELECT (COUNT(DISTINCT ?m) as ?count)" +
            "WHERE" +
            "{" +
            "  ?m wdt:P31 wd:Q8502;" +
            "  p:P625 [" +
            "    psv:P625 [" +
            "      wikibase:geoLatitude ?lat ;" +
            "      wikibase:geoLongitude ?lon ;" +
            "      wikibase:geoGlobe ?globe ;" +
            "    ];" +
            "  ];" +
            "  wdt:P17 ?pays;" +
            "  FILTER ( ?pays = wd:Q142 )" +
            "  FILTER ( ?globe = wd:Q2 )" +
            "}";

    private static final String GET_ALL_MOUNTAINS_QUERY = "SELECT DISTINCT ?m ?mLabel ?lat ?lon ?abstract ?altitude " +
            "WHERE " +
            "{" +
            "  ?m wdt:P31 wd:Q8502; " +
            "  p:P625 [" +
            "    psv:P625 [" +
            "      wikibase:geoLatitude ?lat ;" +
            "      wikibase:geoLongitude ?lon ;" +
            "      wikibase:geoGlobe ?globe ;" +
            "    ];" +
            "  ]" +
            "  OPTIONAL { ?m wd:Q333291 ?abstract. }" +
            "  OPTIONAL { ?m wdt:P2044 ?altitude. }" +
            "  FILTER ( ?globe = wd:Q2 )" +
            "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"%s,fr,en,de\" . } " +
            "} " +
            "LIMIT %s " +
            "OFFSET %s ";

    private static final String GET_FR_MOUNTAINS_QUERY = "SELECT DISTINCT ?m ?mLabel ?lat ?lon ?abstract ?altitude " +
            "WHERE " +
            "{" +
            "  ?m wdt:P31 wd:Q8502; " +
            "  p:P625 [" +
            "    psv:P625 [" +
            "      wikibase:geoLatitude ?lat ;" +
            "      wikibase:geoLongitude ?lon ;" +
            "      wikibase:geoGlobe ?globe ;" +
            "    ];" +
            "  ];" +
            "  wdt:P17 ?pays;" +
            "  OPTIONAL { ?m wd:Q333291 ?abstract. }" +
            "  OPTIONAL { ?m wdt:P2044 ?altitude. }" +
            "  FILTER ( ?pays = wd:Q142 )" +
            "  FILTER ( ?globe = wd:Q2 )" +
            "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"%s,fr,en,de\" . } " +
            "} " +
            "LIMIT %s " +
            "OFFSET %s ";

    private static final String GET_MOUNTAINS_IN_AREA_QUERY = "SELECT DISTINCT ?m ?mLabel ?lat ?lon ?abstract ?altitude " +
            "WHERE " +
            "{" +
            "  ?m wdt:P31 wd:Q8502; " +
            "  p:P625 [" +
            "    psv:P625 [" +
            "      wikibase:geoLatitude ?lat ;" +
            "      wikibase:geoLongitude ?lon ;" +
            "      wikibase:geoGlobe ?globe ;" +
            "    ];" +
            "  ]" +
            "  OPTIONAL { ?m wd:Q333291 ?abstract. }" +
            "  OPTIONAL { ?m wdt:P2044 ?altitude. }" +
            "  FILTER ( ?globe = wd:Q2 )" +
            "  SERVICE wikibase:box {" +
            "    ?place wdt:P625 ?location ." +
            "    bd:serviceParam wikibase:cornerWest \"Point(%s %s)\"^^geo:wktLiteral ." +
            "    bd:serviceParam wikibase:cornerEast \"Point(%s %s)\"^^geo:wktLiteral ." +
            "  } " +
            "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"%s,fr,en,de\" . } " +
            "} " +
            "LIMIT %s " +
            "OFFSET %s ";

    private static final String GET_ALL_COUNTRIES_QUERY = "SELECT ?pays ?paysLabel WHERE {" +
            "  OPTIONAL { ?pays wdt:P17 ?pays. }" +
            "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"%s,fr,en,de\". }" +
            "}";

    private static final String GET_ALL_MOUTAINS_IN_COUNTRY_QUERY = "SELECT DISTINCT ?m ?mLabel ?lat ?lon ?abstract ?altitude WHERE {  " +
            "  ?m wdt:P31 wd:Q8502 ." +
            "  ?m wdt:P17 <%s>;   " +
            "  p:P625 [   " +
            "    psv:P625 [      " +
            "      wikibase:geoLatitude ?lat ;      " +
            "      wikibase:geoLongitude ?lon ;     " +
            "      wikibase:geoGlobe ?globe ;    " +
            "    ];  " +
            "  ]" +
            "  OPTIONAL { ?m wd:Q333291 ?abstract. }  " +
            "  OPTIONAL { ?m wdt:P2044 ?altitude. }" +
            "  FILTER ( ?globe = wd:Q2 )  " +
            "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en,fr,en,de\" . } " +
            "} " +
            "LIMIT %s " +
            "OFFSET %s ";

    private static Uri.Builder uriBuilder() {
        return new Uri.Builder()
                .scheme("https")
                .authority("query.wikidata.org")
                .appendPath("sparql")
                .appendQueryParameter("format", "json");
    }

    public static String query(String sparql) {
        return uriBuilder()
                .appendQueryParameter("query", sparql)
                .build()
                .toString();
    }

    public static Uri uri() {
        return uriBuilder()
                .build();
    }

    public enum WikiDataQueries {

        COUNT_FR_MOUNTAINS(COUNT_FR_MOUTAINS_QUERY),
        COUNT_ALL_MOUNTAINS(COUNT_ALL_MOUNTAINS_QUERY),

        /**
         * First argument  : limit
         * Second argument : offset
         */
        GET_ALL_MOUNTAINS(GET_ALL_MOUNTAINS_QUERY),
        GET_FR_MOUNTAINS(GET_FR_MOUNTAINS_QUERY),
        /**
         * First argument  : cornerWest lat
         * Second argument : cornerWest lon
         * Thrid argument  : cornerEast lat
         * Fourth argument : cornerEast lon
         */
        GET_MOUNTAINS_IN_AREA(GET_MOUNTAINS_IN_AREA_QUERY),

        GET_ALL_COUNTRIES(GET_ALL_COUNTRIES_QUERY),
        GET_ALL_MOUTAINS_IN_COUNTRY(GET_ALL_MOUTAINS_IN_COUNTRY_QUERY);

        private String query;

        WikiDataQueries(String query) {
            this.query = query;
        }

        public String getQuery(int offset, int limit, String... args) {
            final Collection<String> collection = new ArrayList<>();
            Collections.addAll(collection, args);
            collection.add(Locale.getDefault().getLanguage());
            collection.add(Integer.toString(limit));
            collection.add(Integer.toString(offset));
            return String.format(Locale.ENGLISH, query, collection.toArray());
        }
    }
}
