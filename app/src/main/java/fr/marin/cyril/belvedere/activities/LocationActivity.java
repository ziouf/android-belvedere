package fr.marin.cyril.belvedere.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;

import java.util.Collection;

import fr.marin.cyril.belvedere.R;

/**
 * Created by Cyril on 27/04/2016.
 */
public class LocationActivity extends FragmentActivity
        implements LocationListener {

    private static final int LOCATION_UPDATE_TIME = 5 * 1000; // 5 secondes
    private static final int LOCATION_UPDATE_DISTANCE = 15;   // 15 metres
    private static final String LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;
    protected Location location;
    protected GeomagneticField geoField;
    protected LocationManager locationManager;

    private AlertDialog dialog;
    private boolean dialogOpened = false;
    private boolean dialogAlreadyShown = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        this.dialog = this.initLocationServiceDialog();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.isLocationServiceEnabled()) {
            this.initLocation();
            this.registerLocationUpdates();
        } else if (!dialogAlreadyShown) {
            this.askForLocationServiceActivation();
        }
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
                location.getTime()
        );
    }

    /**
     * Initialisation de la localisation
     */
    private void initLocation() {
        if (ActivityCompat.checkSelfPermission(this, LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            this.location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
    }

    /**
     * Inscription au trigger de geolocalisation
     */
    protected void registerLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            this.locationManager.requestLocationUpdates(locationManager.getBestProvider(new Criteria(), true),
                    LOCATION_UPDATE_TIME, LOCATION_UPDATE_DISTANCE, this);
        }
    }

    /**
     * Désinscription au trigger de geolocalisation
     */
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
        this.removeLocationUpdates();
        this.registerLocationUpdates();
    }

    @Override
    public void onProviderDisabled(String provider) {
        this.removeLocationUpdates();
        this.registerLocationUpdates();
    }

    /**
     * Vérification de l'activation des services de géolocalisation
     *
     * @return
     */
    protected boolean isLocationServiceEnabled() {
        final Collection<String> providers = locationManager.getProviders(true);
        return (providers.contains(LocationManager.GPS_PROVIDER)
                || providers.contains(LocationManager.NETWORK_PROVIDER));
    }

    /**
     * Affichage de la popup de demande d'activation des services de geolocalisation
     */
    protected void askForLocationServiceActivation() {
        if (!dialogOpened) {
            dialog.show();
            this.dialogOpened = true;
        }
    }

    /**
     * Initialisation de la popup
     *
     * @return
     */
    private AlertDialog initLocationServiceDialog() {
        return new AlertDialog.Builder(this)
                .setTitle(getString(R.string.location_service_not_enabled))
                .setMessage(getString(R.string.open_location_settings))
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        dialog.dismiss();
                        dialogOpened = false;
                        dialogAlreadyShown = true;
                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //LocationActivity.this.finish();
                        dialog.dismiss();
                        dialogOpened = false;
                        dialogAlreadyShown = true;
                    }
                })
                .create();
    }
}
