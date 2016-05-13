package fr.marin.cyril.belvedere.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by cscm6014 on 29/03/2016.
 */
public class Placemark {
    private String title;
    private Coordinates coordinates;
    private String wiki_uri;
    private String thumbnail_uri;

    private byte[] thumbnail = null;
    private double matchLevel = 0d;
    private double distance = 0d;

    public Placemark(String title, double lat, double lng, double elevation, String wiki_uri) {
        this.coordinates = new Coordinates(new LatLng(lat, lng), elevation);
        this.title = title;
        this.wiki_uri = wiki_uri;
    }

    public Placemark(String title, double lat, double lng, double elevation, String wiki_uri, String thumbnail_uri) {
        this.title = title;
        this.coordinates = new Coordinates(new LatLng(lat, lng), elevation);
        this.wiki_uri = wiki_uri;
        this.thumbnail_uri = thumbnail_uri;
    }

    public Placemark(String title, double lat, double lng, double elevation, String wiki_uri, String thumbnail_uri, byte[] thumbnail) {
        this.coordinates = new Coordinates(new LatLng(lat, lng), elevation);
        this.title = title;
        this.wiki_uri = wiki_uri;
        this.thumbnail_uri = thumbnail_uri;
        this.thumbnail = thumbnail;
    }

    public String getTitle() {
        return title;
    }

    public String getWiki_uri() {
        return wiki_uri;
    }

    public String getThumbnail_uri() {
        return thumbnail_uri;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }


    public Bitmap getThumbnail() {
        return BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length);
    }

    public void setThumbnail(byte[] thumbnail) {
        this.thumbnail = thumbnail;
    }

    public byte[] getThumbnailArray() {
        return this.thumbnail;
    }

    public boolean hasThumbnail() {
        return this.thumbnail != null;
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
                .position(this.getCoordinates().getLatLng())
                .title(this.getTitle())
                .snippet(this.getCoordinates().getElevation() + "m")
                ;
    }
}
