package com.example.guiterapp_1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class CircleOfFifthsView extends View {

    private Paint circlePaint;
    private Paint textPaint;
    private Paint minorTextPaint;
    private Paint degreePaint;
    private Paint degreeBgPaint;
    private Paint tonicDegreeBgPaint;
    private Paint highlightPaint;
    private Paint tonicHighlightPaint;

    private final String[] majorKeys = {"C", "G", "D", "A", "E", "B", "Gb", "Db", "Ab", "Eb", "Bb", "F"};
    private final String[] minorKeys = {"Am", "Em", "Bm", "F#m", "C#m", "G#m", "Ebm", "Bbm", "Fm", "Cm", "Gm", "Dm"};
    
    private int selectedTonicIndex = -1; // -1 means none selected
    private boolean isMinorSelected = false;

    public CircleOfFifthsView(Context context) {
        super(context);
        init();
    }

    public CircleOfFifthsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.WHITE);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(4f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        minorTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        minorTextPaint.setColor(Color.LTGRAY);
        minorTextPaint.setTextSize(30f);
        minorTextPaint.setTextAlign(Paint.Align.CENTER);

        degreePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        degreePaint.setColor(Color.BLACK);
        degreePaint.setTextSize(18f);
        degreePaint.setTextAlign(Paint.Align.CENTER);
        degreePaint.setFakeBoldText(true);

        degreeBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        degreeBgPaint.setColor(Color.rgb(0, 220, 255)); // Light Blue
        degreeBgPaint.setStyle(Paint.Style.FILL);

        tonicDegreeBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tonicDegreeBgPaint.setColor(Color.rgb(255, 140, 0)); // Orange
        tonicDegreeBgPaint.setStyle(Paint.Style.FILL);

        highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setColor(Color.argb(60, 0, 220, 255)); // Semi-transparent Light Blue for neighbors
        highlightPaint.setStyle(Paint.Style.FILL);

        tonicHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tonicHighlightPaint.setColor(Color.argb(120, 255, 140, 0)); // Semi-transparent Orange for tonic
        tonicHighlightPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();
            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;

            float dx = x - centerX;
            float dy = y - centerY;
            double distance = Math.sqrt(dx * dx + dy * dy);
            float radius = Math.min(centerX, centerY) * 0.9f;
            float innerRadius = radius * 0.7f;
            float innermostRadius = radius * 0.4f;

            if (distance >= innermostRadius && distance <= radius) {
                double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90;
                if (angle < 0) angle += 360;
                
                int index = (int) ((angle + 15) / 30) % 12;
                boolean currentIsMinor = distance < innerRadius;

                if (selectedTonicIndex == index && isMinorSelected == currentIsMinor) {
                    selectedTonicIndex = -1; // Toggle off
                } else {
                    selectedTonicIndex = index;
                    isMinorSelected = currentIsMinor;
                    performClick();
                }
                invalidate();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float radius = Math.min(centerX, centerY) * 0.9f;
        float innerRadius = radius * 0.7f;
        float innermostRadius = radius * 0.4f;

        if (selectedTonicIndex != -1) {
            drawSegmentHighlight(canvas, centerX, centerY, radius, innermostRadius, selectedTonicIndex, tonicHighlightPaint);
            drawSegmentHighlight(canvas, centerX, centerY, radius, innermostRadius, (selectedTonicIndex + 1) % 12, highlightPaint);
            drawSegmentHighlight(canvas, centerX, centerY, radius, innermostRadius, (selectedTonicIndex + 11) % 12, highlightPaint);
        }

        canvas.drawCircle(centerX, centerY, radius, circlePaint);
        canvas.drawCircle(centerX, centerY, innerRadius, circlePaint);
        canvas.drawCircle(centerX, centerY, innermostRadius, circlePaint);

        for (int i = 0; i < 12; i++) {
            double angle = Math.toRadians(i * 30 - 90 - 15);
            float startX = (float) (centerX + innermostRadius * Math.cos(angle));
            float startY = (float) (centerY + innermostRadius * Math.sin(angle));
            float endX = (float) (centerX + radius * Math.cos(angle));
            float endY = (float) (centerY + radius * Math.sin(angle));
            canvas.drawLine(startX, startY, endX, endY, circlePaint);
        }

        float textHeight = textPaint.descent() - textPaint.ascent();
        float textOffset = (textHeight / 2) - textPaint.descent();
        float minorTextOffset = ((minorTextPaint.descent() - minorTextPaint.ascent()) / 2) - minorTextPaint.descent();
        float degreeOffset = ((degreePaint.descent() - degreePaint.ascent()) / 2) - degreePaint.descent();

        float majorTextRadius = (radius + innerRadius) / 2f;
        float majorDegreeRadius = innerRadius + 22f;
        float minorTextRadius = (innerRadius + innermostRadius) / 2f;
        float minorDegreeRadius = innermostRadius + 20f;

        for (int i = 0; i < 12; i++) {
            double angle = Math.toRadians(i * 30 - 90);

            canvas.drawText(majorKeys[i], (float) (centerX + majorTextRadius * Math.cos(angle)), (float) (centerY + majorTextRadius * Math.sin(angle)) + textOffset, textPaint);
            canvas.drawText(minorKeys[i], (float) (centerX + minorTextRadius * Math.cos(angle)), (float) (centerY + minorTextRadius * Math.sin(angle)) + minorTextOffset, minorTextPaint);

            if (selectedTonicIndex != -1) {
                if (!isMinorSelected) {
                    if (i == selectedTonicIndex) {
                        drawDegreeBadge(canvas, centerX, centerY, majorDegreeRadius, angle, "I", degreeOffset, tonicDegreeBgPaint);
                        drawDegreeBadge(canvas, centerX, centerY, minorDegreeRadius, angle, "vi", degreeOffset, degreeBgPaint);
                    } else if (i == (selectedTonicIndex + 11) % 12) {
                        drawDegreeBadge(canvas, centerX, centerY, majorDegreeRadius, angle, "IV", degreeOffset, degreeBgPaint);
                        drawDegreeBadge(canvas, centerX, centerY, minorDegreeRadius, angle, "ii", degreeOffset, degreeBgPaint);
                    } else if (i == (selectedTonicIndex + 1) % 12) {
                        drawDegreeBadge(canvas, centerX, centerY, majorDegreeRadius, angle, "V", degreeOffset, degreeBgPaint);
                        drawDegreeBadge(canvas, centerX, centerY, minorDegreeRadius, angle, "iii", degreeOffset, degreeBgPaint);
                    }
                } else {
                    if (i == selectedTonicIndex) {
                        drawDegreeBadge(canvas, centerX, centerY, majorDegreeRadius, angle, "III", degreeOffset, degreeBgPaint);
                        drawDegreeBadge(canvas, centerX, centerY, minorDegreeRadius, angle, "i", degreeOffset, tonicDegreeBgPaint);
                    } else if (i == (selectedTonicIndex + 11) % 12) {
                        drawDegreeBadge(canvas, centerX, centerY, majorDegreeRadius, angle, "VI", degreeOffset, degreeBgPaint);
                        drawDegreeBadge(canvas, centerX, centerY, minorDegreeRadius, angle, "iv", degreeOffset, degreeBgPaint);
                    } else if (i == (selectedTonicIndex + 1) % 12) {
                        drawDegreeBadge(canvas, centerX, centerY, majorDegreeRadius, angle, "VII", degreeOffset, degreeBgPaint);
                        drawDegreeBadge(canvas, centerX, centerY, minorDegreeRadius, angle, "v", degreeOffset, degreeBgPaint);
                    }
                }
            }
        }
    }

    private void drawDegreeBadge(Canvas canvas, float centerX, float centerY, float radius, double angleRad, String label, float textOffset, Paint bgPaint) {
        float x = (float) (centerX + radius * Math.cos(angleRad));
        float y = (float) (centerY + radius * Math.sin(angleRad));
        canvas.drawCircle(x, y, 16f, bgPaint);
        canvas.drawText(label, x, y + textOffset, degreePaint);
    }

    private void drawSegmentHighlight(Canvas canvas, float centerX, float centerY, float outerRadius, float innerRadius, int index, Paint paint) {
        float startAngle = index * 30 - 90 - 15;
        canvas.drawArc(centerX - outerRadius, centerY - outerRadius, centerX + outerRadius, centerY + outerRadius, startAngle, 30, true, paint);
        Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskPaint.setColor(Color.parseColor("#121212"));
        canvas.drawCircle(centerX, centerY, innerRadius, maskPaint);
    }
}
