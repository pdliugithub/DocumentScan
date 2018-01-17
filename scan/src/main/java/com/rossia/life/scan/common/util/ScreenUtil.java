package com.rossia.life.scan.common.util;

import android.app.Activity;
import android.view.Surface;

/**
 * @author pd_liu on 2018/1/5.
 */

public final class ScreenUtil {

    private static final String TAG_LOG = "ScreenUtil";

    private ScreenUtil() {
    }

    /**
     * 获取当前手机旋转的角度，影响因素{portrait、landscape}
     *
     * @param activity activity.
     * @return 当前手机旋转的角度
     */
    public static int getScreenOrientation(Activity activity) {

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();

        switch (rotation) {

            case Surface.ROTATION_270:
                return 270;

            case Surface.ROTATION_180:
                return 180;

            case Surface.ROTATION_90:
                return 90;

            default:
                return 0;
        }
    }

    /**
     * 根据当前手机屏幕的旋转角度，进而计算出Surface预览的角度
     *
     * @param activity activity.
     * @return Camera display orientation.
     */
    public static int getDisplayOrientation(Activity activity) {

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();

        LogUtil.e(TAG_LOG, "rotation:" + rotation);
        switch (rotation) {

            case Surface.ROTATION_0:
                return 90;

            case Surface.ROTATION_90:
                return 0;

            case Surface.ROTATION_180:
                return 270;

            case Surface.ROTATION_270:
                return 180;

            default:
                return 0;

        }
    }
}
