#ifndef STREAM_MANAGING
#define STREAM_MANAGING

#include <jni.h>
#include<oboe/Oboe.h>
#include<string>


class StreamManager {
    // TODO: check results opening streams and start / stop are OK
    // TODO: separate storing data from stream from this class
public:
    virtual std::shared_ptr<oboe::AudioStream> getStream() = 0;

    int turnOn () {
        oboe::Result result = getStream()->requestStart();
        return getResultCode(result);
    }

    int turnOff () {
        oboe::Result result = getStream()->requestStop();
        return getResultCode(result);
    }

    static int getResultCode(oboe::Result result) {
        if (result == oboe::Result::OK) {
            return 0;
        } else {
            return 1;
        }
    }
};

#endif

