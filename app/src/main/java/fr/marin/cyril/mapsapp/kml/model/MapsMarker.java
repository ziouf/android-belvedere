package fr.marin.cyril.mapsapp.kml.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by cscm6014 on 29/03/2016.
 */
public class MapsMarker {
    private Coordinates coordinates;
    private String title;
    private String description;



    public MapsMarker() {
    }

    public MapsMarker(Coordinates coordinates, String title) {
        this.coordinates = coordinates;
        this.title = title;
    }

    public MapsMarker(double lat, double lng, String title) {
        this.coordinates = new Coordinates(new LatLng(lat, lng));
        this.title = title;
    }

    public MapsMarker(LatLng latLng, String title) {
        this.coordinates = new Coordinates(latLng);
        this.title = title;
    }

    public MapsMarker(LatLng latLng, String title, String description) {
        this.coordinates = new Coordinates(latLng);
        this.title = title;
        this.description = description;
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

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public MarkerOptions getMarkerOptions() {
        return new MarkerOptions()
                .position(this.getCoordinates().getLatLng())
                .title(this.getTitle())
                .snippet(this.getCoordinates().getElevation() + "m");
    }
}
