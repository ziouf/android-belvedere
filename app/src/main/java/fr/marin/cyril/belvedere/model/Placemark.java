package fr.marin.cyril.belvedere.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.tools.Objects;
import io.realm.RealmModel;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import io.realm.annotations.Required;

/**
 * Created by Cyril on 29/03/2016.
 */
@RealmClass
public class Placemark implements RealmModel {

    @PrimaryKey
    @Required
    @JsonAlias("m")
    private String id;

    @Required
    @JsonAlias("lat")
    private Double latitude;
    @Required
    @JsonAlias("lon")
    private Double longitude;
    @Required
    @JsonAlias("altitude")
    private Double elevation = 0d;

    @Required
    @JsonAlias("mLabel")
    private String title = "";

    private String comment = "";

    @JsonAlias("article")
    private String article;

    @Ignore
    private double matchLevel = 0d;
    @Ignore
    private double distance = 0d;

    public String getId() {
        return id;
    }

    public void setId(String id) {
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
        return Character.toUpperCase(title.charAt(0)) + title.substring(1);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getArticle() {
        return article;
    }

    public void setArticle(String article) {
        this.article = article;
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
                .position(this.getLatLng())
                .title(this.title)
                .snippet(this.getElevationString())
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.google_maps_marker_peak))
                ;
    }

    @Override
    public String toString() {
        return title;
    }

    public String toStringLong() {
        return "Placemark{" +
                "id='" + id + '\'' +
                ", latitude=" + (Objects.isNull(latitude) ? "null" : latitude) +
                ", longitude=" + (Objects.isNull(longitude) ? "null" : longitude) +
                ", elevation=" + (Objects.isNull(elevation) ? "null" : elevation) +
                ", title='" + (Objects.isNull(title) ? "null" : title) + '\'' +
                ", comment='" + (Objects.isNull(comment) ? "null" : comment) + '\'' +
                '}';
    }
}
