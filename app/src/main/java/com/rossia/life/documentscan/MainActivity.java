package com.rossia.life.documentscan;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import com.rossia.life.scan.ui.detector.CameraApiFragment;
import com.rossia.life.scan.ui.interf.TakePictureCallback;

public class MainActivity extends AppCompatActivity {

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

        CameraApiFragment cameraApiFragment = CameraApiFragment.newInstance();

        getSupportFragmentManager().beginTransaction().add(R.id.container, cameraApiFragment, "api").commit();

        cameraApiFragment.setTakePictureCallback(new TakePictureCallback() {
            @Override
            public void call(Bitmap bitmap) {
                showImg.setImageBitmap(bitmap);
            }
        });
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
