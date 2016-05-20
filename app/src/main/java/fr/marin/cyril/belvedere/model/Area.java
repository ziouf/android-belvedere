package fr.marin.cyril.belvedere.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.VisibleRegion;

/**
 * Created by Cyril on 29/03/2016.
 */
public class Area {
    private LatLng northeast;
    private LatLng southwest;

    private double left;
    private double top;
    private double right;
    private double bottom;

    public Area(LatLng northeast, LatLng southwest) {
        this.northeast = northeast;
        this.southwest = southwest;
        this.init();
    }

    public Area(VisibleRegion vr) {
        this.northeast = vr.latLngBounds.northeast;
        this.southwest = vr.latLngBounds.southwest;
        this.init();
    }

    private void init() {
        this.left = southwest.longitude;
        this.top = northeast.latitude;
        this.right = northeast.longitude;
        this.bottom = southwest.latitude;
    }

    public Boolean isInArea(LatLng latLng) {
        return latLng.latitude < this.top && latLng.latitude > this.bottom
                && latLng.longitude > this.left && latLng.longitude < this.right;
    }

    public LatLng getNortheast() {
        return northeast;
    }

    public LatLng getSouthwest() {
        return southwest;
    }

    public double getLeft() {
        return left;
    }

    public double getTop() {
        return top;
    }

    public double getRight() {
        return right;
    }

    public double getBottom() {
        return bottom;
    }
}
