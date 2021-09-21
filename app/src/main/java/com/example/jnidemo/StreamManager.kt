package com.example.jnidemo

import android.util.Log

abstract class StreamManager() {
    // engineHandle is pointer to C++ engine object, it's used to recreate access
    //  object on C++ side
    abstract fun getEngineHandle(): Long


    companion object ExternalGate {
        init {
            System.loadLibrary("PitchDetectionEngine")
        }

        external fun turnOnStream(engineHandle: Long): Int
        external fun turnOffStream(engineHandle: Long): Int

    }

    fun turnOffStream() {
        val status = ExternalGate.turnOffStream(getEngineHandle())
        if (status == 0) {
            isOpen = false
        } else {
            Log.e("Close stream failed", "status: $status")
        }
    }

    fun turnOnStream() {
        val engineHandle = getEngineHandle()
        val status = ExternalGate.turnOnStream(engineHandle);
        if (status == 0) {
            isOpen = true
            Log.i("Open stream success", "")
        } else {
            Log.e("Open stream failed", "status: $status")
        }
    }

    private var isOpen = false
}


class InputManager(): StreamManager() {
    private var engineHandle: Long = createEngine()

    companion object ExternalGate {
        init {
            System.loadLibrary("PitchDetectionEngine")
        }
        external fun createEngine(): Long
        external fun getPitches(engineHandle: Long): FloatArray

    }

    override fun getEngineHandle(): Long {
        return engineHandle
    }

    fun getPitches(): FloatArray {
        /** Pushes new pitches stored in c++ to pitchesDeque */
        // TODO: optimize getting pitches from c++
        // TODO: clear interface of passing obtained pitches (passing deque from user's component
        //  seems like cyclic dependency)
        return getPitches(engineHandle)
    }
}

class OutputManager(fileDescriptor: Int): StreamManager() {
    companion object ExternalGate {
        init {
            System.loadLibrary("PitchDetectionEngine")
        }
        external fun createEngine(fileDescriptor: Int): Long
    }
    private var engineHandle = createEngine(fileDescriptor)

    override fun getEngineHandle(): Long {
        return engineHandle
    }
}