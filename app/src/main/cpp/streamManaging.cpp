#include <jni.h>
#include<oboe/Oboe.h>
#include<deque>

#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <jni.h>
#include "NDKExtractor.h"


class InputStreamManager : oboe::AudioStreamDataCallback {
    // TODO: check results opening streams and start / stop are OK
    // TODO: separate storing data from stream from this class
public:
    InputStreamManager () {
        oboe::AudioStreamBuilder streamBuilder;

        streamBuilder.setDirection(oboe::Direction::Input);
        streamBuilder.setSampleRate(sampleRate);
        streamBuilder.setChannelCount(inputChannelCount);
        streamBuilder.setDataCallback(this);

        streamBuilder.openStream(stream);

    }
    int turnOn () {
        oboe::Result result = stream->requestStart();
        return getResultCode(result);

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

    // TODO: iterator functionality using hasNextPitch and nextPitch, later will do it better
    bool hasNextPitch() {
        /**Checks whether nextPitch can return new pitch or not */
        return !pitches.empty();
    }

    float nextPitch() {
        float pitch = pitches.back();
        pitches.pop_back();
        return pitch;
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
        pitches.push_front(inputSum);
        return oboe::DataCallbackResult::Continue;
    }

private:
    std::shared_ptr<oboe::AudioStream> stream;
    int sampleRate = 48000;
    int inputChannelCount = oboe::ChannelCount::Stereo;
    std::deque<float> pitches = {4.f, 3.f, 2.f};
};


class OutputStreamManager : oboe::AudioStreamDataCallback {
public:
    explicit OutputStreamManager(const char *audioPath, JNIEnv *env) : {

        AAsset *asset = AAssetManager_open(&mAssetManager, audioPath, AASSET_MODE_UNKNOWN);

        off_t assetSize = AAsset_getLength(asset);
        const long maximumDataSizeInBytes = 12 * assetSize * sizeof(int16_t);
        auto decodedData = new uint8_t[maximumDataSizeInBytes];

        int64_t bytesDecoded = NDKExtractor::decode(asset, decodedData, sampleRate, outChannelCount);
        auto numSamples = bytesDecoded / sizeof(int16_t);
        mNumSamples = numSamples;

        auto outputBuffer = std::make_unique<float[]>(numSamples);

        //#if USE_FFMPEG==1
        //        memcpy(outputBuffer.get(), decodedData, (size_t)bytesDecoded);
        //#else
        // The NDK decoder can only decode to int16, we need to convert to floats
        oboe::convertPcm16ToFloat(
                reinterpret_cast<int16_t*>(decodedData),
                outputBuffer.get(),
                bytesDecoded / sizeof(int16_t));
        //#endif

        delete[] decodedData;
        AAsset_close(asset);

//        return new AAssetDataSource(std::move(outputBuffer),
//                                    numSamples,);
//        mClap = std::make_unique<Player>(mClapSource);
//        mBuffer = std::move(outputBuffer);
        mDataBuffer = outputBuffer.get();

        oboe::AudioStreamBuilder streamBuilder;

        streamBuilder.setDirection(oboe::Direction::Output);
        streamBuilder.setSampleRate(sampleRate);
        streamBuilder.setChannelCount(outChannelCount);
        streamBuilder.setDataCallback(this);

        streamBuilder.openStream(stream);
    }
    int turnOn () {
        oboe::Result result = stream->requestStart();
        return getResultCode(result);

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
        auto* audioData = (float *)&voidAudioData;

        for (int i = 0; i < mNumSamples / outChannelCount; ++i) {
            for (int j = 0; j < outChannelCount; ++j) {
                audioData[(i*outChannelCount)+j] = mDataBuffer[(mReadIndex*outChannelCount)+j];
                mReadIndex++;
            }
        }

        return oboe::DataCallbackResult::Continue;
    }

private:
    int mReadIndex = 0;
    int mNumSamples;
    float* mDataBuffer;
    std::shared_ptr<oboe::AudioStream> stream;
    int sampleRate = 48000;
    const int outChannelCount = oboe::ChannelCount::Stereo;
    AAssetManager &mAssetManager;
};