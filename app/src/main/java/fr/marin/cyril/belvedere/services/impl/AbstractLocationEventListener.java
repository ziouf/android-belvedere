package fr.marin.cyril.belvedere.services.impl;

import android.location.Location;

/**
 * Created by cyril on 12/12/17.
 */

public abstract class AbstractLocationEventListener {
    public abstract void onSensorChanged(Location location);
}
