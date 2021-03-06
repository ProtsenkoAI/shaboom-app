cmake_minimum_required(VERSION 3.10)

project(PitchDetectionEngine)

find_package (oboe REQUIRED CONFIG)

find_library( # Sets the name of the path variable.
        android-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        android )

find_library( media-lib mediandk )
#find_library()


add_library(PitchDetectionEngine
        SHARED
        streamManaging.cpp
        inputStreamManaging.cpp
        outputStreamManaging.cpp
        jni_bridge.cpp
        )

add_subdirectory("AudioFile")

find_library( log-lib log )


# set up tflite
set(JNI_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../jni)
add_library(tflite-lib SHARED IMPORTED)
set_target_properties(tflite-lib
        PROPERTIES IMPORTED_LOCATION
        ${JNI_DIR}/${ANDROID_ABI}/libtensorflowlite_jni.so)
include_directories( ${JNI_DIR} )


target_link_libraries(PitchDetectionEngine
        oboe::oboe
        android
        mediandk
        ${log-lib}
        tflite-lib
        )

