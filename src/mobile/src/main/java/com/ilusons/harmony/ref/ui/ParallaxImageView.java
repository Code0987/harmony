package com.ilusons.harmony.ref.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ParallaxImageView extends AppCompatImageView {

	private boolean centerCrop = true;

	public boolean getCenterCrop() {
		return centerCrop;
	}

	public void setCenterCrop(boolean value) {
		centerCrop = value;
	}

	private float parallaxRatio = 0.75f;

	public float getParallaxRatio() {
		return parallaxRatio;
	}

	public void setParallaxRatio(float value) {
		parallaxRatio = value;
	}

	public interface ParallaxImageListener {
		int[] getValuesForTranslate();
	}

	private ParallaxImageListener parallaxImageListener;

	public ParallaxImageListener getListener() {
		return parallaxImageListener;
	}

	public void setListener(ParallaxImageListener listener) {
		parallaxImageListener = listener;
	}

	public ParallaxImageView(Context context) {
		super(context);

		init(context, null);
	}

	public ParallaxImageView(Context context, AttributeSet attrs) {
		super(context, attrs);

		init(context, attrs);
	}

	public ParallaxImageView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {
		setScaleType(ImageView.ScaleType.MATRIX);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		ensureTranslate();
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);

		ensureTranslate();
	}

	@Override
	public void setImageDrawable(Drawable drawable) {
		super.setImageDrawable(drawable);

		ensureTranslate();
	}

	@Override
	public void setImageBitmap(Bitmap bm) {
		super.setImageBitmap(bm);

		ensureTranslate();
	}

	@Override
	public void setImageResource(int resId) {
		super.setImageResource(resId);

		ensureTranslate();
	}

	@Override
	public void setImageURI(Uri uri) {
		super.setImageURI(uri);

		ensureTranslate();
	}

	private boolean needToTranslate = true;
	private int rowY = -1;
	private int recyclerViewHeight = -1;
	private int recyclerViewY = -1;

	private boolean getValues() {
		int[] values = getListener().getValuesForTranslate();
		if (values == null)
			return false;

		rowY = values[0];
		recyclerViewHeight = values[1];
		recyclerViewY = values[2];

		return true;
	}

	public void reset() {
		needToTranslate = true;
	}

	private boolean ensureTranslate() {
		if (needToTranslate) {
			needToTranslate = !translate();
		}

		return !needToTranslate;
	}

	public synchronized boolean translate() {
		if (getDrawable() == null) {
			return false;
		}

		if (getListener() != null && getValues()) {
			float distanceFromCenter = (recyclerViewY + recyclerViewHeight) / 2 - rowY;

			int drawableHeight = getDrawable().getIntrinsicHeight();
			int imageViewHeight = getMeasuredHeight();
			float scale = 1;

			if (centerCrop) {
				final int viewWidth = getWidth() - getPaddingLeft() - getPaddingRight();
				final int viewHeight = getHeight() - getPaddingTop() - getPaddingBottom();
				final int drawableWidth = getDrawable().getIntrinsicWidth();
				final int drawableHeight1 = getDrawable().getIntrinsicHeight();

				if (drawableWidth * viewHeight > drawableHeight1 * viewWidth) {
					scale = (float) viewHeight / (float) drawableHeight1;
				} else {
					scale = (float) viewWidth / (float) drawableWidth;
				}

				drawableHeight *= scale;
			}

			float difference = drawableHeight - imageViewHeight;
			float move = (distanceFromCenter / recyclerViewHeight) * difference * parallaxRatio;

			Matrix imageMatrix = getImageMatrix();
			if (scale != 1) {
				imageMatrix.setScale(scale, scale);
			}

			float[] matrixValues = new float[9];
			imageMatrix.getValues(matrixValues);

			float y = matrixValues[Matrix.MTRANS_Y];
			imageMatrix.postTranslate(0, (move / 2) - (difference / 2) - y);

			setImageMatrix(imageMatrix);

			invalidate();

			return true;
		} else {
			return false;
		}
	}

}
