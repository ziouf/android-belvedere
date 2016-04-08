package fr.marin.cyril.mapsapp;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import fr.marin.cyril.mapsapp.database.DatabaseService;
import fr.marin.cyril.mapsapp.kml.model.Placemark;
import fr.marin.cyril.mapsapp.tool.Area;

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback,
            GoogleMap.OnMapLoadedCallback, GoogleMap.OnCameraChangeListener {

    private static final int PERMISSION_CODE = 1;

    private GoogleMap mMap;
    private LocationManager locationManager;
    private Collection<Marker> markersShown;

    private boolean databaseServiceBound = false;
    private DatabaseService databaseService;
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection databaseServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DatabaseService.DatabaseServiceBinder binder = (DatabaseService.DatabaseServiceBinder) service;
            databaseService = binder.getService();
            databaseServiceBound = databaseService != null;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            databaseServiceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Init
        this.markersShown = new HashSet<>();

        // Init sensor providers
        this.locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Bind database services
        this.bindService(new Intent(getApplicationContext(), DatabaseService.class),
                databaseServiceConnection, Context.BIND_AUTO_CREATE);

        this.initOnClickActions();
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.centerMapCameraOnMyPosition();
    }

    @Override
    protected void onPause() {

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (databaseServiceBound)
            this.unbindService(databaseServiceConnection);

        super.onDestroy();
    }

    /**
     *
     */
    private void initOnClickActions() {
        FloatingActionButton cameraButton = (FloatingActionButton) this.findViewById(R.id.camera_button);

        // Désactivation du module AR si api < LOLLIPOP
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            cameraButton.hide();
            return;
        }

        // Action au click sur le bouton camera
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), CameraActivity.class));
            }
        });

        // Add other actions below if needed
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                this.initMaps();
            } else {
                Toast.makeText(this, "Permission refusée", Toast.LENGTH_SHORT).show();
            }
        }

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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request LOCATION permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, PERMISSION_CODE);
            }

        } else {

            this.initMaps();

        }
    }

    /**
     * Initialisation de la carte Google Maps
     */
    private void initMaps() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        mMap.setMyLocationEnabled(true);

        this.centerMapCameraOnMyPosition();

        mMap.setOnMapLoadedCallback(this);
        mMap.setOnCameraChangeListener(this);

        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setRotateGesturesEnabled(false);

        mMap.setInfoWindowAdapter(this.getInfoWindowAdapter());
        mMap.setOnInfoWindowClickListener(this.getOnInfoWindowClickListener());

        this.updateMarkersOnMap();
    }

    /**
     *
     */
    private void centerMapCameraOnMyPosition() {
        if (mMap == null) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        Location l = this.locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (l != null) {
            LatLng latLng = new LatLng(l.getLatitude(), l.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
        }
    }

    /**
     *
     * @return
     */
    private GoogleMap.InfoWindowAdapter getInfoWindowAdapter() {
        return new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                View v = getLayoutInflater().inflate(R.layout.info_window, null);

                Placemark m = databaseService.findByLatLng(marker.getPosition());

                TextView tvTitle = (TextView) v.findViewById(R.id.iw_title);
                TextView tvAltitude = (TextView) v.findViewById(R.id.iw_altitude);

                tvTitle.setText(m.getTitle());
                tvAltitude.setText(m.getCoordinates().getElevationString());

                return v;
            }
        };
    }

    /**
     *
     * @return
     */
    private GoogleMap.OnInfoWindowClickListener getOnInfoWindowClickListener() {
        return new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                Placemark m = databaseService.findByLatLng(marker.getPosition());
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(m.getUrl())));
            }
        };
    }

    /**
     * Callback triggered when the map is loaded
     */
    @Override
    public void onMapLoaded() {
        this.updateMarkersOnMap();
    }

    /**
     * Callback triggered when the camera is moved or zoomed
     * @param cameraPosition
     */
    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        this.updateMarkersOnMap();
    }

    /**
     *
     */
    private void updateMarkersOnMap() {
        if(!databaseServiceBound) return;

        Area area = new Area(mMap.getProjection().getVisibleRegion());

        if (markersShown.size() > 0) {
            Collection<Marker> toRemove = new ArrayList<>();
            for (Marker m : markersShown)
                if (!area.isInArea(m.getPosition())) {
                    toRemove.add(m);
                    m.remove();
                }
            markersShown.removeAll(toRemove);
        }

        for (Placemark m : databaseService.findInArea(area))
            if (area.isInArea(m.getCoordinates().getLatLng()))
                markersShown.add(mMap.addMarker(m.getMarkerOptions()));
    }

}
