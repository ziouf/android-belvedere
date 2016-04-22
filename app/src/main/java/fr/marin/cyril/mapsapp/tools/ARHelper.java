package fr.marin.cyril.mapsapp.tools;

import com.google.android.gms.maps.model.LatLng;

import fr.marin.cyril.mapsapp.kml.model.Coordinates;

/**
 * Created by CSCM6014 on 22/04/2016.
 * <p/>
 * Etapes de recherche des sommets ciblés par l'utilisateur:
 * - Calcul de l'azimuth théorique entre l'utilisateur et chaque élément de la base de données
 * - Calcul de l'azimuth réel de l'utilisateur
 * - Définition de la précision souhaitée
 * - Comparaison entre les deux azimuth modulo la précision
 */
public class ARHelper {
    private static final int MIN_VALUE = 0;
    private static final int MAX_VALUE = 1;
    private static final double AZIMUTH_ACCURACY = 5;

    public static double getTheoricalAzimuth(Observer o, Coordinates t) {
        double dX = t.getLatLng().latitude - o.coordinates.getLatLng().latitude;
        double dY = t.getLatLng().longitude - o.coordinates.getLatLng().longitude;
        double phi = Math.toDegrees(Math.atan(Math.abs(dY / dX)));

        if (dX > 0 && dY > 0) return phi;
        else if (dX < 0 && dY > 0) return 180 - phi;
        else if (dX < 0 && dY < 0) return 180 + phi;
        else if (dX > 0 && dY < 0) return 360 - phi;
        else return phi;
    }

    public static double[] getAzimuthAccuracy(double azimuth) {
        double[] minMax = new double[]{azimuth + AZIMUTH_ACCURACY, azimuth - AZIMUTH_ACCURACY};
        if (minMax[MIN_VALUE] < 0) minMax[MIN_VALUE] += 360;
        if (minMax[MAX_VALUE] >= 360) minMax[MAX_VALUE] -= 360;
        return minMax;
    }

    public static boolean isMatchingAccuracy(double azimuth) {
        double[] minMax = ARHelper.getAzimuthAccuracy(azimuth);
        if (minMax[MIN_VALUE] > minMax[MAX_VALUE])
            return (azimuth > 0 && azimuth < minMax[MAX_VALUE])
                    && (azimuth > minMax[MIN_VALUE] && azimuth < 360);
        else
            return (azimuth > minMax[MIN_VALUE] && azimuth < minMax[MAX_VALUE]);
    }

    public static class Observer {
        private Coordinates coordinates = new Coordinates(new LatLng(0, 0), 0d);
        private float azimuth = 0f;
        private float pitch = 0f;

        public Observer(LatLng latLng, double elevation, float azimuth, float pitch) {
            this.coordinates = new Coordinates(latLng, elevation);
            this.azimuth = azimuth;
            this.pitch = pitch;
        }

        public Coordinates getCoordinates() {
            return coordinates;
        }

        public float getAzimuth() {
            return azimuth;
        }

        public float getPitch() {
            return pitch;
        }
    }

}
