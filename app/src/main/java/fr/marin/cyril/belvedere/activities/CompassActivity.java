package fr.marin.cyril.belvedere.activities;

import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * Created by CSCM6014 on 25/04/2016.
 *
 * See : http://stackoverflow.com/a/16386066/4287785
 *
 */
public class CompassActivity extends LocationActivity
        implements SensorEventListener {
    public static final int AZIMUTH = 0;
    public static final int PITCH = 1;
    protected static final String KEY_AZIMUTH = "azimuth";
    protected static final String KEY_PITCH = "pitch";
    protected final float[] data = new float[2];
    protected boolean portrait = true;

    private CompasEventListener compasEventListener;
    private SensorManager sensorManager;

    private long lastUpdate = 0;
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
            graMat = event.values;

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            geoMat = event.values;

        if (graMat != null && geoMat != null) {
            float[] oMat;
            float[] R = new float[9];
            if (SensorManager.getRotationMatrix(R, null, graMat, geoMat)) {
                if (!portrait)
                    SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X, SensorManager.AXIS_Z, R);
                oMat = new float[3];
                SensorManager.getOrientation(R, oMat);
                this.data[AZIMUTH] = (float) Math.toDegrees(oMat[0]);
                this.data[PITCH] = (float) Math.toDegrees(oMat[1]);
            }
        }

        if ((lastUpdate + 250) > System.currentTimeMillis()) return;
        lastUpdate = System.currentTimeMillis();

        if (location == null) return;
        if (location.getExtras() == null) location.setExtras(new Bundle());
        Bundle bundle = location.getExtras();
        bundle.putFloat(KEY_AZIMUTH, data[AZIMUTH]);
        bundle.putFloat(KEY_PITCH, data[PITCH]);

        // Run event
        if (compasEventListener != null)
            compasEventListener.run();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    protected void setOnCompasEvent(CompasEventListener event) {
        this.compasEventListener = event;
    }

    public abstract class CompasEventListener implements Runnable {
        @Override
        public void run() {
            this.onSensorChanged(data);
        }

        public abstract void onSensorChanged(float[] data);
    }
}
