package com.teamabc.customviews;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by Matt on 4/11/2015.
 */

/*
    Gauge is setup in the following manner
                180
                xxx
              xx   xx
          90 xx     xx 270
              xx   xx
                xxx
                 0
*/
public class GaugeView extends View {

    private static final String TAG = GaugeView.class.getSimpleName();

    // Drawing
    private Paint mBackgroundPaint;
    private Bitmap mBackground;

    private RectF mRimRect;
    private Paint mRimPaint;
    private Paint mRimCirclePaint;
    private float mRimSize = 0.02f;

    private RectF mFaceRect;
    private Paint mFacePaint;

    private RectF mScaleRect;
    private Paint mScalePaint;
    private float mScalePosition = 0.05f;

    private Paint mNeedlePaint;
    private Paint mNeedleCenterPaint;
    private Path mNeedlePath;

    private Paint mValueTextPaint;
    private Paint mLabelTextPaint;

    private float degreesPerNick;
    private float mNeedleValue;

    // Configuration variables
    private boolean mShowLabel;
    private String mLabelText;
    private int mLabelTextColor;
    private boolean mShowValue;
    private int mValueTextColor;
    private int mScaleColor;
    private int mScaleMinNumber;
    private int mScaleMaxNumber;
    private int mScaleStartDegrees;
    private int mScaleEndDegrees;
    private int mScaleTotalNicks;
    private int mGaugeFaceStyle;
    private int mGaugeFaceColor;
    private int mNeedleColor;
    private int mNeedleCenterColor;

    public GaugeView(Context context) {
        super(context);
        init();
    }

    public GaugeView (Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.GaugeView,
                0, 0);

        try {
            mShowLabel = a.getBoolean(R.styleable.GaugeView_showLabel, true);
            mLabelText = a.getString(R.styleable.GaugeView_labelText);
            mLabelTextColor = a.getColor(R.styleable.GaugeView_labelTextColor, Color.WHITE);
            mShowValue = a.getBoolean(R.styleable.GaugeView_showValue, true);
            mValueTextColor = a.getColor(R.styleable.GaugeView_valueTextColor, Color.WHITE);
            mScaleColor = a.getColor(R.styleable.GaugeView_scaleColor, Color.WHITE);
            mScaleMinNumber = a.getInt(R.styleable.GaugeView_scaleMinNumber, 0);
            mScaleMaxNumber = a.getInt(R.styleable.GaugeView_scaleMaxNumber, 1000);
            mScaleStartDegrees = a.getInt(R.styleable.GaugeView_scaleStartDegrees, 50);
            mScaleEndDegrees = a.getInt(R.styleable.GaugeView_scaleEndDegrees, 310);
            mScaleTotalNicks = a.getInt(R.styleable.GaugeView_scaleTotalNicks, 10);
            mGaugeFaceStyle = a.getResourceId(R.styleable.GaugeView_gaugeFaceStyle, 0);
            mGaugeFaceColor = a.getColor(R.styleable.GaugeView_gaugeFaceColor, Color.BLACK);
            mNeedleColor = a.getColor(R.styleable.GaugeView_needleColor, Color.RED);
            mNeedleCenterColor = a.getColor(R.styleable.GaugeView_needleCenterColor, Color.WHITE);
        } finally {
            a.recycle();
        }

        init();
    }

    private void init() {
        // Setup paint objects for the different parts of the gauge
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setFilterBitmap(true);

        mLabelTextPaint = new Paint();
        mLabelTextPaint.setAntiAlias(true);
        mLabelTextPaint.setColor(mLabelTextColor);
        mLabelTextPaint.setStyle(Paint.Style.STROKE);
        mLabelTextPaint.setTextAlign(Paint.Align.CENTER);
        mLabelTextPaint.setStrokeWidth(0.007f);
        mLabelTextPaint.setTypeface(Typeface.SANS_SERIF);
        mLabelTextPaint.setLinearText(true);
        mLabelTextPaint.setTextSize(0.08f);
        mLabelTextPaint.setTextScaleX(0.8f);

        mValueTextPaint = mLabelTextPaint;
        mValueTextPaint.setColor(mValueTextColor);

        // Full Size of canvas
        mRimRect = new RectF(0.0f, 0.0f, 1.0f, 1.0f);

        // Rim Style
        mRimPaint = new Paint();
        mRimPaint.setAntiAlias(true);
        // TODO: Learn Linear Gradient
        mRimPaint.setShader(new LinearGradient(0.40f, 0.0f, 0.60f, 1.0f,
                Color.rgb(0xf0, 0xf5, 0xf0),
                Color.rgb(0x30, 0x31, 0x30),
                Shader.TileMode.CLAMP));

        // Border Style
        mRimCirclePaint = new Paint();
        mRimCirclePaint.setAntiAlias(true);
        mRimCirclePaint.setStyle(Paint.Style.STROKE);
        mRimCirclePaint.setColor(Color.argb(0x4f, 0x33, 0x36, 0x33));
        mRimCirclePaint.setStrokeWidth(0.005f);

        // Interior Position and Size
        mFaceRect = new RectF();
        mFaceRect.set(mRimRect.left + mRimSize, mRimRect.top + mRimSize,
                mRimRect.right - mRimSize, mRimRect.bottom - mRimSize);

        // Interior Style
        // TODO: Style
        mFacePaint = new Paint();
        mFacePaint.setAntiAlias(true);
        mFacePaint.setColor(mGaugeFaceColor);
        mFacePaint.setStyle(Paint.Style.FILL);

        // Scale Position
        mScaleRect = new RectF();
        mScaleRect.set(mFaceRect.left + mScalePosition, mFaceRect.top + mScalePosition,
                mFaceRect.right - mScalePosition, mFaceRect.bottom - mScalePosition);
        
        // Scale Style
        // TODO: Style
        mScalePaint = new Paint();
        mScalePaint.setStyle(Paint.Style.STROKE);
        mScalePaint.setColor(mScaleColor);
        mScalePaint.setStrokeWidth(0.005f);
        mScalePaint.setAntiAlias(true);
        mScalePaint.setTypeface(Typeface.SANS_SERIF);
        mScalePaint.setTextAlign(Paint.Align.CENTER);
        mScalePaint.setLinearText(true);
        mScalePaint.setTextSize(0.08f);
        mScalePaint.setTextScaleX(0.8f);

        // Needle Style
        mNeedlePaint = new Paint();
        mNeedlePaint.setAntiAlias(true);
        mNeedlePaint.setColor(mNeedleColor);
        mNeedlePaint.setStyle(Paint.Style.FILL);

        // Needle shape
        mNeedlePath = new Path();
        mNeedlePath.moveTo(0.5f - 0.005f, 0.5f - 0.35f);    // Left Tip
        mNeedlePath.lineTo(0.5f + 0.005f, 0.5f - 0.35f);    // Right Tip
        mNeedlePath.lineTo(0.5f + 0.01f, 0.5f);             // Right Base
        mNeedlePath.lineTo(0.5f - 0.01f, 0.5f);             // Left Base
        mNeedlePath.lineTo(0.5f - 0.005f, 0.5f - 0.35f);    // Back to left tip
        mNeedlePath.addCircle(0.5f, 0.5f, 0.025f, Path.Direction.CW);

        // Center circle style
        mNeedleCenterPaint = new Paint();
        mNeedleCenterPaint.setAntiAlias(true);
        mNeedleCenterPaint.setColor(mNeedleCenterColor);
        mNeedleCenterPaint.setStyle(Paint.Style.FILL);

        // Set needle value to start degrees
        //mNeedleValue = mScaleStartDegrees;
        // Fixes issue of needle no showing on Android > 5.0
        setLayerType(LAYER_TYPE_SOFTWARE, mScalePaint);
    }

    private void drawRim(Canvas canvas) {
        // first, draw the metallic body
        canvas.drawOval(mRimRect, mRimPaint);
        // now the outer rim circle
        canvas.drawOval(mRimRect, mRimCirclePaint);
    }

    private void drawFace(Canvas canvas) {
        canvas.drawOval(mFaceRect, mFacePaint);
    }

    private void drawScale(Canvas canvas) {
        canvas.save(Canvas.MATRIX_SAVE_FLAG);

        // Draw arc
        canvas.drawArc(mScaleRect, 90 + mScaleStartDegrees, mScaleEndDegrees - mScaleStartDegrees, false, mScalePaint);

        // Set start at desired degrees
        canvas.rotate(180 + mScaleStartDegrees, 0.5f, 0.5f);

        float currentAngle = mScaleStartDegrees;
        // Find number of nicks
        // TODO: Fix so this only displays "nice" numbers
        degreesPerNick = (mScaleEndDegrees - mScaleStartDegrees) / mScaleTotalNicks;
        for (int i = 0; i <= mScaleTotalNicks; ++i) {
            float y1 = mScaleRect.top;
            float y2 = y1 - 0.05f;
            // Text position
            float y3 = y1 + 0.1f;

            canvas.drawLine(0.5f, y1, 0.5f, y2, mScalePaint);

            long value = nickToNumber(i);
            // 3:57 AM - lol what am i even doing
            // Rotate so the text is all horizontal
            canvas.rotate(180 - currentAngle, 0.5f, y3);
            // TODO: Numbers shouldn't be hardcoded
            // Set as origin
            canvas.translate(0.5f - 0.07f, y3 - 0.035f);

            // Make text fit inside space given
            //RectF textAreaRect = new RectF(0, 0, 0.14f, 0.07f);
            //canvas.drawRect(textAreaRect, mScalePaint);
            //determineMaxTextSize(Long.toString(value), textAreaRect.width() * getWidth(), textAreaRect.height() * getHeight(), mScalePaint);
            canvas.drawText(Long.toString(value), 0.07f, 0.07f, mScalePaint);

            // Return to original origin
            canvas.translate(-0.5f + 0.07f, -y3 + 0.035f);

            // Rotate back
            canvas.rotate(-180 + currentAngle, 0.5f, y3);

            // Rotate to new angle
            canvas.rotate(degreesPerNick, 0.5f, 0.5f);
            currentAngle += degreesPerNick;
        }

        canvas.restore();
    }

    private void determineMaxTextSize(String str, float maxWidth, float maxHeight, Paint paint){
        int size = 0;

        do {
            paint.setTextSize(++ size);
            Log.d(TAG, String.format("%f < %f && %f < %f", paint.measureText(str), maxWidth, paint.getTextSize(), maxHeight));
        } while(paint.measureText(str) < maxWidth && paint.getTextSize() < maxHeight + 10);

        // Need to convert to float since we are working in the scale of 0 to 1.0
        paint.setTextSize((float)size/getHeight());
    }

    private Long nickToNumber(int nick) {
        float multiple = (mScaleMaxNumber - mScaleMinNumber) / mScaleTotalNicks;
        return (long) (multiple * nick) + mScaleMinNumber;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas);

        Log.d(TAG, "onDraw");
        // Convert from width x height to float coordinates
        float scale = (float) getWidth();
        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        canvas.scale(scale, scale);

        drawValue(canvas);
        drawHand(canvas);

        canvas.restore();
    }

    private void drawBackground(Canvas canvas) {
        if (mBackground == null) {
            Log.d(TAG, "Background not created");
        } else {
            // TODO: Fix where bitmap is set
            canvas.drawBitmap(mBackground, 0, 0, mBackgroundPaint);
        }
    }

    private void drawHand(Canvas canvas) {
        float handAngle = valueToAngle(mNeedleValue);
        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        canvas.rotate(180 + handAngle, 0.5f, 0.5f);
        canvas.drawPath(mNeedlePath, mNeedlePaint);
        canvas.restore();

        canvas.drawCircle(0.5f, 0.5f, 0.01f, mNeedleCenterPaint);
    }

    private void drawLabel(Canvas canvas) {
        if (mShowLabel && mLabelText != null)
            canvas.drawText(mLabelText, 0.5f, 0.8f, mLabelTextPaint);
    }

    private void drawValue(Canvas canvas) {
        if (mShowValue)
            canvas.drawText(String.format("%.0f", mNeedleValue), 0.5f, 0.65f, mValueTextPaint);
    }

    private float valueToAngle(float value) {
        float OldRange = (mScaleMaxNumber - mScaleMinNumber);
        float NewRange = (mScaleEndDegrees - mScaleStartDegrees);
        return (((value - mScaleMinNumber) * NewRange) / OldRange) + mScaleStartDegrees;
    }

    // Public method to change gauge value
    public void setValue(float newValue) {
        // Setup value animator
        ValueAnimator va = ValueAnimator.ofObject(new TypeEvaluator<Float>() {
            @Override
            public Float evaluate(float fraction, Float startValue, Float endValue) {
                return startValue + fraction * (endValue - startValue);
            }
        }, mNeedleValue, newValue);

        va.setDuration(250);
        // Update value
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                Float value = (Float) animation.getAnimatedValue();
                if (value != null)
                    setNeedleValue(value);
            }
        });
        // When done, finalize the animator
        va.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                try {
                    super.finalize();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        });
        va.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.d(TAG, "onSizeChanged");
        regenerateBackground();
    }

    public void onSizeChange() {
        onSizeChanged(getWidth(), getHeight(), getWidth(), getHeight());
    }

    private void regenerateBackground() {
        // Free bitmap
        if (mBackground != null) {
            mBackground.recycle();
        }

        // TODO: Check which is faster
        /*
        mBackground = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        mBackground = BitmapFactory.decodeResource(getResources(), R.drawable.gauge_style_1);
        int width = mBackground.getWidth();
        int height = mBackground.getWidth();
        float scaleWidth = ((float) getWidth()) / width;
        float scaleHeight = ((float) getHeight()) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        mBackground = mBackground.createBitmap(mBackground, 0, 0, width, height, matrix, true);
        */

        Canvas backgroundCanvas;
        // If style not selected
        if (mGaugeFaceStyle == 0) {
            mBackground = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            backgroundCanvas = new Canvas(mBackground);
            float scale = (float) getWidth();
            backgroundCanvas.scale(scale, scale);
            drawRim(backgroundCanvas);
            drawFace(backgroundCanvas);
        }
        else {
            mBackground = BitmapFactory.decodeResource(getResources(), mGaugeFaceStyle);
            mBackground = Bitmap.createScaledBitmap(mBackground, getWidth(), getHeight(), true);
            backgroundCanvas = new Canvas(mBackground);
            float scale = (float) getWidth();
            backgroundCanvas.scale(scale, scale);
        }

        drawLabel(backgroundCanvas);
        drawScale(backgroundCanvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int chosenWidth = chooseDimension(widthMode, widthSize);
        int chosenHeight = chooseDimension(heightMode, heightSize);

        int chosenDimension = Math.min(chosenWidth, chosenHeight);

        setMeasuredDimension(chosenDimension, chosenDimension);
    }

    private int chooseDimension(int mode, int size) {
        if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
            return size;
        } else { // (mode == MeasureSpec.UNSPECIFIED)
            return getPreferredSize();
        }
    }

    // in case there is no size specified
    private int getPreferredSize() {
        return 300;
    }


    // Public getters and setters
    public float getNeedleValue() {
        return mNeedleValue;
    }

    public void setNeedleValue(float mNeedleValue) {
        if (mNeedleValue < mScaleMinNumber)
            this.mNeedleValue = mScaleMinNumber;
        else if (mNeedleValue > mScaleMaxNumber) {
            this.mNeedleValue = mScaleMaxNumber;
        }
        else {
            this.mNeedleValue = mNeedleValue;
        }
        invalidate();
    }

    public boolean ismShowLabel() {
        return mShowLabel;
    }

    public void setShowLabel(boolean mShowLabel) {
        this.mShowLabel = mShowLabel;
        invalidate();
    }

    public String getLabelText() {
        return mLabelText;
    }

    public void setLabelText(String mLabelText) {
        this.mLabelText = mLabelText;
        invalidate();
    }

    public int getLabelTextColor() {
        return mLabelTextColor;
    }

    public void setLabelTextColor(int mLabelTextColor) {
        this.mLabelTextColor = mLabelTextColor;
        invalidate();
    }

    public boolean ismShowValue() {
        return mShowValue;
    }

    public void setShowValue(boolean mShowValue) {
        this.mShowValue = mShowValue;
        invalidate();
    }

    public int getValueTextColor() {
        return mValueTextColor;
    }

    public void setValueTextColor(int mValueTextColor) {
        this.mValueTextColor = mValueTextColor;
        invalidate();
    }

    public int getScaleColor() {
        return mScaleColor;
    }

    public void setScaleColor(int mScaleColor) {
        this.mScaleColor = mScaleColor;
        invalidate();
    }

    public int getScaleMinNumber() {
        return mScaleMinNumber;
    }

    public void setScaleMinNumber(int mScaleMinNumber) {
        this.mScaleMinNumber = mScaleMinNumber;
        invalidate();
    }

    public int getScaleMaxNumber() {
        return mScaleMaxNumber;
    }

    public void setScaleMaxNumber(int mScaleMaxNumber) {
        this.mScaleMaxNumber = mScaleMaxNumber;
        invalidate();
    }

    public int getScaleStartDegrees() {
        return mScaleStartDegrees;
    }

    public void setScaleStartDegrees(int mScaleStartDegrees) {
        this.mScaleStartDegrees = mScaleStartDegrees;
        invalidate();
    }

    public int getScaleEndDegrees() {
        return mScaleEndDegrees;
    }

    public void setScaleEndDegrees(int mScaleEndDegrees) {
        this.mScaleEndDegrees = mScaleEndDegrees;
        invalidate();
    }

    public int getScaleTotalNicks() {
        return mScaleTotalNicks;
    }

    public void setScaleTotalNicks(int mScaleTotalNicks) {
        this.mScaleTotalNicks = mScaleTotalNicks;
        invalidate();
    }

    public int getGaugeFaceStyle() {
        return mGaugeFaceStyle;
    }

    public void setGaugeFaceStyle(int mGaugeFaceStyle) {
        this.mGaugeFaceStyle = mGaugeFaceStyle;
        invalidate();
    }

    public int getGaugeFaceColor() {
        return mGaugeFaceColor;
    }

    public void setGaugeFaceColor(int mGaugeFaceColor) {
        this.mGaugeFaceColor = mGaugeFaceColor;
        invalidate();
    }

    public int getNeedleColor() {
        return mNeedleColor;
    }

    public void setNeedleColor(int mNeedleColor) {
        this.mNeedleColor = mNeedleColor;
        invalidate();
    }

    public int getNeedleCenterColor() {
        return mNeedleCenterColor;
    }

    public void setNeedleCenterColor(int mNeedleCenterColor) {
        this.mNeedleCenterColor = mNeedleCenterColor;
        invalidate();
    }

}
