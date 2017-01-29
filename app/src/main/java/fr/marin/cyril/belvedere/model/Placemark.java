package fr.marin.cyril.belvedere.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

/**
 * Created by Cyril on 29/03/2016.
 */
public class Placemark extends RealmObject {

    @PrimaryKey
    private int id;

    private Double latitude;
    private Double longitude;
    private Double elevation;

    private String title;
    private String comment;
    private String wiki_uri;

    @Ignore
    private double matchLevel = 0d;
    @Ignore
    private double distance = 0d;

    public Placemark() {
    }

    public Placemark(int id, String title, String comment, double lat, double lng, double elevation, String wiki_uri) {
        this.id = id;
        this.latitude = lat;
        this.longitude = lng;
        this.elevation = elevation;

        this.title = title;
        this.comment = comment;
        this.wiki_uri = wiki_uri;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getElevation() {
        return elevation;
    }

    public void setElevation(Double elevation) {
        this.elevation = elevation;
    }

    public LatLng getLatLng() {
        return new LatLng(this.latitude, this.longitude);
    }

    public String getElevationString() {
        return elevation + " m";
    }

    public String getTitle() {
        return title;
    }

    public String getWiki_uri() {
        return wiki_uri;
    }

    public String getComment() {
        return comment;
    }

    public double getMatchLevel() {
        return matchLevel;
    }

    public void setMatchLevel(double matchLevel) {
        this.matchLevel = matchLevel;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public MarkerOptions getMarkerOptions() {
        return new MarkerOptions()
                .position(new LatLng(this.latitude, this.longitude))
                .title(this.title)
                .snippet(this.elevation + "m")
                ;
    }
}
