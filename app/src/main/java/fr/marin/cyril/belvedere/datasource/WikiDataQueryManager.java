package fr.marin.cyril.belvedere.datasource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

/**
 * Created by cyril on 21/10/17.
 */

public class WikiDataQueryManager {
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
            "    bd:serviceParam wikibase:cornerWest \"Point(%1$s %2$s)\"^^geo:wktLiteral ." +
            "    bd:serviceParam wikibase:cornerEast \"Point(%3$s %4$s)\"^^geo:wktLiteral ." +
            "  } " +
            "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"%5$s,fr,en,de\" . } " +
            "}";

    private static final String GET_ALL_COUNTRIES_QUERY = "SELECT ?pays ?paysLabel WHERE {" +
            "  OPTIONAL { ?pays wdt:P17 ?pays. }" +
            "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"%1$s,fr,en,de\". }" +
            "}";

    private static final String GET_ALL_MOUTAINS_IN_COUNTRY_QUERY = "SELECT DISTINCT ?m ?mLabel ?lat ?lon ?abstract ?altitude ?article WHERE {  " +
            "  ?m wdt:P31 wd:Q8502 ." +
            "  ?m wdt:P17 <%1$s>;   " +
            "  p:P625 [   " +
            "    psv:P625 [      " +
            "      wikibase:geoLatitude ?lat ;      " +
            "      wikibase:geoLongitude ?lon ;     " +
            "      wikibase:geoGlobe ?globe ;    " +
            "    ];  " +
            "  ]" +
            "  OPTIONAL { ?m wd:Q333291 ?abstract. }  " +
            "  OPTIONAL { ?m wdt:P2044 ?altitude. }" +
            "  OPTIONAL {" +
            "      ?article schema:about ?m ." +
            "      ?article schema:isPartOf <https://%2$s.wikipedia.org/> ." +
            "    }" +
            "  FILTER ( ?globe = wd:Q2 )  " +
            "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"%2$s,fr,en,de\" . } " +
            "}";

    public enum WikiDataQueries {
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

        public String getQuery(String... args) {
            final Collection<String> collection = new ArrayList<>();
            Collections.addAll(collection, args);
            collection.add(Locale.getDefault().getLanguage());
            return String.format(Locale.ENGLISH, query, collection.toArray());
        }
    }
}
