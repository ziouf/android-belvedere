package fr.marin.cyril.belvedere.activities;

import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;

import java.util.HashSet;

/**
 * Created by Cyril on 25/04/2016.
 *
 * See : http://stackoverflow.com/a/16386066/4287785
 *
 */
public class CompassActivity extends LocationActivity
        implements SensorEventListener {
    public static final int AZIMUTH = 0;
    public static final int PITCH = 1;
    public static final String KEY_AZIMUTH = "azimuth";
    public static final String KEY_PITCH = "pitch";
    private static final float ALPHA = 0.025f;
    protected final float[] data = new float[2];
    protected boolean portrait = true;

    private HashSet<CompasEventListener> compassEventListenerSet;
    private SensorManager sensorManager;

    private float[] graMat;
    private float[] geoMat;

    protected float getAzimuth() {
        float azimuth = data[AZIMUTH];
        if (geoField != null) azimuth += geoField.getDeclination();
        return (azimuth + 360) % 360;
    }

    protected boolean isCompassCompatible() {
        PackageManager pm = getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)
                && pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.compassEventListenerSet = new HashSet<>();
        this.sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isCompassCompatible()) {
            this.sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
            this.sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            graMat = lowPass(event.values.clone(), graMat);

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            geoMat = lowPass(event.values.clone(), geoMat);

        if (graMat != null && geoMat != null) {
            float[] R = new float[9];
            float[] oMat = new float[3];
            if (SensorManager.getRotationMatrix(R, null, graMat, geoMat)) {

                if (!portrait) // Si mode paysage -> Réorientation de la matrice
                    SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X, SensorManager.AXIS_Z, R);

                SensorManager.getOrientation(R, oMat);
                this.data[AZIMUTH] = (float) Math.toDegrees(oMat[0]);
                this.data[PITCH] = (float) Math.toDegrees(oMat[1]);
            }
        }

        if (location != null) {
            if (location.getExtras() == null) location.setExtras(new Bundle());
            location.getExtras().putFloat(KEY_AZIMUTH, data[AZIMUTH]);
            location.getExtras().putFloat(KEY_PITCH, data[PITCH]);
        }

        // Run event
        for (CompasEventListener eventListener : compassEventListenerSet)
            eventListener.run();
    }

    private float[] lowPass(float[] input, float[] output) {
        if (output == null) return input;

        for (int i = 0; i < input.length; ++i)
            output[i] = output[i] + ALPHA * (input[i] - output[i]);

        return output;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    protected CompasEventListener registerCompasEventListener(CompasEventListener eventListener) {
        this.compassEventListenerSet.add(eventListener);
        return eventListener;
    }

    protected void unRegisterCompasEventListener(CompasEventListener eventListener) {
        this.compassEventListenerSet.remove(eventListener);
    }

    public abstract class CompasEventListener implements Runnable {
        @Override
        public void run() {
            this.onSensorChanged(data);
        }

        public abstract void onSensorChanged(float[] data);
    }
}
