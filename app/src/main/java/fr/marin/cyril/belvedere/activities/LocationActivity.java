package fr.marin.cyril.belvedere.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;

/**
 * Created by CSCM6014 on 27/04/2016.
 */
public class LocationActivity extends FragmentActivity
        implements LocationListener {

    private static final int LOCATION_UPDATE_TIME = 5 * 1000; // 5 secondes
    private static final int LOCATION_UPDATE_DISTANCE = 15;   // 15 metres
    private static final String LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;

    protected final Criteria locationCriteria = new Criteria();

    protected Location location;
    protected GeomagneticField geoField;
    protected LocationManager locationManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        this.locationCriteria.setAccuracy(Criteria.ACCURACY_FINE);
        this.locationCriteria.setPowerRequirement(Criteria.POWER_MEDIUM);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.registerLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.removeLocationUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
        this.geoField = new GeomagneticField(
                (float) location.getLatitude(),
                (float) location.getLongitude(),
                (float) location.getAltitude(),
                System.currentTimeMillis()
        );
    }

    protected void registerLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            this.location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            this.locationManager.requestLocationUpdates(locationManager.getBestProvider(locationCriteria, true), LOCATION_UPDATE_TIME, LOCATION_UPDATE_DISTANCE, this);
        }
    }

    protected void removeLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            this.locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
