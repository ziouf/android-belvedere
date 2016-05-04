package fr.marin.cyril.belvedere.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.SurfaceHolder;

import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.tools.Utils;

/**
 * Created by CSCM6014 on 04/05/2016.
 */
class CompassPainter {

    private final Context context;
    private final SurfaceHolder surfaceHolder;

    /**
     * Dimensions
     */
    private int width;
    private int height;
    private int canevasWidth;
    private int canevasHeight;

    /**
     * Drawables
     */
    private Drawable bigScaleDot;
    private Drawable smallScaleDot;

    private String[] compass_Labels;

    /**
     * Constructor
     */
    public CompassPainter(Context context, SurfaceHolder surfaceHolder) {
        this.context = context;
        this.surfaceHolder = surfaceHolder;

        this.bigScaleDot = Utils.getDrawable(context, R.drawable.compass_scale_dot);
        this.bigScaleDot.setLevel(1);
        this.smallScaleDot = Utils.getDrawable(context, R.drawable.compass_scale_dot);
        this.smallScaleDot.setLevel(0);

        this.compass_Labels = context.getResources().getStringArray(R.array.compass_labels_array);

    }


}
