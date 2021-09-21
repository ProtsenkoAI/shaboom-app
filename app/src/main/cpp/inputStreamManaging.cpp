#include <jni.h>
#include<oboe/Oboe.h>
#include<deque>
#include<string>
#include<array>

#include <jni.h>
// TODO: change include to import
#include "streamManaging.cpp"
#define APPNAME "C++NdkCodeOfJniDemo"
#include <android/log.h>


class InputStreamManager : public StreamManager, public oboe::AudioStreamDataCallback {
public:
    InputStreamManager () {
        oboe::AudioStreamBuilder streamBuilder;

        streamBuilder.setDirection(oboe::Direction::Input);
//        streamBuilder.setSampleRate(sampleRate);
        streamBuilder.setChannelCount(inputChannelCount);
        streamBuilder.setDataCallback(this);
        streamBuilder.setPerformanceMode(oboe::PerformanceMode::LowLatency);

        streamBuilder.openStream(stream);

    }
    std::shared_ptr<oboe::AudioStream> getStream() override {
        return stream;
    }

    bool hasNextPitch() const {
        return !pitches.empty();
    }

    float nextPitch() {
        float elem = pitches.back();
        pitches.pop_back();
        return elem;
    }


    oboe::DataCallbackResult onAudioReady(oboe::AudioStream* inpStream,
                                          void *audioData,
                                          int32_t numFrames) override {
        // TODO: pitch detection

        const auto *inputFloats = static_cast<const float *>(audioData);

        auto numSamples = numFrames * inpStream->getChannelCount();
        float inputSum = 0;
        for (int i = 0; i < numSamples; i++) {
            inputSum += *inputFloats;
            inputFloats++;
        }

        __android_log_print(ANDROID_LOG_ERROR, APPNAME, "current input sum %f", inputSum);
        pitches.push_front(inputSum);

        return oboe::DataCallbackResult::Continue;
    }


private:
    std::deque<float> pitches;
    std::shared_ptr<oboe::AudioStream> stream;
//    int sampleRate;
    int inputChannelCount = oboe::ChannelCount::Stereo;
};
