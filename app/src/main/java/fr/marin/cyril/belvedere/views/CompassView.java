package fr.marin.cyril.belvedere.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Cyril on 04/05/2016.
 *
 *
 *
 */
public class CompassView extends View {
    private final Context context;

    private Paint painter;

    private float azimuth = 0f;

    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        setUpPaint();
        attrs.getAttributeFloatValue("", "", 0f);
    }

    public CompassView(Context context) {
        super(context);
        this.context = context;

        setUpPaint();
    }

    private void setUpPaint() {
        painter = new Paint();
        painter.setAntiAlias(true);
        painter.setStrokeWidth(5);
        painter.setStyle(Paint.Style.STROKE);
        painter.setStrokeJoin(Paint.Join.ROUND);
        painter.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

    }

}
