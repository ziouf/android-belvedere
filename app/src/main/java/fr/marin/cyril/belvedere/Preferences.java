package fr.marin.cyril.belvedere;

/**
 * Created by marin on 04/02/2017.
 */

public class Preferences {
    public static final String LAST_UPDATE_DATE = "lastUpdateDate";
    public static final String UPDATE_FREQUENCY_DAYS = "updateFrequencyDays";
    public static final int UPDATE_FREQUENCY_DAYS_DEFAULT = 7;

    public static final String COUNTRIES = "countries";
    public static final int MAX_ON_MAP = Runtime.getRuntime().availableProcessors() * 15;

}
