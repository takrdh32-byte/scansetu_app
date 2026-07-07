#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_scansetu_scansetu_1app_MainActivity_helloFromCpp(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++! ScanSetu native engine ready.";
    return env->NewStringUTF(hello.c_str());
}