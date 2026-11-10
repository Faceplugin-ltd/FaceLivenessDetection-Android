package com.faceplugin.faceliveness.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.faceplugin.faceliveness.R;
import com.faceplugin.facelivenessdk.FaceBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FaceView extends View {

    private Context context;
    private Paint realPaint;
    private Paint spoofPaint;
    private Paint trackPaint;
    private Paint textPaint;
    private Paint textBgPaint;

    private Size frameSize;
    private boolean mirrorX;
    private List<FaceBox> faceBoxes;
    private float lineHeight;

    public FaceView(Context context) {
        this(context, null);
    }

    public FaceView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        realPaint = strokePaint(ContextCompat.getColor(context, R.color.liveness_real));
        spoofPaint = strokePaint(ContextCompat.getColor(context, R.color.liveness_spoof));
        trackPaint = strokePaint(ContextCompat.getColor(context, R.color.liveness_neutral));

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(ContextCompat.getColor(context, R.color.fp_text));
        textPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 13f, getResources().getDisplayMetrics()));
        textPaint.setFakeBoldText(true);
        lineHeight = textPaint.getTextSize() * 1.25f;

        textBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textBgPaint.setStyle(Paint.Style.FILL);
        textBgPaint.setColor(0x99000000);
    }

    private static Paint strokePaint(int color) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(color);
        return paint;
    }

    public void setFrameSize(Size frameSize) {
        this.frameSize = frameSize;
    }

    public void setMirrorX(boolean mirrorX) {
        this.mirrorX = mirrorX;
    }

    public void setFaceBoxes(List<FaceBox> faceBoxes) {
        this.faceBoxes = faceBoxes;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (frameSize == null || faceBoxes == null) return;

        float frameW = frameSize.getWidth();
        float frameH = frameSize.getHeight();
        float viewW = canvas.getWidth();
        float viewH = canvas.getHeight();
        if (frameW <= 0f || frameH <= 0f || viewW <= 0f || viewH <= 0f) return;

        float scale = Math.max(viewW / frameW, viewH / frameH);
        float dx = (viewW - frameW * scale) / 2f;
        float dy = (viewH - frameH * scale) / 2f;

        for (FaceBox faceBox : faceBoxes) {
            float left = mapX(faceBox.x1, scale, dx, viewW);
            float right = mapX(faceBox.x2, scale, dx, viewW);
            float top = faceBox.y1 * scale + dy;
            float bottom = faceBox.y2 * scale + dy;
            if (left > right) {
                float tmp = left;
                left = right;
                right = tmp;
            }

            boolean livenessKnown = hasLiveness(faceBox);
            boolean live = livenessKnown
                    && SettingsActivity.livenessPassed(context, faceBox.liveness, faceBox.livenessLabel);
            Paint boxPaint;
            if (!livenessKnown) {
                boxPaint = trackPaint;
            } else if (live) {
                boxPaint = realPaint;
            } else {
                boxPaint = spoofPaint;
            }

            canvas.drawRect(new Rect((int) left, (int) top, (int) right, (int) bottom), boxPaint);
            drawInfoBlock(canvas, faceBox, left, top, live, livenessKnown);
        }
    }

    private void drawInfoBlock(
            Canvas canvas,
            FaceBox faceBox,
            float boxLeft,
            float boxTop,
            boolean live,
            boolean livenessKnown) {
        List<String> lines = buildLines(faceBox, live, livenessKnown);
        if (lines.isEmpty()) return;

        float padX = 8f;
        float padY = 6f;
        float maxWidth = 0f;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, textPaint.measureText(line));
        }
        float blockW = maxWidth + padX * 2f;
        float blockH = lines.size() * lineHeight + padY * 2f;
        float left = boxLeft;
        float top = boxTop - blockH - 6f;
        if (top < 4f) {
            top = Math.min(boxTop + 6f, canvas.getHeight() - blockH - 4f);
        }
        if (left + blockW > canvas.getWidth() - 4f) {
            left = Math.max(4f, canvas.getWidth() - blockW - 4f);
        }

        canvas.drawRect(left, top, left + blockW, top + blockH, textBgPaint);
        float textY = top + padY + textPaint.getTextSize();
        for (String line : lines) {
            canvas.drawText(line, left + padX, textY, textPaint);
            textY += lineHeight;
        }
    }

    private List<String> buildLines(FaceBox faceBox, boolean live, boolean livenessKnown) {
        List<String> lines = new ArrayList<>(4);
        int trackId = faceBox.trackId >= 0 ? faceBox.trackId : 0;
        lines.add(String.format(Locale.US, "TrackID : %d", trackId));

        if (livenessKnown) {
            String verdict = live ? "Real" : "Spoof";
            int pct = Math.round(Math.max(0f, Math.min(1f, faceBox.liveness)) * 100f);
            lines.add(String.format(Locale.US, "Liveness : %s %d%%", verdict, pct));
        } else {
            lines.add("Liveness : —");
        }

        if (faceBox.face_luminance > 0.001f) {
            int lum = Math.round(Math.max(0f, Math.min(1f, faceBox.face_luminance)) * 100f);
            lines.add(String.format(Locale.US, "Luminance : %d%%", lum));
        } else {
            lines.add("Luminance : —");
        }

        float scoreValue = faceBox.score;
        if (scoreValue < 0f && faceBox.face_quality > 0.001f) {
            scoreValue = faceBox.face_quality;
        }
        if (scoreValue < 0f && faceBox.liveness > 0.001f) {
            scoreValue = faceBox.liveness;
        }
        if (scoreValue >= 0f) {
            int scorePct = Math.round(Math.max(0f, Math.min(1f, scoreValue)) * 100f);
            lines.add(String.format(Locale.US, "Score %d%%", scorePct));
        } else {
            lines.add("Score —");
        }
        return lines;
    }

    private float mapX(float frameX, float scale, float dx, float viewW) {
        float vx = frameX * scale + dx;
        return mirrorX ? viewW - vx : vx;
    }

    private static boolean hasLiveness(FaceBox faceBox) {
        if (faceBox.livenessLabel != null && !faceBox.livenessLabel.isEmpty()) return true;
        return faceBox.liveness > 0.001f;
    }
}
