package com.rossia.life.scan.tensor.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.rossia.life.scan.common.util.LogUtil;

/**
 * @author pd_liu on 2018/1/4.
 *         <p>
 *         ScanImageView：拓展ImageView
 *         不仅拥有ImageView属性，还：实现对图片的上下的扫描效果
 *         </p>
 */

public class ScanImageView extends ImageView {

    private static final String TAG_LOG = "ScanImageView";

    private static final int DEFAULT_REPEAT_COUNT = 1;

    /**
     * 绘制扫描效果的画笔
     */
    private Paint mScanBarPaint;

    /**
     * 需要绘制Line points
     */
    float mLineStartX = 0f;
    float mLineStartY = 0f;
    float mLineStopX = 0f;
    float mLineStopY = 0f;

    private int mWidth;
    private int mHeight;

    /**
     * 扫描的重复次数
     */
    private int mRepeatCount = DEFAULT_REPEAT_COUNT;

    /**
     * 当前扫描到了几次
     */
    private int mCurrentRepeatCount;

    /**
     * 是否开启绘制效果
     */
    private boolean mOpenScanBarActionFlag;

    /**
     * 每次扫描的间隔
     */
    private float mPerScanInterval;

    private Shader mPaintShader;

    private ScanCompleteCallback mScanCompleteCallback;

    public ScanImageView(Context context) {
        this(context, null);
    }

    public ScanImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScanImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ScanImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mScanBarPaint = new Paint();
        mScanBarPaint.setAntiAlias(true);
        mScanBarPaint.setColor(Color.YELLOW);
        mScanBarPaint.setStrokeWidth(5f);

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //如果标记为打开扫描效果
        if (mOpenScanBarActionFlag) {


            if (mCurrentRepeatCount < mRepeatCount) {

                /*
                绘制扫描效果
                 */
                if(mWidth == 0 || mHeight == 0){
                    mWidth = getWidth();
                    mHeight = getHeight();
                    mPerScanInterval = mHeight * 0.01f;
                }

                if(mPaintShader == null){
                    mPaintShader = new LinearGradient(0,0,0,mScanBarPaint.getStrokeWidth(), Color.YELLOW, Color.BLUE, LinearGradient.TileMode.CLAMP);
                }

                mLineStopX = mLineStartX + mWidth;

                if (mLineStartY <= mHeight) {

                    //绘制扫描线条
                    canvas.drawLine(mLineStartX, mLineStartY, mLineStopX, mLineStopY, mScanBarPaint);

                    mLineStartY = mLineStartY + mPerScanInterval;
                    mLineStopY = mLineStopY + mPerScanInterval;

                } else {
                    mLineStartY = 0f;
                    mLineStopY = 0f;
                    mCurrentRepeatCount ++ ;
                }

                //invalidate
                postInvalidate();

            } else {
                /*
                已经扫描结束
                 */
                mCurrentRepeatCount = 0;
                mLineStartY = 0f;
                mLineStopY = 0f;
                mWidth = 0;
                mHeight = 0;
                mOpenScanBarActionFlag = false;

                if (mScanCompleteCallback != null) {
                    mScanCompleteCallback.complete();
                }
            }


        }

    }

    public void startScan(){
        mCurrentRepeatCount = 0;
        mLineStartY = 0f;
        mLineStopY = 0f;
        mOpenScanBarActionFlag = true;
        postInvalidate();
    }

    public void setScanCompleteCallback(ScanCompleteCallback completeCallback) {
        mScanCompleteCallback = completeCallback;
    }

    public void setopenScanBarAction(boolean openScanBar) {
        mOpenScanBarActionFlag = openScanBar;
    }

    public boolean isOpenScanBarAction() {
        return mOpenScanBarActionFlag;
    }

    public interface ScanCompleteCallback {
        void complete();
    }
}
