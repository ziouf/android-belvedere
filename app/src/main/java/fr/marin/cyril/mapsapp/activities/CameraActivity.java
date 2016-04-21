package fr.marin.cyril.mapsapp.activities;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

import fr.marin.cyril.mapsapp.R;
import fr.marin.cyril.mapsapp.camera.Camera;
import fr.marin.cyril.mapsapp.services.Messages;
import fr.marin.cyril.mapsapp.services.SensorService;
import fr.marin.cyril.mapsapp.tools.Utils;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CameraActivity extends Activity {

    private Camera camera;
    private Location location;
    private Messenger mMessenger = new Messenger(new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Messages.MSG_LOCATION_UPDATE:
                    CameraActivity.this.location = (Location) msg.obj;
                    break;
                default:
                    super.handleMessage(msg);
            }
            if (CameraActivity.this.location != null)
                CameraActivity.this.updateTextView();
        }
    });

    private SensorService.SensorServiceConnection sensorServiceConnection =
            new SensorService.SensorServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    super.onServiceConnected(name, service);
                    Messages.sendNewMessage(this.getServiceMessenger(), Messages.MSG_REGISTER_CLIENT,
                            SensorService.LANDSCAPE, 0, null, mMessenger);
                }
            };

    private void updateTextView() {
        String s = "Lat : %s | Lng : %s | Alt : %.0fm\nAzimuth : %.2f deg (%s)\nPitch : %.2f deg";
        TextView cameraTextView = (TextView) findViewById(R.id.cameraTextView);
        if (cameraTextView == null) return;

        double lat = location.getLatitude();
        double lng = location.getLongitude();
        double alt = location.getAltitude();
        float azimuth = location.getExtras().getFloat(SensorService.AZIMUTH);
        float pitch = location.getExtras().getFloat(SensorService.PITCH);

        cameraTextView.setText(String.format(Locale.getDefault(), s, lat, lng,
                alt, azimuth, Utils.getDirectionFromDegrees(azimuth), pitch));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate UI
        setContentView(R.layout.activity_camera);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // Init Camera
        this.camera = Camera.getCameraInstance(this);
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

        // Bind services
        if (sensorServiceConnection != null)
            this.bindService(new Intent(getApplicationContext(), SensorService.class),
                    sensorServiceConnection, Context.BIND_AUTO_CREATE);

        // Resume Camera
        this.camera.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unbind services
        if (sensorServiceConnection.isBound()) {
            Messages.sendNewMessage(sensorServiceConnection.getServiceMessenger(),
                    Messages.MSG_UNREGISTER_CLIENT, 0, 0, null, mMessenger);
            this.unbindService(sensorServiceConnection);
        }

        // Pause Camera
        this.camera.pause();
    }

}
