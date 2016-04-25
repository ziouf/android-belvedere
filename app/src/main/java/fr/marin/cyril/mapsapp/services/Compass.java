package fr.marin.cyril.mapsapp.services;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.SparseArray;

/**
 * Created by CSCM6014 on 25/04/2016.
 */
public class Compass
        implements SensorEventListener {

    public static final String AZIMUTH_P = "azimuth_portrait";
    public static final String AZIMUTH_L = "azimuth_landscape";
    public static final String PITCH_P = "pitch_portrait";
    public static final String PITCH_L = "pitch_landscape";
    private final Context context;
    private final SensorManager sensorManager;
    private final SparseArray<Runnable> tasks = new SparseArray<>();
    private Bundle data = new Bundle();
    private float[] graMat;
    private float[] geoMat;

    public Compass(Context context) {
        this.context = context;

        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    public static Compass getInstance(Context context) {
        return new Compass(context);
    }

    public void pause() {
        this.sensorManager.unregisterListener(this);
    }

    public void resume() {
        this.registerSensorListener(sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
        this.registerSensorListener(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
    }

    private void registerSensorListener(Sensor sensor) {
        if (sensor != null)
            this.sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            graMat = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            geoMat = event.values;

        if (graMat != null && geoMat != null) {
            float[] oMat;
            float[] tmp = new float[9];
            float[] R = new float[9];
            if (SensorManager.getRotationMatrix(tmp, null, graMat, geoMat)) {
                SensorManager.remapCoordinateSystem(tmp, SensorManager.AXIS_X, SensorManager.AXIS_Z, R);

                oMat = new float[3];
                SensorManager.getOrientation(tmp, oMat);
                this.data.putFloat(AZIMUTH_P, (float) Math.toDegrees(oMat[0]));
                this.data.putFloat(PITCH_P, (float) Math.toDegrees(oMat[1]));

                oMat = new float[3];
                SensorManager.getOrientation(R, oMat);
                this.data.putFloat(AZIMUTH_L, (float) Math.toDegrees(oMat[0]));
                this.data.putFloat(PITCH_L, (float) Math.toDegrees(oMat[1]));

            }
        }

        for (int i = 0; i < this.tasks.size(); ++i)
            this.tasks.get(this.tasks.keyAt(i)).run();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public Bundle getData() {
        return data;
    }

    public void addTask(int key, Runnable runnable) {
        tasks.append(key, runnable);
    }
}
