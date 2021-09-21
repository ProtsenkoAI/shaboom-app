#include <jni.h>
#include<oboe/Oboe.h>
#include<deque>
#include<string>
#include<random>

#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <jni.h>

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
//        pitches.push_front(inputSum);// TODO: uncomment!

        return oboe::DataCallbackResult::Continue;
    }

    std::deque<float> pitches = {4.f, 3.f, 2.f};

private:
    std::shared_ptr<oboe::AudioStream> stream;
    int sampleRate = 48000;
    int inputChannelCount = oboe::ChannelCount::Stereo;
};

