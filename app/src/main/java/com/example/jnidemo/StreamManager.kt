package com.example.jnidemo

import android.content.res.AssetManager
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


class InputManager(assetManager: AssetManager): StreamManager() {
    private var engineHandle: Long = createEngine(assetManager)

    companion object ExternalGate {
        init {
            System.loadLibrary("PitchDetectionEngine")
        }
        external fun createEngine(assetManager: AssetManager): Long
        external fun hasNextPitch(engineHandle: Long): Boolean
        external fun nextPitch(engineHandle: Long): Float

    }

    override fun getEngineHandle(): Long {
        return engineHandle
    }

    fun getPitches(pitchesDeque: ArrayDeque<Float>) {
        // TODO: if hasNextPitch call is too slow, replace with one call to getNumPitches
        while (hasNextPitch(engineHandle)) {
            pitchesDeque.addLast(nextPitch(engineHandle))
        }
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