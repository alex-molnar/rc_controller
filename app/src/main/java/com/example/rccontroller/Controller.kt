package com.example.rccontroller

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_controller.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.concurrent.thread

class Controller : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        resetTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_controller)

        thread {
            setTextColors()
            Channel.tmpSend()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_theme_switch -> {
                AppSettings.darkTheme = !AppSettings.darkTheme
                recreate()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun resetTheme(): Unit {
        if (AppSettings.darkTheme) {
            setTheme(R.style.AppThemeDark)
        }
        else {
            setTheme(R.style.AppTheme)
        }
    }

    private fun setTextColors(): Unit {
        if (AppSettings.darkTheme) {
            distanceLabel.setTextColor(resources.getColor(R.color.colorBackground))
            speedLabel.setTextColor(resources.getColor(R.color.colorBackground))
        }
    }
}
