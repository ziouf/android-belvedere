package fr.marin.cyril.belvedere.tools;

import android.location.Location;
import android.util.Log;

import com.annimon.stream.Stream;
import com.google.android.gms.maps.model.LatLng;

import fr.marin.cyril.belvedere.model.Area;
import fr.marin.cyril.belvedere.model.Placemark;

/**
 * Created by Cyril on 22/04/2016.
 * <p/>
 * Etapes de recherche des sommets ciblés par l'utilisateur:
 * - Calcul de l'azimuth théorique entre l'utilisateur et chaque élément de la base de données
 * - Calcul de l'azimuth réel de l'utilisateur
 * - Définition de la précision souhaitée
 * - Comparaison entre les deux azimuth modulo la précision
 */
public class ARPeakFinder {
    private static final String TAG = ARPeakFinder.class.getSimpleName();

    private static final int MIN_VALUE = 0;
    private static final int MAX_VALUE = 1;
    private static final int SEARCH_AREA_LATERAL_KM = 10;
    private static final int SEARCH_AREA_FRONT_KM = 100;
    private static final int[] DISTANCE_STEPS = new int[]{20000};
    private static final double[] ANGULAR_ACCURACY = new double[]{6d, 3d};
    private static final double EARTH_RADIUS_KM = 6371d;

    // Observateur
    private final LatLng oLatLng;
    private final double oElevation;
    private final double oAzimuth;
    private final double oPitch;

    public ARPeakFinder(Location location, double azimuth, double pitch) {
        this.oLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        this.oElevation = location.getAltitude();
        this.oAzimuth = (azimuth + 360) % 360;
        this.oPitch = Math.abs(pitch);
    }

    /**
     * @param distance
     * @return
     */
    private double getAngularAccuracy(final double distance) {
        return Stream.range(0, DISTANCE_STEPS.length)
                .filter(step -> distance < DISTANCE_STEPS[step])
                .map(step -> ANGULAR_ACCURACY[step])
                .findFirst()
                .orElse(ANGULAR_ACCURACY[DISTANCE_STEPS.length]);
    }

    /**
     * @param azimuth
     * @param distance
     * @return
     */
    private AzimuthRange getAzimuthAccuracy(final double azimuth, final double distance) {
        final double angularAccuracy = this.getAngularAccuracy(distance);
        Log.d(TAG, String.format("Azimuth accuracy : %s", angularAccuracy));
        return AzimuthRange.get(azimuth - angularAccuracy, azimuth + angularAccuracy);
    }

    /**
     * Calcul de l'azimuth théorique entre l'observateur et la cible
     *
     * @param latLng
     * @return
     */
    private double getTheoricalAzimuth(final LatLng latLng) {
        final double dX = latLng.latitude - oLatLng.latitude;
        final double dY = latLng.longitude - oLatLng.longitude;
        final double phi = Math.toDegrees(Math.atan(Math.abs(dY / dX)));

        if (dX > 0 && dY > 0) return phi;
        else if (dX < 0 && dY > 0) return 180 - phi;
        else if (dX < 0 && dY < 0) return 180 + phi;
        else if (dX > 0 && dY < 0) return 360 - phi;
        else return phi;
    }

    /**
     * Angle entre le sol et la ligne de mire vers le sommet visé
     *
     * @param latLng
     * @param elevation
     * @return
     */
    private double getTheoricalPitch(final LatLng latLng, final Double elevation) {
        final double h = Math.abs(elevation - oElevation); // Metres
        final double l = Utils.getDistanceBetween(latLng, oLatLng); // Metres
        return Math.toDegrees(Math.atan(h / l));
    }

    /**
     * @param distance
     * @param bearing
     * @return
     */
    private LatLng getLatLngFromDistanceAndBearing(final int distance, final double bearing) {
        final double dist = distance / EARTH_RADIUS_KM;
        final double brng = Math.toRadians((oAzimuth + bearing + 360) % 360);
        final double lat = Math.toRadians(oLatLng.latitude);
        final double lng = Math.toRadians(oLatLng.longitude);

        final double lat2 = Math.toDegrees(Math.asin(Math.sin(lat) * Math.cos(dist) + Math.cos(lat) * Math.sin(dist) * Math.cos(brng)));
        final double lng2Rad = lng + Math.atan2(Math.sin(brng) * Math.sin(dist) * Math.cos(lat), Math.cos(dist) - Math.sin(lat) * Math.sin(lat2));
        final double lng2 = Math.toDegrees((lng2Rad + 3 * Math.PI) % (2 * Math.PI) - Math.PI);

        return new LatLng(lat2, lng2);
    }

    /**
     * @return
     */
    public Area getSearchArea() {
        final LatLng left = getLatLngFromDistanceAndBearing(SEARCH_AREA_LATERAL_KM, -90);
        final LatLng right = getLatLngFromDistanceAndBearing(SEARCH_AREA_LATERAL_KM, 90);
        final LatLng front = getLatLngFromDistanceAndBearing(SEARCH_AREA_FRONT_KM, 0);

        LatLng southWest;
        LatLng northEast;

        if (left.latitude > right.latitude && oLatLng.latitude < front.latitude) { // on regarde en haut à droite
            Log.d(TAG, "getSearchArea : North East");
            southWest = new LatLng(right.latitude, left.longitude);
            northEast = front;
        } else if (left.latitude > right.latitude && oLatLng.latitude > front.latitude) { // on regarde en bas a droite
            Log.d(TAG, "getSearchArea : South East");
            southWest = new LatLng(front.latitude, right.longitude);
            northEast = new LatLng(left.latitude, front.longitude);
        } else if (left.latitude < right.latitude && oLatLng.latitude < front.latitude) { // on regarde en haut a gauche
            Log.d(TAG, "getSearchArea : North West");
            southWest = new LatLng(left.latitude, front.longitude);
            northEast = new LatLng(front.latitude, right.longitude);
        } else if (left.latitude < right.latitude && oLatLng.latitude > front.latitude) { // on regarde en bas a gauche
            Log.d(TAG, "getSearchArea : South West");
            southWest = front;
            northEast = new LatLng(right.latitude, left.longitude);
        } else if (left.latitude == right.latitude && oLatLng.latitude < front.latitude) {
            Log.d(TAG, "getSearchArea : North");
            southWest = left;
            northEast = new LatLng(right.latitude, front.longitude);
        } else if (left.latitude == right.latitude && oLatLng.latitude > front.latitude) {
            Log.d(TAG, "getSearchArea : South");
            southWest = new LatLng(right.latitude, front.longitude);
            northEast = left;
        } else if (left.latitude < right.latitude && oLatLng.latitude == front.latitude) {
            Log.d(TAG, "getSearchArea : East");
            southWest = right;
            northEast = new LatLng(front.latitude, left.longitude);
        } else if (left.latitude > right.latitude && oLatLng.latitude == front.latitude) {
            Log.d(TAG, "getSearchArea : West");
            southWest = new LatLng(front.latitude, left.longitude);
            northEast = right;
        } else {
            Log.d(TAG, "getSearchArea : ... nowere");
            return null;
        }

        return new Area(northEast, southWest);
    }

    /**
     * @return
     */
    public boolean isMatchingPlacemark(Placemark placemark) {
        if (this.isMatchingAccuracy(placemark)) {
            Log.i(TAG, "getMatchingPlacemark | Placemark Matching : " + placemark.getTitle());
            if ((placemark.getDistance() > DISTANCE_STEPS[DISTANCE_STEPS.length - 1])
                    // Si les placemarks sont proches, on choisit le plus proche
                    || (placemark.getDistance() < DISTANCE_STEPS[DISTANCE_STEPS.length - 1])) {

                return true;
            }
        }
        return false;
    }

    /**
     * @param p
     * @return
     */
    public boolean isMatchingAccuracy(final Placemark p) {
        final double distance = Utils.getDistanceBetween(oLatLng, p.getLatLng());
        final double tAzimuth = this.getTheoricalAzimuth(p.getLatLng());
        final double tPitch = this.getTheoricalPitch(p.getLatLng(), p.getElevation());
        p.setDistance(distance);
        p.setMatchLevel((tAzimuth - oAzimuth) + (tPitch - oPitch));
        return isMatchingAzimuth(tAzimuth, distance)
                && isMatchingPitch(tPitch, distance);
    }

    /**
     * @param targetTheoreticalAzimuth
     * @param distance
     * @return
     */
    private boolean isMatchingAzimuth(final double targetTheoreticalAzimuth, final double distance) {
        final AzimuthRange minMax = this.getAzimuthAccuracy(targetTheoreticalAzimuth, distance);
        if (minMax.min > minMax.max)
            return (oAzimuth > 0 && oAzimuth < minMax.max)
                    && (oAzimuth > minMax.min && oAzimuth < 360);
        else
            return (oAzimuth > minMax.min && oAzimuth < minMax.max);
    }

    /**
     * @param theoricalPitch
     * @param distance
     * @return
     */
    private boolean isMatchingPitch(final double theoricalPitch, final double distance) {
        final double angularAccuracy = this.getAngularAccuracy(distance);
        return oPitch > theoricalPitch - angularAccuracy
                && oPitch < theoricalPitch + angularAccuracy;
    }

    /**
     *
     */
    private static final class AzimuthRange {
        final double min;
        final double max;

        private AzimuthRange(double min, double max) {
            this.min = min < 0 ? min + 360 : min;
            this.max = max >= 360 ? max - 360 : max;
        }

        public static AzimuthRange get(double min, double max) {
            return new AzimuthRange(min, max);
        }
    }
}
