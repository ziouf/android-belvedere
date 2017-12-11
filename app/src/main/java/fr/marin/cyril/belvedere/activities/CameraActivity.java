package fr.marin.cyril.belvedere.activities;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.annimon.stream.Stream;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import fr.marin.cyril.belvedere.Config;
import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.camera.Camera;
import fr.marin.cyril.belvedere.enums.Orientation;
import fr.marin.cyril.belvedere.model.Area;
import fr.marin.cyril.belvedere.model.Placemark;
import fr.marin.cyril.belvedere.services.CompassService;
import fr.marin.cyril.belvedere.services.LocationService;
import fr.marin.cyril.belvedere.tools.ARPeakFinder;
import fr.marin.cyril.belvedere.tools.Objects;
import fr.marin.cyril.belvedere.views.CompassView;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class CameraActivity extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = CameraActivity.class.getSimpleName();

    private static final int PERMISSIONS_CODE = new Random(0).nextInt(Short.MAX_VALUE);
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.CAMERA
    };

    private CompassService compassService;
    private CompassService.CompassEventListener compassEventListener;

    private LocationService locationService;
    private LocationService.LocationEventListener locationEventListener;

    private Camera camera;
    private TextView peak_info_tv;

    private Location oLocation;
    private float oAzimuth;
    private float oPitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");

        // Get location from MainActivity
        this.oLocation = getIntent().getParcelableExtra(Config.BundleKeys.LOCATION);

        // Inflate UI
        setContentView(R.layout.activity_camera);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Init services
        this.compassService = CompassService.getInstance(getApplicationContext());
        this.locationService = LocationService.getInstance(getApplicationContext());

        // Init Camera
        this.camera = Camera.getCameraInstance(this);
        this.peak_info_tv = findViewById(R.id.peak_info_tv);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Check permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Request CAMERA permissions
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSIONS_CODE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");

        // Hide action bar
        final ActionBar actionBar = getSupportActionBar();
        if (Objects.nonNull(actionBar) && actionBar.isShowing()) actionBar.hide();

        // Configuration du mode immersif
        getWindow().getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);

        // Resume Camera
        Log.i(TAG, "Camera Service init");
        this.camera.resume();

        // Compass Service
        Log.i(TAG, "Compass Service init");
        this.compassService.setOrientation(Orientation.PAYSAGE);
        this.compassService.resume();
        this.compassEventListener = compassService.registerCompassEventListener(
                new CompassService.CompassEventListener() {
                    @Override
                    public void onSensorChanged(float azimuth, float pitch) {
                        updateCompassView(azimuth);
                        oAzimuth = azimuth;
                        oPitch = pitch;

                        if (Objects.isNull(oLocation) || Objects.isNull(oPitch))
                            return;

                        try (final Realm realm = Realm.getDefaultInstance()) {
                            final Area area = new ARPeakFinder(oLocation, oAzimuth, oPitch).getSearchArea();
                            final RealmResults<Placemark> results = realm.where(Placemark.class)
                                    .between("latitude", area.getBottom(), area.getTop())
                                    .between("longitude", area.getLeft(), area.getRight())
                                    .findAllSortedAsync("elevation", Sort.DESCENDING);

                            results.addChangeListener(CameraActivity.this::onNextPlacemarks);
                            results.load();
                        }
                    }
                }
        );

        // Location Service
        Log.i(TAG, "Location Service init");
        this.locationService.resume();
        this.locationEventListener = locationService.registerLocationEventListener(
                new LocationService.LocationEventListener() {
                    @Override
                    public void onSensorChanged(Location location) {
                        locationService.removeLocationUpdates();
                        oLocation = location;
                    }
                }
        );
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Pause Camera
        this.camera.pause();
        // Location Service
        this.locationService.pause();
        this.locationService.unRegisterLocationEventListener(locationEventListener);
        // Compass Service
        this.compassService.pause();
        this.compassService.unRegisterCompassEventListener(compassEventListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_CODE && grantResults.length == PERMISSIONS.length) {
            if (grantResults[Arrays.asList(PERMISSIONS).indexOf(Manifest.permission.CAMERA)] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "La permission CAMERA est necessaire", Toast.LENGTH_SHORT).show();
                CameraActivity.this.finish();
            }
        }
    }

    private void updateCompassView(float azimuth) {
        final CompassView compassView = findViewById(R.id.camera_compass_view);
        if (Objects.nonNull(compassView)) compassView.updateAzimuthAndRedraw(azimuth);
    }

    private void onNextPlacemarks(Collection<Placemark> placemarks) {
        final ARPeakFinder pf = new ARPeakFinder(oLocation, oAzimuth, oPitch);

        if (placemarks.isEmpty()) {
            Log.d(TAG + ".onNextPlacemarks()", "Placemark empty");
            peak_info_tv.setVisibility(View.INVISIBLE);

        } else {
            Stream.of(placemarks)
                    .filter(pf::isMatchingPlacemark)
                    .limit(1)
                    .forEach(placemark -> {
                        Log.i(TAG + ".onNextPlacemarks()", "Placemark not empty : " + placemark.getTitle());

                        peak_info_tv.setText(String.format("%s\n%s m", placemark.getTitle(), placemark.getElevation()));
                        peak_info_tv.setVisibility(View.VISIBLE);
                    });
        }
    }

}
