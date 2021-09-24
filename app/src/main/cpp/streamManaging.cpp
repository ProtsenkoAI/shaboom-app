#ifndef STREAM_MANAGING
#define STREAM_MANAGING

#include <jni.h>
#include<oboe/Oboe.h>
#include<string>


class StreamManager {
    // TODO: check results opening streams and start / stop are OK
    // TODO: separate storing data from stream from this class
public:
    virtual int turnOn() = 0;
    virtual  int turnOff() = 0;

    static int getResultCode(oboe::Result result) {
        if (result == oboe::Result::OK) {
            return 0;
        } else {
            return 1;
        }
    }
};

#endif

