package fr.marin.cyril.belvedere.services;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.HashSet;

import fr.marin.cyril.belvedere.tools.Orientation;

/**
 * Created by cyril on 31/05/16.
 */
public class CompassService
        implements SensorEventListener {
    private static final String TAG = "CompassService";
    private static final float ALPHA = 0.025f;

    private static CompassService singleton;

    private final Context context;
    private final HashSet<CompassEventListener> compassEventListenerSet;
    private final SensorManager sensorManager;

    private Orientation orientation = Orientation.PORTRAIT;

    private float[] graMat;
    private float[] geoMat;
    private float[] oMat;

    private CompassService(Context context) {
        this.context = context;
        this.compassEventListenerSet = new HashSet<>();
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    public static CompassService getInstance(Context context) {
        if (singleton == null) singleton = new CompassService(context);
        return singleton;
    }

    public void pause() {
        this.sensorManager.unregisterListener(this);
    }

    public void resume() {
        final PackageManager pm = context.getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)
                && pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS)) {
            this.sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
            this.sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            graMat = lowPass(event.values.clone(), graMat);

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            geoMat = lowPass(event.values.clone(), geoMat);

        if (graMat != null && geoMat != null) {
            oMat = new float[3];
            final float[] R = new float[9];
            if (SensorManager.getRotationMatrix(R, null, graMat, geoMat)) {

                if (orientation.equals(Orientation.PAYSAGE)) // Si mode paysage -> Réorientation de la matrice
                    SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X, SensorManager.AXIS_Z, R);

                SensorManager.getOrientation(R, oMat);

                // Run event
                for (CompassEventListener e : compassEventListenerSet)
                    e.run();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private float[] lowPass(float[] input, float[] output) {
        if (output == null) return input;

        for (int i = 0; i < input.length; ++i)
            output[i] = output[i] + ALPHA * (input[i] - output[i]);

        return output;
    }

    public CompassEventListener registerCompassEventListener(CompassEventListener eventListener) {
        this.compassEventListenerSet.add(eventListener);
        return eventListener;
    }

    public void unRegisterCompassEventListener(CompassEventListener eventListener) {
        this.compassEventListenerSet.remove(eventListener);
    }


    public static abstract class CompassEventListener implements Runnable {
        @Override
        public void run() {
            float azimuth = (float) Math.toDegrees(singleton.oMat[0]);
            float pitch = (float) Math.toDegrees(singleton.oMat[1]);

            this.onSensorChanged(azimuth, pitch);
        }

        public abstract void onSensorChanged(float azimuth, float pitch);
    }
}
