package com.rossia.life.scan.ui.interf;

import android.graphics.Bitmap;

/**
 * @author pd_liu on 2018/1/8.
 *         <p>
 *         拍照Callback
 *         </p>
 */

public interface TakePictureCallback {
    /**
     * Call
     *
     * @param bitmap 拍照后处理过的图片位图
     */
    void call(Bitmap bitmap);
}
