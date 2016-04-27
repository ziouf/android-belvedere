package fr.marin.cyril.mapsapp.activities;

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
public class LocationFragmentActivity extends FragmentActivity
        implements LocationListener {

    private static final int LOCATION_UPDATE_TIME = 2000;
    private static final int LOCATION_UPDATE_DISTANCE = 10;

    protected final Criteria locationCriteria = new Criteria();

    protected GeomagneticField geoField;
    protected Location location;

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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;

        this.location = locationManager.getLastKnownLocation(locationManager.getBestProvider(locationCriteria, true));
        this.locationManager.requestLocationUpdates(locationManager.getBestProvider(locationCriteria, true), LOCATION_UPDATE_TIME, LOCATION_UPDATE_DISTANCE, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;

        this.locationManager.removeUpdates(this);
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
