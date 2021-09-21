package com.example.jnidemo

import android.Manifest
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

import java.util.Timer
import java.util.TimerTask

import kotlin.checkNotNull
import kotlin.collections.ArrayDeque


class MainActivity : AppCompatActivity(), OnClickListener {
    // TODO: move replot things to sep. component
    // TODO: create intermediate object between plotting and MicroManager's data

    // TODO: fileDescriptor is not used by cpp now, rewrite file choosing

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val openSongLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()

        ) { uri: Uri? -> fileDescriptor = getFileDescriptorFromUri(uri!!) }

        openSongLauncher.launch(arrayOf("*/*"))

        val requestMicroPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {isGranted: Boolean -> microPermissionCallback(isGranted)
        }

        requestMicroPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

        chart = findViewById(R.id.chart)

        val startButton = getStartButton()
        val stopButton = findViewById<Button>(R.id.button_stop)
        replotTimer = Timer("Replot timer", true)

        startButton.setOnClickListener(this)
        stopButton.setOnClickListener(this)
    }

    private fun getFileDescriptorFromUri(uri: Uri): Int {
        val asset = this.contentResolver!!.openAssetFileDescriptor(uri, "r")!!
        return asset.parcelFileDescriptor.detachFd()
    }

    private fun microPermissionCallback(isGranted: Boolean) {

    }

    private fun getStartButton(): Button {
        val button = findViewById<Button>(R.id.button_start)
        checkNotNull(button)
        return button
    }

    override fun onClick(button: View?) {
        // flag isStarted is used for cases when user taps same button multiple times
        val startButton = getStartButton()
        if (button == startButton) {
            if (! isStarted) {
                start()
                isStarted = true
            }
        } else {
            if (isStarted) {
                stop()
                isStarted = false
            }
        }
    }

    private fun start() {
        outputManager = OutputManager(fileDescriptor!!)

        outputManager!!.turnOnStream()
        inputManager.turnOnStream()
        replotTask = ReplotTask(inputManager, chart!!)
        replotTimer.schedule(replotTask, 0, 50)
    }

    private fun stop() {
        outputManager!!.turnOffStream()
        inputManager.turnOffStream()
        replotTask!!.cancel()
        replotTimer.purge() // needed to schedule the task again
    }

    private var fileDescriptor: Int? = null
    private var isStarted = false

    private var chart: LineChart? = null

    private var inputManager = InputManager()
    private var outputManager: OutputManager? = null

    private var replotTask: ReplotTask? = null

    private var replotTimer = Timer("Replot timer", true)
}


class InputManager() {
    companion object ExternalGate {
        init {
            System.loadLibrary("PitchDetectionEngine")
        }
//        fun getStream() {
//
//        }
        external fun createEngine(): Long
        external fun turnOnStream(engineHandle: Long): Int
        external fun turnOffStream(engineHandle: Long): Int
        external fun hasNewPitches(engineHandle: Long): Boolean
        external fun nextPitch(engineHandle: Long): Float

    }

    fun turnOnStream() {
        if (engineHandle === null) {
            engineHandle = createEngine()
            Log.i("engine handle: ", engineHandle.toString())
        }
        val status = ExternalGate.turnOnStream(engineHandle!!);
        Log.i("status: ", status.toString());
    }

    fun turnOffStream() {
        ExternalGate.turnOffStream(engineHandle!!)
    }

    fun getPitches(pitchesDeque: ArrayDeque<Float>) {
        /** Pushes new pitches stored in c++ to pitchesDeque */
        // TODO: optimize getting pitches from c++
        // TODO: clear interface of passing obtained pitches (passing deque from user's component
        //  seems as cyclic dependency)
        while (hasNewPitches(engineHandle!!)) {
            pitchesDeque.addLast(nextPitch(engineHandle!!))
        }
    }

    private var engineHandle: Long? = null
}


class OutputManager(private var fileDescriptor: Int) {
    // TODO: fix duplication with InputManager
    companion object ExternalGate {
        init {
            System.loadLibrary("PitchDetectionEngine")
        }

        external fun createEngine(fileDescriptor: Int): Long
        external fun turnOnStream(engineHandle: Long): Int
        external fun turnOffStream(engineHandle: Long): Int

    }

    fun turnOnStream() {
        if (engineHandle === null) {
            Log.i("file descriptor: ", fileDescriptor.toString())
            engineHandle = createEngine(fileDescriptor)
            Log.i("engine status", "engine created!")
            Log.i("engine handle: ", engineHandle.toString())
        }
        val status = ExternalGate.turnOnStream(engineHandle!!);
        Log.i("status: ", status.toString());
    }

    fun turnOffStream() {
        ExternalGate.turnOffStream(engineHandle!!)
    }

    private var engineHandle: Long? = null
}

class ReplotTask(private var inputManager: InputManager, private var chart: LineChart) : TimerTask() {
    // TODO: fill pitches with start value if needed for proper plotting
    // TODO: y-axis limits
    private var pitches = ArrayDeque<Float>()
    private var nPoints = 200
    private var minY = -500.0f
    private var maxY = 500.0f

    override fun run() {
        inputManager.getPitches(pitches)

        while (pitches.size > nPoints) {
            pitches.removeFirst()
        }
        pitches.addFirst(minY);
        pitches.addFirst(maxY);

        val dataSet = makeDataSet(pitches)
        dataSet.color = R.color.design_default_color_primary_dark
        dataSet.valueTextColor = R.color.design_default_color_secondary

        val lineData = LineData(dataSet)
        chart.data = lineData
        chart.invalidate()
    }

    private fun makeDataSet(pitches: ArrayDeque<Float>): LineDataSet {
//        val entries = Array<Entry>(pitches.size, )
//        val entries = listOf<Entry>(Entry(1.0f, 1.0f), Entry(2.0f, 2.0f), Entry(3.0f, 3.0f))
        val entries = mutableListOf<Entry>()
        for ((idx, value) in pitches.withIndex()) {
            entries.add(Entry(idx.toFloat(), value))
        }

        return LineDataSet(entries, "Label")
    }

}



