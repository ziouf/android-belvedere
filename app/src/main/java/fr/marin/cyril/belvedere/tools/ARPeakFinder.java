package fr.marin.cyril.belvedere.tools;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import fr.marin.cyril.belvedere.database.RealmDbHelper;
import fr.marin.cyril.belvedere.model.Area;
import fr.marin.cyril.belvedere.model.Placemark;
import io.realm.Realm;

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
    private static final String TAG = "ARPeakFinder";

    private static final int MIN_VALUE = 0;
    private static final int MAX_VALUE = 1;
    private static final int SEARCH_AREA_LATERAL_KM = 10;
    private static final int SEARCH_AREA_FRONT_KM = 100;
    private static final int[] DISTANCE_STEPS = new int[]{20000};
    private static final double[] ANGULAR_ACCURACY = new double[]{6d, 3d};
    private static final double EARTH_RADIUS_KM = 6371d;

    private static ARPeakFinder singleton;

    // Db
    // TODO : Instancier dans onCreate + Fermer dans onDestroy
    private Realm realm = Realm.getDefaultInstance();
    // Observateur
    private LatLng oLatLng;
    private double oElevation;
    private double oAzimuth;
    private double oPitch;

    public ARPeakFinder(Context context) {

    }

    public ARPeakFinder(Context context, Location location, double azimuth, double pitch) {
        this.oLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        this.oElevation = location.getAltitude();
        this.oAzimuth = (azimuth + 360) % 360;
        this.oPitch = Math.abs(pitch);
    }

    public static ARPeakFinder getInstance(Context context) {
        if (singleton == null) singleton = new ARPeakFinder(context);
        return singleton;
    }

    public void updateObserverLocation(final Location location) {
        this.oLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        this.oElevation = location.getAltitude();
        Log.v(TAG, "Update Observer Location");
    }
    public void updateObserverOrientation(final double azimuth, final double pitch) {
        this.oPitch = pitch;
        this.oAzimuth = azimuth;
        Log.v(TAG, "Update Observer Orientation");
    }

    /**
     * @param distance
     * @return
     */
    private double getAngularAccuracy(final double distance) {
        for (int step = 0; step < DISTANCE_STEPS.length; ++step)
            if (distance < DISTANCE_STEPS[step])
                return ANGULAR_ACCURACY[step];
        return ANGULAR_ACCURACY[DISTANCE_STEPS.length];
    }

    /**
     *
     * @param azimuth
     * @param distance
     * @return
     */
    private double[] getAzimuthAccuracy(final double azimuth, final double distance) {
        final double aangularAccuracy = this.getAngularAccuracy(distance);
        Log.d(TAG, String.format("Azimuth accuracy : %s", aangularAccuracy));
        final double[] minMax = new double[]{azimuth - aangularAccuracy, azimuth + aangularAccuracy};
        if (minMax[MIN_VALUE] < 0) minMax[MIN_VALUE] += 360;
        if (minMax[MAX_VALUE] >= 360) minMax[MAX_VALUE] -= 360;
        return minMax;
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
     *
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
        double lng2 = lng + Math.atan2(Math.sin(brng) * Math.sin(dist) * Math.cos(lat), Math.cos(dist) - Math.sin(lat) * Math.sin(lat2));
        lng2 = Math.toDegrees((lng2 + 3 * Math.PI) % (2 * Math.PI) - Math.PI);

        return new LatLng(lat2, lng2);
    }

    /**
     *
     * @return
     */
    private Area getSearchArea() {
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
    public Placemark getMatchingPlacemark() {
        Placemark placemark = null;
        for (Placemark p : RealmDbHelper.findInArea(realm, this.getSearchArea(), Placemark.class)) {
            if (this.isMatchingAccuracy(p)) {
                Log.i(TAG, "getMatchingPlacemark | Placemark Matching : " + p.getTitle());
                if (placemark == null
                        // Si les placemarks sont à l'horizon, on choisit le sommet le plus haut
                        || (p.getDistance() > DISTANCE_STEPS[DISTANCE_STEPS.length - 1] && p.getElevation() > placemark.getElevation())
                        // Si les placemarks sont proches, on choisit le plus proche
                        || (p.getDistance() < DISTANCE_STEPS[DISTANCE_STEPS.length - 1] && p.getMatchLevel() < placemark.getMatchLevel())) {

                    placemark = p;
                }
            }
        }
        return placemark;
    }

    /**
     *
     * @param p
     * @return
     */
    private boolean isMatchingAccuracy(final Placemark p) {
        final double distance = Utils.getDistanceBetween(oLatLng, p.getLatLng());
        final double tAzimuth = this.getTheoricalAzimuth(p.getLatLng());
        final double tPitch = this.getTheoricalPitch(p.getLatLng(), p.getElevation());
        p.setDistance(distance);
        p.setMatchLevel((tAzimuth - oAzimuth) + (tPitch - oPitch));
        return isMatchingAzimuth(tAzimuth, distance)
                && isMatchingPitch(tPitch, distance);
    }

    /**
     *
     * @param targetTheoreticalAzimuth
     * @param distance
     * @return
     */
    private boolean isMatchingAzimuth(final double targetTheoreticalAzimuth, final double distance) {
        final double[] minMax = this.getAzimuthAccuracy(targetTheoreticalAzimuth, distance);
        if (minMax[MIN_VALUE] > minMax[MAX_VALUE])
            return (oAzimuth > 0 && oAzimuth < minMax[MAX_VALUE])
                    && (oAzimuth > minMax[MIN_VALUE] && oAzimuth < 360);
        else
            return (oAzimuth > minMax[MIN_VALUE] && oAzimuth < minMax[MAX_VALUE]);
    }

    /**
     *
     * @param theoricalPitch
     * @param distance
     * @return
     */
    private boolean isMatchingPitch(final double theoricalPitch, final double distance) {
        final double angularAccuracy = this.getAngularAccuracy(distance);
        return oPitch > theoricalPitch - angularAccuracy
                && oPitch < theoricalPitch + angularAccuracy;
    }

}
