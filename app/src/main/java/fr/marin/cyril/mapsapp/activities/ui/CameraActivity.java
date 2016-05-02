package fr.marin.cyril.mapsapp.activities.ui;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.content.pm.ActivityInfo;
import android.hardware.SensorEvent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.Collection;
import java.util.Locale;

import fr.marin.cyril.mapsapp.R;
import fr.marin.cyril.mapsapp.activities.CompassActivity;
import fr.marin.cyril.mapsapp.camera.Camera;
import fr.marin.cyril.mapsapp.kml.model.Placemark;
import fr.marin.cyril.mapsapp.tools.ARPeakFinder;
import fr.marin.cyril.mapsapp.tools.Utils;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CameraActivity extends CompassActivity {
    private static final String TAG = "CameraActivity";

    private ARPeakFinder ar;
    private Camera camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate UI
        setContentView(R.layout.activity_camera);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        this.portrait = false;

        // Init Camera
        this.camera = Camera.getCameraInstance(this);
        this.ar = new ARPeakFinder(this);

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


        this.ar.setObserverLocation(location);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Pause Camera
        this.camera.pause();
    }

    @Override
    public void onLocationChanged(Location location) {
        super.onLocationChanged(location);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        super.onSensorChanged(event);

        this.ar.setObserverAzimuth(this.getAzimuth());

        Runnable r = new Runnable() {
            @Override
            public void run() {
                Collection<Placemark> placemarks = CameraActivity.this.ar.getMatchingPlacemark();
                if (placemarks.size() == 0) return;

                StringBuilder sb = new StringBuilder();
                sb.append("Sie = ").append(placemarks.size()).append(" [");
                for (Placemark p : placemarks)
                    sb.append(p.getTitle()).append(",");
                sb.append("]");

                Log.i(TAG, sb.toString());
            }
        };

        r.run();
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
}
