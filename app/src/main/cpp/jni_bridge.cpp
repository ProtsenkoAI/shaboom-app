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
    auto* manager = new InputStreamManager();
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

//extern "C"
//JNIEXPORT jfloatArray JNICALL
//Java_com_example_jnidemo_InputManager_00024ExternalGate_getPitches(JNIEnv *env, jobject thiz,
//                                                                   jlong engine_handle) {
//    // TODO: implement getPitches()
//    auto* manager = inputManagerFromHandle(engine_handle);
//    std::deque<float>& pitches = manager->takePitches();
//
//    jfloatArray callerArray = env->NewFloatArray(pitches.size());
//
//    if (callerArray == NULL) {
//        // TODO: refactor passing type to log msg
//        __android_log_print(ANDROID_LOG_ERROR, APPNAME,
//                            "failed to init java array with type %s", "jfloat");
//        return nullptr;
//    } else {
//        for (int i = 0; i < pitches.size(); i++) {
//            float pitch = pitches.back();
//            pitches.pop_back();
//            // we are responsive to clear deque
//            auto jPitch = jfloat(pitch);
//            env->SetFloatArrayRegion(callerArray, i, 1, &jPitch);
//        }
//    }
//    return callerArray;
//}

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