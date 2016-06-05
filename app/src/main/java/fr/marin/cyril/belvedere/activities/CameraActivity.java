package fr.marin.cyril.belvedere.activities;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.camera.Camera;
import fr.marin.cyril.belvedere.model.Placemark;
import fr.marin.cyril.belvedere.services.CompassService;
import fr.marin.cyril.belvedere.services.LocationService;
import fr.marin.cyril.belvedere.tools.ARPeakFinder;
import fr.marin.cyril.belvedere.tools.Orientation;
import fr.marin.cyril.belvedere.views.CompassView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";

    private final Handler handler = new Handler(Looper.getMainLooper());

    private ScheduledExecutorService mScheduler;

    private CompassService compassService;
    private CompassService.CompassEventListener compassEventListener;
    private LocationService locationService;
    private LocationService.LocationEventListener locationEventListener;

    private Camera camera;
    private ImageView peak_thumbnail_img;
    private TextView peak_info_tv;
    private ScheduledFuture arTask = null;
    private ARPeakFinder arPeakFinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");

        // Inflate UI
        setContentView(R.layout.activity_camera);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Init services
        this.compassService = CompassService.getInstance(getApplicationContext());
        this.locationService = LocationService.getInstance(getApplicationContext());

        // Init Camera
        this.camera = Camera.getCameraInstance(this);
        this.peak_thumbnail_img = (ImageView) findViewById(R.id.peak_thumbnail_img);
        this.peak_info_tv = (TextView) findViewById(R.id.peak_info_tv);

        // Init AR
        this.arPeakFinder = ARPeakFinder.getInstance(getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");

        // Hide action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();

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
                        arPeakFinder.updateObserverOrientation(azimuth, pitch);
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
                        arPeakFinder.updateObserverLocation(location);
                    }
                }
        );

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        mScheduler = Executors.newSingleThreadScheduledExecutor();
        Log.i(TAG, "ArTask init");
        arTask = mScheduler.scheduleAtFixedRate(new ARTask(handler, this), 500, 125, TimeUnit.MILLISECONDS);
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
    }

    private void updateCompassView(float azimuth) {
        CompassView compassView = (CompassView) this.findViewById(R.id.camera_compass_view);
        if (compassView != null)
            compassView.updateAzimuthAndRedraw(azimuth);
    }

    private class ARTask implements Runnable {
        private static final String TAG = "ARTask";
        private final Handler handler;
        private final ARPeakFinder ar;

        public ARTask(Handler handler, Context context) {
            this.handler = handler;
            this.ar = new ARPeakFinder(context);

            Log.i(TAG, "Constructor");
        }

        @Override
        public void run() {
            Log.i(TAG, "Run");
            final Placemark placemark = ar.getMatchingPlacemark();

            if (placemark == null)
                Log.d(TAG, "ARTask : new nearest Placemark null");
            else
                Log.d(TAG, "ARTask : new nearest Placemark : " + placemark.getTitle());

            Log.d(TAG, "Send to GUI");
            handler.post(this.updateGUI(placemark));
            Log.i(TAG, "End Run");
        }

        private Runnable updateGUI(final Placemark placemark) {
            return new Runnable() {
                private static final String TAG = "UpdateGUI";

                @Override
                public void run() {
                    Log.i(TAG, "RUN");
                    if (placemark == null) {
                        Log.i(TAG, "Placemark null");
                        peak_thumbnail_img.setVisibility(View.INVISIBLE);
                        peak_info_tv.setVisibility(View.INVISIBLE);
                        return;
                    }

                    // Check si thumbnail != null avant de l'afficher
                    if (placemark.hasThumbnail()) {
                        peak_thumbnail_img.setImageBitmap(placemark.getThumbnail());
                        peak_thumbnail_img.setVisibility(View.VISIBLE);
                    } else {
                        peak_thumbnail_img.setVisibility(View.INVISIBLE);
                    }

                    String s = placemark.getTitle() + "\n" +
                            placemark.getCoordinates().getElevation() + " m";
                    peak_info_tv.setText(s);
                    peak_info_tv.setVisibility(View.VISIBLE);
                }
            };
        }
    }
}
