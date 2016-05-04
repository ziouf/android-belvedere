package fr.marin.cyril.belvedere.views;

import android.content.Context;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by CSCM6014 on 04/05/2016.
 */
public class CompassView extends SurfaceView
        implements SurfaceHolder.Callback {
    private final Context context;
    private CompassPainter painter;

    public CompassView(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
