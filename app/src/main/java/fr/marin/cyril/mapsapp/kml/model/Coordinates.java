package fr.marin.cyril.mapsapp.kml.model;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by cscm6014 on 30/03/2016.
 */
public class Coordinates {
    private LatLng latLng;
    private Integer elevation = 0;

    public Coordinates() {
    }

    public Coordinates(LatLng latLng) {
        this.latLng = latLng;
    }

    public Coordinates(LatLng latLng, Integer elevation) {
        this.latLng = latLng;
        this.elevation = elevation;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }

    public Integer getElevation() {
        return elevation;
    }

    public void setElevation(Integer elevation) {
        this.elevation = elevation;
    }
}
