package fr.marin.cyril.mapsapp;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.location.LocationManager;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Arrays;

import fr.marin.cyril.mapsapp.database.DatabaseService;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CameraActivity extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int PERMISSIONS_CODE = 1;

    private GeomagneticField geomagneticField;
    private Sensor compas;
    private SensorManager sensorManager;
    private Location location;
    private LocationManager locationManager;

    private boolean databaseServiceBound = false;
    private DatabaseService databaseService;
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection databaseServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DatabaseService.DatabaseServiceBinder binder = (DatabaseService.DatabaseServiceBinder) service;
            databaseService = binder.getService();
            databaseServiceBound = databaseService != null;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            databaseServiceBound = false;
        }
    };

    private TextureView textureView;
    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            if (ActivityCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                return;

            CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

            try {
                cameraManager.openCamera(cameraManager.getCameraIdList()[0], stateCallback, null);
            }
            catch (CameraAccessException e) {

            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };;

    private Size previewSize = new Size(1920, 1080);
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder previewBuilder;
    private CameraCaptureSession previewSession;
    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture == null) return;

            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface surface = new Surface(texture);

            try {
                previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            } catch (CameraAccessException e) {

            }

            previewBuilder.addTarget(surface);

            try {
                cameraDevice.createCaptureSession(Arrays.asList(surface), previewStateCallback, null);
            } catch (CameraAccessException e) {

            }
        }

        @Override
        public void onClosed(CameraDevice camera) {
            super.onClosed(camera);
            cameraDevice = null;
        }

        @Override
        public void onDisconnected(CameraDevice camera) {

        }

        @Override
        public void onError(CameraDevice camera, int error) {

        }
    };
    private CameraCaptureSession.StateCallback previewStateCallback = new CameraCaptureSession.StateCallback() {
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
            camera_loading_splash.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();

        // Bind database services
        this.bindService(new Intent(getApplicationContext(), DatabaseService.class),
                databaseServiceConnection, Context.BIND_AUTO_CREATE);

        setContentView(R.layout.activity_camera);

        this.init();
    }

    private void init() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                String[] permissions = new String[] {
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CAMERA
                };
                this.requestPermissions(permissions, PERMISSIONS_CODE);
            }
        } else {

            this.locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            this.location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            this.sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            this.compas = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);


            textureView = (TextureView) findViewById(R.id.textureView);
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case PERMISSIONS_CODE:
                if (grantResults.length > 0) {
                    for (int i : grantResults)
                        if (i != PackageManager.PERMISSION_GRANTED) return;
                    this.init();
                }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private SensorEventListener compasEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            TextView cameraTextView = (TextView) findViewById(R.id.cameraTextView);
            cameraTextView.setText(String.format("Lat : %s | Lng : %s | Alt : %s | Heading : %s | Bearing : %s",
                    location.getLatitude(), location.getLongitude(), location.getAltitude(),
                    0d, location.getBearing()));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    @Override
    protected void onResume() {

        getWindow().getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);

        this.sensorManager.registerListener(compasEventListener, compas, SensorManager.SENSOR_DELAY_UI);

        super.onResume();
    }

    @Override
    protected void onPause() {
        if (cameraDevice != null) cameraDevice.close();

        this.sensorManager.unregisterListener(compasEventListener);

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (cameraDevice != null) cameraDevice.close();

        this.sensorManager.unregisterListener(compasEventListener);

        // Unbind database service
        this.unbindService(databaseServiceConnection);
        super.onDestroy();
    }

}
