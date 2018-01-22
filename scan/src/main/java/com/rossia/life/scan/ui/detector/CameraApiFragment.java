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
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Size;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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
import com.rossia.life.scan.transfer.SensorMoveControl;
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
 *         点击TextureView，进行自动对焦
 *         当拍照完成后，进行扫描图片效果展示
 *         增加：对检测的区域进行反复的学习、检查
 *         增加：自动拍照模式下，手机移动时不进行拍照
 *         增加：将拍照声音的设置方法公开性
 *         </p>
 *         <p>
 *         Note:
 *         拍照使用最大清晰度的照片
 *         预览使用所支持的最大预览尺寸
 *         </p>
 */

public class CameraApiFragment extends Fragment {

    private static final String TAG_LOG = "CameraApiFragment";

    /**
     * 传感器控制者用于检测手机移动状态
     */
    private SensorMoveControl mSensorMoveControl;

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

    private Classifier mTensorFlowObjectDetector;

    /**
     * 所拍照的Bitmap
     */
    private Bitmap mTakePictureBitmap;

    /**
     * Background thread and handler.
     */
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

    private float mDetectionInterval = 50;

    /**
     * 记录着检测出的Location Rect 在屏幕上相对应的Location Rect.
     */
    private RectF mDetectScreenLocationRectF;
    private RectF mPreDetectScreenLocationRectF;

    /**
     * 自动拍照时，进行需要计算精度的次数
     */
    private int mAutoTakeCalculateNumber;

    private static final int TAKE_CALCULATE_NUMBER = 50;
    /**
     * Callback for android.hardware.Camera API.
     */
    private Camera.PreviewCallback mCameraPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(final byte[] data, Camera camera) {

            LogUtil.e(TAG_LOG, "\t\t\tonPreviewFrame");

            if (mIsProcessingFrame) {
                LogUtil.e(TAG_LOG, "Dropping frame!");
                return;
            }

            if (mRgbBytes == null) {
                Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
                mPreviewHeight = previewSize.height;
                mPreviewWidth = previewSize.width;
                mRgbBytes = new int[mPreviewWidth * mPreviewHeight];
                onPreviewSizeChosen(new Size(mPreviewWidth, mPreviewHeight), ScreenUtil.getDisplayOrientation(getActivity()));
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

            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

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
            LogUtil.e(TAG_LOG, "preview size: width" + previewSize.getWidth() + "\theight:\t" + previewSize.getHeight());

            //设置图片格式
            parameters.setPictureFormat(ImageFormat.JPEG);
            //设置照片质量[0 - 100 质量依次上升](为了达到订单的最大清晰度，设置质量为最大)
            parameters.setJpegQuality(100);

            List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();
            int maxPictureWidth = 0;
            int maxPictureHeight = 0;
            for (Camera.Size pictureSize :
                    pictureSizes) {
                if (pictureSize.width > maxPictureWidth && pictureSize.height > maxPictureHeight) {
                    maxPictureWidth = pictureSize.width;
                    maxPictureHeight = pictureSize.height;
                }
            }
            LogUtil.e(TAG_LOG, "picture size: width" + maxPictureWidth + "\theight:\t" + maxPictureHeight);
            parameters.setPictureSize(previewSizeMax.getWidth(), previewSizeMax.getHeight());
            mCamera.setParameters(parameters);


            //默认预览的图片是被旋转的，需要进行此行代码设置进行纠正
            mCamera.setDisplayOrientation(ScreenUtil.getDisplayOrientation(getActivity()));

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
            if (mTakePictureCallback != null) {
                mTakePictureCallback.call(mTakePictureBitmap);
            }
            //拍照完成后，重启预览功能.
            mCamera.startPreview();

            if (mOpenCameraFlashLight) {
                //更新闪光灯设置[拍照时，会默认关闭手电筒，所以，如过想要重新打开手电筒，需要：先关闭，再开启]
                startFlash(false);
                startFlash(true);
            }

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
            mPreDetectScreenLocationRectF = mDetectScreenLocationRectF;
            mDetectScreenLocationRectF = rectF;

            /*
            这里处理自动拍照、拍照逻辑
             */
            if (mOpenAutoTakePicture) {

                float intervalLeft = Math.abs(mDetectScreenLocationRectF.left - mPreDetectScreenLocationRectF.left);
                float intervalTop = Math.abs(mDetectScreenLocationRectF.top - mPreDetectScreenLocationRectF.top);
                float intervalRight = Math.abs(mDetectScreenLocationRectF.right - mPreDetectScreenLocationRectF.right);
                float intervalBottom = Math.abs(mDetectScreenLocationRectF.bottom - mPreDetectScreenLocationRectF.bottom);


                if (intervalLeft <= mDetectionInterval && intervalTop <= mDetectionInterval && intervalRight <= mDetectionInterval && intervalBottom <= mDetectionInterval) {

                    /*
                    符合规则，进行计算值加一
                    当计算值累加到TAKE_CALCULATE_NUMBER时，进行拍照
                     */
                    mAutoTakeCalculateNumber++;
                    if (mAutoTakeCalculateNumber < TAKE_CALCULATE_NUMBER) {
                        return false;
                    }
                    /*
                    当两次之间的间隔不小于固定的阀值，才能进行自动拍照的下一步
                     */
                    //自动拍照加入时间限制[三秒拍照一次]
                    long current = System.currentTimeMillis();
                    if (current - mPreTakeTime < TIME_AUTO_TAKE_PICTURE_INTERVAL) {
                        return false;
                    }
                    mAutoTakeCalculateNumber = 0;
                    //自动拍照
                    mTakePictureImg.performClick();
                    mTakePictureImg.setPressed(true);
                } else {
                    /*
                    只要有一次不符合规则，就进行重置为0
                     */
                    mAutoTakeCalculateNumber = 0;
                }

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
                    //当没有检测出文档时，进行重置自动拍照检测的计算值为0
                    mAutoTakeCalculateNumber = 0;
                    float textSize = 50f;
                    Paint mPaint = new Paint();
                    mPaint.setAntiAlias(true);
                    mPaint.setColor(Color.BLACK);
                    mPaint.setTextSize(textSize);

                    String tip = "请将识别文档置与屏幕中间，并减少干扰";

                    float x = mTextureView.getWidth() / 2f - textSize * tip.length() / 2f;
                    float y = mTextureView.getHeight() / 2f;
                    canvas.drawText(tip, x, y, mPaint);
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
        mTextureView.setOnClickListener(mOnClickListener);
        mCameraScanFlashImg = view.findViewById(R.id.scan_flash_img);
        mTrackingOverlay = view.findViewById(R.id.tracking_overlay);
        mTakePictureImg = view.findViewById(R.id.take_picture_btn);
        mAutoTakePictureSwitch = view.findViewById(R.id.auto_take_picture_switch);
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

    }

    @Override
    public void onResume() {
        super.onResume();

        startCamera();

        mSensorMoveControl = SensorMoveControl.newInstance(getContext());
        mSensorMoveControl.startSensor();
        mSensorMoveControl.setSensorMoveListener(mSensorMoveListener);
    }

    /**
     * 监听当前设备的移动状态
     */
    private SensorMoveControl.SensorMoveListener mSensorMoveListener = new SensorMoveControl.SensorMoveListener() {
        @Override
        public void onMoving() {
            //当手机移动，就开始将自动拍照的计算次数置为0.
            String tip = "";
            if (mAutoTakeCalculateNumber == 0) {
                tip = "请将识别文档置与屏幕中间，并减少干扰";
            } else {
                tip = "识别中，请减少移动手机";
            }
            final String tipDrawed = tip;

            mAutoTakeCalculateNumber = 0;
            mDrawColorView.draw(new DrawColorView.DrawColorListener() {
                @Override
                public void drawColor(Canvas canvas) {
                    float textSize = 50f;
                    Paint mPaint = new Paint();
                    mPaint.setAntiAlias(true);
                    mPaint.setColor(Color.BLACK);
                    mPaint.setTextSize(textSize);

                    float x = mTextureView.getWidth() / 2f - textSize * tipDrawed.length() / 2f;
                    float y = mTextureView.getHeight() / 2f;
                    canvas.drawText(tipDrawed, x, y, mPaint);
                }
            });
        }

        @Override
        public void onStaticing() {
            //当手机静止后，进行对焦
            //Camera设置的自动对焦已经带有对焦功能了
            return;
        }
    };

    public void startCamera() {
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
        stopCamera();
        mSensorMoveControl.stopSensor();
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
     * <p>
     * 关闭相机：停止预览、Thread退出、Handler移除消息.
     * </p>
     */
    public void stopCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mHandlerThread.quitSafely();
            mBackgroundHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * <p>
     * 释放相机资源
     * </p>
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCamera != null) {
            mCamera.release();
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
                takePictureFocus(mDetectScreenLocationRectF);
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
        LogUtil.e(TAG_LOG, "startFlash: " + open);
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
                //将资源旋转与预览的角度相一致
                matrix.postRotate(ScreenUtil.getDisplayOrientation(getActivity()), width / 2.0f, height / 2.0f);
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

    private RectF mFocusPictureRect;

    /**
     * 拍照前进行自动对焦，对焦完成之后再进行拍照
     */
    private boolean takePictureFocus(@Nullable final RectF rectF) {
        if (mProcessTakePictureFlag) {
            //已经正在拍照中，避免重复操作，不往下进行
            return false;
        }

        //标记目前正在拍照中
        mProcessTakePictureFlag = true;

        mFocusPictureRect = rectF;

        /*
        相机的对焦
        */
        //Applications should call autoFocus(AutoFocusCallback) to start the focus if focus mode is FOCUS_MODE_AUTO or FOCUS_MODE_MACRO.
        mCamera.autoFocus(mAutoFocusCallback);
        return true;
    }

    /**
     * 设置拍照完成后的Callback
     * @param takePictureCallback {@link TakePictureCallback}
     */
    public void setTakePictureCallback(TakePictureCallback takePictureCallback) {
        mTakePictureCallback = takePictureCallback;
    }

    private Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {

            LogUtil.e(TAG_LOG, "autoFocus:\t" + success);
            if (success) {
                mCamera.cancelAutoFocus();
                takePicture(mFocusPictureRect);

            } else {
                //对焦失败，继续对焦
            }
        }
    };

    private Camera.AutoFocusMoveCallback mAutoFocusMoveCallback = new Camera.AutoFocusMoveCallback() {
        @Override
        public void onAutoFocusMoving(boolean start, Camera camera) {
            if (!start) {
                    /*
                    对焦结束
                     */
                LogUtil.e(TAG_LOG, "autoFocus:\t" + start);
                takePicture(mFocusPictureRect);
            }
        }
    };

    public void setOpenAutoTakePicture(boolean autoTakePicture){
        mOpenAutoTakePicture = autoTakePicture;
    }
    public boolean isOpenAutoTakePicture(){
        return mOpenAutoTakePicture;
    }

    public void setTopViewMarginTop(int top){
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mCameraScanFlashImg.getLayoutParams();
        layoutParams.setMargins(0,top,0, 0);
        mTakePictureImg.setLayoutParams(layoutParams);
    }

    public void setBottomMarginBottom(int bottom){
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mTakePictureImg.getLayoutParams();
        layoutParams.setMargins(0,0,0, bottom);
        mTakePictureImg.setLayoutParams(layoutParams);
    }

    public Camera getCamera(){
        return mCamera;
    }

    /**
     * 拍照声音状态
     * @return 状态
     */
    public boolean isOpenShutterSoundTakePicture(){
        return mOpenShutterSoundTakePicture;
    }

    /**
     * 设置相机声音
     * @param isOpen
     */
    public void setOpenShutterSoundTakePicture(boolean isOpen){
        mOpenShutterSoundTakePicture = isOpen;
        CameraUtil.startShutterSound(mCamera, mOpenShutterSoundTakePicture);
    }
}
