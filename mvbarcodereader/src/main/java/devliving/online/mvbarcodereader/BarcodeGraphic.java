/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package devliving.online.mvbarcodereader;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

import com.google.android.gms.vision.barcode.Barcode;

import devliving.online.mvbarcodereader.camera.GraphicOverlay;

/**
 * Graphic instance for rendering barcode position, size, and ID within an associated graphic
 * overlay view.
 */
public class BarcodeGraphic extends GraphicOverlay.Graphic {

    final int STROKE_WIDTH = 16;
    final int CORNER_WIDTH = 56;

    private int mId;

    private static final int COLOR_CHOICES[] = {
            Color.parseColor("#fba549"),
            Color.parseColor("#418bfa"),
            Color.parseColor("#aafc7d"),
            Color.parseColor("#8149fb")
    };

    private static int mCurrentColorIndex = 0;

    private Paint mRectPaint, mOverlayPaint;

    private volatile Barcode mBarcode;

    BarcodeGraphic(GraphicOverlay overlay) {
        super(overlay);

        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.length;
        final int selectedColor = COLOR_CHOICES[mCurrentColorIndex];

        mRectPaint = new Paint();
        mRectPaint.setColor(selectedColor);
        mRectPaint.setStyle(Paint.Style.STROKE);
        mRectPaint.setStrokeWidth(STROKE_WIDTH);
        mRectPaint.setStrokeCap(Paint.Cap.ROUND);
        mRectPaint.setStrokeJoin(Paint.Join.ROUND);

        mOverlayPaint = new Paint();
        mOverlayPaint.setStyle(Paint.Style.FILL);
        mOverlayPaint.setColor(selectedColor);
        mOverlayPaint.setAlpha(100);
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public Barcode getBarcode() {
        return mBarcode;
    }

    /**
     * Updates the barcode instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateItem(Barcode barcode) {
        mBarcode = barcode;
        postInvalidate();
    }

    public boolean isPointInsideBarcode(float x, float y) {
        Barcode barcode = mBarcode;
        if (barcode != null) {
            RectF rect = getViewBoundingBox(barcode);
            Log.d("BARCODE", "rect: t: " + rect.top + ", l: " + rect.left + ", r: " + rect.right + ", b: " + rect.bottom +
                    "/ x: " + x + ", y: " + y);
            return rect.contains(x, y);
        }

        return false;
    }

    private RectF getViewBoundingBox(Barcode barcode) {
        // Draws the bounding box around the barcode.
        RectF rect = new RectF(barcode.getBoundingBox());
        rect.left = translateX(rect.left);
        rect.top = translateY(rect.top);
        rect.right = translateX(rect.right);
        rect.bottom = translateY(rect.bottom);

        return rect;
    }
    /**
     * Draws the barcode annotations for position, size, and raw value on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        Barcode barcode = mBarcode;
        if (barcode == null) {
            return;
        }

        // Draws the bounding box around the barcode.
        RectF rect = getViewBoundingBox(barcode);

        canvas.drawRect(rect, mOverlayPaint);

        /**
         * Draw the top left corner
         */
        canvas.drawLine(rect.left, rect.top, rect.left + CORNER_WIDTH, rect.top, mRectPaint);
        canvas.drawLine(rect.left, rect.top, rect.left, rect.top + CORNER_WIDTH, mRectPaint);

        /**
         * Draw the bottom left corner
         */
        canvas.drawLine(rect.left, rect.bottom, rect.left, rect.bottom - CORNER_WIDTH, mRectPaint);
        canvas.drawLine(rect.left, rect.bottom, rect.left + CORNER_WIDTH, rect.bottom, mRectPaint);

        /**
         * Draw the top right corner
         */
        canvas.drawLine(rect.right, rect.top, rect.right - CORNER_WIDTH, rect.top, mRectPaint);
        canvas.drawLine(rect.right, rect.top, rect.right, rect.top + CORNER_WIDTH, mRectPaint);

        /**
         * Draw the bottom right corner
         */
        canvas.drawLine(rect.right, rect.bottom, rect.right - CORNER_WIDTH, rect.bottom, mRectPaint);
        canvas.drawLine(rect.right, rect.bottom, rect.right, rect.bottom - CORNER_WIDTH, mRectPaint);
    }
}
