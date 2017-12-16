package fr.marin.cyril.belvedere.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.annimon.stream.Stream;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import fr.marin.cyril.belvedere.Config;
import fr.marin.cyril.belvedere.Preferences;
import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.activities.CameraActivity;
import fr.marin.cyril.belvedere.enums.Orientation;
import fr.marin.cyril.belvedere.model.Area;
import fr.marin.cyril.belvedere.model.Placemark;
import fr.marin.cyril.belvedere.services.CompassService;
import fr.marin.cyril.belvedere.services.ILocationEventListener;
import fr.marin.cyril.belvedere.services.ILocationService;
import fr.marin.cyril.belvedere.services.impl.LocationServiceFactory;
import fr.marin.cyril.belvedere.tools.MapsMarkerManager;
import fr.marin.cyril.belvedere.tools.Objects;
import fr.marin.cyril.belvedere.tools.PlacemarkSearchAdapter;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by cyril on 31/05/16.
 */
public class MapsFragment
        extends Fragment
        implements OnMapReadyCallback {

    private static final String TAG = MapsFragment.class.getSimpleName();

    private View rootView;
    private Marker compassMarker;
    private MapsMarkerManager markerManager;

    private Realm realm;
    private GoogleMap mMap;
    private Location location;

    private ILocationService locationService;
    private ILocationEventListener locationEventListener;

    private CompassService compassService;
    private CompassService.CompassEventListener compassServiceEventListener;

    // Cette classe ne fonctionne pas en singleton à cause du fragment GoogleMaps
    public static MapsFragment getInstance() {
        return new MapsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.realm = Realm.getDefaultInstance();
    }

    @Override
    public void onDestroy() {
        this.realm.close();
        super.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (Objects.isNull(this.rootView))
            this.rootView = inflater.inflate(R.layout.fragment_maps, container, false);

        return this.initActionBar(rootView);
    }

    private View initActionBar(View rootView) {
        if (Objects.isNull(rootView)) return null;

        final AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (Objects.nonNull(activity)) {
            // Set a Toolbar to replace the ActionBar.
            activity.setSupportActionBar(rootView.findViewById(R.id.toolbar));

            // Configuration de l'Actionbar
            final ActionBar actionBar = activity.getSupportActionBar();
            if (Objects.nonNull(actionBar)) {
                actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_USE_LOGO);
            }
        }

        final AutoCompleteTextView searchQuery = rootView.findViewById(R.id.search_edittext);
        searchQuery.setAdapter(new PlacemarkSearchAdapter(getActivity()));
        searchQuery.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Placemark p = (Placemark) parent.getAdapter().getItem(position);
                markerManager.putIfNotPresent(p);
                // Zoom on placemark
                final LatLng latLng = p.getLatLng();
                if (Objects.nonNull(latLng)) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12), new GoogleMap.CancelableCallback() {
                        @Override
                        public void onFinish() {
                            markerManager.getMarker(p).showInfoWindow();
                        }

                        @Override
                        public void onCancel() {

                        }
                    });
                }
                // Close keyboard
                final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (Objects.nonNull(imm)) {
                    imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
                }
            }
        });

        final ImageButton cancel = rootView.findViewById(R.id.search_cancel);
        cancel.setOnClickListener(view -> searchQuery.setText(null));

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        locationService = LocationServiceFactory.getLocationService(getActivity());
        compassService = CompassService.getInstance(getActivity());

        // Initialisation du fragment Maps
        final SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_maps_fragment);
        mapFragment.getMapAsync(this);

        // Initialisation des FAB
        this.initFloatingActionButtons();
    }

    private void initFloatingActionButtons() {
        // Initialisation des FAB
        final FloatingActionButton cameraButton = getActivity().findViewById(R.id.camera_button);
        final FloatingActionButton myPosButton = getActivity().findViewById(R.id.myPosition_button);

        // Désactivation du bouton AR si le terminal ne dispose pas des capteurs ou autorisations suffisantes
        final PackageManager pm = getActivity().getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)
                || !pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)
                || !pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS))
            cameraButton.setVisibility(View.GONE);

        // Action au click sur le bouton camera
        cameraButton.setOnClickListener(v -> {
            Log.i(TAG, "Click cameraButton");
            final Intent intent = new Intent(getActivity().getApplicationContext(), CameraActivity.class);
            intent.putExtra(Config.BundleKeys.LOCATION, MapsFragment.this.location);
            startActivity(intent);
        });

        // Action au click sur le bouton myPosButton
        myPosButton.setOnClickListener(v -> {
            Log.i(TAG, "Click myPosButton");
            // Centrage de la vue sur la geolocalisation de l'utilisateur
            centerMapCameraOnMyPosition();
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        markerManager = new MapsMarkerManager(mMap);
        realm.addChangeListener(realm -> MapsFragment.this.updateMarkersOnMap());

        // For showing a move to my loction button
        if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            mMap.setMyLocationEnabled(true);

        // Update markers on Map
        mMap.setOnCameraIdleListener(MapsFragment.this::updateMarkersOnMap);

        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setRotateGesturesEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        mMap.setInfoWindowAdapter(this.getInfoWindowAdapter());
        mMap.setOnMarkerClickListener(this.getOnMarkerClickListener());
        mMap.setOnInfoWindowClickListener(this.getOnInfoWindowClickListener());

        this.centerMapCameraOnMyPosition();

        final PackageManager pm = getActivity().getPackageManager();
        if (Objects.isNull(compassMarker)
                && (pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)
                && pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS))) {

            this.compassMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(0f, 0f))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_compas_arrow))
            );

            locationEventListener = locationService.registerLocationEventListener(
                    new ILocationEventListener() {
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
                            if (Objects.isNull(compassMarker) || Objects.isNull(locationService))
                                return;

                            if (Objects.nonNull(location)) {
                                final GeomagneticField geoField = new GeomagneticField(
                                        (float) location.getLatitude(),
                                        (float) location.getLongitude(),
                                        (float) location.getAltitude(),
                                        location.getTime()
                                );
                                azimuth += geoField.getDeclination();
                            }
                            azimuth += 360;
                            azimuth %= 360;
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
                if (Objects.isNull(marker))
                    return null;
                if (Objects.nonNull(compassMarker) && marker.getId().equals(compassMarker.getId()))
                    return null;

                final Placemark p = markerManager.getPlacemark(marker);

                final View v = getActivity().getLayoutInflater().inflate(R.layout.maps_info_window, null);
                final TextView tvTitle = v.findViewById(R.id.iw_title);
                final TextView tvAltitude = v.findViewById(R.id.iw_altitude);

                tvTitle.setText(p.getTitle());
                tvAltitude.setText(p.getElevationString());

                return v;
            }
        };
    }

    /**
     * @return
     */
    private GoogleMap.OnInfoWindowClickListener getOnInfoWindowClickListener() {
        return marker -> {
            final Placemark p = markerManager.getPlacemark(marker);
            if (Objects.nonNull(p.getArticle()) && !p.getArticle().isEmpty())
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(p.getArticle())));
            else
                Toast.makeText(MapsFragment.this.getActivity(), R.string.toast_wiki_url_not_found, Toast.LENGTH_SHORT).show();
        };
    }

    private GoogleMap.OnMarkerClickListener getOnMarkerClickListener() {
        return marker -> (Objects.nonNull(compassMarker)) && marker.getId().equals(compassMarker.getId());
    }

    /**
     *
     */
    private void centerMapCameraOnMyPosition() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;
        if (Objects.isNull(mMap)) return;
        if (Objects.isNull(location)) return;

        final LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Objects.nonNull(locationService)) {
            locationService.pause();
            locationService.unRegisterLocationEventListener(locationEventListener);
        }
        if (Objects.nonNull(compassService)) {
            compassService.pause();
            compassService.unRegisterCompassEventListener(compassServiceEventListener);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Objects.nonNull(locationService)) {
            locationService.resume();
            if (Objects.nonNull(locationEventListener))
                locationService.registerLocationEventListener(locationEventListener);
        }
        if (Objects.nonNull(compassService)) {
            compassService.resume();
            compassService.setOrientation(Orientation.PORTRAIT);
            if (Objects.nonNull(compassServiceEventListener))
                compassService.registerCompassEventListener(compassServiceEventListener);
        }
    }

    private void updateMarkersOnMap() {
        final Area area = new Area(mMap.getProjection().getVisibleRegion());
        final RealmResults<Placemark> results = realm.where(Placemark.class)
                .between("latitude", area.getBottom(), area.getTop())
                .between("longitude", area.getLeft(), area.getRight())
                .findAllSortedAsync("elevation", Sort.DESCENDING);
        results.addChangeListener(this::onNextPlacemarks);
        results.load();
    }

    private void onNextPlacemarks(RealmResults<Placemark> placemarks) {
        this.markerManager.putIfNotPresent(
                Stream.range(0, Math.min(placemarks.size(), Preferences.MAX_ON_MAP))
                        .map(placemarks::get)
                        .map(realm::copyFromRealm)
                        .toList()
        );
    }
}
