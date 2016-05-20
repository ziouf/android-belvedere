package fr.marin.cyril.belvedere.model;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Cyril on 30/03/2016.
 */
public class Coordinates {
    private LatLng latLng;
    private Double elevation = 0d;

    public Coordinates() {
    }

    public Coordinates(LatLng latLng) {
        this.latLng = latLng;
    }

    public Coordinates(LatLng latLng, Double elevation) {
        this.latLng = latLng;
        this.elevation = elevation;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }

    public Double getElevation() {
        return elevation;
    }

    public void setElevation(Double elevation) {
        this.elevation = elevation;
    }

    public String getElevationString() {
        return elevation.toString() + " m";
    }
}
