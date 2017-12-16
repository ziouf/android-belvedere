package fr.marin.cyril.belvedere.camera;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import junit.framework.Assert;

import java.util.Collections;

import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.tools.Objects;

/**
 * Created by Cyril on 21/04/2016.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
final class CameraApi21
        extends Camera
        implements TextureView.SurfaceTextureListener {

    private static final String TAG = CameraApi21.class.getSimpleName();

    private Size mPreviewSize;
    private TextureView mTextureView;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder previewBuilder;
    private CameraCaptureSession previewSession;

    CameraApi21(Activity context) {
        super(context);

        // Get sreen size
        Point size = new Point();
        context.getWindowManager().getDefaultDisplay().getRealSize(size);
        this.mPreviewSize = new Size(size.x, size.y);

        // Init Camera view
        getContext().findViewById(R.id.camera_preview_sv).setVisibility(View.GONE);
        this.mTextureView = context.findViewById(R.id.camera_preview_tv);
        if (this.mTextureView != null) this.mTextureView.setSurfaceTextureListener(this);
    }

    @Override
    public void pause() {
        super.pause();

        // Close Camera
        if (cameraDevice != null) {
            cameraDevice.close();
        }
    }

    @Override
    public void resume() {
        super.resume();

        // Re-ouverture de la camera
        if (this.mTextureView != null && this.mTextureView.isAvailable()) {
            this.mTextureView.setSurfaceTextureListener(this);
            this.onSurfaceTextureAvailable(this.mTextureView.getSurfaceTexture(),
                    this.mTextureView.getWidth(), this.mTextureView.getHeight());

            this.transformImage(mTextureView.getWidth(), mTextureView.getHeight());
        }
    }

    private void transformImage(int width, int height) {
        if (Objects.isNull(mPreviewSize) || Objects.isNull(mTextureView)) {
            return;
        }
        Matrix matrix = new Matrix();
        int rotation = getContext().getWindowManager().getDefaultDisplay().getRotation();
        RectF textureRectF = new RectF(0, 0, width, height);
        RectF previewRectF = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = textureRectF.centerX();
        float centerY = textureRectF.centerY();
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            previewRectF.offset(centerX - previewRectF.centerX(),
                    centerY - previewRectF.centerY());
            matrix.setRectToRect(textureRectF, previewRectF, Matrix.ScaleToFit.FILL);
            float scale = Math.max((float) width / mPreviewSize.getWidth(),
                    (float) height / mPreviewSize.getHeight());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            return;

        final CameraManager cameraManager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
        Assert.assertNotNull("CameraManager must not be null", cameraManager);

        try {
            final String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.openCamera(cameraId, this.getCameraDeviceStateCallback(), null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Exception", e);
        }

        this.transformImage(width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private CameraDevice.StateCallback getCameraDeviceStateCallback() {
        return new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                cameraDevice = camera;
                SurfaceTexture texture = mTextureView.getSurfaceTexture();
                if (Objects.isNull(texture)) return;

                try {
                    previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                } catch (CameraAccessException ignore) {

                }

                texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                Surface surface = new Surface(texture);
                previewBuilder.addTarget(surface);

                try {
                    cameraDevice.createCaptureSession(Collections.singletonList(surface), getPreviewSessionStateCallback(), null);
                } catch (CameraAccessException ignore) {

                }
            }

            @Override
            public void onClosed(@NonNull CameraDevice camera) {
                super.onClosed(camera);
                previewSession.close();
                previewSession = null;
                cameraDevice = null;
                previewBuilder = null;
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                Toast.makeText(getContext(), "CameraDevice disconnected", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                Toast.makeText(getContext(), "CameraDevice error", Toast.LENGTH_SHORT).show();
            }
        };
    }

    private CameraCaptureSession.StateCallback getPreviewSessionStateCallback() {
        return new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                previewSession = session;

                previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

                HandlerThread backgroundThread = new HandlerThread("CameraPreview");
                backgroundThread.start();

                Handler backgroundHandler = new Handler(backgroundThread.getLooper());

                try {
                    previewSession.setRepeatingRequest(previewBuilder.build(), null, backgroundHandler);
                } catch (CameraAccessException ignore) {
                    Log.e(TAG, "Exception", ignore);
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                Toast.makeText(getContext(), "CaptureSession configuration failed", Toast.LENGTH_SHORT).show();
            }
        };
    }

}
