#include "AudioFile/AudioFile.h"
#include <string>
#include <jni.h>
#include<oboe/Oboe.h>
#include <android/log.h>

#include"streamManaging.cpp"
// TODO: refactor logging
#define APPNAME "C++NdkCodeOfJniDemo"


class OutputStreamManager : public StreamManager, oboe::AudioStreamDataCallback {
// TODO: fix sample rate issues
public:

    explicit OutputStreamManager(int fd) {
        bool loadSuccess = audioFile.load(fd);

        if (!loadSuccess) {
            __android_log_print(ANDROID_LOG_ERROR, APPNAME,
                                "failed to load audio with %d descriptor", fd);
            exit(999);
        } else {
        }

        oboe::AudioStreamBuilder streamBuilder;

        streamBuilder.setDirection(oboe::Direction::Output);
        streamBuilder.setSampleRate(sampleRate);
        streamBuilder.setChannelCount(oboe::Stereo);
        streamBuilder.setDataCallback(this);
        streamBuilder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
        streamBuilder.openStream(stream);
    }

    std::shared_ptr<oboe::AudioStream> getStream() override {
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "getStream called");
        return stream;
    };

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
    std::shared_ptr<oboe::AudioStream> stream;
    int sampleRate = 48000;
};