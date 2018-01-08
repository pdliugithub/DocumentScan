package com.rossia.life.scan.transfer;

/**
 * Created by pd_liu on 2017/12/29.
 */

public class TransferSample {

    /**
     * 加载C++ 的编译产出，这是必须的代码.
     */
    static {
        System.loadLibrary("native-lib");
    }

    /**
     * 这是对外暴露的接口，在接口内部对JNI进行调用.
     *
     * @param a 数值
     */
    public String convertIntToString(int a) {

        /*
        调用底层
         */
        StringClass stringClass = new StringClass();
        jni_string(5, stringClass.value);

        return stringClass.value;
    }

    /**
     * 这是调用C++代码的JNI方法，String
     */

    private static native int jni_string(int input, String out);

    private static native void jni_2(int input, StringClass output);

    class StringClass {
        String value;
    }
}
