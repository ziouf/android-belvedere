package fr.marin.cyril.belvedere.services;

import android.location.Location;

/**
 * Created by cyril on 12/12/17.
 */

public interface ILocationEventListener {
    void onSensorChanged(Location location);
}
