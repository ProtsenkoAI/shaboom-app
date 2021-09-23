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
#include "../jni/model_builder.h"

#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>


class InputStreamManager : public StreamManager, public oboe::AudioStreamDataCallback {
public:
    explicit InputStreamManager (AAssetManager* assetManager) {
        // Open the file from assets in BUFFER_MODE
// Streaming might work too?
        AAsset* asset = AAssetManager_open(assetManager, "crepe-medium.tflite", AASSET_MODE_BUFFER);

        if(asset == NULL)
        {
            __android_log_print(ANDROID_LOG_ERROR, APPNAME, "couldn't load asset");
            exit(1);
        }
        off_t start;
        off_t length;
        const int fd = AAsset_openFileDescriptor(asset, &start, &length);

        off_t  dataSize = AAsset_getLength(asset);
        const void* const memory = AAsset_getBuffer(asset);

// Use as const char*
        const char* const memChar = (const char*) memory;

// Create a new Buffer for the FlatBuffer with the size needed.
// It has to exist alongside the FlatBuffer model as long as the model shall exist!
// char* flatBuffersBuffer; (declared in the header file of the class in which I use this).
        flatBuffersBuffer = new char[dataSize];

        __android_log_print(ANDROID_LOG_ERROR, APPNAME, "copy asset to buffer, size: %ld", dataSize);
        for(int i = 0; i < dataSize; i++)
        {
            flatBuffersBuffer[i] = memChar[i];
        }

        pitchModel = tflite::FlatBufferModel::BuildFromBuffer(flatBuffersBuffer, dataSize);

        if (!pitchModel) {
            __android_log_print(ANDROID_LOG_ERROR, APPNAME, "couldn't load from buffer");
        }
        __android_log_print(ANDROID_LOG_INFO, APPNAME, "successfully loaded pitchModel");

// Make sure NOT to delete the flatBuffersBuffer now! If you would do it your models won't work!
// As long as you want to use the FlatBuffer model, the flatBuffersBuffer has to exist!

//        if (this->model)
//        {
//            // And so on...
//        }
        // TODO: move pitch detection to separate object
//        pitchModel = TfLiteModelCreateFromFile(modelPath);
        TfLiteInterpreterOptions* options = TfLiteInterpreterOptionsCreate();

        tfInterpreter = TfLiteInterpreterCreate(pitchModel, options);
        TfLiteInterpreterAllocateTensors(tfInterpreter); // TODO: understand why we use it
        inputTensor = TfLiteInterpreterGetInputTensor(tfInterpreter, 0);
        outTensor = TfLiteInterpreterGetOutputTensor(tfInterpreter, 0);

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

//        TfLiteStatus from_status = TfLiteTensorCopyFromBuffer(
//                inputTensor,
//                inputFloats,
//                TfLiteTensorByteSize(inputTensor));
//        TfLiteStatus interpreter_invoke_status = TfLiteInterpreterInvoke(tfInterpreter);
//        TfLiteStatus to_status = TfLiteTensorCopyToBuffer(
//                outTensor,
//                pitchProbs,
//                TfLiteTensorByteSize(outTensor));

        // TODO
        // 1. normalize


        // 2. get model predictions



        // 3. clear out too low and too high pitch predictions

        // 4. get confidence and predicted pitch

        __android_log_print(ANDROID_LOG_INFO, APPNAME, "current out size %d", pitchModelOutSize);
        pitches.push_front(numFrames);

        return oboe::DataCallbackResult::Continue;
    }


private:
    char* flatBuffersBuffer;

    int modelInputSize = 1024;
    static const int pitchModelOutSize = 360;

    TfLiteModel * pitchModel {};
    TfLiteInterpreter * tfInterpreter;
    TfLiteTensor * inputTensor;
    const TfLiteTensor * outTensor;
    double pitchProbs[pitchModelOutSize] {};

    std::deque<float> pitches;
    std::shared_ptr<oboe::AudioStream> stream;
//    int sampleRate;
    int inputChannelCount = oboe::ChannelCount::Stereo;
};
