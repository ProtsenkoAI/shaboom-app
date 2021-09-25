package com.example.jnidemo

import android.content.res.AssetManager
import android.content.res.Resources
import android.telecom.Call
import android.util.Log
import com.github.mikephil.charting.charts.LineChart
import java.util.*
import kotlin.collections.ArrayDeque

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

    open fun turnOffStream() {
        val status = ExternalGate.turnOffStream(getEngineHandle())
        isOpen = false
        if (status != 0) {
            Log.e("Close stream failed", "status: $status")
        }

    }

    open fun turnOnStream() {
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

class OutputManager(fileDescriptor: Int, private val songEndCallback: () -> Unit): StreamManager() {
    // TODO: use songEndCallback
    companion object ExternalGate {
        init {
            System.loadLibrary("PitchDetectionEngine")
        }
        external fun createEngine(fileDescriptor: Int): Long
        external fun getIsPlaying(engineHandle: Long): Boolean
    }
    private var engineHandle = createEngine(fileDescriptor)

    fun getIsPlaying(): Boolean {
        return ExternalGate.getIsPlaying(engineHandle)
    }
    override fun getEngineHandle(): Long {
        return engineHandle
    }

    override fun turnOnStream() {
        super.turnOnStream()
        callEndCallbackTask = CallSongEndCallbackTask(songEndCallback, ::getIsPlaying)
        // just call native 2 times a second and if outputstream is stopped, call callback
        // delay is so big because out stream should be started at first
        checkIsPlayingTimer.schedule(callEndCallbackTask, 10000, 500)
    }

    override fun turnOffStream() {
        super.turnOffStream()
        callEndCallbackTask!!.cancel()
        checkIsPlayingTimer.purge() // needed to schedule the task again
    }

    private var callEndCallbackTask: CallSongEndCallbackTask? = null
    private var checkIsPlayingTimer = Timer("Check is playing timer", true)
}


class CallSongEndCallbackTask(private val songEndCallback: () -> Unit,
                              private val getIsPlaying: () -> Boolean
) : TimerTask() {
    override fun run() {
        println("calling get is playing")
        if (!getIsPlaying()) {
            println("calling song end callback")
            songEndCallback()
        }
    }

}