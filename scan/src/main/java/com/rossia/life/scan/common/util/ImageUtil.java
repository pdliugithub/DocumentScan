package com.rossia.life.scan.common.util;

import android.graphics.Matrix;

/**
 * @author pd_liu on 2018/1/2.
 *         <p>
 *         图片处理工具类
 *         </p>
 */

public final class ImageUtil {

    /**
     * Returns a transformation matrix from one reference frame into another.
     * Handles cropping (if maintaining aspect ratio is desired) and rotation.
     *
     * @param sourceWidth         Width of source frame.
     * @param sourceHeight        Height of source frame.
     * @param dstWidth            Width of destination frame.
     * @param dstHeight           Height of destination frame.
     * @param applyRotation       Amount of rotation to apply from one frame to another.
     *                            Must be a multiple of 90.
     * @param maintainAspectRatio If true, will ensure that scaling in x and y remains constant,
     *                            cropping the image if necessary.
     * @return The transformation fulfilling the desired requirements.
     */
    public static Matrix getTransformationMatrix(
            int sourceWidth
            , int sourceHeight
            , int dstWidth
            , int dstHeight
            , int applyRotation
            , boolean maintainAspectRatio) {

        final Matrix matrix = new Matrix();

        if (applyRotation != 0) {

            // Translate so center of image is at origin.
            //将图片移动到中心点
            matrix.postTranslate(-sourceWidth / 2.0f, -sourceHeight / 2.0f);

            // Rotate around origin.
            //将图片旋转
            matrix.postRotate(applyRotation);
        }

        //如果有的话，说明已经应用的旋转，然后确定每个轴需要多少缩放。
        //是否需要转置
        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;

        //如果需要，那么将宽、高进行转置
        final int inWidth = transpose ? sourceHeight : sourceWidth;
        final int inHeight = transpose ? sourceWidth : sourceHeight;


        //Apply scaling if necessary.
        //判断原始图片，与需要的图片是否需要进行Scaling操作
        if (inWidth != dstWidth || inHeight != dstHeight) {

            final float scaleFactoryX = dstWidth / (float) inWidth;
            final float scaleFactoryY = dstHeight / (float) inHeight;


            if (maintainAspectRatio) {

                // Scale by minimum factor so that dst is filled completely while
                // maintaining the aspect ratio. Some image may fall off the edge.
                //按最小因子进行缩放，使dst完全填充，同时保持纵横比。有些图像可能会从边缘掉下来。
                // TODO: 2018/1/2 以下代码可能会导致图片边缘丢失
                final float scaleFactor = Math.max(scaleFactoryX, scaleFactoryY);
                matrix.postScale(scaleFactor, scaleFactor);
            } else {
                // Scale exactly to fill dst from src.
                //将原始图片精确地完整填充目标图片
                matrix.postScale(scaleFactoryX, scaleFactoryY);
            }

        }


        if (applyRotation != 0) {
            // Translate back from origin centered reference to destination frame.
            //将原点中心的引用转换为目标帧
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;
    }
}
