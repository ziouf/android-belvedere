package fr.marin.cyril.mapsapp.activities.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import fr.marin.cyril.mapsapp.R;
import fr.marin.cyril.mapsapp.activities.CompassFragmentActivity;
import fr.marin.cyril.mapsapp.database.DatabaseHelper;
import fr.marin.cyril.mapsapp.kml.model.Placemark;
import fr.marin.cyril.mapsapp.tools.Area;
import fr.marin.cyril.mapsapp.tools.Utils;

public class MapsActivity extends CompassFragmentActivity
        implements OnMapReadyCallback, GoogleMap.OnMapLoadedCallback, GoogleMap.OnCameraChangeListener {

    private final Collection<Marker> markersShown = new HashSet<>();
    private final DatabaseHelper db = new DatabaseHelper(MapsActivity.this);

    private Marker compassMarker;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.google_maps_fragment);
        mapFragment.getMapAsync(this);

        // Initialisation des FAB
        FloatingActionButton cameraButton = (FloatingActionButton) this.findViewById(R.id.camera_button);
        FloatingActionButton myPosButton = (FloatingActionButton) this.findViewById(R.id.myPosition_button);

        if (!Utils.isCompassAvailable(this)) cameraButton.setVisibility(View.GONE);

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
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(Location location) {
        super.onLocationChanged(location);

        this.compassMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));

        TextView tv = (TextView) findViewById(R.id.debug_location_info);
        tv.setText(String.format("lat : %s | lng : %s | alt : %s",
                location.getLatitude(), location.getLongitude(), location.getAltitude()));
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
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
        mMap.setOnMarkerClickListener(this.getOnMarkerClickListener());
        mMap.setOnInfoWindowClickListener(this.getOnInfoWindowClickListener());

        if (Utils.isCompassAvailable(getApplicationContext())) {
            this.compassMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(location.getLatitude(), location.getLongitude()))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_compas_arrow))
            );

            this.setOnCompasEvent(new CompassFragmentActivity.CompasEventListener() {
                private TextView azimuth_tv = (TextView) findViewById(R.id.debug_azimuth_info);

                @Override
                public void onSensorChanged(float[] data) {
                    float azimuth = getAzimuth();
                    compassMarker.setRotation(azimuth);
                    azimuth_tv.setText(String.format("azimuth : %s°", (int) azimuth));
                }
            });
        }

        this.centerMapCameraOnMyPosition();
        this.updateMarkersOnMap();
    }

    /**
     *
     */
    private void centerMapCameraOnMyPosition() {
        if (mMap == null) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;
        if (location == null)
            location = locationManager.getLastKnownLocation(locationManager.getBestProvider(locationCriteria, true));

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
                if (marker.getId().equals(compassMarker.getId())) return null;

                View v = getLayoutInflater().inflate(R.layout.info_window, null);

                Placemark m = db.findPlacemarkByLatLng(marker.getPosition());

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
                Placemark m = db.findPlacemarkByLatLng(marker.getPosition());
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(m.getUrl())));
            }
        };
    }

    private GoogleMap.OnMarkerClickListener getOnMarkerClickListener() {
        return new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return marker.getId().equals(compassMarker.getId());
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

        for (Placemark m : db.findPlacemarkInArea(area))
            markersShown.add(mMap.addMarker(m.getMarkerOptions()));
    }
}
