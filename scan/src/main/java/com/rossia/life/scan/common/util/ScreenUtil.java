package com.rossia.life.scan.common.util;

import android.app.Activity;
import android.content.Context;
import android.view.Surface;

/**
 * @author pd_liu on 2018/1/5.
 */

public final class ScreenUtil {

    private static final String TAG_LOG = "ScreenUtil";

    private ScreenUtil() {
    }

    public static int getScreenOrientation(Activity activity) {

        switch (activity.getWindowManager().getDefaultDisplay().getRotation()) {

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
}
