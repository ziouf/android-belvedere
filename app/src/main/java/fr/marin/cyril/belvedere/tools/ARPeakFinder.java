package fr.marin.cyril.belvedere.tools;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.Collection;
import java.util.HashSet;

import fr.marin.cyril.belvedere.database.DatabaseHelper;
import fr.marin.cyril.belvedere.model.Area;
import fr.marin.cyril.belvedere.model.Coordinates;
import fr.marin.cyril.belvedere.model.Placemark;

/**
 * Created by CSCM6014 on 22/04/2016.
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
    private static final int SEARCH_AREA_LATERAL_KM = 15;
    private static final int SEARCH_AREA_FRONT_KM = 80;
    private static final int[] DISTANCE_STEPS = new int[]{10000, 15000, 20000};
    private static final double[] AZIMUTH_ACCURACY = new double[]{5d, 2.5d, 1, 0.25d};
    private static final double EARTH_RADIUS = 6371d;

    private final DatabaseHelper db;

    // Observateur
    private LatLng oLatLng;
    private double oElevation;
    private double oAzimuth;
    private double oPitch;

    public ARPeakFinder(Context context) {
        this.db = DatabaseHelper.getInstance(context);
    }

    private static double getAzimuthAccuracy(double distance) {
        for (int step = 0; step < DISTANCE_STEPS.length; ++step)
            if (distance < DISTANCE_STEPS[step])
                return AZIMUTH_ACCURACY[step];
        return AZIMUTH_ACCURACY[DISTANCE_STEPS.length];
    }

    public static double[] getAzimuthAccuracy(double azimuth, double distance) {
        double azimuth_accuracy = ARPeakFinder.getAzimuthAccuracy(distance);
        Log.d(TAG, String.format("Azimuth accuracy : %s", azimuth_accuracy));
        double[] minMax = new double[]{azimuth - azimuth_accuracy, azimuth + azimuth_accuracy};
        if (minMax[MIN_VALUE] < 0) minMax[MIN_VALUE] += 360;
        if (minMax[MAX_VALUE] >= 360) minMax[MAX_VALUE] -= 360;
        return minMax;
    }

    /**
     * Calcul de l'azimuth théorique entre l'observateur et la cible
     *
     * @param t
     * @return
     */
    private double getTheoricalAzimuth(Coordinates t) {
        double dX = t.getLatLng().latitude - oLatLng.latitude;
        double dY = t.getLatLng().longitude - oLatLng.longitude;
        double phi = Math.toDegrees(Math.atan(Math.abs(dY / dX)));

        if (dX > 0 && dY > 0) return phi;
        else if (dX < 0 && dY > 0) return 180 - phi;
        else if (dX < 0 && dY < 0) return 180 + phi;
        else if (dX > 0 && dY < 0) return 360 - phi;
        else return phi;
    }

    private LatLng getLatLngFromDistanceAndBearing(int distance, double bearing) {
        double dist = distance / EARTH_RADIUS;
        double brng = Math.toRadians((oAzimuth + bearing + 360) % 360);
        double lat = Math.toRadians(oLatLng.latitude);
        double lng = Math.toRadians(oLatLng.longitude);

        double lat2 = Math.toDegrees(Math.asin(Math.sin(lat) * Math.cos(dist) + Math.cos(lat) * Math.sin(dist) * Math.cos(brng)));
        double lng2 = lng + Math.atan2(Math.sin(brng) * Math.sin(dist) * Math.cos(lat), Math.cos(dist) - Math.sin(lat) * Math.sin(lat2));
        lng2 = Math.toDegrees((lng2 + 3 * Math.PI) % (2 * Math.PI) - Math.PI);

        return new LatLng(lat2, lng2);
    }

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

    public Collection<Placemark> getMatchingPlacemark(Location location) {
        final HashSet<Placemark> matchingPlacemark = new HashSet<>();
        final Area area = getSearchArea();

        if (area == null) return matchingPlacemark;

        for (Placemark p : db.findPlacemarkInArea(area)) {
            final double distance = Utils.getDistanceBetween(location, p.getCoordinates().getLatLng());
            final double theoricalAzimuth = getTheoricalAzimuth(p.getCoordinates());

            if (isMatchingAccuracy(theoricalAzimuth, distance)) {
                double elevation_delta = Math.abs(p.getCoordinates().getElevation() - location.getAltitude());
                double lvl = distance * elevation_delta;

                p.setMatchLevel(lvl);
                matchingPlacemark.add(p);

                Log.i(TAG, "getMatchingPlacemark | Placemark Matching : " + p.getTitle() + " | MatchLevel : " + p.getMatchLevel());
            }
        }

        return matchingPlacemark;
    }

    /**
     * @param targetTheoreticalAzimuth
     * @return
     */
    public boolean isMatchingAccuracy(double targetTheoreticalAzimuth, double distance) {
        double[] minMax = ARPeakFinder.getAzimuthAccuracy(targetTheoreticalAzimuth, distance);
        if (minMax[MIN_VALUE] > minMax[MAX_VALUE])
            return (oAzimuth > 0 && oAzimuth < minMax[MAX_VALUE])
                    && (oAzimuth > minMax[MIN_VALUE] && oAzimuth < 360);
        else
            return (oAzimuth > minMax[MIN_VALUE] && oAzimuth < minMax[MAX_VALUE]);
    }


    public void setObserverLocation(Location l) {
        this.oLatLng = new LatLng(l.getLatitude(), l.getLongitude());
        this.oElevation = l.getAltitude();
    }

    public void setObserverAzimuth(double azimuth) {
        this.oAzimuth = azimuth;
    }
}
