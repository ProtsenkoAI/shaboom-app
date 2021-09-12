cmake_minimum_required(VERSION 3.10)

project(PitchDetectionEngine)

find_package (oboe REQUIRED CONFIG)

find_library( # Sets the name of the path variable.
        android-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        android )

find_library( media-lib mediandk )


add_library(PitchDetectionEngine
        SHARED
        streamManaging.cpp
        NDKExtractor.cpp
        NDKExtractor.h
        jni_bridge.cpp
        )

#add_library(AudioFile
#        SHARED
#        AudioFile/AudioFile.h
#        )
add_subdirectory("AudioFile")

target_link_libraries(PitchDetectionEngine oboe::oboe android mediandk
        )

#target_link_libraries(
#
#        ${log-lib}
#        ${android-lib})