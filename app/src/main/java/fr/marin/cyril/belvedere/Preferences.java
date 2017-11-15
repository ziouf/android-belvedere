package fr.marin.cyril.belvedere;

/**
 * Created by marin on 04/02/2017.
 */

public enum Preferences {
    LAST_UPDATE_DATE(0),
    UPDATE_FREQUENCY_DAYS(7),
    COUNTRIES(0);

    public static final int MAX_ON_MAP = Runtime.getRuntime().availableProcessors() * 15;
    private final long defaultValue;

    Preferences(long defaultValue) {
        this.defaultValue = defaultValue;
    }

    public long defaultValue() {
        return defaultValue;
    }
}
