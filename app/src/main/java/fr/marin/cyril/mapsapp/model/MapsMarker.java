package fr.marin.cyril.mapsapp.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by cscm6014 on 29/03/2016.
 */
public class MapsMarker {
    private LatLng latLng;
    private Integer elevation = 0;
    private String title;
    private String description;

    public MapsMarker() {
    }

    public MapsMarker(double lat, double lng, String title) {
        this.latLng = new LatLng(lat, lng);
        this.title = title;
    }

    public MapsMarker(LatLng latLng, String title) {
        this.latLng = latLng;
        this.title = title;
    }

    public MapsMarker(LatLng latLng, String title, String description) {
        this.latLng = latLng;
        this.title = title;
        this.description = description;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getElevation() {
        return elevation;
    }

    public void setElevation(Integer elevation) {
        this.elevation = elevation;
    }

    public MarkerOptions getMarkerOptions() {
        return new MarkerOptions()
                .position(this.getLatLng())
                .title(this.getTitle())
                .snippet(this.getElevation() + "m");
    }
}
