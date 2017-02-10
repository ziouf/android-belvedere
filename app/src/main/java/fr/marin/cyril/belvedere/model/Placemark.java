package fr.marin.cyril.belvedere.model;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import fr.marin.cyril.belvedere.R;
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
    private int id;

    @Required
    private Double latitude;
    @Required
    private Double longitude;
    @Required
    private Double elevation;

    @Required
    private String title;

    private String comment;

    @Required
    private String type;

    @Ignore
    private double matchLevel = 0d;
    @Ignore
    private double distance = 0d;

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

    public void setTitle(String title) {
        this.title = title;
    }

    public PlacemarkType getType() {
        return PlacemarkType.valueOf(type);
    }

    public void setType(PlacemarkType type) {
        this.type = type.name();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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
        MarkerOptions markerOptions = new MarkerOptions()
                .position(new LatLng(this.latitude, this.longitude))
                .title(this.title)
                .snippet(this.elevation + "m");

        switch (this.getType()) {
            case MOUNTAIN:
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.google_maps_marker_mount));
                break;
            case PEAK:
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.google_maps_marker_peak));
                break;
        }

        return markerOptions;
    }
}
