package com.eve.player

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        configureWindow()
    }

    private fun configureWindow() {
        window.setNavigationBarColor(getColor(R.color.eve_black))
        window.setStatusBarColor(getColor(R.color.eve_black))
    }
}
