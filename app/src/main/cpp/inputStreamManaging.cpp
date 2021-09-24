#include <jni.h>
#include<oboe/Oboe.h>
#include<deque>
#include<string>
#include<array>
#include<algorithm>
#include <jni.h>
// TODO: change include to import
#include "streamManaging.cpp"
#define APPNAME "C++NdkCodeOfJniDemo"
#include <android/log.h>

#include "../jni/c/c_api.h"
#include "../jni/c/common.h"
#include "../jni/builtin_ops.h"
#include "../jni/c/c_api_types.h"

#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

#include <cstdint>


class InputStreamManager : public StreamManager, public oboe::AudioStreamDataCallback {
    // TODO: rewrite for stereo
    // TODO: add filtering of pitches based on accumulated sum and count (to prevent spikes
    //  in silence)
public:
    explicit InputStreamManager (char* modelPath) {
        // TODO: move pitch detection to separate object
        pitchModel = TfLiteModelCreateFromFile(modelPath);
        TfLiteInterpreterOptions* options = TfLiteInterpreterOptionsCreate();

        tfInterpreter = TfLiteInterpreterCreate(pitchModel, options);
        TfLiteInterpreterAllocateTensors(tfInterpreter); // TODO: understand why we use it
        inputTensor = TfLiteInterpreterGetInputTensor(tfInterpreter, 0);
        outTensor = TfLiteInterpreterGetOutputTensor(tfInterpreter, 0);

        oboe::AudioStreamBuilder streamBuilder;

        streamBuilder.setDirection(oboe::Direction::Input);
        streamBuilder.setSampleRate(modelSampleRate);
        streamBuilder.setChannelCount(inputChannelCount);
        streamBuilder.setDataCallback(this);
        streamBuilder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
        streamBuilder.setFramesPerDataCallback(modelInputSize);

        streamBuilder.openStream(stream);

    }

    int turnOn () override {
        oboe::Result result = stream->requestStart();
        return getResultCode(result);
    }

    int turnOff () override {
        oboe::Result result = stream->requestStop();
        return getResultCode(result);
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
        if (numFrames <= 0) {
            throw std::invalid_argument("numFrames should be positive");
        }
        const auto *inputFloats = static_cast<const float *>(audioData);

//        auto numSamples = numFrames * inpStream->getChannelCount();
//        float inputSum = 0;
//        for (int i = 0; i < numSamples; i++) {
//            inputSum += *inputFloats;
//            inputFloats++;
//        }

        float inpMean = calcMean(inputFloats, numFrames);
        float inpStd = calcStd(inputFloats, inpMean, numFrames);
        __android_log_print(ANDROID_LOG_INFO, APPNAME, "inp mean %f", inpMean);
        __android_log_print(ANDROID_LOG_INFO, APPNAME, "inp std %f", inpStd);

        // TODO: maybe should apply batches here (e.g, now input has size (1024,), but
        //  should be (1, 1024)
        float inputNormalized[modelInputSize] = {};
        for (int i = 0; i < numFrames; i++) {
            inputNormalized[i] = (inputFloats[i] - inpMean) / inpStd;
        }

        float* inputNormalizedPointer = inputNormalized;

        // 2. get model predictions
        TfLiteStatus from_status = TfLiteTensorCopyFromBuffer(
                inputTensor,
                inputNormalizedPointer,
                TfLiteTensorByteSize(inputTensor));

        TfLiteStatus interpreter_invoke_status = TfLiteInterpreterInvoke(tfInterpreter);
        TfLiteStatus to_status = TfLiteTensorCopyToBuffer(
                outTensor,
                pitchProbs,
                TfLiteTensorByteSize(outTensor));

        __android_log_print(ANDROID_LOG_INFO, APPNAME, "from status %u", from_status);
        __android_log_print(ANDROID_LOG_INFO, APPNAME, "invoke status %u", interpreter_invoke_status);
        __android_log_print(ANDROID_LOG_INFO, APPNAME, "to status %u", to_status);

        int bestPitch = -1;
        float currPitchMax = -1.0f;

        for (int i = std::max(minPitch, 0); i <= std::min(maxPitch, pitchModelOutSize - 1); i++) {
            if (pitchProbs[0][i] > currPitchMax) {
                currPitchMax = pitchProbs[0][i];
                bestPitch = i;
            }
        }
        if (currPitchMax >= pitchThresh) {
            pitches.push_front(bestPitch);
        } else {
            pitches.push_front(-1);
        }

        return oboe::DataCallbackResult::Continue;
    }


private:
    static float calcMean(const float * arr, int length) {
        float sum = 0;
        for (int i = 0; i < length; i++) {
            sum += arr[i];
        }
        return sum / length;
    }

    float calcStd(const float * arr, float& mean, int length) {
        float var = 0;
        for(int i = 0; i < length; i++)
        {
            var += (arr[i] - mean) * (arr[i] - mean);
        }
        var /= length;
        float standardDeviation = sqrt(var);
        return standardDeviation;
    }
    // TODO: set min amd max pitches (and probably threshold) using user data
    const int minPitch = 80;
    const int maxPitch = 220;
    constexpr const static float pitchThresh = 0.5;

    const static int modelInputSize = 1024;
    static const int pitchModelOutSize = 360;
    static const int modelSampleRate = 16000;

    TfLiteModel * pitchModel {};
    TfLiteInterpreter * tfInterpreter;
    TfLiteTensor * inputTensor;
    const TfLiteTensor * outTensor;
    float pitchProbs[1][pitchModelOutSize] {};

    std::deque<float> pitches;
    std::shared_ptr<oboe::AudioStream> stream;
    int inputChannelCount = oboe::ChannelCount::Mono;
};
