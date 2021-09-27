package com.example.jnidemo

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.github.mikephil.charting.charts.LineChart
import java.io.File
import java.util.*

import com.google.gson.Gson


class SongActivity: FragmentActivity(), View.OnClickListener {
    // TODO: add "+" and "-" buttons
    // TODO: add metric fragment
    override fun onCreate(savedInstanceState: Bundle?) {
         val passedDataBundle = intent.extras!!
        val songAudioPath = passedDataBundle.getString("song_data_path") + "audio.wav"
        val songAudio = File(songAudioPath)
        val targetPitchesFile = File(passedDataBundle.getString("song_data_path") + "pitches.json")
        audioFileDescriptor = contentResolver.openFileDescriptor(Uri.fromFile(songAudio), "r")!!.detachFd()

        targetPitches = loadPitchesFromFile(targetPitchesFile)
        Log.i("target pitches", targetPitches.toString())

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song)

        chart = findViewById(R.id.chart)
        chart!!.setNoDataText("Hit the start button!")
        replotTask = ReplotTask(inputManager, targetPitches!!, chart!!, resources)
        replotTask!!.run()

        val startButton = getStartButton()
        val stopButton = findViewById<Button>(R.id.button_stop)

        startButton.setOnClickListener(this)
        stopButton.setOnClickListener(this)

        val requestMicroPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {isGranted: Boolean -> microPermissionCallback(isGranted)
        }

        requestMicroPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        outputManager = OutputManager(audioFileDescriptor!!, ::songEndCallback)
    }

    private fun loadPitchesFromFile(pitchFile: File): Array<Float> {
        val fileContent = pitchFile.readText()
        val gson = Gson()
        val pitches = gson.fromJson(fileContent, Array<Float>::class.java)
        return pitches
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
        println("called start()")
        // TODO: reset state of stream managers on calling turn on and turn off

        // TODO: to overcome latency, maybe should wait for some time and call native method like
        //  *start song* - output starts to copy from song file, input starts to save pitches

        if (microPermGranted) {
            replotTask = ReplotTask(inputManager, targetPitches!!, chart!!, resources)
            replotTask!!.run()
            replotTimer.schedule(replotTask, 0, replotMs)
            inputManager.turnOnStream()
        }
        outputManager!!.turnOnStream()

    }

    private fun stop() {
        println("called stop")
        if (microPermGranted) {
            replotTask!!.cancel()
            replotTask = null
            replotTimer.purge() // needed to schedule the task again
        }
        outputManager!!.turnOffStream()
        if (microPermGranted) {
            print("turn off stream etc")
            inputManager.turnOffStream()
        }
    }

    private fun songEndCallback() {
        stop()
        isStarted = false
    }

    private var targetPitches: Array<Float>? = null
    private var audioFileDescriptor: Int? = null
    private var microPermGranted = false
    private var isStarted = false
    private var replotMs: Long = 20

    private var chart: LineChart? = null

    private var replotTask: ReplotTask? = null
    private var replotTimer = Timer("Replot timer", true)

    private var inputManager = InputManager()
    private var outputManager: OutputManager? = null


}