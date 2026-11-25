package com.meccael.tonal.application;

import android.graphics.Bitmap;
import java.lang.reflect.Array;

/* JADX INFO: loaded from: classes.dex */
class FastBlur {
    FastBlur() {
    }

    static Bitmap doBlur(Bitmap bitmap, int i) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] iArr = new int[width * height];
        bitmap.getPixels(iArr, 0, width, 0, 0, width, height);
        int i2 = width - 1;
        int i3 = height - 1;
        int i4 = width * height;
        int i5 = i + i + 1;
        int[] iArr2 = new int[i4];
        int[] iArr3 = new int[i4];
        int[] iArr4 = new int[i4];
        int[] iArr5 = new int[Math.max(width, height)];
        int i6 = (i5 + 1) >> 1;
        int i7 = i6 * i6;
        int[] iArr6 = new int[i7 * 256];
        for (int i8 = 0; i8 < i7 * 256; i8++) {
            iArr6[i8] = i8 / i7;
        }
        int i9 = 0;
        int i10 = 0;
        int[][] iArr7 = (int[][]) Array.newInstance((Class<?>) Integer.TYPE, i5, 3);
        int i11 = i + 1;
        for (int i12 = 0; i12 < height; i12++) {
            int i13 = 0;
            int i14 = 0;
            int i15 = 0;
            int i16 = 0;
            int i17 = 0;
            int i18 = 0;
            int i19 = 0;
            int i20 = 0;
            int i21 = 0;
            for (int i22 = -i; i22 <= i; i22++) {
                int i23 = iArr[Math.min(i2, Math.max(i22, 0)) + i9];
                int[] iArr8 = iArr7[i22 + i];
                iArr8[0] = (16711680 & i23) >> 16;
                iArr8[1] = (65280 & i23) >> 8;
                iArr8[2] = i23 & 255;
                int iAbs = i11 - Math.abs(i22);
                i15 += iArr8[0] * iAbs;
                i14 += iArr8[1] * iAbs;
                i13 += iArr8[2] * iAbs;
                if (i22 > 0) {
                    i21 += iArr8[0];
                    i20 += iArr8[1];
                    i19 += iArr8[2];
                } else {
                    i18 += iArr8[0];
                    i17 += iArr8[1];
                    i16 += iArr8[2];
                }
            }
            int i24 = i;
            for (int i25 = 0; i25 < width; i25++) {
                iArr2[i9] = iArr6[i15];
                iArr3[i9] = iArr6[i14];
                iArr4[i9] = iArr6[i13];
                int i26 = i15 - i18;
                int i27 = i14 - i17;
                int i28 = i13 - i16;
                int[] iArr9 = iArr7[((i24 - i) + i5) % i5];
                int i29 = i18 - iArr9[0];
                int i30 = i17 - iArr9[1];
                int i31 = i16 - iArr9[2];
                if (i12 == 0) {
                    iArr5[i25] = Math.min(i25 + i + 1, i2);
                }
                int i32 = iArr[iArr5[i25] + i10];
                iArr9[0] = (16711680 & i32) >> 16;
                iArr9[1] = (65280 & i32) >> 8;
                iArr9[2] = i32 & 255;
                int i33 = i21 + iArr9[0];
                int i34 = i20 + iArr9[1];
                int i35 = i19 + iArr9[2];
                i15 = i26 + i33;
                i14 = i27 + i34;
                i13 = i28 + i35;
                i24 = (i24 + 1) % i5;
                int[] iArr10 = iArr7[i24 % i5];
                i18 = i29 + iArr10[0];
                i17 = i30 + iArr10[1];
                i16 = i31 + iArr10[2];
                i21 = i33 - iArr10[0];
                i20 = i34 - iArr10[1];
                i19 = i35 - iArr10[2];
                i9++;
            }
            i10 += width;
        }
        for (int i36 = 0; i36 < width; i36++) {
            int i37 = 0;
            int i38 = 0;
            int i39 = 0;
            int i40 = 0;
            int i41 = 0;
            int i42 = 0;
            int i43 = 0;
            int i44 = 0;
            int i45 = 0;
            int i46 = (-i) * width;
            for (int i47 = -i; i47 <= i; i47++) {
                int iMax = Math.max(0, i46) + i36;
                int[] iArr11 = iArr7[i47 + i];
                iArr11[0] = iArr2[iMax];
                iArr11[1] = iArr3[iMax];
                iArr11[2] = iArr4[iMax];
                int iAbs2 = i11 - Math.abs(i47);
                i39 += iArr2[iMax] * iAbs2;
                i38 += iArr3[iMax] * iAbs2;
                i37 += iArr4[iMax] * iAbs2;
                if (i47 > 0) {
                    i45 += iArr11[0];
                    i44 += iArr11[1];
                    i43 += iArr11[2];
                } else {
                    i42 += iArr11[0];
                    i41 += iArr11[1];
                    i40 += iArr11[2];
                }
                if (i47 < i3) {
                    i46 += width;
                }
            }
            int i48 = i36;
            int i49 = i;
            for (int i50 = 0; i50 < height; i50++) {
                iArr[i48] = ((-16777216) & iArr[i48]) | (iArr6[i39] << 16) | (iArr6[i38] << 8) | iArr6[i37];
                int i51 = i39 - i42;
                int i52 = i38 - i41;
                int i53 = i37 - i40;
                int[] iArr12 = iArr7[((i49 - i) + i5) % i5];
                int i54 = i42 - iArr12[0];
                int i55 = i41 - iArr12[1];
                int i56 = i40 - iArr12[2];
                if (i36 == 0) {
                    iArr5[i50] = Math.min(i50 + i11, i3) * width;
                }
                int i57 = i36 + iArr5[i50];
                iArr12[0] = iArr2[i57];
                iArr12[1] = iArr3[i57];
                iArr12[2] = iArr4[i57];
                int i58 = i45 + iArr12[0];
                int i59 = i44 + iArr12[1];
                int i60 = i43 + iArr12[2];
                i39 = i51 + i58;
                i38 = i52 + i59;
                i37 = i53 + i60;
                i49 = (i49 + 1) % i5;
                int[] iArr13 = iArr7[i49];
                i42 = i54 + iArr13[0];
                i41 = i55 + iArr13[1];
                i40 = i56 + iArr13[2];
                i45 = i58 - iArr13[0];
                i44 = i59 - iArr13[1];
                i43 = i60 - iArr13[2];
                i48 += width;
            }
        }
        bitmap.setPixels(iArr, 0, width, 0, 0, width, height);
        return bitmap;
    }
}
