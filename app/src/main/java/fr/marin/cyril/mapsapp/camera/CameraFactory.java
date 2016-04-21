package fr.marin.cyril.mapsapp.camera;

import android.app.Activity;
import android.os.Build;

/**
 * Created by CSCM6014 on 21/04/2016.
 */
public class CameraFactory {

    public static Camera getImpl(Activity context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return new CameraApi01(context);
        } else {
            return new CameraApi21(context);
        }
    }
}
