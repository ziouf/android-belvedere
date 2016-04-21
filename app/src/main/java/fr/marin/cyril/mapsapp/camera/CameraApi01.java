package fr.marin.cyril.mapsapp.camera;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.io.IOException;

import fr.marin.cyril.mapsapp.R;

/**
 * Created by CSCM6014 on 21/04/2016.
 */
class CameraApi01
        extends Camera
        implements SurfaceHolder.Callback {

    private SurfaceHolder mHolder;
    private android.hardware.Camera mCamera;

    public CameraApi01(Activity context) {
        super(context);

        context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        context.findViewById(R.id.camera_binocular_mask).setVisibility(View.GONE);
        context.findViewById(R.id.camera_preview_tv).setVisibility(View.GONE);

        mCamera = CameraApi01.getCameraInstance();
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    }

    public static android.hardware.Camera getCameraInstance() {
        android.hardware.Camera c = null;
        try {
            c = android.hardware.Camera.open();
        } catch (Exception e) {
            Log.e("Camera", "Not available");
        }
        return c;
    }

    private SurfaceHolder getHolder() {
        SurfaceView preview = (SurfaceView) getContext().findViewById(R.id.camera_preview_sv);
        return preview.getHolder();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException ignore) {
        }

        getContext().findViewById(R.id.camera_loading).setVisibility(View.GONE);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mHolder.getSurface() == null) return;

        try {
            mCamera.stopPreview();
        } catch (Exception ignore) {
        }

        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (IOException ignore) {
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void pause() {
        super.pause();

        mCamera.release();
    }

    @Override
    public void resume() {
        super.resume();


    }
}
