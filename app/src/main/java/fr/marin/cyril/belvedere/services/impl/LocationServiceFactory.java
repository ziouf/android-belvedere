package fr.marin.cyril.belvedere.services.impl;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.Collection;
import java.util.Random;

import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.services.ILocationService;
import fr.marin.cyril.belvedere.tools.Objects;

/**
 * Created by cyril on 12/12/17.
 */
public class LocationServiceFactory {
    static final int LOCATION_UPDATE_TIME = 5 * 1000; // 5 secondes
    static final int LOCATION_UPDATE_DISTANCE = 15;   // 15 metres

    private static final int PERMISSIONS_CODE = new Random(0).nextInt(Short.MAX_VALUE);
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    public static ILocationService getLocationService(Context context) {
        if (!isLocationServiceEnabled(context)) {
            final AlertDialog dialog = initLocationServiceDialog(context);
            dialog.show();
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request LOCATION permissions
            ActivityCompat.requestPermissions((Activity) context, PERMISSIONS, PERMISSIONS_CODE);
        }

        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS ?
                GoogleAPILocationService.getInstance(context) : /* With Google Play API */
                NativeLocationService.getInstance(context);  /* Native Location services */
    }

    private static boolean isLocationServiceEnabled(Context context) {
        final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (Objects.isNull(locationManager)) return false;

        final Collection<String> providers = locationManager.getProviders(true);
        return (providers.contains(LocationManager.GPS_PROVIDER)
                || providers.contains(LocationManager.NETWORK_PROVIDER));
    }

    private static AlertDialog initLocationServiceDialog(final Context context) {
        return new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.location_service_not_enabled))
                .setMessage(context.getString(R.string.open_location_settings))
                .setPositiveButton(context.getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(context.getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
    }

}
