package fr.marin.cyril.mapsapp.tools;

import android.content.Context;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.util.Collection;
import java.util.HashSet;

import fr.marin.cyril.mapsapp.database.DatabaseHelper;
import fr.marin.cyril.mapsapp.kml.model.Coordinates;
import fr.marin.cyril.mapsapp.kml.model.Placemark;

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
    private static final int MIN_VALUE = 0;
    private static final int MAX_VALUE = 1;
    private static final int SEARCH_AREA_LATERAL_KM = 5;
    private static final int SEARCH_AREA_FRONT_KM = 100;
    private static final double AZIMUTH_ACCURACY = 1d;
    private static final double EARTH_RADIUS = 6371d;

    private final Context context;
    private final DatabaseHelper db;

    // Observateur
    private LatLng oLatLng;
    private double oElevation;
    private double oAzimuth;
    private double oPitch;

    public ARPeakFinder(Context context) {
        this.context = context;
        this.db = new DatabaseHelper(context);
    }

    public static double[] getAzimuthAccuracy(double azimuth) {
        double[] minMax = new double[]{azimuth - AZIMUTH_ACCURACY, azimuth + AZIMUTH_ACCURACY};
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
        double brng = Math.toRadians(oAzimuth + bearing);
        double lat = Math.toRadians(oLatLng.latitude);
        double lng = Math.toRadians(oLatLng.longitude);

        double lat2 = Math.toDegrees(Math.asin(Math.sin(lat) * Math.cos(dist) + Math.cos(lat) * Math.sin(dist) * Math.cos(brng)));
        double lng2 = lng + Math.atan2(Math.sin(brng) * Math.sin(dist) * Math.cos(lat), Math.cos(dist) - Math.sin(lat) * Math.sin(lat2));
        lng2 = Math.toDegrees((lng2 + 3 * Math.PI) % (2 * Math.PI) - Math.PI);

        return new LatLng(lat2, lng2);
    }

    private Area getSearchArea() {
        LatLng left = getLatLngFromDistanceAndBearing(SEARCH_AREA_LATERAL_KM, -90);
        LatLng right = getLatLngFromDistanceAndBearing(SEARCH_AREA_LATERAL_KM, 90);
        LatLng front = getLatLngFromDistanceAndBearing(SEARCH_AREA_FRONT_KM, 0);

        LatLng southWest;
        LatLng northEast;

        if (left.latitude > right.latitude && oLatLng.latitude > front.latitude) // on regarde en haut à droite
        {
            southWest = new LatLng(right.latitude, left.longitude);
            northEast = front;
        } else if (left.latitude > right.latitude && oLatLng.latitude < front.latitude) // on regarde en bas a droite
        {
            southWest = new LatLng(front.latitude, right.longitude);
            northEast = new LatLng(left.latitude, front.longitude);
        } else if (left.latitude < right.latitude && oLatLng.latitude > front.latitude) // on regarde en haut a gauche
        {
            southWest = new LatLng(left.latitude, front.longitude);
            northEast = new LatLng(front.latitude, right.longitude);
        } else // on regarde en bas a gauche
        {
            southWest = front;
            northEast = new LatLng(right.latitude, left.longitude);
        }

        return new Area(northEast, southWest);
    }

    public Collection<Placemark> getMatchingPlacemark() {
        HashSet<Placemark> matchingPlacemark = new HashSet<>();

        for (Placemark p : db.findPlacemarkInArea(getSearchArea())) {
            double theoricalAzimuth = getTheoricalAzimuth(p.getCoordinates());
            if (isMatchingAccuracy(theoricalAzimuth)) {
                matchingPlacemark.add(p);
            }
        }

        return matchingPlacemark;
    }

    /**
     * @param targetTheoreticalAzimuth
     * @return
     */
    public boolean isMatchingAccuracy(double targetTheoreticalAzimuth) {
        double[] minMax = ARPeakFinder.getAzimuthAccuracy(targetTheoreticalAzimuth);
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
