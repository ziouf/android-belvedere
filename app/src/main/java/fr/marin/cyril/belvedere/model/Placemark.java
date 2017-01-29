package fr.marin.cyril.belvedere.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import io.realm.RealmObject;

/**
 * Created by Cyril on 29/03/2016.
 */
public class Placemark extends RealmObject {

    private double latitude;
    private double longitude;
    private double elevation;

    private String title;
    private String comment;
    private String wiki_uri;
    private String thumbnail_uri;

    private byte[] thumbnail = null;
    private double matchLevel = 0d;
    private double distance = 0d;

    public Placemark() {
    }

    public Placemark(String title, String comment, double lat, double lng, double elevation, String wiki_uri, String thumbnail_uri) {
        this.latitude = lat;
        this.longitude = lng;
        this.elevation = elevation;
        this.title = title;
        this.comment = comment;
        this.wiki_uri = wiki_uri;
        this.thumbnail_uri = thumbnail_uri;
    }

    public Placemark(String title, String comment, double lat, double lng, double elevation, String wiki_uri, String thumbnail_uri, byte[] thumbnail) {
        this.latitude = lat;
        this.longitude = lng;
        this.elevation = elevation;
        this.title = title;
        this.comment = comment;
        this.wiki_uri = wiki_uri;
        this.thumbnail_uri = thumbnail_uri;
        this.thumbnail = thumbnail;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public LatLng getLatLng() {
        return new LatLng(this.latitude, this.longitude);
    }

    public double getElevation() {
        return elevation;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
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

    public String getThumbnail_uri() {
        return thumbnail_uri;
    }

    public String getComment() {
        return comment;
    }

    public Bitmap getThumbnail() {
        if (thumbnail == null) return null;
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
                .position(new LatLng(this.latitude, this.longitude))
                .title(this.title)
                .snippet(this.elevation + "m")
                ;
    }

    public void downloadThumbnail() {
        try (InputStream is = getRedirectedConnection(new URL(this.thumbnail_uri)).getInputStream()) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
            BitmapFactory.decodeStream(is).compress(Bitmap.CompressFormat.WEBP, 25, baos);
            this.thumbnail = baos.toByteArray();
        } catch (IOException e) {
            Log.e(this.getClass().getSimpleName(), "Exception lors du téléchargement du thmubnail", e);
        }
    }

    private HttpURLConnection getRedirectedConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        int status = connection.getResponseCode();
        while (status == HttpURLConnection.HTTP_SEE_OTHER
                || status == HttpURLConnection.HTTP_MOVED_PERM
                || status == HttpURLConnection.HTTP_MOVED_TEMP) {
            url = new URL(connection.getHeaderField("Location"));
            // fermeture de la connexion actuelle
            connection.disconnect();
            connection = (HttpURLConnection) url.openConnection();
            status = connection.getResponseCode();
        }
        return connection;
    }
}
