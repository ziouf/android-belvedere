package fr.marin.cyril.mapsapp;

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

import java.util.HashSet;
import java.util.Set;

import fr.marin.cyril.mapsapp.services.Messages;

/**
 * Created by CSCM6014 on 11/04/2016.
 */
public class SensorService extends Service
        implements SensorEventListener, LocationListener {

    public static final String AZIMUTH = "azimuth";
    public static final String PITCH = "pitch";

    private Location location;
    private float[] graMat;
    private float[] geoMat;
    private float azimuth;
    private float pitch;

    private Set<Messenger> mClients = new HashSet<>();
    private final Messenger mMessenger = new Messenger(new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Messages.MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case Messages.MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case Messages.MSG_REQUEST_LOCATION:
                    Messages.sendMessage(msg.replyTo, prepareMessage());
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    });
    private LocationManager locationManager;
    private SensorManager sensorManager;

    private Message prepareMessage() {
        Bundle data = new Bundle();

        data.setClassLoader(getClassLoader());
        data.putFloat(SensorService.AZIMUTH, azimuth);
        data.putFloat(SensorService.PITCH, pitch);
        location.setExtras(data);

        Message msg = Message.obtain(null, Messages.MSG_LOCATION_UPDATE);
        msg.obj = location;

        return msg;
    }

    private void sendUpdates() {
        Messages.sendMessageToAll(mClients, this.prepareMessage());
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            graMat = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            geoMat = event.values;

        if (graMat != null && geoMat != null) {
            float[] tmp = new float[9];
            float[] R = new float[9];
            if (SensorManager.getRotationMatrix(tmp, null, graMat, geoMat)) {
                SensorManager.remapCoordinateSystem(tmp, SensorManager.AXIS_X, SensorManager.AXIS_Z, R);

                float[] oMat = new float[3];
                SensorManager.getOrientation(R, oMat);

                this.azimuth = (float) Math.toDegrees(oMat[0]);
                this.pitch = (float) Math.toDegrees(oMat[1]);
            }
        }

        this.sendUpdates();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
        this.sendUpdates();
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

        PackageManager packageManager = getPackageManager();

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_BAROMETER)) {
            Sensor presure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
            this.sensorManager.registerListener(this, presure, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)
                && packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS)) {
            Sensor compas = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            this.sensorManager.registerListener(this, compas, SensorManager.SENSOR_DELAY_NORMAL);
            this.sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, this);
            this.location = this.locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
    }

    @Override
    public void onDestroy() {
        this.sensorManager.unregisterListener(this);
        this.sensorManager = null;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            this.locationManager.removeUpdates(this);
        }

        super.onDestroy();
    }

    public static class SensorServiceConnection implements ServiceConnection {
        private Messenger clientMessenger;
        private Messenger serviceMessenger;
        private boolean bound = false;

        public SensorServiceConnection(Messenger clientMessenger) {
            this.clientMessenger = clientMessenger;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            this.serviceMessenger = new Messenger(service);
            this.bound = true;
            Messages.sendNewMessage(serviceMessenger, Messages.MSG_REGISTER_CLIENT, null, clientMessenger);
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
}
