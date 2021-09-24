package com.example.jnidemo

import android.Manifest
import android.content.res.Resources
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
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.io.File
import java.io.FileOutputStream

import java.util.Timer
import java.util.TimerTask

import kotlin.checkNotNull
import kotlin.collections.ArrayDeque


class MainActivity : AppCompatActivity(), OnClickListener {
    // TODO (not urgent): move replot things to sep. component
    // TODO: create intermediate object between plotting and MicroManager's data
    // TODO: move working with ML model to stream managing

    override fun onCreate(savedInstanceState: Bundle?) {
//        val path = applicationInfo.dataDir
//        File(path).walkTopDown().forEach { println(it) }
        // copy model to files dir
        val modelPath = "crepe-medium.tflite"
        val inpModelStream = assets.open(modelPath)

        val outFile = File(getFilesDir(), modelPath)
        val outModelStream = FileOutputStream(outFile)

        inpModelStream.copyTo(outModelStream)

        // check if file exists
        println("created file exists and path")
        println(outFile.exists().toString() + " " + outFile.absolutePath)


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

    fun songEndCallback() {
        isStarted = false
        stop()
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
        outputManager = OutputManager(fileDescriptor!!, ::songEndCallback )

        outputManager!!.turnOnStream()
        if (microPermGranted) {
            replotTask = ReplotTask(inputManager, chart!!, resources)
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


class ReplotTask(private var inputManager: InputManager,
                 private var chart: LineChart,
                 private var resources: Resources
                ) : TimerTask() {
    // TODO: fill pitches with start value if needed for proper plotting
    private var pitches = ArrayDeque<Float>()
    private val nPoints = 200
    private val userPitchNPoints = 60
    private var minY = 80.0f
    private var maxY = 220.0f

    override fun run() {
        val tsStart = System.currentTimeMillis()
        inputManager.getPitches(pitches)

        while (pitches.size > userPitchNPoints) {
            pitches.removeFirst()
        }

        val dataSets = makeDataSetParts(pitches)

        val lineData = LineData(dataSets as List<ILineDataSet>?)
        chart.data = lineData

        // styling
        val xAxis = chart.xAxis
        val leftAxis = chart.axisLeft
        val rightAxis = chart.axisRight

        leftAxis.axisMinimum = minY
        leftAxis.axisMaximum = maxY

        chart.legend.isEnabled = false
        chart.description.isEnabled = false
        xAxis.isEnabled = false
        leftAxis.isEnabled = false
        rightAxis.isEnabled = false

        xAxis.axisMinimum = 0.0f
        xAxis.axisMaximum = nPoints.toFloat()

        chart.invalidate()
        val tsEnd = System.currentTimeMillis()
        Log.i("time needed in ms", (tsEnd - tsStart).toString())
    }

    private fun makeDataSetParts(pitches: ArrayDeque<Float>): ArrayList<ILineDataSet> {
        val datasets = ArrayList<ILineDataSet>()

        // TODO: (maybe) reuse old entries
        var entries = mutableListOf<Entry>()
        for ((idx, value) in pitches.withIndex()) {
            if (value != -1.0f) { // -1 is filling value when there's no detected pitch
                entries.add(Entry(idx.toFloat(), value))
            } else {
                if (entries.size > 0) {
                    datasets.add(createStyledDataSet(entries))
                    entries = mutableListOf<Entry>()
                }
            }
        }
        if (entries.size > 0) {
            datasets.add(createStyledDataSet(entries))
        }
        return datasets
    }

    private fun createStyledDataSet(entries: MutableList<Entry>): LineDataSet {
        val dataSet = LineDataSet(entries, "Label")
        dataSet.color = resources.getColor(R.color.design_default_color_primary)

        dataSet.fillAlpha = 0
        dataSet.lineWidth = 3.0f
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        return dataSet
    }
}
