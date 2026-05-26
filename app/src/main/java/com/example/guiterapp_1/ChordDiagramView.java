package com.example.guiterapp_1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class ChordDiagramView extends View {

    private int[] frets = {-1, -1, -1, -1, -1, -1};
    private String chordName = "";
    
    private Paint gridPaint;
    private Paint dotPaint;
    private Paint textPaint;
    private Paint fretNumPaint;

    public ChordDiagramView(Context context) {
        super(context);
        init();
    }

    public ChordDiagramView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.WHITE);
        gridPaint.setStrokeWidth(2f);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.CYAN);
        dotPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        fretNumPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fretNumPaint.setColor(Color.WHITE);
        fretNumPaint.setTextSize(24f);
        fretNumPaint.setTextAlign(Paint.Align.LEFT);
    }

    public void setChordData(int[] frets, String name) {
        this.frets = frets;
        this.chordName = name;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float paddingLeft = 40f;
        float paddingRight = 60f;
        float paddingTop = 80f;
        float paddingBottom = 40f;

        float width = getWidth() - paddingLeft - paddingRight;
        float height = getHeight() - paddingTop - paddingBottom;

        float stringSpacing = width / 5f;
        float numFretLines = 5;
        float fretSpacing = height / numFretLines;

        // Draw title
        canvas.drawText(chordName, getWidth() / 2f, 40f, textPaint);

        // Calculate base fret
        int minFret = Integer.MAX_VALUE;
        int maxFret = 0;
        for (int f : frets) {
            if (f > 0) {
                if (f < minFret) minFret = f;
                if (f > maxFret) maxFret = f;
            }
        }

        int baseFret = 1;
        // Shift baseFret if the lowest note is not on the first fret (even if it's just the 3rd fret)
        if (minFret != Integer.MAX_VALUE && minFret > 1) {
            baseFret = minFret;
        }

        // Draw fret number on the right if not open position
        if (baseFret > 1) {
            canvas.drawText(String.valueOf(baseFret), paddingLeft + width + 15f, paddingTop + fretSpacing / 2f + 10f, fretNumPaint);
        }

        // Draw strings (vertical)
        for (int i = 0; i < 6; i++) {
            float x = paddingLeft + i * stringSpacing;
            canvas.drawLine(x, paddingTop, x, paddingTop + height, gridPaint);
        }

        // Draw frets (horizontal)
        for (int i = 0; i <= numFretLines; i++) {
            float y = paddingTop + i * fretSpacing;
            // Draw nut thicker only if baseFret is 1
            if (i == 0 && baseFret == 1) {
                gridPaint.setStrokeWidth(6f);
            } else {
                gridPaint.setStrokeWidth(2f);
            }
            canvas.drawLine(paddingLeft, y, paddingLeft + width, y, gridPaint);
        }
        gridPaint.setStrokeWidth(2f); // Reset

        // Draw dots
        for (int i = 0; i < frets.length; i++) {
            int fret = frets[i];
            float x = paddingLeft + i * stringSpacing;
            
            if (fret == 0) {
                // Open string
                canvas.drawCircle(x, paddingTop - 15f, 8f, gridPaint);
            } else if (fret > 0) {
                // Fret position relative to baseFret
                float relativeFret = fret - baseFret + 0.5f;
                if (relativeFret >= 0 && relativeFret <= numFretLines) {
                    float y = paddingTop + relativeFret * fretSpacing;
                    canvas.drawCircle(x, y, 12f, dotPaint);
                }
            } else {
                // Muted (X)
                canvas.drawText("X", x, paddingTop - 10f, textPaint);
            }
        }
    }
}
