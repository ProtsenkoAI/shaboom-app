#include <jni.h>
#include<oboe/Oboe.h>
#include<deque>
#include<string>
#include<array>

#include <jni.h>
// TODO: change include to import
#include "streamManaging.cpp"

class InputStreamManager : public StreamManager, public oboe::AudioStreamDataCallback {
public:
    InputStreamManager () {
        oboe::AudioStreamBuilder streamBuilder;

        streamBuilder.setDirection(oboe::Direction::Input);
        streamBuilder.setSampleRate(sampleRate);
        streamBuilder.setChannelCount(inputChannelCount);
        streamBuilder.setDataCallback(this);
        streamBuilder.setPerformanceMode(oboe::PerformanceMode::LowLatency);

        streamBuilder.openStream(stream);

    }
    std::shared_ptr<oboe::AudioStream> getStream() override {
        return stream;
    }

    std::deque<float>& takePitches() {
        // NOTE: of course, returning deque to user we delegate our responsibility of removing
        //  already taken data. Another approach is copy values to another container and then
        //  remove pitches from private object, but now we don't want these overheads.
        return pitches;
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

    std::deque<float> pitches;

private:
    std::shared_ptr<oboe::AudioStream> stream;
    int sampleRate = 48000;
    int inputChannelCount = oboe::ChannelCount::Stereo;
};
