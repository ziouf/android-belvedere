package fr.marin.cyril.belvedere.services.impl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.annimon.stream.Stream;

import java.util.HashSet;

import fr.marin.cyril.belvedere.services.ILocationEventListener;
import fr.marin.cyril.belvedere.services.ILocationService;

/**
 * Created by cyril on 31/05/16.
 */
class NativeLocationService
        implements ILocationService, LocationListener {
    private static final String TAG = NativeLocationService.class.getSimpleName();

    private final HashSet<ILocationEventListener> locationEventListenerSet;

    private Location location;
    private LocationManager locationManager;

    private NativeLocationService(Context context) {
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.locationEventListenerSet = new HashSet<>();
    }

    static NativeLocationService getInstance(Context context) {
        return new NativeLocationService(context);
    }

    public void resume() {
        this.initLocation();
        this.registerLocationUpdates();
    }

    public void pause() {
        this.removeLocationUpdates();
    }


    /**
     * Initialisation de la localisation
     */
    @SuppressLint("MissingPermission")
    private void initLocation() {
        this.location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * Inscription au trigger de geolocalisation
     */
    @SuppressLint("MissingPermission")
    private void registerLocationUpdates() {
        this.locationManager.requestLocationUpdates(locationManager.getBestProvider(new Criteria(), true),
                LocationServiceFactory.LOCATION_UPDATE_TIME, LocationServiceFactory.LOCATION_UPDATE_DISTANCE, this);
    }

    /**
     * Désinscription au trigger de geolocalisation
     */
    private void removeLocationUpdates() {
        this.locationManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;

        Stream.of(locationEventListenerSet)
                .forEach(l -> l.onSensorChanged(location));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        this.removeLocationUpdates();
        this.registerLocationUpdates();
    }

    @Override
    public void onProviderDisabled(String provider) {
        this.removeLocationUpdates();
        this.registerLocationUpdates();
    }

    public Location getLocation() {
        return location;
    }


    public ILocationEventListener registerLocationEventListener(ILocationEventListener eventListener) {
        this.locationEventListenerSet.add(eventListener);
        return eventListener;
    }

    public void unRegisterLocationEventListener(ILocationEventListener eventListener) {
        this.locationEventListenerSet.remove(eventListener);
    }
}
