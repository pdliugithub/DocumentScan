package com.rossia.life.scan.common.util;

import android.util.Log;

/**
 * @author pd_liu on 2017/12/29.
 *         <p>
 *         Log日志打印工具类
 *         </p>
 */

public final class LogUtil {

    private volatile static boolean sDebug = true;

    private LogUtil() {
    }

    public static void setDebug(boolean isDebug) {
        sDebug = isDebug;
    }

    public static void e(String tag, String message) {
        if (sDebug) {
            Log.e(tag, message);
        }
    }

}
