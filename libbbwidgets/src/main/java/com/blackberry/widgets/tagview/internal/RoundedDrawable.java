
package com.blackberry.widgets.tagview.internal;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class RoundedDrawable extends Drawable {
    private final Paint mPaint = new Paint();
    private RectF mRectF;
    private Bitmap mBitmap;
    private boolean mDrawBorder = false;
    private final Paint mBorderPaint = new Paint();
    private RectF mDrawRectF;

    public RoundedDrawable(Bitmap bitmap) {
        init(bitmap);
    }

    public RoundedDrawable(Bitmap bitmap, Rect bounds) {
        init(bitmap, bounds);
    }

    public RoundedDrawable(Resources resources, int id) {
        init(BitmapFactory.decodeResource(resources, id));
    }

    private void init(Bitmap bitmap) {
        init(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()));
    }

    private void init(Bitmap bitmap, Rect bounds) {
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setColor(0xFF989897);
        mBorderPaint.setAntiAlias(true);
        
        mRectF = new RectF(bounds);
        setBounds(bounds);

        mBitmap = Bitmap.createScaledBitmap(bitmap, bounds.width(), bounds.height(), false);

        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setShader(new BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));

    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawOval(mDrawRectF, mPaint);
        if (mDrawBorder) {
            canvas.drawOval(mDrawRectF, mBorderPaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        if (mPaint.getAlpha() != alpha) {
            mPaint.setAlpha(alpha);
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicHeight() {
        return mBitmap.getHeight();
    }

    @Override
    public int getIntrinsicWidth() {
        return mBitmap.getWidth();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        mRectF.set(bounds);
        mDrawRectF = calculateRectWithBorder();
        super.onBoundsChange(bounds);
    }

    /**
     * @return Whether or not to draw a border. Defaults to false.
     */
    public boolean getDrawBorder() {
        return mDrawBorder;
    }

    /**
     * @param drawBorder Whether or not to draw a border
     */
    public void setDrawBorder(boolean drawBorder) {
        mDrawBorder = drawBorder;
        mDrawRectF = calculateRectWithBorder();
    }

    private RectF calculateRectWithBorder() {
        if (!mDrawBorder) {
            return mRectF;
        } else {
            mDrawRectF = new RectF(mRectF);
            mDrawRectF.inset(mBorderPaint.getStrokeWidth() / 2.0f, mBorderPaint.getStrokeWidth() / 2.0f);
            return mDrawRectF;
        }
    }

    /**
     * @return The width of the border
     */
    public float getBorderWidth() {
        return mBorderPaint.getStrokeWidth();
    }

    /**
     * @param borderWidth The width of the border. Defaults to the default
     *            stroke width of {@link Paint#getStrokeWidth()}
     */
    public void setBorderWidth(float borderWidth) {
        mBorderPaint.setStrokeWidth(borderWidth);
        mDrawRectF = calculateRectWithBorder();
    }

    /**
     * @return The color of the border to draw. Defaults to 0x989897.
     */
    public int getBorderColor() {
        return mBorderPaint.getColor();
    }

    /**
     * @param color The color of the border to draw
     */
    public void setBorderColor(int color) {
        mBorderPaint.setColor(color);
    }
}
