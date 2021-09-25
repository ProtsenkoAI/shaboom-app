package com.example.jnidemo

import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import java.util.*

class SongActivity: AppCompatActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
         val passedDataBundle = intent.extras!!
        val songDataPath = passedDataBundle.getString("song_data_path")
        println("songDataPath inside SongActivity $songDataPath")
//        audioFileDescriptor = passedDataBundle!!.getInt("fileDescriptor")
//        targetPitches = passedDataBundle.getFloatArray("targetPitches")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song)

        chart = findViewById(R.id.chart)

        val startButton = getStartButton()
        val stopButton = findViewById<Button>(R.id.button_stop)
        replotTimer = Timer("Replot timer", true)

        startButton.setOnClickListener(this)
        stopButton.setOnClickListener(this)


        val requestMicroPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {isGranted: Boolean -> microPermissionCallback(isGranted)
        }

        requestMicroPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

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
        outputManager = OutputManager(audioFileDescriptor!!, ::songEndCallback )

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

    fun songEndCallback() {
        isStarted = false
        stop()
    }

    private var targetPitches: FloatArray? = null;
    private var audioFileDescriptor: Int? = null
    private var microPermGranted = false
    private var isStarted = false
    private var replotMs: Long = 100

    private var chart: LineChart? = null

    private var replotTask: ReplotTask? = null
    private var replotTimer = Timer("Replot timer", true)

    private var inputManager = InputManager()
    private var outputManager: OutputManager? = null


}