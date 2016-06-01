package fr.marin.cyril.belvedere.tools;

import android.Manifest;
import android.content.Context;
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
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;

import java.util.Collection;

import fr.marin.cyril.belvedere.R;

/**
 * Created by cyril on 31/05/16.
 */
public class LocationService implements LocationListener {

    private static final int LOCATION_UPDATE_TIME = 5 * 1000; // 5 secondes
    private static final int LOCATION_UPDATE_DISTANCE = 15;   // 15 metres

    private static LocationService singleton = null;
    private final Context context;

    private Location location;
    private GeomagneticField geoField;
    private LocationManager locationManager;

    private AlertDialog dialog;
    private boolean dialogOpened = false;
    private boolean dialogAlreadyShown = false;

    public LocationService(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.dialog = this.initLocationServiceDialog(context);
    }

    public static LocationService getInstance(Context context) {
        if (singleton == null) singleton = new LocationService(context);
        return singleton;
    }

    public void resume() {
        if (this.isLocationServiceEnabled()) {
            this.initLocation();
            this.registerLocationUpdates();
        } else if (!dialogAlreadyShown) {
            this.askForLocationServiceActivation();
        }
    }

    public void pause() {
        this.removeLocationUpdates();
    }


    /**
     * Initialisation de la localisation
     */
    private void initLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            this.location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
    }

    /**
     * Inscription au trigger de geolocalisation
     */
    public void registerLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            this.locationManager.requestLocationUpdates(locationManager.getBestProvider(new Criteria(), true),
                    LOCATION_UPDATE_TIME, LOCATION_UPDATE_DISTANCE, this);
        }
    }

    /**
     * Désinscription au trigger de geolocalisation
     */
    protected void removeLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            this.locationManager.removeUpdates(this);
        }
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
    public boolean isLocationServiceEnabled() {
        final Collection<String> providers = locationManager.getProviders(true);
        return (providers.contains(LocationManager.GPS_PROVIDER)
                || providers.contains(LocationManager.NETWORK_PROVIDER));
    }

    /**
     * Affichage de la popup de demande d'activation des services de geolocalisation
     */
    public void askForLocationServiceActivation() {
        if (!dialogOpened) {
            dialog.show();
            this.dialogOpened = true;
        }
    }

    public Location getLocation() {
        return location;
    }

    public GeomagneticField getGeoField() {
        return geoField;
    }

    /**
     * Initialisation de la popup
     *
     * @return
     */
    private AlertDialog initLocationServiceDialog(final Context context) {
        return new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.location_service_not_enabled))
                .setMessage(context.getString(R.string.open_location_settings))
                .setPositiveButton(context.getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        dialog.dismiss();
                        dialogOpened = false;
                        dialogAlreadyShown = true;
                    }
                })
                .setNegativeButton(context.getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        dialogOpened = false;
                        dialogAlreadyShown = true;
                    }
                })
                .create();
    }
}
