package fr.marin.cyril.mapsapp.tools;

import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by CSCM6014 on 25/04/2016.
 */
public class Compass
        implements SensorEventListener {
    public static final int AZIMUTH = 0;
    public static final int PITCH = 1;
    private static final Compass singleton = new Compass();
    private final float[] data = new float[2];

    private CompasEventListener compasEventListener;
    private SensorManager sensorManager;

    private boolean portrait = true;

    private long lastUpdate = 0;
    private float[] graMat;
    private float[] geoMat;

    public static Compass getInstance(Context context, boolean portrait) {
        singleton.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        singleton.portrait = portrait;
        return singleton;
    }

    public static float getAzimuth(float[] data, GeomagneticField geoField) {
        if (geoField != null)
            return data[AZIMUTH] + geoField.getDeclination();
        return data[AZIMUTH];
    }

    public void pause() {
        this.sensorManager.unregisterListener(this);
    }

    public void resume() {
        this.registerSensorListener(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        this.registerSensorListener(sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
    }

    private void registerSensorListener(Sensor sensor) {
        if (sensor != null)
            this.sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if ((lastUpdate + 250) > System.currentTimeMillis()) return;
        lastUpdate = System.currentTimeMillis();

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            graMat = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            geoMat = event.values;

        if (graMat != null && geoMat != null) {
            float[] oMat;
            float[] R = new float[9];
            if (SensorManager.getRotationMatrix(R, null, graMat, geoMat)) {
                if (portrait)
                    SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X, SensorManager.AXIS_Z, R);
                oMat = new float[3];
                SensorManager.getOrientation(R, oMat);
                this.data[AZIMUTH] = (float) Math.toDegrees(oMat[0]);
                this.data[PITCH] = (float) Math.toDegrees(oMat[1]);
            }
        }

        // Run event
        if (compasEventListener != null)
            compasEventListener.run();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void OnCompasEvent(CompasEventListener event) {
        this.compasEventListener = event;
    }

    public static abstract class CompasEventListener implements Runnable {
        @Override
        public void run() {
            this.onSensorChanged(singleton.data);
        }

        public abstract void onSensorChanged(float[] data);
    }
}
