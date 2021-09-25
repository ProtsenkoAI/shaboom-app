package com.example.jnidemo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.SimpleAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Path
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.InputStream


class SongChoosingActivity : AppCompatActivity()  {
    // TODO: add bundle to SongActivity intent
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

        // TODO: remove file choosing
//        val openSongLauncher = registerForActivityResult(
//            ActivityResultContracts.OpenDocument()
//        ) { uri: Uri? -> fileDescriptor = getFileDescriptorFromUri(uri!!) }
//
//        openSongLauncher.launch(arrayOf("*/*"))

        // TODO: get songs data from SongsDataManager
        val song1 = HashMap<String, Any>()
        song1["name"] = "Imagine Dragons - Radioactive"
        val song1Dir = getFilesDir().absolutePath + "/song1/"
        song1["data_path"] = song1Dir
        fetchSongData(0, song1Dir)

        val song2 = HashMap<String, Any>()
        song2["name"] = "Arctic Monkeys - Mad Sounds"
        val song2Dir = getFilesDir().absolutePath + "/song2/"
        song2["data_path"] = song2Dir
        fetchSongData(1, song2Dir)

        songsData!!.add(song1)
        songsData!!.add(song2)

        val songsDataKeys = arrayOf("name")
        val songsDataFieldTypes = intArrayOf(R.id.textView)

        songsAdapter = SimpleAdapter(this, songsData, R.layout.song_list_elem, songsDataKeys, songsDataFieldTypes)

        listView!!.adapter = songsAdapter
    }



    private fun fetchSongData(songId: Int, dataDir: String) {
        val dirAsFile = File(dataDir)
        if (!dirAsFile.exists()) {
            dirAsFile.mkdir();
        }

        val songSavePath = File(dataDir, "audio.wav")
        if (!songSavePath.exists()) {
            lifecycleScope.launch {
                try {
                    val reqResult = ShaBoomApi.retrofitService.getSongFile(songId)
                    println("request result! ${reqResult}")

                    val input = reqResult.body()!!.byteStream()
                    saveBytes(input, songSavePath)
//            _status.value = "Success: number of photos retrieved: ${listRes.size}"

                } catch (e: Exception) {
                    println("Failure: ${e.message}")
                }
            }
        } else {
            println("song audio.wav file already exists")
        }
    }

    fun saveBytes(input: InputStream, outFile: File) {
//        val outStream = FileOutputStream(outFile)
//        outStream.use { output ->
//            val buffer = ByteArray(4 * 1024) // or other buffer size
//            var read: Int
//            while (input.read(buffer).also { read = it } != -1) {
//                output.write(buffer, 0, read)
//            }
//            output.flush()
//        }
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

    private var songsAdapter: SimpleAdapter? = null
    private var listView: ListView? = null;
    private var songsData: ArrayList<HashMap<String,Any>>? = null;
}
