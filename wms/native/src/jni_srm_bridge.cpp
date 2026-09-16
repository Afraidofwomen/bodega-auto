#include <jni.h>
#include "srm_bridge.h"

extern "C" {

JNIEXPORT jint JNICALL
Java_com_bodega_wms_gateway_SrmNative_start(JNIEnv *env, jobject, jstring config) {
    const char *utf = env->GetStringUTFChars(config, nullptr);
    int rc = srm_bridge_start(utf);
    env->ReleaseStringUTFChars(config, utf);
    return rc;
}

JNIEXPORT jint JNICALL
Java_com_bodega_wms_gateway_SrmNative_store(JNIEnv *env, jobject, jstring cmd, jstring slot, jstring sku) {
    const char *c = env->GetStringUTFChars(cmd, nullptr);
    const char *s = env->GetStringUTFChars(slot, nullptr);
    const char *k = env->GetStringUTFChars(sku, nullptr);
    int rc = srm_bridge_store(c, s, k);
    env->ReleaseStringUTFChars(cmd, c);
    env->ReleaseStringUTFChars(slot, s);
    env->ReleaseStringUTFChars(sku, k);
    return rc;
}

JNIEXPORT jint JNICALL
Java_com_bodega_wms_gateway_SrmNative_retrieve(JNIEnv *env, jobject, jstring cmd, jstring slot) {
    const char *c = env->GetStringUTFChars(cmd, nullptr);
    const char *s = env->GetStringUTFChars(slot, nullptr);
    int rc = srm_bridge_retrieve(c, s);
    env->ReleaseStringUTFChars(cmd, c);
    env->ReleaseStringUTFChars(slot, s);
    return rc;
}

JNIEXPORT jint JNICALL
Java_com_bodega_wms_gateway_SrmNative_abort(JNIEnv *env, jobject, jstring cmd, jstring action) {
    const char *c = env->GetStringUTFChars(cmd, nullptr);
    const char *a = env->GetStringUTFChars(action, nullptr);
    int rc = srm_bridge_abort(c, a);
    env->ReleaseStringUTFChars(cmd, c);
    env->ReleaseStringUTFChars(action, a);
    return rc;
}

JNIEXPORT void JNICALL
Java_com_bodega_wms_gateway_SrmNative_stop(JNIEnv *, jobject) {
    srm_bridge_stop();
}

}
