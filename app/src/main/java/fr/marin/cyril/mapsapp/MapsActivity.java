package fr.marin.cyril.mapsapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

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

import fr.marin.cyril.mapsapp.database.DatabaseHelper;
import fr.marin.cyril.mapsapp.kml.model.Placemark;
import fr.marin.cyril.mapsapp.services.Messages;
import fr.marin.cyril.mapsapp.tool.Area;

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleMap.OnMapLoadedCallback, GoogleMap.OnCameraChangeListener {

    private SensorService.SensorServiceConnection sensorServiceConnection = new SensorService.SensorServiceConnection(this.mMessenger);
    private DatabaseHelper db = new DatabaseHelper(MapsActivity.this);
    private GoogleMap mMap;
    private Location myLocation;
    private final Messenger mMessenger = new Messenger(new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Messages.MSG_LOCATION_UPDATE:
                    if (msg.obj == null) return;
                    MapsActivity.this.myLocation = (Location) msg.obj;

                    TextView tv = (TextView) findViewById(R.id.location_info);
                    tv.setText(String.format("lat : %s | lng : %s | alt : %s", myLocation.getLatitude(), myLocation.getLongitude(), myLocation.getAltitude()));
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    });
    private LocationManager locationManager;
    private Collection<Marker> markersShown;

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

        this.initOnClickActions();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Bind services
        if (sensorServiceConnection != null)
            this.bindService(new Intent(getApplicationContext(), SensorService.class),
                    sensorServiceConnection, Context.BIND_AUTO_CREATE);

        this.centerMapCameraOnMyPosition();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unbind services
        if (sensorServiceConnection.isBound()) {
            Messages.sendNewMessage(sensorServiceConnection.getServiceMessenger(),
                    Messages.MSG_UNREGISTER_CLIENT, null, mMessenger);
            this.unbindService(sensorServiceConnection);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     *
     */
    private void initOnClickActions() {
        FloatingActionButton cameraButton = (FloatingActionButton) this.findViewById(R.id.camera_button);
        FloatingActionButton myPosition = (FloatingActionButton) this.findViewById(R.id.myPosition_button);

        // Désactivation du module AR si api < LOLLIPOP ou si Permission CAMERA  refusée
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
                || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            cameraButton.hide();
        } else {
            // Action au click sur le bouton camera
            cameraButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(getApplicationContext(), CameraActivity.class));
                }
            });
        }

        // Action au click sur le bouton myPosition
        myPosition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            this.centerMapCameraOnMyPosition();
        }

        mMap.setOnMapLoadedCallback(this);
        mMap.setOnCameraChangeListener(this);

        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setRotateGesturesEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

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

        if (this.myLocation == null)
            this.myLocation = this.locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (this.myLocation != null) {
            LatLng latLng = new LatLng(this.myLocation.getLatitude(), this.myLocation.getLongitude());
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
