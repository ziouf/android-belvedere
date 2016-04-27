package fr.marin.cyril.mapsapp.tools;

import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Created by CSCM6014 on 13/04/2016.
 */
public class Utils {

    /**
     * Retourne true si le téléphone dispose des capteurs suffisants pour utiliser la Réalité Augmentée
     *
     * @return
     */
    public static boolean isCompassAvailable(Context context) {
        PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)
                && pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);
    }

    public static String getDirectionFrom0to360Degrees(float degrees) {
        if (degrees >= 360 - 22.5 || degrees < 22.5) {
            return "N";
        }
        if (degrees >= 22.5 && degrees < 67.5) {
            return "NE";
        }
        if (degrees >= 67.5 && degrees < 112.5) {
            return "E";
        }
        if (degrees >= 112.5 && degrees < 157.5) {
            return "SE";
        }
        if (degrees >= 157.5 && degrees < 360 - 157.5) {
            return "S";
        }
        if (degrees >= 360 - 157.5 && degrees < 360 - 112.5) {
            return "SW";
        }
        if (degrees >= 360 - 112.5 && degrees < 360 - 67.5) {
            return "W";
        }
        if (degrees >= 360 - 67.5 && degrees < 360 - 22.5) {
            return "NW";
        }

        return null;
    }

    public static String getDirectionFromMinus180to180Degrees(float degrees) {
        if (degrees >= -22.5 && degrees < 22.5) {
            return "N";
        }
        if (degrees >= 22.5 && degrees < 67.5) {
            return "NE";
        }
        if (degrees >= 67.5 && degrees < 112.5) {
            return "E";
        }
        if (degrees >= 112.5 && degrees < 157.5) {
            return "SE";
        }
        if (degrees >= 157.5 || degrees < -157.5) {
            return "S";
        }
        if (degrees >= -157.5 && degrees < -112.5) {
            return "SW";
        }
        if (degrees >= -112.5 && degrees < -67.5) {
            return "W";
        }
        if (degrees >= -67.5 && degrees < -22.5) {
            return "NW";
        }

        return null;
    }
}