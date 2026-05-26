package com.example.guiterapp_1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GuitarNeckView extends View {

    public static class NoteMarker {
        public int string; // 0 to 5
        public int fret;   // 0 to N
        public String label;
        public int color;

        public NoteMarker(int string, int fret, String label, int color) {
            this.string = string;
            this.fret = fret;
            this.label = label;
            this.color = color;
        }
    }

    private List<NoteMarker> markers = new ArrayList<>();
    private int numFrets = 25;
    private float fretWidth = 100f;
    
    private Paint gridPaint;
    private Paint dotPaint;
    private Paint fretNumPaint;
    private Paint markerTextPaint;
    private Paint bgPaint;
    private Paint inlayPaint;

    public GuitarNeckView(Context context) {
        super(context);
        init();
    }

    public GuitarNeckView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setStrokeWidth(3f);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setStyle(Paint.Style.FILL);

        fretNumPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fretNumPaint.setColor(Color.GRAY);
        fretNumPaint.setTextSize(24f);
        fretNumPaint.setTextAlign(Paint.Align.CENTER);

        markerTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markerTextPaint.setColor(Color.BLACK);
        markerTextPaint.setTextSize(20f);
        markerTextPaint.setTextAlign(Paint.Align.CENTER);
        markerTextPaint.setFakeBoldText(true);

        bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#2C2C2C"));

        inlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        inlayPaint.setColor(Color.parseColor("#444444"));
    }

    public void setMarkers(List<NoteMarker> markers) {
        this.markers = markers;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float paddingLeft = 60f;
        float paddingRight = 40f;
        int width = (int) (paddingLeft + paddingRight + (numFrets - 0.5f) * fretWidth);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float paddingLeft = 60f;
        float paddingRight = 40f;
        float paddingTop = 40f;
        float paddingBottom = 60f;

        float width = getWidth() - paddingLeft - paddingRight;
        float height = getHeight() - paddingTop - paddingBottom;

        float stringSpacing = height / 5f;

        // Draw Fretboard Background
        canvas.drawRect(paddingLeft, paddingTop, paddingLeft + width, paddingTop + height, bgPaint);

        // Draw Frets (Vertical lines)
        for (int i = 0; i < numFrets; i++) {
            float x = paddingLeft + i * fretWidth;
            if (i == 0) {
                gridPaint.setStrokeWidth(8f);
                gridPaint.setColor(Color.WHITE);
            } else {
                gridPaint.setStrokeWidth(3f);
                gridPaint.setColor(Color.parseColor("#BDBDBD"));
            }
            canvas.drawLine(x, paddingTop, x, paddingTop + height, gridPaint);
            
            // Draw fret numbers
            if (i > 0) {
                canvas.drawText(String.valueOf(i), x - fretWidth / 2f, paddingTop + height + 40f, fretNumPaint);
            }
        }

        // Draw Strings (Horizontal lines)
        gridPaint.setColor(Color.parseColor("#E0E0E0"));
        for (int i = 0; i < 6; i++) {
            float y = paddingTop + i * stringSpacing;
            gridPaint.setStrokeWidth(2f + i * 0.5f);
            canvas.drawLine(paddingLeft, y, paddingLeft + width, y, gridPaint);
        }

        // Draw Fret Inlays
        int[] inlays = {3, 5, 7, 9, 12, 15, 17, 19, 21, 24};
        for (int fret : inlays) {
            if (fret < numFrets) {
                float x = paddingLeft + (fret - 0.5f) * fretWidth;
                if (fret % 12 == 0) { // 12 and 24
                    canvas.drawCircle(x, paddingTop + height * 0.25f, 10f, inlayPaint);
                    canvas.drawCircle(x, paddingTop + height * 0.75f, 10f, inlayPaint);
                } else {
                    canvas.drawCircle(x, paddingTop + height / 2f, 10f, inlayPaint);
                }
            }
        }

        // Draw Note Markers
        for (NoteMarker marker : markers) {
            if (marker.fret < numFrets) {
                float x;
                if (marker.fret == 0) {
                    x = paddingLeft - 20f;
                } else {
                    x = paddingLeft + (marker.fret - 0.5f) * fretWidth;
                }
                float y = paddingTop + marker.string * stringSpacing;

                dotPaint.setColor(marker.color);
                canvas.drawCircle(x, y, 18f, dotPaint);

                if (marker.label != null) {
                    float textY = y + (markerTextPaint.getTextSize() / 3f);
                    canvas.drawText(marker.label, x, textY, markerTextPaint);
                }
            }
        }
    }
}
