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

#include "../jni/c/c_api.h"
#include "../jni/c/common.h"
#include "../jni/builtin_ops.h"


class InputStreamManager : public StreamManager, public oboe::AudioStreamDataCallback {
public:
    explicit InputStreamManager (const char* modelPath) {
        pitchModel = TfLiteModelCreateFromFile(modelPath);

        oboe::AudioStreamBuilder streamBuilder;

        streamBuilder.setDirection(oboe::Direction::Input);
//        streamBuilder.setSampleRate(sampleRate);
        streamBuilder.setChannelCount(inputChannelCount);
        streamBuilder.setDataCallback(this);
        streamBuilder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
        streamBuilder.setFramesPerDataCallback(modelInputSize);

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

//        auto numSamples = numFrames * inpStream->getChannelCount();
//        float inputSum = 0;
//        for (int i = 0; i < numSamples; i++) {
//            inputSum += *inputFloats;
//            inputFloats++;
//        }

        // 1. normalize


        // 2. get model predictions

        
        // 3. clear out too low and too high pitch predictions

        // 4. get confidence and predicted pitch

        __android_log_print(ANDROID_LOG_ERROR, APPNAME, "current input size %d", numFrames);
        pitches.push_front(numFrames);

        return oboe::DataCallbackResult::Continue;
    }


private:
    TfLiteModel * pitchModel;
    std::deque<float> pitches;
    std::shared_ptr<oboe::AudioStream> stream;
//    int sampleRate;
    int inputChannelCount = oboe::ChannelCount::Stereo;
    int modelInputSize = 1024;
};
