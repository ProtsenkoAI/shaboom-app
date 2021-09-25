package com.example.jnidemo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.SimpleAdapter
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Path

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


        val song1 = HashMap<String, Any>()
        song1["name"] = "Imagine Dragons - Radioactive"
        song1["data_path"] = getFilesDir().absolutePath + "song1/"

        val song2 = HashMap<String, Any>()
        song2["name"] = "Arctic Monkeys - Mad Sounds"
        song2["data_path"] = getFilesDir().absolutePath + "song2/"

        songsData!!.add(song1)
        songsData!!.add(song2)
//
//        // By a for loop, entering different types of data in HashMap,
//        // and adding this map including it's datas into the ArrayList
//        // as list item and this list is the second parameter of the SimpleAdapter
//        for(i in fruitNames.indices){
//            val map=HashMap<String,Any>()
//
//            // Data entry in HashMap
//            map["fruitName"] = fruitNames[i]
//            map["fruitImage"] = fruitImageIds[i]
//
//            // adding the HashMap to the ArrayList
//            list.add(map)
//        }

        val songsDataKeys = arrayOf("name")
        val songsDataFieldTypes = intArrayOf(R.id.textView)

        songsAdapter = SimpleAdapter(this, songsData, R.layout.song_list_elem, songsDataKeys, songsDataFieldTypes)

        listView!!.adapter = songsAdapter

    }

    fun songChosen(view: View) {
        val position = listView!!.getPositionForView(view)
        println("position $position")
        val songDataPath = songsData!![position].get("data_path") as String
        println("data path ${songDataPath}")


        val songBundle = Bundle()
        songBundle.putString("song_data_path", songDataPath)

        val songIntent = Intent(this, SongActivity::class.java)
        songIntent.putExtras(songBundle)
        startActivity(songIntent)
    }


//    private fun getFileDescriptorFromUri(uri: Uri): Int {
//        val asset = this.contentResolver!!.openAssetFileDescriptor(uri, "r")!!
//        return asset.parcelFileDescriptor.detachFd()
//    }

//    private var fileDescriptor: Int? = null;
    private var songsAdapter: SimpleAdapter? = null
    private var listView: ListView? = null;
    private var songsData: ArrayList<HashMap<String,Any>>? = null;
}
