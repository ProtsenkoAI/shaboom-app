#include <jni.h>
#include"streamManaging.cpp"

InputStreamManager* managerFromHandle (jlong engineHandle) {
    auto* manager = reinterpret_cast<InputStreamManager *>(engineHandle);
    return manager;
}


extern "C"
JNIEXPORT jlong JNICALL
Java_com_example_jnidemo_InputManager_00024ExternalGate_createEngine(JNIEnv *env, jobject thiz) {
    auto* manager = new InputStreamManager();
    return reinterpret_cast<jlong>(manager);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_jnidemo_InputManager_00024ExternalGate_turnOnStream(JNIEnv *env, jobject thiz,
                                                                      jlong engine_handle) {
    auto* manager = managerFromHandle(engine_handle);
    auto status = manager->turnOn();
    return reinterpret_cast<jint>(status);
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_example_jnidemo_InputManager_00024ExternalGate_turnOffStream(JNIEnv *env, jobject thiz,
                                                                     jlong engine_handle) {
    auto* manager = managerFromHandle(engine_handle);
    auto status = manager->turnOff();
    return reinterpret_cast<jint>(status);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_jnidemo_InputManager_00024ExternalGate_hasNewPitches(JNIEnv *env, jobject thiz,
                                                                      jlong engine_handle) {
    auto* manager = managerFromHandle(engine_handle);
    auto hasNewPitches = manager->hasNextPitch();
    return jboolean(hasNewPitches);

}
extern "C"
JNIEXPORT jfloat JNICALL
Java_com_example_jnidemo_InputManager_00024ExternalGate_nextPitch(JNIEnv *env, jobject thiz,
                                                                  jlong engine_handle) {
    auto* manager = managerFromHandle(engine_handle);
    float pitch = manager->nextPitch();
    return jfloat(pitch);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_example_jnidemo_OutputManager_00024ExternalGate_createEngine(JNIEnv *env, jobject thiz,
                                                                      jstring songPath) {
    const char *path = env->GetStringUTFChars(songPath, NULL);
//    long long one = 1;
//    return reinterpret_cast<jlong>(one);
    auto* manager = new OutputStreamManager(path, env);
    return reinterpret_cast<jlong>(manager);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_jnidemo_OutputManager_00024ExternalGate_turnOnStream(JNIEnv *env, jobject thiz,
                                                                      jlong engine_handle) {
    // TODO: implement turnOnStream()
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_example_jnidemo_OutputManager_00024ExternalGate_turnOffStream(JNIEnv *env, jobject thiz,
                                                                       jlong engine_handle) {
    // TODO: implement turnOffStream()
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_jnidemo_OutputManager_00024ExternalGate_hasNewPitches(JNIEnv *env, jobject thiz,
                                                                       jlong engine_handle) {
    // TODO: implement hasNewPitches()
}
extern "C"
JNIEXPORT jfloat JNICALL
Java_com_example_jnidemo_OutputManager_00024ExternalGate_nextPitch(JNIEnv *env, jobject thiz,
                                                                   jlong engine_handle) {
    // TODO: implement nextPitch()
}