package fr.marin.cyril.belvedere;

/**
 * Created by marin on 04/02/2017.
 */

public enum Preferences {
    LAST_UPDATE_DATE(0),
    UPDATE_FREQUENCY_DAYS(2);

    private final long defaultValue;

    Preferences(long defaultValue) {
        this.defaultValue = defaultValue;
    }

    public long defaultValue() {
        return defaultValue;
    }
}
