package com.rossia.life.scan.common.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

/**
 * @author pd_liu on 2018/1/10.
 *         <p>
 *         图片增强
 *         </p>
 *         <p>
 *         支持图片的亮度、对比度、饱和度调节{@link #handleImage(int)} .
 *         </p>
 */

public class PhotoEnhanceUtil {

    /**
     * 处理图片的模式：饱和度、亮度、对比度
     */
    public final int Enhance_Saturation = 0;
    public final int Enhance_Brightness = 1;
    public final int Enhance_Contrast = 2;

    /**
     * Bitmap
     */
    private Bitmap mBitmap;
    private float saturationNum = 1.0F;
    private float brightNum = 0.0F;
    private float contrastNum = 1.0F;
    private ColorMatrix mAllMatrix = null;
    private ColorMatrix saturationMatrix = null;
    private ColorMatrix contrastMatrix = null;
    private ColorMatrix brightnessMatrix = null;

    public PhotoEnhanceUtil() {
    }

    public PhotoEnhanceUtil(Bitmap bitmap) {
        this.mBitmap = bitmap;
    }

    public float getSaturation() {
        return this.saturationNum;
    }

    public void setSaturation(int saturationNum) {
        this.saturationNum = (float) saturationNum * 1.0F / 128.0F;
    }

    public float getBrightness() {
        return this.brightNum;
    }

    public void setBrightness(int brightNum) {
        this.brightNum = (float) (brightNum - 128);
    }

    public float getContrast() {
        return this.contrastNum;
    }

    public void setContrast(int contrastNum) {
        this.contrastNum = (float) ((double) (contrastNum / 2 + 64) / 128.0D);
    }

    public Bitmap handleImage(int type) {
        Bitmap bmp = Bitmap.createBitmap(this.mBitmap.getWidth(), this.mBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        if (this.mAllMatrix == null) {
            this.mAllMatrix = new ColorMatrix();
        }

        if (this.saturationMatrix == null) {
            this.saturationMatrix = new ColorMatrix();
        }

        if (this.contrastMatrix == null) {
            this.contrastMatrix = new ColorMatrix();
        }

        if (this.brightnessMatrix == null) {
            this.brightnessMatrix = new ColorMatrix();
        }

        switch (type) {
            case 0:
                this.saturationMatrix.reset();
                this.saturationMatrix.setSaturation(this.saturationNum);
                break;
            case 1:
                this.brightnessMatrix.reset();
                this.brightnessMatrix.set(new float[]{1.0F, 0.0F, 0.0F, 0.0F, this.brightNum, 0.0F, 1.0F, 0.0F, 0.0F, this.brightNum, 0.0F, 0.0F, 1.0F, 0.0F, this.brightNum, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F});
                break;
            case 2:
                float regulateBright = 0.0F;
                regulateBright = (1.0F - this.contrastNum) * 128.0F;
                this.contrastMatrix.reset();
                this.contrastMatrix.set(new float[]{this.contrastNum, 0.0F, 0.0F, 0.0F, regulateBright, 0.0F, this.contrastNum, 0.0F, 0.0F, regulateBright, 0.0F, 0.0F, this.contrastNum, 0.0F, regulateBright, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F});
        }

        this.mAllMatrix.reset();
        this.mAllMatrix.postConcat(this.saturationMatrix);
        this.mAllMatrix.postConcat(this.brightnessMatrix);
        this.mAllMatrix.postConcat(this.contrastMatrix);
        paint.setColorFilter(new ColorMatrixColorFilter(this.mAllMatrix));
        canvas.drawBitmap(this.mBitmap, 0.0F, 0.0F, paint);
        return bmp;
    }
}
