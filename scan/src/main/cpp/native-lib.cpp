#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring

JNICALL
Java_com_rossia_life_documentscan_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_rossia_life_scan_transfer_TransferSample_jni_1string(JNIEnv *env, jclass type, jint input,
                                                              jstring out_) {
    const char *out = env->GetStringUTFChars(out_, 0);

    // TODO

    env->ReleaseStringUTFChars(out_, out);
}extern "C"
JNIEXPORT void JNICALL
Java_com_rossia_life_scan_transfer_TransferSample_jni_12(JNIEnv *env, jclass type, jint input,
                                                         jobject output) {

    // TODO

}