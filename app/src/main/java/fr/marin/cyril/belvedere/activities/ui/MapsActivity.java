package fr.marin.cyril.belvedere.activities.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
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

import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.activities.CompassActivity;
import fr.marin.cyril.belvedere.database.DatabaseHelper;
import fr.marin.cyril.belvedere.model.Area;
import fr.marin.cyril.belvedere.model.Placemark;

public class MapsActivity extends CompassActivity
        implements OnMapReadyCallback, GoogleMap.OnMapLoadedCallback, GoogleMap.OnCameraChangeListener {

    private final Collection<Marker> markersShown = new HashSet<>();

    private Marker compassMarker;
    private GoogleMap mMap;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        db = DatabaseHelper.getInstance(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.google_maps_fragment);
        mapFragment.getMapAsync(this);

        // Initialisation des FAB
        FloatingActionButton cameraButton = (FloatingActionButton) this.findViewById(R.id.camera_button);
        FloatingActionButton myPosButton = (FloatingActionButton) this.findViewById(R.id.myPosition_button);

        if (!isCompassCompatible()) cameraButton.setVisibility(View.GONE);

        // Désactivation du module AR si le terminal de dispose pas de CAMERA ou si Permission CAMERA refusée
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
                // Demande d'activation des services de geolocalisation si désactivés
                if (!MapsActivity.this.isLocationServiceEnabled())
                    MapsActivity.this.askForLocationServiceActivation();
                // Centrage de la vue sur la geolocalisation de l'utilisateur
                MapsActivity.this.centerMapCameraOnMyPosition();
                // Abonnement au trigger de geolocalisation
                MapsActivity.this.registerLocationUpdates();
            }
        });

        // Add other actions below if needed
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isLocationServiceEnabled())
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

        if (compassMarker == null)
            this.initCompassMarkerIcon();
        else
            compassMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));

        TextView tv = (TextView) findViewById(R.id.debug_location_info);
        tv.setText(String.format("lat : %s | lng : %s | alt : %s",
                location.getLatitude(), location.getLongitude(), location.getAltitude()));

        this.centerMapCameraOnMyPosition();
        this.removeLocationUpdates();
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

        this.updateMarkersOnMap();

        this.centerMapCameraOnMyPosition();
        if (compassMarker == null && isLocationServiceEnabled())
            this.initCompassMarkerIcon();
    }

    private void initCompassMarkerIcon() {
        if (!isCompassCompatible()) return;
        if (location == null) return;

        this.compassMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(location.getLatitude(), location.getLongitude()))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_compas_arrow))
        );

        this.registerCompasEventListener(new CompassActivity.CompasEventListener() {
            private TextView azimuth_tv = (TextView) findViewById(R.id.debug_azimuth_info);

            @Override
            public void onSensorChanged(float[] data) {
                float azimuth = getAzimuth();
                compassMarker.setRotation(azimuth);
                azimuth_tv.setText(String.format("azimuth : %s°", (int) azimuth));
            }
        });
    }

    /**
     *
     */
    private void centerMapCameraOnMyPosition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;
        if (mMap == null) return;
        if (location == null) return;

        final LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
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
                if (compassMarker != null && marker.getId().equals(compassMarker.getId()))
                    return null;

                final Placemark m = db.findPlacemarkByLatLng(marker.getPosition());

                final View v = getLayoutInflater().inflate(R.layout.maps_info_window, null);
                final ImageView imgThumbnail = (ImageView) v.findViewById(R.id.iw_thumbnail);
                final TextView tvTitle = (TextView) v.findViewById(R.id.iw_title);
                final TextView tvAltitude = (TextView) v.findViewById(R.id.iw_altitude);
                final TextView tvComment = (TextView) v.findViewById(R.id.iw_comment);

                if (m.getThumbnailArray() != null) {
                    Bitmap thumbnail = m.getThumbnail();
                    imgThumbnail.setImageBitmap(thumbnail);
                    imgThumbnail.setVisibility(View.VISIBLE);
                    imgThumbnail.setMaxWidth(thumbnail.getWidth());
                    imgThumbnail.setMaxHeight(thumbnail.getHeight());
                }

                tvTitle.setText(m.getTitle());
                tvAltitude.setText(m.getCoordinates().getElevationString());
                tvComment.setText(m.getComment());
                tvComment.setMovementMethod(new ScrollingMovementMethod());

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
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(m.getWiki_uri())));
            }
        };
    }

    private GoogleMap.OnMarkerClickListener getOnMarkerClickListener() {
        return new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return (compassMarker != null) && marker.getId().equals(compassMarker.getId());
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

        for (Placemark m : db.findPlacemarkInArea(area)) {
            markersShown.add(mMap.addMarker(m.getMarkerOptions()));
        }
    }
}
