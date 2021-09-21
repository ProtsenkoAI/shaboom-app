#include "AudioFile/AudioFile.h"
#include <string>
#include <jni.h>
#include<oboe/Oboe.h>
#include<deque>
#include<random>

#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

#include <android/log.h>

#define APPNAME "C++NdkCodeOfJniDemo"


class OutputStreamManager : oboe::AudioStreamDataCallback {
// TODO: fix sample rate issues
public:
    explicit OutputStreamManager(int fd) {
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "The value of 1 + 1 is %d", 1+1);
        bool loadSuccess = audioFile.load(fd);

        if (!loadSuccess) {
            exit(999);
        } else {
        }

        oboe::AudioStreamBuilder streamBuilder;

        streamBuilder.setDirection(oboe::Direction::Output);
        streamBuilder.setSampleRate(sampleRate);
        streamBuilder.setChannelCount(oboe::Stereo);
        streamBuilder.setDataCallback(this);
        streamBuilder.setPerformanceMode(oboe::PerformanceMode::LowLatency);

//        streamBuilder.setBufferCapacityInFrames(240 / 20);

        streamBuilder.openStream(stream);
    }
    int turnOn () {
        oboe::Result result = stream->requestStart();
        return getResultCode(result);
        return 0;
    }

    int turnOff () {
        oboe::Result result = stream->requestStop();
        return getResultCode(result);
    }

    static int getResultCode(oboe::Result result) {
        if (result == oboe::Result::OK) {
            return 0;
        } else {
            return 1;
        }
    }

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream* inpStream,
                                          void *voidAudioData,
                                          int32_t numFrames) override {
        auto *audioData = static_cast<float *>(voidAudioData);
        auto channelCount = inpStream->getChannelCount();

        for (int i = 0; i < numFrames; ++i) {
            for (int j = 0; j < channelCount; ++j) {
                audioData[(i * channelCount) + j] = (float) audioFile.samples[j][mReadIndex];
            }
            mReadIndex++;
        }

        return oboe::DataCallbackResult::Continue;
    }

private:
    AudioFile<float> audioFile;
    long int mReadIndex = 0;
//    int mNumSamples;
//    float* mDataBuffer{};
    std::shared_ptr<oboe::AudioStream> stream;
    int sampleRate = 48000;
};