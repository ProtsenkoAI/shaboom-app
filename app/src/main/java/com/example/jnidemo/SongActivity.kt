package com.example.jnidemo

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.github.mikephil.charting.charts.LineChart
import java.io.File
import java.util.*

import com.google.gson.Gson


class SongActivity: FragmentActivity(), View.OnClickListener {
    // TODO: add metric fragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song)

         val passedDataBundle = intent.extras!!
        val songAudioPath = passedDataBundle.getString("song_data_path") + "audio.wav"
        val songAudio = File(songAudioPath)
        val targetPitchesFile = File(passedDataBundle.getString("song_data_path") + "pitches.json")
        audioFileDescriptor = contentResolver.openFileDescriptor(Uri.fromFile(songAudio), "r")!!.detachFd()

        targetPitches = loadPitchesFromFile(targetPitchesFile)
        Log.i("target pitches", targetPitches.toString())


        startButton = findViewById<Button>(R.id.button_start)
        stopButton = findViewById<Button>(R.id.button_stop)
        plusToneButton = findViewById<ImageButton>(R.id.plus_tonality_button)
        minusToneButton = findViewById<ImageButton>(R.id.minus_tonality_button)
        println("start button ${startButton.toString()}")
        performanceBar = findViewById<ProgressBar>(R.id.user_performance_bar)


        chart = findViewById(R.id.chart)
        chart!!.setNoDataText("Hit the start button!")
        replotTask = ReplotTask(inputManager, targetPitches!!, chart!!, performanceBar!!, resources, toneShift)
        replotTask!!.run()

        startButton!!.setOnClickListener(this)
        stopButton!!.setOnClickListener(this)

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

    override fun onClick(button: View?) {
        // flag isStarted is used for cases when user taps same button multiple times
        // TODO: separate onClick to coded classes of buttons to remove if-else's choosing button
        if (button == startButton) {
            if (! isStarted) {
                start()
                isStarted = true
            }
        } else if (button == stopButton) { // otherwise it's stop button
            if (isStarted) {
                stop()
                isStarted = false
            }
        } else if (button == plusToneButton) {
            toneShift += toneStep
            replotTask?. run {
                replotTask!!.setToneShift(toneShift)
            }
        } else if (button == minusToneButton) {
            toneShift -= toneStep
            replotTask?. run {
                replotTask!!.setToneShift(toneShift)
            }
        } else {
            // TODO: add appropriate exception
            throw RuntimeException("unknown button!")
        }
    }


    private fun start() {
        println("called start()")
        // TODO: reset state of stream managers on calling turn on and turn off

        // TODO: to overcome latency, maybe should wait for some time and call native method like
        //  *start song* - output starts to copy from song file, input starts to save pitches

        if (microPermGranted) {
            replotTask = ReplotTask(inputManager, targetPitches!!, chart!!, performanceBar!!, resources, toneShift)
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
    private var replotMs: Long = 50
    private var toneShift = 0
    private val toneStep = 3


    private var startButton: Button? = null
    private var stopButton: Button? = null
    private var plusToneButton: ImageButton? = null
    private var minusToneButton: ImageButton? = null
    private var performanceBar: ProgressBar? = null

    private var chart: LineChart? = null

    private var replotTask: ReplotTask? = null
    private var replotTimer = Timer("Replot timer", true)

    private var inputManager = InputManager()
    private var outputManager: OutputManager? = null


}