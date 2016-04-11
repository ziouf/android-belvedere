package fr.marin.cyril.mapsapp;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by CSCM6014 on 11/04/2016.
 */
public class SensorService extends Service {

    public static final int COMPAS = 0;
    public static final int ORIENTATION = 1;

    private IBinder binder = new SensorServiceBinder();

    private float altitude;
    private float[] orientation;
    private Sensor compas;
    private Sensor presure;
    private SensorManager sensorManager;
    private SensorServiceListener compasSensorServiceListener;
    private SensorEventListener compasSensorEventListener = new SensorEventListener() {
        SensorServiceListener listener = compasSensorServiceListener;

        @Override
        public void onSensorChanged(SensorEvent event) {
            orientation = event.values;
            listener.onChange(orientation);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
    private SensorServiceListener altitudeSensorServiceListener;
    private SensorEventListener presureEventListener = new SensorEventListener() {
        SensorServiceListener listener = altitudeSensorServiceListener;

        @Override
        public void onSensorChanged(SensorEvent event) {
            altitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, event.values[0]);
            listener.onChange(new float[]{altitude});
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    public float getAltitude() {
        return altitude;
    }

    public float[] getOrientation() {
        return orientation;
    }

    public void setCompasSensorServiceListener(SensorServiceListener listener) {
        compasSensorServiceListener = listener;
    }

    public void setAltitudeSensorServiceListener(SensorServiceListener listener) {
        altitudeSensorServiceListener = listener;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public abstract static class SensorServiceListener {
        public abstract void onChange(float[] values);
    }

    public static class SensorServiceConnection implements ServiceConnection {
        boolean bound = false;
        SensorService sensorService;

        public boolean isBound() {
            return bound;
        }

        public SensorService getSensorService() {
            return sensorService;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SensorServiceBinder binder = (SensorServiceBinder) service;
            sensorService = binder.getService();
            bound = sensorService != null;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }
    }

    public class SensorServiceBinder extends Binder {
        public SensorService getService() {
            return SensorService.this;
        }
    }
}
