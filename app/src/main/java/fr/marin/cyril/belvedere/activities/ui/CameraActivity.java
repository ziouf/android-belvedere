package fr.marin.cyril.belvedere.activities.ui;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.activities.CompassActivity;
import fr.marin.cyril.belvedere.camera.Camera;
import fr.marin.cyril.belvedere.kml.model.Placemark;
import fr.marin.cyril.belvedere.tools.ARPeakFinder;
import fr.marin.cyril.belvedere.tools.Utils;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CameraActivity extends CompassActivity {
    private static final String TAG = "CameraActivity";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ScheduledExecutorService mScheduler = Executors.newScheduledThreadPool(1);
    TextView peak_info_tv;
    private Camera camera;
    private ScheduledFuture arTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate UI
        setContentView(R.layout.activity_camera);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        this.portrait = false;

        // Init Camera
        this.camera = Camera.getCameraInstance(this);
        peak_info_tv = (TextView) findViewById(R.id.peak_info_tv);

        this.setOnCompasEvent(new CompassActivity.CompasEventListener() {
            @Override
            public void onSensorChanged(float[] data) {
                updateTextView();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Hide action bar
        ActionBar actionBar = getActionBar();
        if (actionBar != null) actionBar.hide();

        // Configuration du mode immersif
        getWindow().getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);

        // Resume Camera
        this.camera.resume();

        arTask = mScheduler.scheduleAtFixedRate(new ARTask(), 0, 125, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Pause Camera
        this.camera.pause();

        if (arTask != null) arTask.cancel(true);
    }

    private void updateTextView() {
        String s = "Lat : %s | Lng : %s | Alt : %.0fm\nAzimuth : %.2f deg (%s)\nPitch : %.2f deg";
        TextView cameraTextView = (TextView) findViewById(R.id.debug_camera_tv);
        if (cameraTextView == null) return;

        double lat = location.getLatitude();
        double lng = location.getLongitude();
        double alt = location.getAltitude();
        float azimuth = location.getExtras().getFloat(CompassActivity.KEY_AZIMUTH);
        float pitch = location.getExtras().getFloat(CompassActivity.KEY_PITCH);

        cameraTextView.setText(String.format(Locale.getDefault(), s, lat, lng,
                alt, azimuth, Utils.getDirectionFromMinus180to180Degrees(azimuth), pitch));
    }

    private class ARTask implements Runnable {
        private ARPeakFinder ar = new ARPeakFinder(CameraActivity.this);
        private double matchLevel;
        private float distance;
        private Placemark nearest = null;

        @Override
        public void run() {
            matchLevel = Float.MAX_VALUE;
            ar.setObserverAzimuth(CameraActivity.this.getAzimuth());
            ar.setObserverLocation(CameraActivity.this.location);

            Collection<Placemark> placemarks = ar.getMatchingPlacemark();
            if (placemarks.size() == 0) return;

            for (Placemark p : placemarks) {
                if (p.getMatchLevel() < matchLevel) {
                    Log.i(TAG, "ARTask : new nearest Placemark : " + p.getTitle());
                    matchLevel = p.getMatchLevel();
                    distance = Utils.getDistanceBetween(location, p.getCoordinates().getLatLng());
                    nearest = p;
                }
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    peak_info_tv.setText(String.format(Locale.getDefault(), "[%s : %.2f km]", nearest.getTitle(), distance / 1000f));
                }
            });
        }
    }
}