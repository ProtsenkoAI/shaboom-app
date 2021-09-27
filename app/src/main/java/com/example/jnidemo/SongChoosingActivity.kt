package com.example.jnidemo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ListView
import android.widget.SimpleAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.launch
import java.io.InputStream


class SongChoosingActivity : FragmentActivity()  {
    // TODO: add bundle to SongActivity intent
    // TODO: disable start of song activity when data is downloading

    // TODO: add "Stand by me" to music list
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_choosing)


        listView = findViewById<ListView>(R.id.songs_list)
        songsData = ArrayList<HashMap<String,Any>>()


        // copy model to files dir
        val modelPath = "crepe-medium.tflite"
        val inpModelStream = assets.open(modelPath)
        val outFile = File(getFilesDir(), modelPath)
        val outModelStream = FileOutputStream(outFile)
        inpModelStream.copyTo(outModelStream)

        lifecycleScope.launch {
            // first create all visual elements, then fetch data
            val songsDataRes = ShaBoomApi.retrofitService.getUserSongs()
            println("user songs $songsDataRes")

            for (song in songsDataRes) {
                val songAdapterData = HashMap<String, Any>()
                songAdapterData["name"] = song.name
                songAdapterData["songPerformer"] = song.performer
                val songDir = getFilesDir().absolutePath + "/song_${song.id}/"
                songAdapterData["data_path"] = songDir
                songsData!!.add(songAdapterData)
            }
            setAdapter()
            for (song in songsDataRes) {
                val songDir = getFilesDir().absolutePath + "/song_${song.id}/"
                fetchSongData(song.id, songDir)
            }
        }
    }

    private fun setAdapter() {
        val songsDataKeys = arrayOf("name", "songPerformer")
        val songsDataFieldTypes = intArrayOf(R.id.songName, R.id.songPerformer)
        songsAdapter = SimpleAdapter(this, songsData, R.layout.song_list_elem, songsDataKeys, songsDataFieldTypes)

        listView!!.adapter = songsAdapter
    }

    private fun fetchSongData(songId: Int, dataDir: String) {
        val dirAsFile = File(dataDir)
        if (!dirAsFile.exists()) {
            dirAsFile.mkdir();
        }

        val songSavePath = File(dataDir, "audio.wav")

        if (!songSavePath.exists() or forceDownload) {
            lifecycleScope.launch {
                try {
                    val reqResult = ShaBoomApi.retrofitService.getSongFile(songId)
                    println("request result! $reqResult")
                    val input = reqResult.body()!!.byteStream()
                    saveBytes(input, songSavePath)

                } catch (e: Exception) {
                    println("Failure: ${e.message}")
                }
            }
        } else {
            println("song audio.wav file already exists")
        }

        val pitchesSavePath = File(dataDir, "pitches.json")

        if (!pitchesSavePath.exists() or forceDownload) {
            lifecycleScope.launch {
                try {
                    val reqResult = ShaBoomApi.retrofitService.getSongPitches(songId)
                    println("request result! ${reqResult.body()}")
                    pitchesSavePath.writeText(reqResult.body().toString())
                } catch (e: Exception) {
                    println("Failure: ${e.message}")
                }
            }
        } else {
            println("song pitches.json file already exists")
        }
    }

    fun saveBytes(input: InputStream, outFile: File) {
        val fileReader = ByteArray(4096);
        val outStream = FileOutputStream(outFile)
        while (true) {
            val read = input.read(fileReader);
            if (read == -1) {
                break;
            }
            outStream.write(fileReader, 0, read);
        }
        outStream.flush()
        outStream.close()
    }

    fun songChosen(view: View) {
        val position = listView!!.getPositionForView(view)
        val songDataPath = songsData!![position].get("data_path") as String

        val songBundle = Bundle()
        songBundle.putString("song_data_path", songDataPath)

        val songIntent = Intent(this, SongActivity::class.java)
        songIntent.putExtras(songBundle)
        startActivity(songIntent)
    }

    private val forceDownload = false
    private var songsAdapter: SimpleAdapter? = null
    private var listView: ListView? = null
    private var songsData: ArrayList<HashMap<String,Any>>? = null
}
