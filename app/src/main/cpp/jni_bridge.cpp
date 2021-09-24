#include <jni.h>
#include <string>

#include"streamManaging.cpp"
#include"inputStreamManaging.cpp"
#include"outputStreamManaging.cpp"
#include<deque>
#define APPNAME "C++NdkCodeOfJniDemo"

StreamManager* streamManagerFromHandle (jlong engineHandle) {
    auto* manager = reinterpret_cast<StreamManager *>(engineHandle);
    return manager;
}


InputStreamManager* inputManagerFromHandle (jlong engineHandle) {
    auto* manager = reinterpret_cast<InputStreamManager *>(engineHandle);
    return manager;
}

jfloatArray createdArr;


extern "C"
JNIEXPORT jint JNICALL
Java_com_example_jnidemo_StreamManager_00024ExternalGate_turnOnStream(JNIEnv*, jobject,
                                                                      jlong engine_handle) {
    auto* manager = streamManagerFromHandle(engine_handle);
    auto status = manager->turnOn();
    return reinterpret_cast<jint>(status);
}


extern "C"
JNIEXPORT jint JNICALL
Java_com_example_jnidemo_StreamManager_00024ExternalGate_turnOffStream(JNIEnv*, jobject,
                                                                       jlong engine_handle) {
    auto* manager = streamManagerFromHandle(engine_handle);
    auto status = manager->turnOff();
    return reinterpret_cast<jint>(status);
}


extern "C"
JNIEXPORT jlong JNICALL
Java_com_example_jnidemo_InputManager_00024ExternalGate_createEngine(JNIEnv*, jobject) {
    // TODO: refactor work with model path

    char cwd[1024];
    if (getcwd(cwd, sizeof(cwd)) != NULL)
        __android_log_print(ANDROID_LOG_INFO, "ndk cwd", "%s", cwd);

    char modelPath[] = "/data/user/0/com.example.jnidemo/files/crepe-medium.tflite";
    char* modelPathPointer = modelPath;
    auto* manager = new InputStreamManager(modelPathPointer);
    return reinterpret_cast<jlong>(manager);
}


extern "C"
JNIEXPORT jlong JNICALL
Java_com_example_jnidemo_OutputManager_00024ExternalGate_createEngine(JNIEnv*, jobject,
                                                                      jint file_descriptor) {
    const int fd = reinterpret_cast<int>(file_descriptor);

    auto* manager = new OutputStreamManager(fd);
    return reinterpret_cast<jlong>(manager);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_jnidemo_InputManager_00024ExternalGate_hasNextPitch(JNIEnv *env, jobject thiz,
                                                                     jlong engine_handle) {
    auto* manager = inputManagerFromHandle(engine_handle);
    bool hasNextPitch = manager->hasNextPitch();
    return jboolean(hasNextPitch);

}


extern "C"
JNIEXPORT jfloat JNICALL
Java_com_example_jnidemo_InputManager_00024ExternalGate_nextPitch(JNIEnv *env, jobject thiz,
                                                                  jlong engine_handle) {
    auto *manager = inputManagerFromHandle(engine_handle);
    float nextPitch = manager->nextPitch();
    return jfloat(nextPitch);
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_jnidemo_OutputManager_00024ExternalGate_getIsPlaying(JNIEnv *env,
                                                                      jobject thiz,
                                                                      jlong engine_handle) {
    auto* manager = reinterpret_cast<OutputStreamManager *>(engine_handle);
    bool isPlaying = manager->getIsPlaying();
    return jboolean(isPlaying);
}