package fr.marin.cyril.mapsapp.camera;

import android.app.Activity;

/**
 * Created by CSCM6014 on 21/04/2016.
 */
public abstract class Camera {

    private final Activity context;

    public Camera(Activity context) {
        this.context = context;
    }

    protected Activity getContext() {
        return context;
    }

    public void pause() {
    }

    public void resume() {
    }

}
