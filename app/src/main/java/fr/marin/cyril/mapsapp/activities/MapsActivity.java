package fr.marin.cyril.mapsapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import fr.marin.cyril.mapsapp.R;
import fr.marin.cyril.mapsapp.database.DatabaseHelper;
import fr.marin.cyril.mapsapp.kml.model.Placemark;
import fr.marin.cyril.mapsapp.services.Compass;
import fr.marin.cyril.mapsapp.tools.Area;

public class MapsActivity extends FragmentActivity
        implements LocationListener, OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleMap.OnMapLoadedCallback, GoogleMap.OnCameraChangeListener {

    private static final int LOCATION_UPDATE_TIME = 1000;
    private static final int LOCATION_UPDATE_DISTANCE = 10;

    private final DatabaseHelper db = new DatabaseHelper(MapsActivity.this);
    private final Criteria locationCriteria = new Criteria();

    private boolean firstLocationChange = true;
    private Location location;
    private LocationManager locationManager;

    private Compass compass;
    private GoogleMap mMap;

    private Marker myLocationMarker;
    private Collection<Marker> markersShown;

    /**
     * Retourne true si le téléphone dispose des capteurs suffisants pour utiliser la Réalité Augmentée
     *
     * @return
     */
    private boolean isARCompatible() {
        PackageManager pm = getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)
                && pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);
    }

    private void updateMyLocationMarker(Bundle data) {
        if (data == null || !isARCompatible()) return;
        myLocationMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
        myLocationMarker.setRotation(Float.valueOf((data.getFloat(Compass.AZIMUTH_P) + 360) % 360).longValue());
    }

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
        this.compass = Compass.getInstance(MapsActivity.this);
        this.locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        this.locationCriteria.setAccuracy(Criteria.ACCURACY_COARSE);
        this.locationCriteria.setPowerRequirement(Criteria.POWER_MEDIUM);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            this.location = locationManager.getLastKnownLocation(this.locationManager.getBestProvider(locationCriteria, true));
        }

        //
        this.initFloatingActionButtons();
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.compass.resume();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            this.locationManager.requestLocationUpdates(this.locationManager.getBestProvider(locationCriteria, true), LOCATION_UPDATE_TIME, LOCATION_UPDATE_DISTANCE, this);
        }

        this.centerMapCameraOnMyPosition();
    }

    @Override
    protected void onPause() {
        super.onPause();

        this.compass.pause();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            this.locationManager.removeUpdates(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
        if (this.firstLocationChange) MapsActivity.this.centerMapCameraOnMyPosition();
        this.firstLocationChange = false;

        TextView tv = (TextView) findViewById(R.id.debug_location_info);
        tv.setText(String.format("lat : %s | lng : %s | alt : %s",
                location.getLatitude(), location.getLongitude(), location.getAltitude()));
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

    /**
     *
     */
    private void initFloatingActionButtons() {
        FloatingActionButton cameraButton = (FloatingActionButton) this.findViewById(R.id.camera_button);
        FloatingActionButton myPosButton = (FloatingActionButton) this.findViewById(R.id.myPosition_button);

        if (!isARCompatible()) cameraButton.setVisibility(View.GONE);

        // Désactivation du module AR si api < LOLLIPOP ou si Permission CAMERA  refusée
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)
                || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraButton.setVisibility(View.GONE);
        }

        // Action au click sur le bouton camera
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("", "Click cameraButton");
                startActivity(new Intent(getApplicationContext(), CameraActivity.class));
            }
        });

        // Action au click sur le bouton myPosButton
        myPosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("", "Click myPosButton");
                MapsActivity.this.centerMapCameraOnMyPosition();
            }
        });

        // Add other actions below if needed
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        mMap.setOnMapLoadedCallback(this);
        mMap.setOnCameraChangeListener(this);

        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setRotateGesturesEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        mMap.setInfoWindowAdapter(this.getInfoWindowAdapter());
        mMap.setOnInfoWindowClickListener(this.getOnInfoWindowClickListener());

        if (isARCompatible())
            myLocationMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(0, 0))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_compas_arrow)));

        Runnable updateMyLocationMarker = new Runnable() {
            @Override
            public void run() {
                MapsActivity.this.updateMyLocationMarker(compass.getData());

                TextView tv = (TextView) findViewById(R.id.debug_azimuth_info);
                tv.setText(String.format("azimuth : %s", compass.getData().getFloat(Compass.AZIMUTH_P)));
            }
        };

        this.compass.addTask(updateMyLocationMarker.hashCode(), updateMyLocationMarker);

        this.updateMarkersOnMap();
    }

    /**
     *
     */
    private void centerMapCameraOnMyPosition() {
        if (mMap == null || location == null) return;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
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
                if (marker.getId().equals(myLocationMarker.getId())) return null;

                View v = getLayoutInflater().inflate(R.layout.info_window, null);

                Placemark m = db.findByLatLng(marker.getPosition());

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
                Placemark m = db.findByLatLng(marker.getPosition());
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

        for (Placemark m : db.findInArea(area))
            markersShown.add(mMap.addMarker(m.getMarkerOptions()));
    }
}
