package fr.marin.cyril.belvedere.camera;

import android.app.Activity;
import android.os.Build;

/**
 * Created by Cyril on 21/04/2016.
 */
public abstract class Camera {

    private final Activity context;

    Camera(Activity context) {
        this.context = context;
    }

    public static Camera getCameraInstance(Activity context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return new CameraApi01(context);
        } else {
            return new CameraApi21(context);
        }
    }

    Activity getContext() {
        return context;
    }

    public void pause() {
    }

    public void resume() {
    }
}
