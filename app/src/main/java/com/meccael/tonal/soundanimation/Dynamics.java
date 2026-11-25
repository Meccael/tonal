package com.meccael.tonal.soundanimation;

/* JADX INFO: loaded from: classes.dex */
public class Dynamics {
    private boolean mIsToTarget = false;
    private float mPosition;
    private int mStep;
    private float mTargetPosition;

    public Dynamics(int i, float f) {
        this.mStep = i;
        this.mPosition = f;
    }

    public float getPosition() {
        return this.mPosition;
    }

    public boolean isAtRest() {
        return this.mIsToTarget;
    }

    public void setAtRest(boolean z) {
        this.mIsToTarget = z;
    }

    public void setPosition(float f) {
        this.mPosition = f;
        this.mIsToTarget = true;
    }

    public void setTargetPosition(float f) {
        this.mTargetPosition = f;
        this.mIsToTarget = false;
    }

    public void update() {
        if (this.mIsToTarget) {
            return;
        }
        if (this.mTargetPosition > this.mPosition) {
            this.mPosition += this.mStep;
            if (this.mPosition >= this.mTargetPosition) {
                this.mPosition = this.mTargetPosition;
                this.mIsToTarget = true;
                return;
            }
            return;
        }
        this.mPosition -= this.mStep;
        if (this.mPosition <= this.mTargetPosition) {
            this.mPosition = this.mTargetPosition;
            this.mIsToTarget = true;
        }
    }
}
