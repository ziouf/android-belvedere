package fr.marin.cyril.belvedere.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Cyril on 04/05/2016.
 *
 *
 *
 */
public class CompassView extends View {

    private static final int textSize = 40;
    private static final int scaleWidth = 2;
    private static final int scaleHeight = 10;

    private int azimuth = 0;
    private Paint painter = new Paint();

    public CompassView(Context context) {
        super(context);
        this.init(null, 0);
    }

    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init(attrs, 0);
    }

    public CompassView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init(attrs, defStyleAttr);
    }

    private void init(AttributeSet attrs, int defStyleAttr) {
        // Shapes
        painter.setAntiAlias(true);
        painter.setColor(Color.CYAN);
        painter.setStyle(Paint.Style.FILL);

        // Text
        painter.setTextSize(textSize);
        painter.setTextAlign(Paint.Align.CENTER);

        painter.setStrokeWidth(1);
        painter.setStrokeJoin(Paint.Join.ROUND);
        painter.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        this.drawScale(canvas);


        //canvas.drawText("Text", getWidth()/2, getHeight()/2, painter);

    }

    private void drawScale(Canvas canvas) {
        for (int i = 0; i < getWidth(); ++i) {
            // North
            if (i * 360 / getWidth() == azimuth) {
                int w = (getWidth() / 2 - i) % (getWidth() / 2);
                final Rect r = new Rect(w - scaleWidth, getHeight() / 2 + scaleHeight, w + scaleWidth, getHeight());
                canvas.drawRect(r, painter);
                canvas.drawText("North", w, getHeight() / 2, painter);
            }
        }
    }

    /**
     * @param azimuth
     */
    public void updateAzimuthAndRedraw(float azimuth) {
        this.azimuth = (int) (azimuth % 360 + 360);
        invalidate();
    }
}
