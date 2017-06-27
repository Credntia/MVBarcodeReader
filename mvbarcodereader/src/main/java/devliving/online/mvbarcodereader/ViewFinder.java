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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

public class ViewFinder extends View {

    Paint paint;

    final int STROKE_WIDTH = 16;
    final int L = 50;
    private static final int mVFColor = Color.parseColor("#fb1111");
    private Point centerOfCanvas;

    public Point getCenterOfCanvas() {
        return centerOfCanvas;
    }

    public ViewFinder(Context context) {
        super(context);
        init();
    }

    public ViewFinder(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ViewFinder(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init(){

        final int selectedColor = mVFColor;

        paint = new Paint();
        paint = new Paint();
        paint.setColor(selectedColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(STROKE_WIDTH);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int canvasW = getWidth();
        int canvasH = getHeight();
        centerOfCanvas = new Point(canvasW / 2, canvasH / 2);

        canvas.drawLine(centerOfCanvas.x - L,centerOfCanvas.y ,centerOfCanvas.x + L,centerOfCanvas.y ,paint);
        canvas.drawLine(centerOfCanvas.x ,centerOfCanvas.y- L ,centerOfCanvas.x ,centerOfCanvas.y+ L ,paint);

    }

}