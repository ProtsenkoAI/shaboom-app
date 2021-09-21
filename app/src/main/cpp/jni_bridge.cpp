#include <jni.h>
#include"inputStreamManaging.cpp"

InputStreamManager* inputManagerFromHandle (jlong engineHandle) {
    auto* manager = reinterpret_cast<InputStreamManager *>(engineHandle);
    return manager;
}

#include"outputStreamManaging.cpp"

OutputStreamManager* outputManagerFromHandle (jlong engineHandle) {
    auto* manager = reinterpret_cast<OutputStreamManager *>(engineHandle);
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
    auto* manager = inputManagerFromHandle(engine_handle);
    auto status = manager->turnOn();
//    int status = 0;
//    return reinterpret_cast<jint>(status);
    return reinterpret_cast<jint>(status);
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_example_jnidemo_InputManager_00024ExternalGate_turnOffStream(JNIEnv *env, jobject thiz,
                                                                     jlong engine_handle) {
    auto* manager = inputManagerFromHandle(engine_handle);
    auto status = manager->turnOff();
    return reinterpret_cast<jint>(status);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_jnidemo_InputManager_00024ExternalGate_hasNewPitches(JNIEnv *env, jobject thiz,
                                                                      jlong engine_handle) {
    auto* manager = inputManagerFromHandle(engine_handle);
    auto hasNewPitches = manager->hasNextPitch();
    return jboolean(hasNewPitches);

}
extern "C"
JNIEXPORT jfloat JNICALL
Java_com_example_jnidemo_InputManager_00024ExternalGate_nextPitch(JNIEnv *env, jobject thiz,
                                                                  jlong engine_handle) {
    auto* manager = inputManagerFromHandle(engine_handle);
    float pitch = manager->nextPitch();
    return jfloat(pitch);
}

//#include "/home/arseny/Android/Sdk/ndk/23.0.7599858/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include/android/file_descriptor_jni.h"
#include <string>

extern "C"
JNIEXPORT jlong JNICALL
Java_com_example_jnidemo_OutputManager_00024ExternalGate_createEngine(JNIEnv *env, jobject thiz,
                                                                      jint file_descriptor) {
    // TODO: remove using file_descriptor;
//    const char *path = env->GetStringUTFChars(songPath, NULL);
//    const std::string path = "/storage/emulated/0/Download/file_example_WAV_10MG.wav";
    const int fd = reinterpret_cast<int>(file_descriptor);

    auto* manager = new OutputStreamManager(fd);
    return reinterpret_cast<jlong>(manager);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_jnidemo_OutputManager_00024ExternalGate_turnOnStream(JNIEnv *env, jobject thiz,
                                                                      jlong engine_handle) {
    auto* manager = outputManagerFromHandle(engine_handle);
    int status = manager->turnOn();
//    int one = 1;
//    return reinterpret_cast<jint>(one);
    return reinterpret_cast<jint>(status);
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_example_jnidemo_OutputManager_00024ExternalGate_turnOffStream(JNIEnv *env, jobject thiz,
                                                                       jlong engine_handle) {
    auto* manager = outputManagerFromHandle(engine_handle);
    int status = manager->turnOff();
    return reinterpret_cast<jint>(status);
}