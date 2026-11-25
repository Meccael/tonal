package com.meccael.tonal.soundanimation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import androidx.core.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import java.lang.reflect.Array;
import java.util.Random;
import com.meccael.tonal.R;

/* JADX INFO: loaded from: classes.dex */
public class BeatView extends View {
    public static final int DEFAULT_BLOCK_SPACING = 20;
    public static final int DEFAULT_NUMBER_BLOCK = 3;
    public static final int DEFAULT_NUMBER_RANDOM_VALUES = 10;
    public static final int DEFAULT_SPEED = 10;
    public static final boolean DEFAULT_START_OFF = false;
    public static final int DEFAULT_STOP_SIZE = 30;
    public static final int FPS = 60;
    public static final String LOG_TAG = "VuMeterView";
    public static final int STATE_PAUSE = 0;
    public static final int STATE_PLAYING = 2;
    public static final int STATE_STOP = 1;
    private int mBlockNumber;
    private int mBlockPass;
    private float mBlockSpacing;
    private float[][] mBlockValues;
    private int mBlockWidth;
    private int mColor;
    private int mContentHeight;
    private int mContentWidth;
    private Dynamics[] mDestinationValues;
    private int mDrawPass;
    private int mLeft;
    private int mPaddingBottom;
    private int mPaddingLeft;
    private int mPaddingRight;
    private int mPaddingTop;
    private Paint mPaint;
    private Random mRandom;
    private int mRight;
    private int mSpeed;
    private int mState;
    private float mStopSize;
    private int mTop;

    public BeatView(Context context) {
        super(context);
        this.mPaint = new Paint();
        this.mRandom = new Random();
        init(null, 0);
    }

    public BeatView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mPaint = new Paint();
        this.mRandom = new Random();
        init(attributeSet, 0);
    }

    public BeatView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mPaint = new Paint();
        this.mRandom = new Random();
        init(attributeSet, i);
    }

    private void changeDynamicsTarget(int i, float f) {
        incrementAndGetDrawPass();
        this.mDestinationValues[i].setTargetPosition(f);
    }

    private int incrementAndGetDrawPass() {
        this.mDrawPass++;
        if (this.mDrawPass >= 10) {
            this.mDrawPass = 0;
        }
        return this.mDrawPass;
    }

    private void init(AttributeSet attributeSet, int i) {
        TypedArray typedArrayObtainStyledAttributes = getContext().obtainStyledAttributes(attributeSet, R.styleable.VuMeterView, i, 0);
        this.mColor = typedArrayObtainStyledAttributes.getColor(0, ViewCompat.MEASURED_STATE_MASK);
        this.mBlockNumber = typedArrayObtainStyledAttributes.getInt(R.styleable.VuMeterView_blockNumber, 3);
        this.mBlockSpacing = typedArrayObtainStyledAttributes.getDimension(R.styleable.VuMeterView_blockSpacing, 20.0f);
        this.mSpeed = typedArrayObtainStyledAttributes.getInt(R.styleable.VuMeterView_speed, 10);
        this.mStopSize = typedArrayObtainStyledAttributes.getDimension(R.styleable.VuMeterView_stopSize, 30.0f);
        boolean z = typedArrayObtainStyledAttributes.getBoolean(R.styleable.VuMeterView_startOff, false);
        typedArrayObtainStyledAttributes.recycle();
        initialiseCollections();
        this.mPaint.setColor(this.mColor);
        if (z) {
            this.mState = 0;
        } else {
            this.mState = 2;
        }
        this.mRight = 0;
        this.mPaddingBottom = 0;
        this.mPaddingRight = 0;
        this.mTop = 0;
        this.mLeft = 0;
        this.mPaddingTop = 0;
        this.mPaddingLeft = 0;
        this.mContentWidth = 0;
        this.mContentHeight = 0;
        this.mBlockPass = 0;
        this.mDrawPass = 0;
    }

    private void initialiseCollections() {
        this.mBlockValues = (float[][]) Array.newInstance((Class<?>) Float.TYPE, this.mBlockNumber, 10);
        this.mDestinationValues = new Dynamics[this.mBlockNumber];
        updateRandomValues();
    }

    private void pickNewDynamics(int i, float f) {
        this.mDestinationValues[this.mBlockPass] = new Dynamics(this.mSpeed, f);
        incrementAndGetDrawPass();
        this.mDestinationValues[this.mBlockPass].setTargetPosition(i * this.mBlockValues[this.mBlockPass][this.mDrawPass]);
    }

    private void updateRandomValues() {
        for (int i = 0; i < this.mBlockNumber; i++) {
            for (int i2 = 0; i2 < 10; i2++) {
                this.mBlockValues[i][i2] = this.mRandom.nextFloat();
                if (this.mBlockValues[i][i2] < 0.1d) {
                    this.mBlockValues[i][i2] = 0.1f;
                }
            }
        }
    }

    public int getBlockNumber() {
        return this.mBlockNumber;
    }

    public float getBlockSpacing() {
        return this.mBlockSpacing;
    }

    public int getColor() {
        return this.mColor;
    }

    public int getSpeed() {
        return this.mSpeed;
    }

    @Override // android.view.View
    @SuppressLint({"DrawAllocation"})
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.mPaddingLeft = getPaddingLeft();
        this.mPaddingTop = getPaddingTop();
        this.mPaddingRight = getPaddingRight();
        this.mPaddingBottom = getPaddingBottom();
        this.mContentWidth = (getWidth() - this.mPaddingLeft) - this.mPaddingRight;
        this.mContentHeight = (getHeight() - this.mPaddingTop) - this.mPaddingBottom;
        if (this.mBlockWidth == 0) {
            this.mBlockWidth = (int) ((this.mContentWidth - ((this.mBlockNumber - 1) * this.mBlockSpacing)) / this.mBlockNumber);
            if (this.mState == 0) {
                int i = (int) (this.mContentHeight - this.mStopSize);
                for (int i2 = 0; i2 < this.mBlockNumber; i2++) {
                    this.mDestinationValues[i2] = new Dynamics(this.mSpeed, i);
                    this.mDestinationValues[i2].setAtRest(true);
                }
            }
        }
        this.mBlockPass = 0;
        this.mBlockPass = 0;
        while (this.mBlockPass < this.mBlockNumber) {
            this.mLeft = this.mPaddingLeft + (this.mBlockPass * this.mBlockWidth);
            this.mLeft = (int) (this.mLeft + (this.mBlockSpacing * this.mBlockPass));
            this.mRight = this.mLeft + this.mBlockWidth;
            if (this.mDestinationValues[this.mBlockPass] == null) {
                pickNewDynamics(this.mContentHeight, this.mContentHeight * this.mBlockValues[this.mBlockPass][this.mDrawPass]);
            }
            if (this.mDestinationValues[this.mBlockPass].isAtRest() && this.mState == 2) {
                changeDynamicsTarget(this.mBlockPass, this.mContentHeight * this.mBlockValues[this.mBlockPass][this.mDrawPass]);
            } else if (this.mState != 0) {
                this.mDestinationValues[this.mBlockPass].update();
            }
            this.mTop = this.mPaddingTop + ((int) this.mDestinationValues[this.mBlockPass].getPosition());
            canvas.drawRect(this.mLeft, this.mTop, this.mRight, this.mContentHeight, this.mPaint);
            this.mBlockPass++;
        }
        postInvalidateDelayed(16L);
    }

    public void pause() {
        this.mState = 0;
    }

    public void resume(boolean z) {
        if (this.mState == 0) {
            this.mState = 2;
            return;
        }
        this.mState = 2;
        if (z) {
            return;
        }
        for (int i = 0; i < this.mBlockNumber; i++) {
            this.mDestinationValues[i].setPosition(this.mContentHeight * this.mBlockValues[i][this.mDrawPass]);
            changeDynamicsTarget(i, this.mContentHeight * this.mBlockValues[i][this.mDrawPass]);
        }
    }

    public void setBlockNumber(int i) {
        this.mBlockNumber = i;
        initialiseCollections();
        this.mBlockPass = 0;
        this.mBlockWidth = 0;
    }

    public void setBlockSpacing(float f) {
        this.mBlockSpacing = f;
        this.mBlockWidth = 0;
    }

    public void setColor(int i) {
        this.mColor = i;
    }

    public void setSpeed(int i) {
        this.mSpeed = i;
    }

    public void stop(boolean z) {
        if (this.mDestinationValues == null) {
            initialiseCollections();
        }
        this.mState = 1;
        int i = (int) (this.mContentHeight - this.mStopSize);
        if (this.mDestinationValues.length <= 0) {
            return;
        }
        for (int i2 = 0; i2 < this.mBlockNumber; i2++) {
            if (this.mDestinationValues[i2] != null) {
                if (z) {
                    this.mDestinationValues[i2].setTargetPosition(i);
                } else {
                    this.mDestinationValues[i2].setPosition(i);
                }
            }
        }
    }
}
