package com.meccael.tonal.wheelbar;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

/* JADX INFO: loaded from: classes.dex */
class SavedState extends View.BaseSavedState {
    public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() { // from class: com.meccael.tonal.wheelbar.SavedState.1
        @Override // android.os.Parcelable.Creator
        public SavedState createFromParcel(Parcel parcel) {
            return new SavedState(parcel);
        }

        @Override // android.os.Parcelable.Creator
        public SavedState[] newArray(int i) {
            return new SavedState[i];
        }
    };
    double angle;

    private SavedState(Parcel parcel) {
        super(parcel);
        this.angle = ((Double) parcel.readValue(null)).doubleValue();
    }

    SavedState(Parcelable parcelable) {
        super(parcelable);
    }

    public String toString() {
        return "HorizontalWheelView.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " angle=" + this.angle + "}";
    }

    @Override // android.view.View.BaseSavedState, android.view.AbsSavedState, android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeValue(Double.valueOf(this.angle));
    }
}
