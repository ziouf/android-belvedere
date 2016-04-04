package fr.marin.cyril.mapsapp;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import fr.marin.cyril.mapsapp.database.DatabaseService;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class CameraActivity extends AppCompatActivity {

    private static final String LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;

    private LocationManager locationManager;
    private SensorManager sensorManager;
    private Sensor sensor;

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        getWindow().getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);

        this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        this.sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // Bind database services
        this.bindService(new Intent(getApplicationContext(), DatabaseService.class),
                databaseServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this, LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            this.locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        sensorManager.unregisterListener(sensorEventListener);

        super.onPause();
    }
}
