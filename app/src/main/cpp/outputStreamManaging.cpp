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
        __android_log_print(ANDROID_LOG_INFO, APPNAME,
                            "NDK loading descriptor %d", fd);
        bool loadSuccess = audioFile.load(fd);
        numFileSamples = audioFile.getNumSamplesPerChannel();
        sampleRate = audioFile.getSampleRate();

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

    bool getIsPlaying () const {
        return isPlaying;
    }

    int turnOn () override {
        mReadIndex = 0;
        oboe::Result result = stream->requestStart();
        int resCode = getResultCode(result);
        if (resCode == 0) {
            isPlaying = true;
        }
        return resCode;
    }

    int turnOff () override {
        oboe::Result result = stream->requestStop();
        int resCode = getResultCode(result);
        isPlaying = false;
        return resCode;
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
            if (mReadIndex >= numFileSamples - 1) {
                __android_log_print(ANDROID_LOG_INFO, APPNAME,
                                    "end of song");
                turnOff();
                return oboe::DataCallbackResult::Stop;
            }
        }

        return oboe::DataCallbackResult::Continue;
    }

private:
    AudioFile<float> audioFile;
    int numFileSamples;
    bool isPlaying = false;
    long int mReadIndex = 0;
    std::shared_ptr<oboe::AudioStream> stream;
    int sampleRate;
};