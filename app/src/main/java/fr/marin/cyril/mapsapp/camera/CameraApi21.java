package fr.marin.cyril.mapsapp.camera;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
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
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.Collections;

import fr.marin.cyril.mapsapp.R;

/**
 * Created by CSCM6014 on 21/04/2016.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class CameraApi21
        extends Camera
        implements TextureView.SurfaceTextureListener {

    private Size previewSize;
    private TextureView textureView;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder previewBuilder;
    private CameraCaptureSession previewSession;

    public CameraApi21(Activity context) {
        super(context);

        // Get sreen size
        Point size = new Point();
        context.getWindowManager().getDefaultDisplay().getRealSize(size);
        this.previewSize = new Size(size.y, size.x);

        // Init Camera view
        getContext().findViewById(R.id.camera_preview_sv).setVisibility(View.GONE);
        this.textureView = (TextureView) context.findViewById(R.id.camera_preview_tv);
        if (this.textureView != null) this.textureView.setSurfaceTextureListener(this);
    }

    @Override
    public void pause() {
        super.pause();

        // Close Camera
        if (cameraDevice != null) {
            cameraDevice.close();

            ImageView camera_loading_splash = (ImageView) getContext().findViewById(R.id.camera_loading);
            if (camera_loading_splash != null)
                camera_loading_splash.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void resume() {
        super.resume();

        // Re-ouverture de la camera
        if (this.textureView != null && this.textureView.isAvailable()) {
            this.textureView.setSurfaceTextureListener(this);
            this.onSurfaceTextureAvailable(this.textureView.getSurfaceTexture(),
                    this.textureView.getWidth(), this.textureView.getHeight());
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            return;

        CameraManager cameraManager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);

        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.openCamera(cameraId, this.getCameraDeviceStateCallback(), null);
        } catch (CameraAccessException e) {

        }
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
                SurfaceTexture texture = textureView.getSurfaceTexture();
                if (texture == null) return;

                try {
                    previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                } catch (CameraAccessException ignore) {

                }

                texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
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

                }

                ImageView camera_loading_splash = (ImageView) getContext().findViewById(R.id.camera_loading);
                if (camera_loading_splash != null)
                    camera_loading_splash.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                Toast.makeText(getContext(), "CaptureSession configuration failed", Toast.LENGTH_SHORT).show();
            }
        };
    }

}
