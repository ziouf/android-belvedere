package fr.marin.cyril.belvedere.activities.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

/**
 * Created by CSCM6014 on 27/04/2016.
 */
public class LocationFragment extends android.support.v4.app.Fragment
        implements LocationListener {

    private static final int LOCATION_UPDATE_TIME = 2000;
    private static final int LOCATION_UPDATE_DISTANCE = 10;

    protected final Criteria locationCriteria = new Criteria();

    protected GeomagneticField geoField;
    protected Location location;

    protected LocationManager locationManager;


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        this.locationCriteria.setAccuracy(Criteria.ACCURACY_FINE);
        this.locationCriteria.setPowerRequirement(Criteria.POWER_MEDIUM);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            this.location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            this.locationManager.requestLocationUpdates(locationManager.getBestProvider(locationCriteria, true), LOCATION_UPDATE_TIME, LOCATION_UPDATE_DISTANCE, this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            this.locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (this.location == null) onFirstLocationChanged(location);
        this.location = location;
        this.geoField = new GeomagneticField(
                (float) location.getLatitude(),
                (float) location.getLongitude(),
                (float) location.getAltitude(),
                System.currentTimeMillis()
        );
    }

    protected void onFirstLocationChanged(Location location) {

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
