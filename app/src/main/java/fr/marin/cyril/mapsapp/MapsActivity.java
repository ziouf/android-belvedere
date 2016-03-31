package fr.marin.cyril.mapsapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.util.Linkify;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import fr.marin.cyril.mapsapp.database.DatabaseHelper;
import fr.marin.cyril.mapsapp.kml.parser.KmlParser;
import fr.marin.cyril.mapsapp.kml.model.MapsMarker;
import fr.marin.cyril.mapsapp.tool.MapArea;

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback, GoogleMap.OnMapLoadedCallback, GoogleMap.OnCameraChangeListener {

    private static final Long LOCATION_REFRESH_TIME = 10000L;
    private static final Float LOCATION_REFRESH_DISTANCE = 25F;

    private LocationManager locationManager;
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

        // Action au click sur le bouton camera
        FloatingActionButton cameraButton = (FloatingActionButton) this.findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MapsActivity.this, CameraActivity.class));
            }
        });
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

        mMap.setMyLocationEnabled(true);
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnCameraChangeListener(this);

        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setRotateGesturesEnabled(false);

        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                View v = getLayoutInflater().inflate(R.layout.info_window, null);

                MapsMarker m = dbHelper.findByLatLng(marker.getPosition());

                TextView tvTitle = (TextView) v.findViewById(R.id.iw_title);
                TextView tvAltitude = (TextView) v.findViewById(R.id.iw_altitude);

                tvTitle.setText(m.getTitle());
                tvAltitude.setText(m.getCoordinates().getElevationString());

                return v;
            }
        });

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                MapsMarker m = dbHelper.findByLatLng(marker.getPosition());
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(m.getUrl())));
            }
        });

        this.updateMarkersOnMap(mMap);

        // Obtention du gestionnaire de Geolocalisation
        LocationListener mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        this.locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            this.locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    LOCATION_REFRESH_TIME, LOCATION_REFRESH_DISTANCE, mLocationListener);

    }

    /**
     * Callback triggered when the map is loaded
     */
    @Override
    public void onMapLoaded() {
        this.updateMarkersOnMap(mMap);
    }

    /**
     * Callback triggered when the camera is moved or zoomed
     * @param cameraPosition
     */
    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        this.updateMarkersOnMap(mMap);
    }

    /**
     *
     * @param mMap
     */
    private void updateMarkersOnMap(GoogleMap mMap) {
        MapArea area = new MapArea(mMap.getProjection().getVisibleRegion());

        this.removeOffScreenMarkers(area);

        for (MapsMarker m : this.dbHelper.findInArea(area))
            if (area.isInArea(m.getCoordinates().getLatLng()))
                markersShown.add(mMap.addMarker(m.getMarkerOptions()));
    }

    /**
     *
     * @param area
     */
    private void removeOffScreenMarkers(MapArea area) {
        Collection<Marker> toRemove = new ArrayList<>();

        if (markersShown.size() > 0)
        for (Marker m : markersShown)
            if (!area.isInArea(m.getPosition())) {
                toRemove.add(m);
                m.remove();
            }


        markersShown.removeAll(toRemove);
    }

}
