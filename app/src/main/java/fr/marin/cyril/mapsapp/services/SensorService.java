package fr.marin.cyril.mapsapp.services;

import android.Manifest;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by CSCM6014 on 11/04/2016.
 */
public class SensorService extends Service
        implements SensorEventListener, LocationListener {

    public static final int PORTRAIT = 0;
    public static final int LANDSCAPE = 1;

    public static final String AZIMUTH = "azimuth";
    public static final String PITCH = "pitch";

    private static final int NOTIFICATION_FREQ_MS = 250;

    private Location location;
    private float[] graMat;
    private float[] geoMat;
    private float azimuth_portrait;
    private float pitch_portrait;
    private float azimuth_land;
    private float pitch_land;

    private ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
    private HashMap<Messenger, Integer> mClients = new HashMap<>();
    private final Messenger mMessenger = new Messenger(new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Messages.MSG_REGISTER_CLIENT:
                    mClients.put(msg.replyTo, msg.arg1);
                    break;
                case Messages.MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case Messages.MSG_REQUEST_LOCATION:
                    if (location != null) {
                        Bundle data = new Bundle();
                        data.setClassLoader(getClassLoader());

                        data.putFloat(SensorService.AZIMUTH, mClients.get(msg.replyTo) == PORTRAIT ? azimuth_portrait : azimuth_land);
                        data.putFloat(SensorService.PITCH, mClients.get(msg.replyTo) == PORTRAIT ? pitch_portrait : pitch_land);
                        location.setExtras(data);
                    }
                    Message m = Message.obtain(null, Messages.MSG_REQUEST_LOCATION_RESPONSE);
                    m.obj = location;
                    Messages.sendMessage(msg.replyTo, m);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    });
    private LocationManager locationManager;
    private SensorManager sensorManager;

    private void sendUpdatesToAllClients() {
        Bundle data = new Bundle();
        data.setClassLoader(getClassLoader());

        for (HashMap.Entry<Messenger, Integer> client : mClients.entrySet()) {

            data.putFloat(SensorService.AZIMUTH, client.getValue() == PORTRAIT ? azimuth_portrait : azimuth_land);
            data.putFloat(SensorService.PITCH, client.getValue() == PORTRAIT ? pitch_portrait : pitch_land);
            location.setExtras(data);

            Message msg = Message.obtain(null, Messages.MSG_LOCATION_UPDATE);
            msg.obj = location;

            Messages.sendMessage(client.getKey(), msg);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            graMat = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            geoMat = event.values;
        if (event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR)
            geoMat = event.values;


        if (graMat != null && geoMat != null) {
            float[] oMat;
            float[] tmp = new float[9];
            float[] R = new float[9];
            if (SensorManager.getRotationMatrix(tmp, null, graMat, geoMat)) {
                SensorManager.remapCoordinateSystem(tmp, SensorManager.AXIS_X, SensorManager.AXIS_Z, R);

                oMat = new float[3];
                SensorManager.getOrientation(tmp, oMat);
                this.azimuth_portrait = (float) Math.toDegrees(oMat[0]);
                this.pitch_portrait = (float) Math.toDegrees(oMat[1]);

                oMat = new float[3];
                SensorManager.getOrientation(R, oMat);
                this.azimuth_land = (float) Math.toDegrees(oMat[0]);
                this.pitch_land = (float) Math.toDegrees(oMat[1]);

            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        this.locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        this.registerSensorListener(sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
        this.registerSensorListener(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, this);
            this.location = this.locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        this.pool.scheduleAtFixedRate(new LocationUpdateNotifyer(), 1000, NOTIFICATION_FREQ_MS, TimeUnit.MILLISECONDS);
    }

    private void registerSensorListener(Sensor sensor) {
        if (sensor != null)
            this.sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onDestroy() {
        this.sensorManager.unregisterListener(this);
        this.sensorManager = null;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            this.locationManager.removeUpdates(this);
        }

        this.pool.shutdown();

        super.onDestroy();
    }

    public static class SensorServiceConnection implements ServiceConnection {
        private Messenger serviceMessenger;
        private boolean bound = false;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            this.serviceMessenger = new Messenger(service);
            this.bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            this.serviceMessenger = null;
            this.bound = false;
        }

        public boolean isBound() {
            return bound;
        }

        public Messenger getServiceMessenger() {
            return serviceMessenger;
        }
    }

    private class LocationUpdateNotifyer implements Runnable {
        @Override
        public void run() {
            SensorService.this.sendUpdatesToAllClients();
        }
    }
}
