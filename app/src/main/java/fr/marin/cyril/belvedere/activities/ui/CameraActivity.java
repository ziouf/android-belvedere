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
import android.view.WindowManager;
import android.widget.ImageView;
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
import fr.marin.cyril.belvedere.model.Placemark;
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

    private Camera camera;
    private ImageView peak_thumbnail_img;
    private TextView peak_info_tv;
    private ScheduledFuture arTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate UI
        setContentView(R.layout.activity_camera);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.portrait = false;
        this.locateOnlyOnce = true;

        // Init Camera
        this.camera = Camera.getCameraInstance(this);
        this.peak_thumbnail_img = (ImageView) findViewById(R.id.peak_thumbnail_img);
        this.peak_info_tv = (TextView) findViewById(R.id.peak_info_tv);
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

        //
        this.registerCompasEventListener(new CompassActivity.CompasEventListener() {
            @Override
            public void onSensorChanged(float[] data) {
                updateDebugTextView();
            }
        });

        arTask = mScheduler.scheduleAtFixedRate(new ARTask(handler), 0, 125, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Pause Camera
        this.camera.pause();

        if (arTask != null) arTask.cancel(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void updateDebugTextView() {
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
        private final Handler handler;
        private final ARPeakFinder ar;

        private float distance;
        private double matchLevel;
        private Placemark nearest = null;

        public ARTask(Handler handler) {
            this.handler = handler;
            this.ar = new ARPeakFinder(CameraActivity.this);
        }

        @Override
        public void run() {
            matchLevel = Float.MAX_VALUE;
            ar.setObserverAzimuth(CameraActivity.this.getAzimuth());
            ar.setObserverLocation(CameraActivity.this.location);

            Collection<Placemark> placemarks = ar.getMatchingPlacemark();
            if (placemarks.size() == 0) return;

            for (Placemark p : placemarks) {
                if (p.getMatchLevel() < matchLevel) {
                    Log.d(TAG, "ARTask : new nearest Placemark : " + p.getTitle());
                    matchLevel = p.getMatchLevel();
                    distance = Utils.getDistanceBetween(location, p.getCoordinates().getLatLng());
                    nearest = p;
                }
            }
            handler.post(this.updateGUI());
        }

        private Runnable updateGUI() {
            return new Runnable() {
                @Override
                public void run() {
                    // Check si thumbnail != null avant de l'afficher
                    if (nearest.getThumbnailArray() != null)
                        peak_thumbnail_img.setImageBitmap(nearest.getThmubnail());

                    String s = nearest.getTitle() + "\n" +
                            nearest.getCoordinates().getElevation() + " m";
                    peak_info_tv.setText(s);
                }
            };
        }
    }
}
