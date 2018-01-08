package com.rossia.life.scan.ui.detector;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.rossia.life.scan.R;
import com.rossia.life.scan.common.util.CameraUtil;
import com.rossia.life.scan.common.util.ImageUtil;
import com.rossia.life.scan.common.util.ImageUtils;
import com.rossia.life.scan.common.util.LogUtil;
import com.rossia.life.scan.common.util.ScreenUtil;
import com.rossia.life.scan.tensor.TensorFlowObjectDetectionAPIModel;
import com.rossia.life.scan.tensor.env.BorderedText;
import com.rossia.life.scan.tensor.interf.Classifier;
import com.rossia.life.scan.tensor.tracking.MultiBoxTracker;
import com.rossia.life.scan.tensor.widget.OverlayView;
import com.rossia.life.scan.tensor.widget.ScanImageView;
import com.rossia.life.scan.ui.interf.TakePictureCallback;
import com.rossia.life.scan.ui.view.DrawColorView;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * @author pd_liu on 2017/12/29.
 *         <p>
 *         相机Camera为android.hardware.CameraApi
 *         </p>
 *         <p>
 *         Usage
 *         拍照 {@link #takePicture(RectF)}
 *         <p>
 *         </p>
 */

public class CameraApiFragment extends Fragment {

    private static final String TAG_LOG = "CameraApiFragment";

    private static final int ROTATION_90 = 90;

    /**
     * 渴望的、想得到的预览尺寸
     */
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);

    /**
     * 当前预览使用的Camera对象
     * {@link Camera}
     */
    private Camera mCamera;

    /**
     * TextureView.{@link TextureView}
     */
    private TextureView mTextureView;

    /**
     * Rgb bytes.
     */
    private int[] mRgbBytes;

    /**
     * 预览的视图的宽、高
     */
    private int mPreviewHeight;
    private int mPreviewWidth;

    private Runnable mImageConverterRunnable;
    private Runnable mReadyForNextImageRunnable;

    /**
     * 当前是否在进行处理图片Frame中
     */
    private boolean mIsProcessingFrame;

    /**
     * 相机扫描闪光灯切换图片
     */
    private ImageView mCameraScanFlashImg;

    /**
     * 打开相机的闪光灯
     */
    private boolean mOpenCameraFlashLight;

    /**
     * 浮层绘制视图
     */
    private OverlayView mTrackingOverlay;

    /**
     * 点击进行拍照视图
     */
    private ImageView mTakePictureImg;

    /**
     * 正在处理拍照
     */
    private boolean mProcessTakePictureFlag;

    /**
     * 自动拍照之间的间隔为3000毫秒[3秒]
     */
    private static final long TIME_AUTO_TAKE_PICTURE_INTERVAL = 3000;

    /**
     * 预览拍照的图片
     */
    private ImageView mPreviewTakeImg;

    /**
     * 大图预览视图
     */
    private ScanImageView mScanShowImg;

    /**
     * 遮罩层
     */
    private DrawColorView mDrawColorView;

    /**
     * 自动拍选择按钮
     */
    private Switch mAutoTakePictureSwitch;
    private Switch mShutterSoundTakePictureSwitch;

    private Classifier mTensorFlowObjectDetector;


    private Bitmap mTakePictureBitmap;

    private HandlerThread mHandlerThread;
    private Handler mBackgroundHandler;

    private Bitmap mRgbFrameBitmap;
    private Bitmap mCroppedBitmap;

    /**
     * 需要进行裁剪的图片的Matrix矩阵
     */
    private Matrix mFrameToCropTransformMatrix;
    /**
     * {@link #mFrameToCropTransformMatrix}反转的产物
     */
    private Matrix mCropToFrameTransformMatrix;

    /**
     * 是否完成检测
     */
    private boolean mComputingDetection;

    /**
     * 时间戳
     */
    private long mTimeStamp = 0;

    private byte[][] mYuvBytes = new byte[3][];

    private TakePictureCallback mTakePictureCallback;

    /**
     * TensorFlowObjectDetectionAPIModelFile.
     */
    private static final String TF_OD_API_MODEL_FILE = "file:///android_asset/ssd_mobilenet_v1_android_export.pb";
    /**
     * TensorFlowObjectDetectionAPILabelsFile.
     */
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/coco_labels_list.txt";
    /**
     * TensorFlowObjectDetectionAPIInputSize.
     */
    private static final int TF_OD_API_INPUT_SIZE = 300;

    private int mCropSize;
    /**
     * Camera orientation relative to screen canvas
     * 传感器旋转角度
     */
    private int mSensorOrientation;

    /**
     * Text的Size [dip]
     */
    private static final float TEXT_SIZE_DIP = 10;

    private MultiBoxTracker mMultiBoxTracker;

    /**
     * 是否开启自动拍功能
     */
    private boolean mOpenAutoTakePicture;
    private boolean mOpenShutterSoundTakePicture;

    private byte[] mLuminanceCopy;

    private Bitmap mCropCopyBitmap;

    /**
     * 记录着检测出的Location Rect 在屏幕上相对应的Location Rect.
     */
    private RectF mDetectScreenLocationRectF;


    /**
     * Callback for android.hardware.Camera API.
     */
    private Camera.PreviewCallback mCameraPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(final byte[] data, Camera camera) {

            Log.e(TAG_LOG, "\t\t\tonPreviewFrame");

            if (mIsProcessingFrame) {
                LogUtil.e(TAG_LOG, "Dropping frame!");
                return;
            }

            if (mRgbBytes == null) {
                Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
                mPreviewHeight = previewSize.height;
                mPreviewWidth = previewSize.width;
                mRgbBytes = new int[mPreviewWidth * mPreviewHeight];
                onPreviewSizeChosen(new Size(mPreviewWidth, mPreviewHeight), ROTATION_90);
            }

            mIsProcessingFrame = true;

            mYuvBytes[0] = data;

            //更新图片转换的任务
            mImageConverterRunnable = new Runnable() {
                @Override
                public void run() {
                    ImageUtils.convertYUV420SPToARGB8888(data, mPreviewWidth, mPreviewHeight, mRgbBytes);
                }
            };

            //更新下一步图片的回掉
            mReadyForNextImageRunnable = new Runnable() {
                @Override
                public void run() {
                    mCamera.addCallbackBuffer(data);
                    mIsProcessingFrame = false;
                }
            };

            //处理图片
            processImage();
        }
    };


    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

            int cameraID = CameraUtil.getCameraId();
            mCamera = Camera.open(cameraID);

            /*
            配置相机的相关参数
             */
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

            //支持的预览的尺寸
            List<Camera.Size> supportCameraSizes = parameters.getSupportedPreviewSizes();

            //获取所支持的最大的预览尺寸
            Size previewSizeMax = null;

            int maxWidthSize = 0;
            int maxHeightSize = 0;
            Size maxSize = new Size(0, 0);
            for (Camera.Size size :
                    supportCameraSizes) {
                if (size.width > maxWidthSize && size.height > maxHeightSize) {
                    maxWidthSize = size.width;
                    maxHeightSize = size.height;
                }
                LogUtil.e(TAG_LOG, "support size: width" + size.width + "\theight:\t" + size.height);
            }

            //最大预览
            previewSizeMax = new Size(maxWidthSize, maxHeightSize);

            List<Camera.Size> cameraSizes = parameters.getSupportedPreviewSizes();
            Size[] sizes = new Size[cameraSizes.size()];
            int i = 0;
            for (Camera.Size size : cameraSizes) {
                sizes[i++] = new Size(size.width, size.height);
            }
            Size previewSize =
                    CameraUtil.chooseOptimalSize(
                            sizes, DESIRED_PREVIEW_SIZE.getWidth(), DESIRED_PREVIEW_SIZE.getHeight());

            //设置相机预览尺寸
            parameters.setPreviewSize(previewSizeMax.getWidth(), previewSizeMax.getHeight());
            LogUtil.e(TAG_LOG, "preview size: width" + previewSizeMax.getWidth() + "\theight:\t" + previewSizeMax.getHeight());

            //设置图片格式
            parameters.setPictureFormat(ImageFormat.JPEG);
            //设置照片质量[0 - 100 质量依次上升]
            parameters.setJpegQuality(50);
            parameters.setPictureSize(previewSizeMax.getWidth(), previewSizeMax.getHeight());
            mCamera.setParameters(parameters);

            //默认预览的图片是被旋转的，需要进行此行代码设置进行纠正
            mCamera.setDisplayOrientation(90);

            try {
                mCamera.setPreviewTexture(surface);

            } catch (IOException e) {
                e.printStackTrace();
                mCamera.release();
            }

            mCamera.setPreviewCallbackWithBuffer(mCameraPreviewCallback);
            Camera.Size cameraSize = mCamera.getParameters().getPreviewSize();
            LogUtil.e(TAG_LOG, "cameraSize size: width" + cameraSize.height + "\theight:\t" + cameraSize.width);
            //因为：DisplayOrientation 旋转90度，所以getYUVByteSize（width, height）;
            mCamera.addCallbackBuffer(new byte[getYUVByteSize(cameraSize.height, cameraSize.width)]);
            mCamera.startPreview();
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


    private CameraApiFragment() {
    }

    /**
     * 选中改变监听
     */
    private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            if (mAutoTakePictureSwitch == buttonView) {
                mOpenAutoTakePicture = isChecked;
                return;
            }
            if (mShutterSoundTakePictureSwitch == buttonView) {
                mOpenShutterSoundTakePicture = isChecked;
                CameraUtil.startShutterSound(mCamera, mOpenShutterSoundTakePicture);
            }
        }
    };

    private ScanImageView.ScanCompleteCallback mScanCompleteCallback = new ScanImageView.ScanCompleteCallback() {
        @Override
        public void complete() {
            if (mOpenAutoTakePicture) {
                mPreTakeTime = System.currentTimeMillis();
                mTakePictureImg.setPressed(false);
            }
            //预览拍照图片视图上展示图片
            if(mTakePictureCallback != null){
                mTakePictureCallback.call(mTakePictureBitmap);
            }
            //拍照完成后，重启预览功能.
            mCamera.startPreview();


            //拍照完成后，将标记置为false
            mProcessTakePictureFlag = false;

            mScanShowImg.setVisibility(View.GONE);
        }
    };

    private MultiBoxTracker.OnDrawRectCompleteCallback mOnDrawRectCompleteCallback = new MultiBoxTracker.OnDrawRectCompleteCallback() {
        @Override
        public boolean drawRectComplete(RectF rectF) {

            mDrawColorView.draw(new DrawColorView.DrawColorListener() {
                @Override
                public void drawColor(Canvas canvas) {
                    canvas.drawColor(Color.parseColor("#00808080"));
                }
            });
            /*
            rect 为相对于手机屏幕的识别出的对象位置
             */
            LogUtil.e(TAG_LOG, "识别出对象相对于屏幕位置为：" + rectF);
            mDetectScreenLocationRectF = rectF;
            /*
            这里处理自动拍照、拍照逻辑
             */
            if (mOpenAutoTakePicture) {
                //自动拍照加入时间限制[三秒拍照一次]
                long current = System.currentTimeMillis();
                if (current - mPreTakeTime < TIME_AUTO_TAKE_PICTURE_INTERVAL) {
                    return false;
                }
                //自动拍照
                mTakePictureImg.performClick();
                mTakePictureImg.setPressed(true);
            }
            return false;
        }
    };

    private MultiBoxTracker.DetectionNothingCallback mDetectionNothingCallback = new MultiBoxTracker.DetectionNothingCallback() {
        @Override
        public boolean call() {
            //画面变灰色
            mDrawColorView.draw(new DrawColorView.DrawColorListener() {
                @Override
                public void drawColor(Canvas canvas) {
                    canvas.drawColor(Color.parseColor("#AA808080"));
                }
            });
            return false;
        }
    };

    public static CameraApiFragment newInstance() {

        CameraApiFragment fragment = new CameraApiFragment();
        return fragment;
    }

    private int getYUVByteSize(int width, int height) {
        // The luminance plane requires 1 byte per pixel.
        final int ySize = width * height;

        // The UV plane works on 2x2 blocks, so dimensions with odd size must be rounded up.
        // Each 2x2 block takes 2 bytes to encode, one each for U and V.
        final int uvSize = ((width + 1) / 2) * ((height + 1) / 2) * 2;

        return ySize + uvSize;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera_connection, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTextureView = view.findViewById(R.id.texture_view);
        mCameraScanFlashImg = view.findViewById(R.id.scan_flash_img);
        mTrackingOverlay = view.findViewById(R.id.tracking_overlay);
        mTakePictureImg = view.findViewById(R.id.take_picture_btn);
        mAutoTakePictureSwitch = view.findViewById(R.id.auto_take_picture_switch);
        mShutterSoundTakePictureSwitch = view.findViewById(R.id.shutter_sound_take_picture_switch);
        mPreviewTakeImg = view.findViewById(R.id.preview_take_img);
        mScanShowImg = view.findViewById(R.id.scan_img);
        mScanShowImg.setVisibility(View.GONE);
        mScanShowImg.setScanCompleteCallback(mScanCompleteCallback);

        mDrawColorView = view.findViewById(R.id.draw_color_view);
        //OnClickListener
        mCameraScanFlashImg.setOnClickListener(mOnClickListener);
        mTakePictureImg.setOnClickListener(mOnClickListener);
        mPreviewTakeImg.setOnClickListener(mOnClickListener);

        //Check listener.
        mAutoTakePictureSwitch.setOnCheckedChangeListener(mOnCheckedChangeListener);
        mShutterSoundTakePictureSwitch.setOnCheckedChangeListener(mOnCheckedChangeListener);

    }

    @Override
    public void onResume() {
        super.onResume();

        mHandlerThread = new HandlerThread("inference");
        mHandlerThread.start();
        mBackgroundHandler = new Handler(mHandlerThread.getLooper());

        if (mTextureView.isAvailable()) {
            mCamera.startPreview();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopCamera();
    }

    public void runInBackground(Runnable runnable) {
        mBackgroundHandler.post(runnable);
    }

    /**
     * 处理图片
     */
    private void processImage() {

        LogUtil.e(TAG_LOG, "\t\t\tprocessImage");
        //时间戳
        ++mTimeStamp;

        final long currentTimeStamp = mTimeStamp;

        //获取onPreviewFrame中的byte[]数据流
        byte[] originalLuminance = getLuminance();

        //此行代码并没有实质性的走通^_^
        mMultiBoxTracker.onFrame(mPreviewWidth, mPreviewHeight, mPreviewWidth, mSensorOrientation, originalLuminance, mTimeStamp);

        //开始绘制边框
        mTrackingOverlay.postInvalidate();


        if (mComputingDetection) {
            readyForNextImageRunnable();
            return;
        }

        mComputingDetection = true;

        mRgbFrameBitmap.setPixels(getRgbBytes(), 0, mPreviewWidth, 0, 0, mPreviewWidth, mPreviewHeight);

        if (mLuminanceCopy == null) {
            mLuminanceCopy = new byte[originalLuminance.length];
        }

        System.arraycopy(originalLuminance, 0, mLuminanceCopy, 0, originalLuminance.length);

        readyForNextImageRunnable();

        final Canvas canvas = new Canvas(mCroppedBitmap);
        canvas.drawBitmap(mRgbFrameBitmap, mFrameToCropTransformMatrix, new Paint());

        runInBackground(new Runnable() {
            @Override
            public void run() {

                /*
                此为子线程中的操作
                */
                LogUtil.e(TAG_LOG, "此为子线程中的操作");
                final long startTime = SystemClock.uptimeMillis();
                final List<Classifier.Recognition> results = mTensorFlowObjectDetector.recognizeImage(mCroppedBitmap);

                mCropCopyBitmap = Bitmap.createBitmap(mCroppedBitmap);

                /*
                绘制右下角的视图
                */
                final Canvas canvas = new Canvas(mCropCopyBitmap);
                final Paint paint = new Paint();
                paint.setColor(Color.RED);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5.0f);

                float minimumConfidence = 0.6f;

                final List<Classifier.Recognition> mappedRecognitions =
                        new LinkedList<Classifier.Recognition>();

                for (final Classifier.Recognition result : results) {
                    final RectF location = result.getLocation();
                    if (location != null && result.getConfidence() >= minimumConfidence) {
                        canvas.drawRect(location, paint);


                        int left = (int) location.left;
                        int top = (int) location.top;
                        int width = (int) (location.right - location.left);
                        int height = (int) (location.bottom - location.top);
                        LogUtil.e(TAG_LOG, "\t\t\tlocation: left top right bottom :\t" + left + "\t" + top + "\t" + location.right + "\t" + location.bottom);
                        final Bitmap bitmap = Bitmap.createBitmap(mCroppedBitmap, left, top, width, height);


                        mCropToFrameTransformMatrix.mapRect(location);
                        result.setLocation(location);
                        mappedRecognitions.add(result);

                    }
                }

                mMultiBoxTracker.trackResults(mappedRecognitions, mLuminanceCopy, currentTimeStamp);
                mTrackingOverlay.postInvalidate();

                //rquestRender内的操作的伪代码：mTrackingOverlay.postInvalidate(); [重复操作]
//                requestRender();
                mComputingDetection = false;

            }
        });
    }

    private int[] getRgbBytes() {
        mImageConverterRunnable.run();
        return mRgbBytes;
    }


    private void readyForNextImageRunnable() {
        if (mReadyForNextImageRunnable != null) {
            mReadyForNextImageRunnable.run();
        }
    }

    private byte[] getLuminance() {
        return mYuvBytes[0];
    }


    /**
     * 预览大小改变
     */
    private void onPreviewSizeChosen(Size size, int rotation) {


        float textSizeDip = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());

        BorderedText borderedText = new BorderedText(textSizeDip);
        borderedText.setTypeface(Typeface.MONOSPACE);

        mMultiBoxTracker = new MultiBoxTracker(getContext());
        //设置绘制完成Callback.
        mMultiBoxTracker.setOnDrawRectCompleteCallback(mOnDrawRectCompleteCallback);
        mMultiBoxTracker.setDetectionNothingCallback(mDetectionNothingCallback);

        try {
            mTensorFlowObjectDetector = TensorFlowObjectDetectionAPIModel.create(
                    getContext().getAssets()
                    , TF_OD_API_MODEL_FILE
                    , TF_OD_API_LABELS_FILE
                    , TF_OD_API_INPUT_SIZE
            );
            mCropSize = TF_OD_API_INPUT_SIZE;
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext().getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT).show();
        }

        mSensorOrientation = rotation - ScreenUtil.getScreenOrientation(getActivity());
        LogUtil.e(TAG_LOG, "Camera orientation relative to screen canvas :" + mSensorOrientation);

        mRgbFrameBitmap = Bitmap.createBitmap(mPreviewWidth, mPreviewHeight, Bitmap.Config.ARGB_8888);

        LogUtil.e(TAG_LOG, "mCropSize:" + mCropSize);
        mCroppedBitmap = Bitmap.createBitmap(mCropSize, mCropSize, Bitmap.Config.ARGB_8888);

        mFrameToCropTransformMatrix = ImageUtil.getTransformationMatrix(mPreviewWidth, mPreviewHeight, mCropSize, mCropSize, mSensorOrientation, false);

        mCropToFrameTransformMatrix = new Matrix();
        //进行反转
        mFrameToCropTransformMatrix.invert(mCropToFrameTransformMatrix);

        //将绘制检测对象边框任务保存起来
        mTrackingOverlay.addCallback(new OverlayView.DrawCallback() {
            @Override
            public void callback(Canvas canvas) {
                mMultiBoxTracker.draw(canvas);
            }
        });


    }

    /**
     * 关闭相机
     */
    private void stopCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
        }
    }

    /**
     * View的点击事件
     */
    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (mCameraScanFlashImg == v) {
                /*
                切换闪光灯
                 */
                startFlash(!mOpenCameraFlashLight);
                return;
            }

            if (mTakePictureImg == v) {
                /*
                进行拍照
                 */
                takePicture(mDetectScreenLocationRectF);
                return;
            }

            if (mPreviewTakeImg == v) {
                /*
                进行大图浏览
                 */
                //大图浏览时，关闭自动拍照
                mAutoTakePictureSwitch.setChecked(false);
                return;
            }

        }
    };

    /**
     * 闪光灯的开、关
     *
     * @param open 是否开启闪光灯
     * @return 是否成功执行
     */
    public boolean startFlash(boolean open) {
        //切换闪光灯ON OFF
        CameraUtil.startFlash(mCamera, open);

        return switchFlashImageView(open);
    }

    /**
     * 切换闪光灯View的展示
     *
     * @param open 是否打开闪光灯
     * @return 是否成功执行
     */
    private boolean switchFlashImageView(boolean open) {
        if (mCameraScanFlashImg == null) {
            return false;
        }
        mOpenCameraFlashLight = open;
        if (open) {
            mCameraScanFlashImg.setImageResource(R.mipmap.scan_flash_on);
        } else {
            mCameraScanFlashImg.setImageResource(R.mipmap.scan_flash_off);
        }
        return true;
    }


    private long mPreTakeTime;


    ProgressDialog mProgressDialog;

    /**
     * 拍照
     *
     * @param rectF 需要裁剪的位置区域
     * @return
     */
    private boolean takePicture(@Nullable final RectF rectF) {
        if (mProcessTakePictureFlag) {
            //已经正在拍照中，避免重复操作，不往下进行
            return false;
        }

        //标记目前正在拍照中
        mProcessTakePictureFlag = true;

        mCamera.takePicture(new Camera.ShutterCallback() {
            @Override
            public void onShutter() {
                /*
                这里加入识别成功滴一声
                 */
                if (mProgressDialog == null) {
                    mProgressDialog = new ProgressDialog(getContext());
                    mProgressDialog.setMessage("加载图片中....");
                    mProgressDialog.create();
                }
                mProgressDialog.show();
            }
        }, null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                //拍照后获取到的原始图片
                Bitmap takePictureSource = BitmapFactory.decodeByteArray(data, 0, data.length);
                if (takePictureSource == null) {
                    return;
                }
                LogUtil.e(TAG_LOG, "\t\t\ttakePictureSource:width:" + takePictureSource.getWidth() + "\theight:" + takePictureSource.getHeight());
                /*
                将拍照的图片进行旋转角度
                 */
                int width = takePictureSource.getWidth();
                int height = takePictureSource.getHeight();

                Matrix matrix = new Matrix();
                //旋转90度.
                matrix.postRotate(90, width / 2.0f, height / 2.0f);
                //旋转正常角度后的Bitmap.
                Bitmap rotatedBitmap = Bitmap.createBitmap(takePictureSource, 0, 0, width, height, matrix, true);


                /*
                根据旋转后的正常角度的Bitmap
                裁剪出识别框尺寸的Bitmap
                 */
                if (rectF == null) {
                    /*
                    没有识别出来对象边框区域，则不进行裁剪
                     */
                    //旋转后的图片为拍照图片
                    mTakePictureBitmap = rotatedBitmap;
                } else {
                    int cropX = (int) rectF.left + 1;
                    int cropY = (int) rectF.top + 1;
                    int cropWidth = (int) (rectF.right - rectF.left) - 1;
                    int cropHeight = (int) (rectF.bottom - rectF.top) - 1;

                    //裁剪后的Bitmap
                    Bitmap croppedBitmap = Bitmap.createBitmap(rotatedBitmap, cropX, cropY, cropWidth, cropHeight, new Matrix(), true);

                    //裁剪后的图片为拍照图片
                    mTakePictureBitmap = croppedBitmap;
                }

                //取消加载进度框
                mProgressDialog.cancel();

                mScanShowImg.setImageBitmap(mTakePictureBitmap);
                mScanShowImg.setVisibility(View.VISIBLE);
                mScanShowImg.startScan();


            }
        });
        return true;
    }

    public void setTakePictureCallback(TakePictureCallback takePictureCallback){
        mTakePictureCallback = takePictureCallback;
    }

}
