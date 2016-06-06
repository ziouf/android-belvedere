package fr.marin.cyril.belvedere.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

import fr.marin.cyril.belvedere.Config;
import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.activities.CameraActivity;
import fr.marin.cyril.belvedere.database.DatabaseHelper;
import fr.marin.cyril.belvedere.model.Area;
import fr.marin.cyril.belvedere.model.Placemark;
import fr.marin.cyril.belvedere.services.CompassService;
import fr.marin.cyril.belvedere.services.LocationService;
import fr.marin.cyril.belvedere.tools.Orientation;

import static fr.marin.cyril.belvedere.services.CompassService.getInstance;

/**
 * Created by cyril on 31/05/16.
 */
public class MapsFragment extends Fragment implements OnMapReadyCallback {
    private static final String TAG = "MapsFragment";
    private static View view;
    private final Collection<Marker> markersShown = new HashSet<>();
    private Marker compassMarker;
    private GoogleMap mMap;
    private DatabaseHelper db;
    private Location location;

    private LocationService locationService;
    private LocationService.LocationEventListener locationEventListener;
    private CompassService compassService;
    private CompassService.CompassEventListener compassServiceEventListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        }

        try {
            view = inflater.inflate(R.layout.fragment_maps, container, false);
        } catch (InflateException e) {
            Log.w(TAG, "Error when inflating UI", e);
        }

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        db = DatabaseHelper.getInstance(getActivity().getApplicationContext());
        locationService = LocationService.getInstance(getActivity());
        compassService = getInstance(getActivity());

        FragmentManager childFragmentManager = getChildFragmentManager();

        // Initialisation du fragment Maps
        SupportMapFragment mapFragment = (SupportMapFragment) childFragmentManager.findFragmentById(R.id.google_maps_fragment);
        if (mapFragment != null)
            mapFragment.getMapAsync(this);

        // Initialisation des FAB
        initFloatingActionButtons();

    }

    private void initFloatingActionButtons() {
        // Initialisation des FAB
        FloatingActionButton cameraButton = (FloatingActionButton) getActivity().findViewById(R.id.camera_button);
        FloatingActionButton myPosButton = (FloatingActionButton) getActivity().findViewById(R.id.myPosition_button);

        // Désactivation du bouton AR si le terminal ne dispose pas des capteurs ou autorisations suffisantes
        PackageManager pm = getActivity().getPackageManager();
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || !pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)
                || !pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)
                || !pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS))
            cameraButton.setVisibility(View.GONE);


        // Action au click sur le bouton camera
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Click cameraButton");
                final Intent intent = new Intent(getActivity().getApplicationContext(), CameraActivity.class);
                intent.putExtra(Config.BundleKeys.LOCATION, MapsFragment.this.location);
                startActivity(intent);
            }
        });

        // Action au click sur le bouton myPosButton
        myPosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Click myPosButton");
                // Demande d'activation des services de geolocalisation si désactivés
                if (!locationService.isLocationServiceEnabled())
                    locationService.askForLocationServiceActivation();
                // Centrage de la vue sur la geolocalisation de l'utilisateur
                centerMapCameraOnMyPosition();
                // Abonnement au trigger de geolocalisation
                locationService.registerLocationUpdates();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // For showing a move to my loction button
        if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            mMap.setMyLocationEnabled(true);

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                MapsFragment.this.updateMarkersOnMap();
            }
        });
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                MapsFragment.this.updateMarkersOnMap();
            }
        });

        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setRotateGesturesEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        mMap.setInfoWindowAdapter(this.getInfoWindowAdapter());
        mMap.setOnMarkerClickListener(this.getOnMarkerClickListener());
        mMap.setOnInfoWindowClickListener(this.getOnInfoWindowClickListener());

        this.updateMarkersOnMap();
        this.centerMapCameraOnMyPosition();

        final PackageManager pm = getActivity().getPackageManager();
        if (compassMarker == null
                && (pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)
                && pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS))) {

            this.compassMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(0f, 0f))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_compas_arrow))
            );

            locationEventListener = locationService.registerLocationEventListener(
                    new LocationService.LocationEventListener() {
                        @Override
                        public void onSensorChanged(Location location) {
                            MapsFragment.this.location = location;
                            compassMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
                        }
                    }
            );

            compassServiceEventListener = compassService.registerCompassEventListener(
                    new CompassService.CompassEventListener() {
                        @Override
                        public void onSensorChanged(float azimuth, float pitch) {
                            if (compassMarker == null || locationService == null) return;

                            GeomagneticField geoField = locationService.getGeoField();
                            if (geoField != null) azimuth += geoField.getDeclination();
                            azimuth += 360;
                            azimuth %= 360;

                            Log.d(TAG, String.format("azimuth : %s° | pitch : %s°", (int) azimuth, (int) pitch));
                            compassMarker.setRotation(azimuth);
                        }
                    });
        }

    }

    /**
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

                final View v = getActivity().getLayoutInflater().inflate(R.layout.maps_info_window, null);
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
     *
     */
    private void centerMapCameraOnMyPosition() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;
        if (mMap == null) return;
        if (locationService.getLocation() == null) return;

        final Location location = locationService.getLocation();
        final LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
    }

    @Override
    public void onPause() {
        super.onPause();
        if (locationService != null) {
            locationService.pause();
            locationService.unRegisterLocationEventListener(locationEventListener);
        }
        if (compassService != null) {
            compassService.pause();
            compassService.unRegisterCompassEventListener(compassServiceEventListener);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (locationService != null) {
            locationService.resume();
            if (locationEventListener != null)
                locationService.registerLocationEventListener(locationEventListener);
        }
        if (compassService != null) {
            compassService.resume();
            compassService.setOrientation(Orientation.PORTRAIT);
            if (compassServiceEventListener != null)
                compassService.registerCompassEventListener(compassServiceEventListener);
        }
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
