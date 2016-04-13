package fr.marin.cyril.mapsapp;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by CSCM6014 on 11/04/2016.
 */
public class SensorService extends Service {

    public static final int MSG_REGISTER_CLIENT = 0;
    public static final int MSG_UNREGISTER_CLIENT = 1;
    public static final int MSG_ALTITUDE_UPDATED = 10;
    public static final int MSG_ORIENTATION_UPDATED = 11;

    public static final String ALTITUDE = "altitude";
    public static final String AZIMUTH = "azimuth";
    public static final String PITCH = "pitch";

    private Set<Messenger> mClients = new HashSet<>();
    final Messenger messenger = new Messenger(new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SensorService.MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case SensorService.MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    });
    private SensorManager sensorManager;
    private SensorEventListener presureEventListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            float altitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, event.values[0]);

            Bundle values = new Bundle();
            values.putFloat(SensorService.ALTITUDE, altitude);
            sendNewMessageToAll(SensorService.MSG_ALTITUDE_UPDATED, values);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
    private SensorEventListener orientationSensorEventListener = new SensorEventListener() {
        float[] mGravity;
        float[] mGeomagnetic;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                mGravity = event.values;
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                mGeomagnetic = event.values;

            if (mGravity == null || mGeomagnetic == null) return;

            float[] tmp = new float[9];
            float[] R = new float[9];
            if (!SensorManager.getRotationMatrix(tmp, null, mGravity, mGeomagnetic)) return;
            SensorManager.remapCoordinateSystem(tmp, SensorManager.AXIS_X, SensorManager.AXIS_Z, R);

            float[] oMat = new float[3];
            SensorManager.getOrientation(R, oMat);

            Bundle values = new Bundle();
            values.putFloat(SensorService.AZIMUTH, (float) Math.toDegrees(oMat[0]));
            values.putFloat(SensorService.PITCH, (float) Math.toDegrees(oMat[1]));

            sendNewMessageToAll(SensorService.MSG_ORIENTATION_UPDATED, values);

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    void sendNewMessageToAll(int type, Bundle data) {
        for (Messenger messenger : mClients)
            this.sendNewMessage(messenger, type, data);
    }

    void sendNewMessage(Messenger messenger, int type, Bundle data) {
        Message msg = Message.obtain(null, type);
        msg.setData(data);

        try {
            messenger.send(msg);

        } catch (RemoteException ignore) {

        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Toast.makeText(getApplicationContext(), "Sensor Service Created", Toast.LENGTH_SHORT).show();

        this.sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        PackageManager packageManager = getPackageManager();

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_BAROMETER)) {
            Sensor presure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
            this.sensorManager.registerListener(this.presureEventListener, presure, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)
                && packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS)) {
            Sensor compas = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            this.sensorManager.registerListener(this.orientationSensorEventListener, compas, SensorManager.SENSOR_DELAY_NORMAL);
            this.sensorManager.registerListener(this.orientationSensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onDestroy() {
        this.sensorManager.unregisterListener(this.presureEventListener);
        this.sensorManager.unregisterListener(this.orientationSensorEventListener);

        this.sensorManager = null;

        //Toast.makeText(getApplicationContext(), "Sensor Service Destroyed", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }
}
