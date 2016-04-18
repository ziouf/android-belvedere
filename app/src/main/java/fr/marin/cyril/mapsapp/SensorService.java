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
import android.support.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

import fr.marin.cyril.mapsapp.services.Messages;

/**
 * Created by CSCM6014 on 11/04/2016.
 */
public class SensorService extends Service
        implements SensorEventListener {

    public static final String ALTITUDE = "altitude";
    public static final String AZIMUTH = "azimuth";
    public static final String PITCH = "pitch";

    private float[] graMat;
    private float[] geoMat;
    private float altitude;

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
                default:
                    super.handleMessage(msg);
            }
        }
    });
    private SensorManager sensorManager;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PRESSURE)
            altitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, event.values[0]);
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            graMat = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            geoMat = event.values;

        Bundle values = new Bundle();
        values.putFloat(SensorService.ALTITUDE, altitude);

        if (graMat != null && geoMat != null) {
            float[] tmp = new float[9];
            float[] R = new float[9];
            if (SensorManager.getRotationMatrix(tmp, null, graMat, geoMat)) {
                SensorManager.remapCoordinateSystem(tmp, SensorManager.AXIS_X, SensorManager.AXIS_Z, R);

                float[] oMat = new float[3];
                SensorManager.getOrientation(R, oMat);

                values.putFloat(SensorService.AZIMUTH, (float) Math.toDegrees(oMat[0]));
                values.putFloat(SensorService.PITCH, (float) Math.toDegrees(oMat[1]));
            }
        }

        Messages.sendNewMessageToAll(mClients, Messages.MSG_SENSOR_UPDATE, values, mMessenger);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

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
    }

    @Override
    public void onDestroy() {
        this.sensorManager.unregisterListener(this);
        this.sensorManager = null;
        super.onDestroy();
    }
}
