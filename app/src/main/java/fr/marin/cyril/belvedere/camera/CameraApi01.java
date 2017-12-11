package fr.marin.cyril.belvedere.camera;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.io.IOException;

import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.tools.Objects;

/**
 * Created by Cyril on 21/04/2016.
 */
final class CameraApi01
        extends Camera
        implements SurfaceHolder.Callback {

    private static final String TAG = CameraApi21.class.getSimpleName();

    private SurfaceHolder mHolder;
    private android.hardware.Camera mCamera;

    CameraApi01(Activity context) {
        super(context);

        context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        context.findViewById(R.id.camera_binocular_mask).setVisibility(View.GONE);
        context.findViewById(R.id.camera_preview_tv).setVisibility(View.GONE);

        mCamera = CameraApi01.getCameraInstance();
        mHolder = ((SurfaceView) getContext().findViewById(R.id.camera_preview_sv)).getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private static android.hardware.Camera getCameraInstance() {
        android.hardware.Camera c = null;
        try {
            c = android.hardware.Camera.open();
        } catch (Exception e) {
            Log.e(TAG, "Not available");
        }
        return c;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (Objects.isNull(mHolder.getSurface())) return;

        this.stopCameraPreview();
        this.startCameraPreview(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    private void startCameraPreview(SurfaceHolder holder) {
        if (Objects.isNull(this.mCamera))
            this.mCamera = CameraApi01.getCameraInstance();

        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException ignore) {
            Log.e(TAG, "Start preview failed");
        }
    }

    private void stopCameraPreview() {
        try {
            mCamera.stopPreview();
        } catch (Exception ignore) {
            Log.e(TAG, "Stop preview failed");
        }
    }

    @Override
    public void pause() {
        super.pause();

        mCamera.release();
        mCamera = null;
    }

    @Override
    public void resume() {
        super.resume();

        this.stopCameraPreview();
        this.startCameraPreview(mHolder);
    }
}
