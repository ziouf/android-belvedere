package fr.marin.cyril.belvedere.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by Cyril on 04/05/2016.
 */
public class CompassView extends View {
    private static final String TAG = "CompassView";

    private static final int A = 0;
    private static final int L = 1;
    private static final int R = 2;

    private static final String[] CARD = new String[]{"N", "NE", "E", "SE", "S", "SO", "O", "NO"};

    private static final int COMPASS_WINDOW = 45;
    private static final int TEXT_SIZE = 30;
    private static final int SCALE_WIDTH = 2;
    private static final int SCALE_HEIGHT = 10;

    private int[] azimuth = new int[4];
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
        painter.setTextSize(TEXT_SIZE);
        painter.setTextAlign(Paint.Align.CENTER);

        painter.setStrokeWidth(1);
        painter.setStrokeJoin(Paint.Join.ROUND);
        painter.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Set Text Size
        painter.setTextSize(getHeight() * 4 / 5);

        for (int a = -360, i = 0; a < 360; a += 5) {
            if (a % COMPASS_WINDOW == 0) {
                final int left = (a - COMPASS_WINDOW + 360) % 360;
                final int right = (a + COMPASS_WINDOW + 360) % 360;

                if (left > right) {
                    if (left < azimuth[A] && azimuth[A] < right + 360)
                        this.drawScale(canvas, this.calculatWidth(a), CARD[i % CARD.length]);
                } else {
                    if (left < azimuth[A] && azimuth[A] < right)
                        this.drawScale(canvas, this.calculatWidth(a), CARD[i % CARD.length]);
                }

                ++i;

            } else if (a % 5 == 0) {

                this.drawScale(canvas, this.calculatWidth(a), null);

            }
        }

    }

    private int calculatWidth(int angle) {
        int a = azimuth[A] - angle;
        // Transformation azimuth -> pixels en fonction de la largeur de la view
        int b = (a * getWidth()) / (COMPASS_WINDOW * 2);
        // Centrage sur l'écran
        int c = b + getWidth() / 2;

        return c % getWidth();
    }

    private void drawScale(Canvas canvas, int width, String label) {
        if (label != null) {
            final Rect r = new Rect((getWidth() - width) - SCALE_WIDTH, getHeight() * 3 / 4, (getWidth() - width) + SCALE_WIDTH, getHeight());
            canvas.drawRect(r, painter);
            canvas.drawText(label, (getWidth() - width), getHeight() * 2 / 3, painter);
        } else {
            final Rect r = new Rect((getWidth() - width) - SCALE_WIDTH, getHeight() / 2, (getWidth() - width) + SCALE_WIDTH, getHeight());
            canvas.drawRect(r, painter);
        }
    }


    /**
     * @param azimuth
     */
    public void updateAzimuthAndRedraw(float azimuth) {
        this.azimuth[A] = (int) (azimuth + 360) % 360;
        this.azimuth[L] = (this.azimuth[A] - COMPASS_WINDOW) % 360;
        this.azimuth[R] = (this.azimuth[A] + COMPASS_WINDOW) % 360;

        invalidate();

        Log.i(TAG, "Update azimuth and redraw view");
    }

}
