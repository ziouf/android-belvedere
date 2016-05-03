package fr.marin.cyril.belvedere.kml.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by cscm6014 on 29/03/2016.
 */
public class Placemark {
    private Coordinates coordinates;
    private String title;
    private String url;
    private double matchLevel = 0d;

    public Placemark() {
    }

    public Placemark(Coordinates coordinates, String title) {
        this.coordinates = coordinates;
        this.title = title;
    }

    public Placemark(double lat, double lng, String title) {
        this.coordinates = new Coordinates(new LatLng(lat, lng));
        this.title = title;
    }

    public Placemark(LatLng latLng, String title) {
        this.coordinates = new Coordinates(latLng);
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public double getMatchLevel() {
        return matchLevel;
    }

    public void setMatchLevel(double matchLevel) {
        this.matchLevel = matchLevel;
    }

    public MarkerOptions getMarkerOptions() {
        return new MarkerOptions()
                .position(this.getCoordinates().getLatLng())
                .title(this.getTitle())
                .snippet(this.getCoordinates().getElevation() + "m")
                ;
    }
}
