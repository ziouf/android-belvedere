package fr.marin.cyril.belvedere.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.VisibleRegion;

/**
 * Created by Cyril on 29/03/2016.
 */
public class Area implements Parcelable {
    public static final Creator<Area> CREATOR = new Creator<Area>() {
        @Override
        public Area createFromParcel(Parcel in) {
            return new Area(in);
        }

        @Override
        public Area[] newArray(int size) {
            return new Area[size];
        }
    };
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

    private Area(Parcel in) {
        northeast = in.readParcelable(LatLng.class.getClassLoader());
        southwest = in.readParcelable(LatLng.class.getClassLoader());
        left = in.readDouble();
        top = in.readDouble();
        right = in.readDouble();
        bottom = in.readDouble();
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeDouble(left);
        parcel.writeDouble(top);
        parcel.writeDouble(right);
        parcel.writeDouble(bottom);
        parcel.writeParcelable(northeast, PARCELABLE_WRITE_RETURN_VALUE);
        parcel.writeParcelable(southwest, PARCELABLE_WRITE_RETURN_VALUE);
    }

    @Override
    public String toString() {
        return "Area{" +
                "left=" + left +
                ", top=" + top +
                ", right=" + right +
                ", bottom=" + bottom +
                '}';
    }
}
