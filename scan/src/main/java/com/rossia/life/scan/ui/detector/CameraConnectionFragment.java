package com.rossia.life.scan.ui.detector;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.rossia.life.scan.R;
import com.rossia.life.scan.common.util.LogUtil;

/**
 * @author pd_liu 2017/12/29.
 *         <p>
 *         相机
 *         </p>
 *         <p>
 *         Note:
 *         1、不支持前置摄像头进行捕捉画面
 *         </p>
 *         A simple {@link Fragment} subclass.
 *         Activities that contain this fragment must implement the
 *         {@link CameraConnectionFragment.OnFragmentInteractionListener} interface
 *         to handle interaction events.
 *         Use the {@link CameraConnectionFragment#newInstance} factory method to
 *         create an instance of this fragment.
 */
public class CameraConnectionFragment extends Fragment {

    private static final String TAG_LOG = "CameraConnectionFragment";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private OnFragmentInteractionListener mListener;

    /**
     * Texture view display the camera output resource of image
     */
    private TextureView mTextureView;

    /**
     * 当前Fragment显示的Layout的资源ID
     */
    private int mFragmentContentLayoutID;

    public CameraConnectionFragment(int layoutId) {
        // Required empty public constructor
        this.mFragmentContentLayoutID = layoutId;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param layoutId Fragment layout resource id.
     * @return A new instance of fragment CameraConnectionFragment.
     */
    public static CameraConnectionFragment newInstance(int layoutId) {
        CameraConnectionFragment fragment = new CameraConnectionFragment(layoutId);
        return fragment;
    }

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera_connection, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //findView.
        mTextureView = view.findViewById(R.id.texture_view);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mTextureView.isAvailable()) {
            startCamera();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * 启动相机
     */
    private void startCamera() {

        try {
            //当前的CameraID
            String cameraId = chooseCamera();


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return CameraId
     * @throws CameraAccessException
     */
    private String chooseCamera() throws CameraAccessException {

        final Activity activity = getActivity();

        CameraManager cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);

        //获取所有的相机ID列表
        String[] cameraIdList = cameraManager.getCameraIdList();

        for (String cameraId : cameraIdList) {

            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);

            // We don't use a front facing camera in this sample.
            final Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);

            if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                /*
                不支持前置摄像头进行捕捉画面
                 */
                continue;
            }

            //此相机设备支持的可用流配置; 还包括每种格式/尺寸组合的最小帧持续时间和停顿持续时间。
            StreamConfigurationMap scalerStreamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            if (scalerStreamConfigurationMap == null) {
                // TODO: 2018/1/2
                return null;
            }

            //相机设备是外部相机，相对于设备的屏幕没有固定的朝向。或则支持硬件等级为INFO_SUPPORTED_HARDWARE_LEVEL_FULL
//            if (facing == CameraCharacteristics.LENS_FACING_EXTERNAL || isHardwareLevelSupported(cameraCharacteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)) {
//                LogUtil.e(TAG_LOG, "Camera API level 2 ? : true");
//            }

            return cameraId;
        }

        return null;
    }

    /**
     * Returns true if the device supports the required hardware level, or better.
     *
     * @param characteristics {@link CameraCharacteristics}
     * @param requiredLevel   requiredLevel.
     * @return whether the device supports the required hardware level.
     */
//    private boolean isHardwareLevelSupported(CameraCharacteristics characteristics, int requiredLevel) {
//
//        int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3);
//
//        //支持的硬件级别是摄像机设备功能的高级描述，将多个功能汇总到一个领域。
//        // 每个级别都增加了前一级的附加功能，并且始终是前一级的严格超集。排序是LEGACY < LIMITED < FULL < LEVEL_3。
//        if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
//            return requiredLevel == deviceLevel;
//        }
//
//        return requiredLevel <= deviceLevel;
//    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
