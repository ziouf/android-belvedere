package fr.marin.cyril.belvedere.sparql.model;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by cyril on 08/05/16.
 */
public class PeakInfo {

    private String name;
    private LatLng location;
    private double elevation;

    public PeakInfo(String name, LatLng location, double elevation) {
        this.name = name;
        this.location = location;
        this.elevation = elevation;
    }

    public PeakInfo(float elevation) {
        this.elevation = elevation;
    }


    public String getName() {
        return name;
    }

    public LatLng getLocation() {
        return location;
    }

    public double getElevation() {
        return elevation;
    }
}
