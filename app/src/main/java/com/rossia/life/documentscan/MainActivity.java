package com.rossia.life.documentscan;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.chrisbanes.photoview.PhotoView;
import com.rossia.life.scan.common.util.PhotoEnhanceUtil;
import com.rossia.life.scan.ui.detector.CameraApiFragment;
import com.rossia.life.scan.ui.interf.TakePictureCallback;

/**
 * @author pd_liu 2017/1/10.
 *         <p>
 *         暂时，自动识别拍照、裁剪。
 *         </p>
 */
public class MainActivity extends AppCompatActivity {

    private PhotoView mPhotoView;

    private Bitmap mTakePictureBitmap;

    private CameraApiFragment mCameraApiFragment;

    /**
     * 对图片进行、对比度的加强
     */
    private PhotoEnhanceUtil mPhotoEnhance;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        final ImageView showImg = findViewById(R.id.show_img);
        mPhotoView = findViewById(R.id.photo_view);

        mCameraApiFragment = CameraApiFragment.newInstance();

        getSupportFragmentManager().beginTransaction().add(R.id.container, mCameraApiFragment, "api").commit();

        mCameraApiFragment.setTakePictureCallback(new TakePictureCallback() {
            @Override
            public void call(Bitmap bitmap) {
                mTakePictureBitmap = bitmap;
                showImg.setImageBitmap(bitmap);
            }
        });

        showImg.setOnClickListener(mOnClick);
        showImg.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mPhotoView.setImageBitmap(mTakePictureBitmap);
                mPhotoView.setVisibility(View.VISIBLE);
                return false;
            }
        });

        new Handler().post(new Runnable() {
            @Override
            public void run() {
//                cameraApiFragment.setTopViewMarginTop(100);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        mCameraApiFragment.startDetect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCameraApiFragment.stopDetect();
    }

    private View.OnClickListener mOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (v.getId() == R.id.show_img) {
                if(mTakePictureBitmap == null){
                    return;
                }
                mPhotoEnhance = new PhotoEnhanceUtil(mTakePictureBitmap);

                mPhotoEnhance.setContrast(200);
                Bitmap source = mPhotoEnhance.handleImage(mPhotoEnhance.Enhance_Contrast);
                mPhotoView.setImageBitmap(source);
                mPhotoView.setVisibility(View.VISIBLE);
                return;
            }
        }
    };

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    @Override
    public void onBackPressed() {
        if (mPhotoView.getVisibility() == View.VISIBLE) {
            mPhotoView.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }
}
