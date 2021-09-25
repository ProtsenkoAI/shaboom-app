package com.example.jnidemo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    // TODO (not urgent): move replot things to sep. component
    // TODO: create intermediate object between plotting and MicroManager's data
    // TODO: move working with ML model to stream managing

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val chooseSongIntent = Intent(this, SongChoosingActivity::class.java)
        startActivity(chooseSongIntent)
    }
}
