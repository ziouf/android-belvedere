package fr.marin.cyril.mapsapp;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import fr.marin.cyril.mapsapp.database.DatabaseHelper;
import fr.marin.cyril.mapsapp.kml.parser.KmlParser;
import fr.marin.cyril.mapsapp.kml.model.MapsMarker;
import fr.marin.cyril.mapsapp.tool.MapArea;

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback, GoogleMap.OnMapLoadedCallback, GoogleMap.OnCameraChangeListener{

    private LatLng myPosition = new LatLng(45.46472,5.92528);
    private GoogleMap mMap;
    private Collection<InputStream> kmlfiles;
    private Collection<Marker> markersShown;

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        this.dbHelper = new DatabaseHelper(this);

        this.kmlfiles = new HashSet<>();
        this.kmlfiles.add(this.getResources().openRawResource(R.raw.sommets_des_alpes_francaises));

        this.markersShown = new HashSet<>();

        this.dbHelper.insertAllIfEmpty(new KmlParser().parseAll(this.kmlfiles));

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markersShown or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnCameraChangeListener(this);

        this.updateMarkersOnMap(mMap);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPosition, 11));
    }

    @Override
    public void onMapLoaded() {
        this.updateMarkersOnMap(mMap);
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        this.updateMarkersOnMap(mMap);
    }

    private void updateMarkersOnMap(GoogleMap mMap) {
        MapArea area = new MapArea(mMap.getProjection().getVisibleRegion());

        this.removeOffScreenMarkers(area);

        for (MapsMarker m : this.dbHelper.findInArea(area))
            if (area.isInArea(m.getCoordinates().getLatLng()))
                markersShown.add(mMap.addMarker(m.getMarkerOptions()));
    }

    private void removeOffScreenMarkers(MapArea area) {
        Collection<Marker> toRemove = new ArrayList<>();

        for (Marker m : markersShown)
            if (!area.isInArea(m.getPosition())) {
                toRemove.add(m);
                m.remove();
            }


        markersShown.removeAll(toRemove);
    }

}
