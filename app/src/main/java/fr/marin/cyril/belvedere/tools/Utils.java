package fr.marin.cyril.belvedere.tools;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Cyril on 13/04/2016.
 */
class Utils {
    private static final String TAG = Utils.class.getSimpleName();

    /**
     * @param a
     * @param b
     * @return
     */
    public static float getDistanceBetween(LatLng a, LatLng b) {
        float[] result = new float[3];
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, result);
        return result[0];
    }

    /**
     * Distance en metres
     *
     * @param a
     * @param b
     * @return
     */
    public static float getDistanceBetween(Location a, LatLng b) {
        float[] result = new float[3];
        Location.distanceBetween(a.getLatitude(), a.getLongitude(), b.latitude, b.longitude, result);
        return result[0];
    }

    /**
     *
     * @param degrees
     * @return
     */
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

    /**
     *
     * @param degrees
     * @return
     */
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

    /**
     * @param context
     * @param id
     * @return
     */
    public static String getSHA1FromResource(Context context, int id) {
        return Utils.getSHA1FromInputStream(context.getResources().openRawResource(id));
    }

    /**
     * @param is
     * @return
     */
    private static String getSHA1FromInputStream(InputStream is) {
        StringBuilder sb = new StringBuilder();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            BufferedInputStream bis = new BufferedInputStream(is);
            byte[] buffer = new byte[4096];
            while (bis.read(buffer) != -1) {
                digest.update(buffer);
            }
            bis.close();

            for (byte b : digest.digest())
                sb.append(String.format("%02x", b));

        } catch (NoSuchAlgorithmException | IOException e) {
            Log.e(TAG, e.getMessage());
        }

        return sb.toString();
    }
}
