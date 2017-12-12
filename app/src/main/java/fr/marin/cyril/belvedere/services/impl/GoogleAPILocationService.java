package fr.marin.cyril.belvedere.services.impl;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;

import com.annimon.stream.Stream;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.HashSet;

import fr.marin.cyril.belvedere.services.ILocationService;

/**
 * Created by cyril on 12/12/17.
 * https://developer.android.com/training/location/receive-location-updates.html
 */

class GoogleAPILocationService implements ILocationService {
    private static final String TAG = GoogleAPILocationService.class.getSimpleName();

    private final HashSet<AbstractLocationEventListener> locationEventListenerSet;
    private final FusedLocationProviderClient mFusedLocationClient;
    private final LocationRequest mLocationRequest;
    private final Context context;
    private Location location;
    private final LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            // Update location
            GoogleAPILocationService.this.location = locationResult.getLastLocation();
            // Call listeners
            Stream.of(locationEventListenerSet).forEach(l -> l.onSensorChanged(GoogleAPILocationService.this.location));
        }
    };

    public GoogleAPILocationService(Context context) {
        this.context = context;
        this.mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        this.mLocationRequest = this.createLocationRequest();
        this.locationEventListenerSet = new HashSet<>();
    }

    public static ILocationService getInstance(Context context) {
        return new GoogleAPILocationService(context);
    }

    private LocationRequest createLocationRequest() {
        return new LocationRequest()
                .setInterval(LocationServiceFactory.LOCATION_UPDATE_TIME)
                .setFastestInterval(LocationServiceFactory.LOCATION_UPDATE_TIME / 2)
                .setSmallestDisplacement(LocationServiceFactory.LOCATION_UPDATE_DISTANCE)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    public void resume() {
        this.startLocationUpdates();
    }

    public void pause() {
        this.stopLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            this.mFusedLocationClient.requestLocationUpdates(
                    this.mLocationRequest,
                    this.mLocationCallback,
                    null
            );
        }
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    @Override
    public AbstractLocationEventListener registerLocationEventListener(AbstractLocationEventListener eventListener) {
        this.locationEventListenerSet.add(eventListener);
        return eventListener;
    }

    @Override
    public void unRegisterLocationEventListener(AbstractLocationEventListener eventListener) {
        this.locationEventListenerSet.remove(eventListener);
    }
}
