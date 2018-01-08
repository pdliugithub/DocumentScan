package com.rossia.life.scan.ui.detector;

import android.os.Bundle;

/**
 * @author by pd_liu on 2017/12/29.
 *         <p>
 *         支持Camera2包下的最新Api
 *         </p>
 */

public class CameraApi2Fragment {

    private static final String TAG_LOG = "CameraApi2Fragment";

    private String mCameraID;

    private int mFragmentLayoutID;

    private CameraApi2Fragment(final String cameraId, final int layout){
        this.mCameraID = cameraId;
        this.mFragmentLayoutID = layout;
    }

    public static CameraApi2Fragment newInstance(String cameraId, int layoutId) {

        CameraApi2Fragment fragment = new CameraApi2Fragment(cameraId, layoutId);
        return fragment;
    }


}
