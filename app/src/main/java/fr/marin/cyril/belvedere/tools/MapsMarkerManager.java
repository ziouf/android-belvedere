package fr.marin.cyril.belvedere.tools;

import android.util.SparseArray;

import com.annimon.stream.Stream;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

import java.util.Collection;

import fr.marin.cyril.belvedere.model.Placemark;

/**
 * Created by cyril on 10/12/17.
 */
public class MapsMarkerManager {
    private final SparseArray<Marker> markers = new SparseArray<>();
    private final SparseArray<Placemark> placemarks = new SparseArray<>();
    private final GoogleMap mMap;

    public MapsMarkerManager(GoogleMap mMap) {
        this.mMap = mMap;
    }

    private void putMarker(Placemark placemark) {
        final Marker marker = mMap.addMarker(placemark.getMarkerOptions());
        markers.put(placemark.hashCode(), marker);
        placemarks.put(marker.hashCode(), placemark);
    }

    private void putAll(Collection<Placemark> placemarkCollection) {
        Stream.of(placemarkCollection).forEach(this::putMarker);
    }

    public void putIfNotPresent(Placemark placemark) {
        this.putIfNotPresent(Stream.of(placemark));
    }

    public void putIfNotPresent(Collection<Placemark> placemarkCollection) {
        this.putIfNotPresent(Stream.of(placemarkCollection));
    }

    private void putIfNotPresent(Stream<Placemark> placemarkStream) {
        placemarkStream
                .filter(p -> Objects.isNull(markers.get(p.hashCode())))
                .forEach(this::putMarker);
    }

    public void clearAndPutAll(Collection<Placemark> placemarkCollection) {
        this.removeOthers(placemarkCollection);
        this.putIfNotPresent(Stream.of(placemarkCollection));
    }

    private void remove(Placemark placemark) {
        if (Objects.isNull(placemark)) return;
        final Marker marker = markers.get(placemark.hashCode());
        this.remove(placemark, marker);
    }

    private void remove(Marker marker) {
        if (Objects.isNull(marker)) return;
        final Placemark placemark = placemarks.get(marker.hashCode());
        this.remove(placemark, marker);
    }

    private void remove(Placemark placemark, Marker marker) {
        marker.remove();
        markers.remove(placemark.hashCode());
        placemarks.remove(marker.hashCode());
    }

    private void removeOthers(Collection<Placemark> placemarkCollection) {
        Stream.range(0, placemarks.size())
                .map(placemarks::valueAt)
                .filterNot(placemarkCollection::contains)
                .forEach(this::remove);
    }

    private void clear() {
        Stream.range(0, markers.size())
                .map(markers::valueAt)
                .forEach(this::remove);
    }

    public Placemark getPlacemark(Marker marker) {
        return placemarks.get(marker.hashCode());
    }

    public Marker getMarker(Placemark placemark) {
        return markers.get(placemark.hashCode());
    }
}
