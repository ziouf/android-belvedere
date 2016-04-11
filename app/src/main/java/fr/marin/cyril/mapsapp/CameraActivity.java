package fr.marin.cyril.mapsapp;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;

import fr.marin.cyril.mapsapp.database.DatabaseService;

// TODO : Implémentation de SensorService
/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CameraActivity extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback, TextureView.SurfaceTextureListener {

    private static final int PERMISSIONS_CODE = 2;

    private DatabaseService.DatabaseServiceConnection databaseServiceConnection = new DatabaseService.DatabaseServiceConnection();
    //private SensorService.SensorServiceConnection sensorServiceConnection = new SensorService.SensorServiceConnection();

    private Location location;
    private SensorService.SensorServiceListener compasEventListener = new SensorService.SensorServiceListener() {
        @Override
        public void onChange(float[] values) {
            float Gx = values[0];
            float Gy = values[1];
            float Gz = values[2];
            String s = "Lat : %s | Lng : %s | Alt : %s\nHeading : %sx %sy %sz\nBearing : %s";

            TextView cameraTextView = (TextView) findViewById(R.id.cameraTextView);
            if (cameraTextView != null)
                cameraTextView.setText(String.format(s, location.getLatitude(), location.getLongitude(), location.getAltitude(),
                        Gx, Gy, Gz, location.getBearing()));
        }
    };

    private Size previewSize;
    private TextureView textureView;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder previewBuilder;
    private CameraCaptureSession previewSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        this.previewSize = new Size(size.y, size.x);

        // Check for permissions
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                String[] permissions = new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CAMERA
                };
                ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_CODE);
            }
        } else {

            this.init();

        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Configuration du mode immersif
        this.initUI();

        // Bind services
        this.bindService(new Intent(getApplicationContext(), DatabaseService.class),
                databaseServiceConnection, Context.BIND_AUTO_CREATE);
        //this.bindService(new Intent(getApplicationContext(), SensorService.class),
        //        sensorServiceConnection, Context.BIND_AUTO_CREATE);

        // Re-ouverture de la camera
        if (this.textureView != null && this.textureView.isAvailable()) {
            this.textureView.setSurfaceTextureListener(this);
            this.onSurfaceTextureAvailable(this.textureView.getSurfaceTexture(),
                    this.textureView.getWidth(), this.textureView.getHeight());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unbind services
        if (databaseServiceConnection.isBound()) this.unbindService(databaseServiceConnection);
        //if (sensorServiceConnection.isBound()) this.unbindService(sensorServiceConnection);

        // Close Camera
        if (cameraDevice != null) {
            cameraDevice.close();

            ImageView camera_loading_splash = (ImageView) findViewById(R.id.camera_loading);
            if (camera_loading_splash != null)
                camera_loading_splash.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_CODE) {
            if (grantResults.length == 2
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                this.init();

            } else {
                Toast.makeText(this, "Permission refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void init() {
        // Configuration du mode immersif
        this.initUI();

        // Inflate UI
        setContentView(R.layout.activity_camera);

        // Initialisation de l'activity
        this.initActivity();
    }

    private void initUI() {
        // Hide action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();

        getWindow().getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void initActivity() {
        // Init Camera view
        this.textureView = (TextureView) findViewById(R.id.textureView);
        if (this.textureView != null) this.textureView.setSurfaceTextureListener(this);

        // Check location permissions
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;
        // Get location
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        this.location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (ActivityCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            return;

        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.openCamera(cameraId, this.getCameraDeviceStateCallback(), null);
        }
        catch (CameraAccessException e) {

        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        this.onSurfaceTextureAvailable(surface, width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private CameraDevice.StateCallback getCameraDeviceStateCallback() {
        return new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice camera) {
                cameraDevice = camera;
                SurfaceTexture texture = textureView.getSurfaceTexture();
                if (texture == null) return;

                try {
                    previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                } catch (CameraAccessException e) {

                }

                texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
                Surface surface = new Surface(texture);
                previewBuilder.addTarget(surface);

                try {
                    cameraDevice.createCaptureSession(Collections.singletonList(surface), getPreviewSessionStateCallback(), null);
                } catch (CameraAccessException e) {

                }
            }

            @Override
            public void onClosed(CameraDevice camera) {
                super.onClosed(camera);
                previewSession.close();
                previewSession = null;
                cameraDevice = null;
                previewBuilder = null;
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                Toast.makeText(getApplicationContext(), "CameraDevice disconnected", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(CameraDevice camera, int error) {
                Toast.makeText(getApplicationContext(), "CameraDevice error", Toast.LENGTH_SHORT).show();
            }
        };
    }

    private CameraCaptureSession.StateCallback getPreviewSessionStateCallback() {
        return new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                previewSession = session;

                previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

                HandlerThread backgroundThread = new HandlerThread("CameraPreview");
                backgroundThread.start();

                Handler backgroundHandler = new Handler(backgroundThread.getLooper());

                try {
                    previewSession.setRepeatingRequest(previewBuilder.build(), null, backgroundHandler);
                } catch (CameraAccessException e) {

                }

                ImageView camera_loading_splash = (ImageView) findViewById(R.id.camera_loading);
                if (camera_loading_splash != null)
                    camera_loading_splash.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session) {
                Toast.makeText(getApplicationContext(), "CaptureSession configuration failed", Toast.LENGTH_SHORT).show();
            }
        };
    }
}
