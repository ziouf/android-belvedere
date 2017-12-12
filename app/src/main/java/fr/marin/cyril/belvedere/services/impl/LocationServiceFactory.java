package fr.marin.cyril.belvedere.services.impl;

import android.content.Context;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import fr.marin.cyril.belvedere.services.ILocationService;

/**
 * Created by cyril on 12/12/17.
 */
public class LocationServiceFactory {
    public static final int LOCATION_UPDATE_TIME = 5 * 1000; // 5 secondes
    public static final int LOCATION_UPDATE_DISTANCE = 15;   // 15 metres

    public static ILocationService getLocationService(Context context) {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS ?
                GoogleAPILocationService.getInstance(context) : /* With Google Play API */
                NativeLocationService.getInstance(context);  /* Native Location services */
    }

}
