package fr.marin.cyril.belvedere.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import fr.marin.cyril.belvedere.Config;
import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.camera.Camera;
import fr.marin.cyril.belvedere.database.RealmDbHelper;
import fr.marin.cyril.belvedere.model.Placemark;
import fr.marin.cyril.belvedere.services.CompassService;
import fr.marin.cyril.belvedere.services.LocationService;
import fr.marin.cyril.belvedere.tools.ARPeakFinder;
import fr.marin.cyril.belvedere.tools.Orientation;
import fr.marin.cyril.belvedere.views.CompassView;
import io.realm.Realm;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CameraActivity extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = CameraActivity.class.getSimpleName();

    private static final int PERMISSIONS_CODE = UUID.randomUUID().hashCode();
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.CAMERA
    };

    private final Handler handler = new Handler(Looper.getMainLooper());

    private ScheduledExecutorService mScheduler;

    private Realm realm;

    private CompassService compassService;
    private CompassService.CompassEventListener compassEventListener;
    private LocationService locationService;
    private LocationService.LocationEventListener locationEventListener;

    private Camera camera;
    private ImageView peak_thumbnail_img;
    private TextView peak_info_tv;

    private Location oLocation;
    private float oAzimuth;
    private float oPitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");

        this.realm = Realm.getDefaultInstance();

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
        this.peak_thumbnail_img = findViewById(R.id.peak_thumbnail_img);
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
        if (actionBar != null && actionBar.isShowing()) actionBar.hide();

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
    protected void onPostResume() {
        super.onPostResume();
        // Init AR
        this.mScheduler = Executors.newScheduledThreadPool(1);
        Log.i(TAG, "ArTask init");
        mScheduler.scheduleAtFixedRate(new ARTask(handler, this), 0, 25, TimeUnit.MILLISECONDS);
        Log.i(TAG, "ArTask scheduled");
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

        //if (arTask != null) arTask.cancel(true);
        mScheduler.shutdownNow();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.realm.close();
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
        if (compassView != null)
            compassView.updateAzimuthAndRedraw(azimuth);
    }

    private Runnable updateGUI(final Placemark placemark) {
        return new Runnable() {
            private static final String TAG = "UpdateGUI";

            @Override
            public void run() {
                if (placemark == null) {
                    Log.d(TAG, "Placemark null");
                    peak_thumbnail_img.setVisibility(View.INVISIBLE);
                    peak_info_tv.setVisibility(View.INVISIBLE);
                    return;
                }
                Log.i(TAG, "Placemark not null : " + placemark.getTitle());

                peak_info_tv.setText(String.format("%s\n%s m", placemark.getTitle(), placemark.getElevation()));
                peak_info_tv.setVisibility(View.VISIBLE);
            }
        };
    }

    private class ARTask implements Runnable {
        private static final String TAG = "ARTask";
        private final Handler handler;
        private final Context context;

        public ARTask(Handler handler, Context context) {
            this.handler = handler;
            this.context = context;
        }

        @Override
        public void run() {
            RealmDbHelper realm = RealmDbHelper.getInstance();
            Log.d(TAG, "Run");
            if (oLocation == null) return;

            Log.d(TAG, "Run with Azimuth : " + oAzimuth + " Pitch : " + oPitch);
            final ARPeakFinder ar = new ARPeakFinder(context, oLocation, oAzimuth, oPitch);
            ar.setPlacemarks(realm.findInArea(ar.getSearchArea(), Placemark.class));

            Placemark placemark = realm.copyFromRealm(ar.getMatchingPlacemark());

            Log.d(TAG, "Send to GUI");
            handler.post(updateGUI(placemark));
            Log.d(TAG, "End Run");
            realm.close();
        }
    }
}
