package com.rossia.life.scan.transfer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.rossia.life.scan.common.util.LogUtil;

import java.util.Calendar;

/**
 * @author pd_liu on 2018/1/16.
 *         <p>
 *         传感器移动事件控制器
 *         </p>
 */

public class SensorMoveControl implements SensorEventListener {

    private static final String TAG_LOG = "SensorMoveControl";

    private SensorManager mSensorManager;

    private Sensor mSensor;

    private SensorMoveListener mSensorMoveListener;

    private Calendar mCalendar;

    boolean isFocusing = false;
    boolean canFocusIn = false;  //内部是否能够对焦控制机制
    boolean canFocus = false;

    private int mX, mY, mZ;

    public static final int STATUS_NONE = 0;
    public static final int STATUS_STATIC = 1;
    public static final int STATUS_MOVE = 2;
    private int STATUE = STATUS_NONE;

    private long lastStaticStamp = 0;

    /**
     * 延迟的时间
     */
    public static final int DELEY_DURATION = 500;

    private SensorMoveControl(Context context) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true);
    }

    public static SensorMoveControl newInstance(Context context) {
        return new SensorMoveControl(context);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == null) {
            return;
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            int x = (int) event.values[0];
            int y = (int) event.values[1];
            int z = (int) event.values[2];
            mCalendar = Calendar.getInstance();
            long stamp = mCalendar.getTimeInMillis();// 1393844912

            int second = mCalendar.get(Calendar.SECOND);// 53

            if (STATUE != STATUS_NONE) {
                int px = Math.abs(mX - x);
                int py = Math.abs(mY - y);
                int pz = Math.abs(mZ - z);
//                Log.d(TAG, "pX:" + px + "  pY:" + py + "  pZ:" + pz + "    stamp:"
//                        + stamp + "  second:" + second);
                double value = Math.sqrt(px * px + py * py + pz * pz);
                if (value > 1.0) {//1.4
//                    textviewF.setText("检测手机在移动..");
                    LogUtil.e(TAG_LOG, "检测手机在移动mobile moving");
                    STATUE = STATUS_MOVE;
                    if (mSensorMoveListener != null) {
                        mSensorMoveListener.onMoving();
                    }
                } else {
//                    textviewF.setText("检测手机静止..");
                    LogUtil.e(TAG_LOG, "检测手机静止mobile static");
                    //上一次状态是move，记录静态时间点
                    if (STATUE == STATUS_MOVE) {
                        lastStaticStamp = stamp;
                        canFocusIn = true;
                    }

                    if (canFocusIn) {
                        if (stamp - lastStaticStamp > DELEY_DURATION) {
                            //移动后静止一段时间，可以发生对焦行为
                            if (!isFocusing) {
                                canFocusIn = false;
//                                onCameraFocus();
                                if (mSensorMoveListener != null) {
                                    mSensorMoveListener.onStaticing();
                                }
                            }
                        }
                    }

                    STATUE = STATUS_STATIC;
                }
            } else {
                lastStaticStamp = stamp;
                STATUE = STATUS_STATIC;
            }

            mX = x;
            mY = y;
            mZ = z;
        }
    }

    public void startSensor() {
        STATUE = STATUS_NONE;
        canFocusIn = false;
        mX = 0;
        mY = 0;
        mZ = 0;

        canFocus = true;
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stopSensor() {
        mSensorManager.unregisterListener(this, mSensor);
        canFocus = false;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void setSensorMoveListener(SensorMoveListener sensorMoveListener) {
        mSensorMoveListener = sensorMoveListener;
    }

    public interface SensorMoveListener {
        void onMoving();

        void onStaticing();
    }
}
