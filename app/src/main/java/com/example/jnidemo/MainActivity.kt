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
    // TODO (not urgent): move replot things to sep. component
    // TODO: create intermediate object between plotting and MicroManager's data

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
        microPermGranted = isGranted
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
        } else { // otherwise it's stop button
            if (isStarted) {
                stop()
                isStarted = false
            }
        }
    }

    private fun start() {
        outputManager = OutputManager(fileDescriptor!!)

        outputManager!!.turnOnStream()
        if (microPermGranted) {
            replotTask = ReplotTask(inputManager, chart!!)
            inputManager.turnOnStream()
//            replotTask!!.run() // run to warm up before scheduling
            replotTimer.schedule(replotTask, 0, replotMs)
        }
    }

    private fun stop() {
        outputManager!!.turnOffStream()
        if (microPermGranted) {
            inputManager.turnOffStream()
            replotTask!!.cancel()
            replotTimer.purge() // needed to schedule the task again
        }
    }

    private var fileDescriptor: Int? = null
    private var isStarted = false
    private var microPermGranted = false
    private var replotMs: Long = 100

    private var chart: LineChart? = null

    private var inputManager = InputManager()
    private var outputManager: OutputManager? = null

    private var replotTask: ReplotTask? = null
    private var replotTimer = Timer("Replot timer", true)

}


class ReplotTask(private var inputManager: InputManager, private var chart: LineChart) : TimerTask() {
    // TODO: fill pitches with start value if needed for proper plotting
    // TODO: rename pitches1 to pitches
    private var pitches1 = ArrayDeque<Float>()
    private val nPoints = 200
    // TODO: ylims
    private var minY = -100.0f
    private var maxY = 100.0f

    override fun run() {
        val tsStart = System.currentTimeMillis()
        inputManager.getPitches(pitches1)

        while (pitches1.size > nPoints) {
            pitches1.removeFirst()
        }
        pitches1.addFirst(minY)
        pitches1.addFirst(maxY)

        val dataSet = makeDataSet(pitches1)
        dataSet.color = R.color.design_default_color_primary_dark
        dataSet.valueTextColor = R.color.design_default_color_secondary

        val lineData = LineData(dataSet)
        chart.data = lineData
        chart.invalidate()
        val tsEnd = System.currentTimeMillis()
        Log.i("time needed in ms", (tsEnd - tsStart).toString())
    }

    private fun makeDataSet(pitches: ArrayDeque<Float>): LineDataSet {
        // TODO: (maybe) reuse old entries
        val entries = mutableListOf<Entry>()
        for ((idx, value) in pitches.withIndex()) {
            entries.add(Entry(idx.toFloat(), value))
        }
        return LineDataSet(entries, "Label")
    }

}



