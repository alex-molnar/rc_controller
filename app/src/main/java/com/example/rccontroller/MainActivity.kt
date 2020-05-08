package com.example.rccontroller

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        resetTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        thread {
            setTextColors()
            connectButton.setOnClickListener(onConnectClicked)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val menuItem = menu?.findItem(R.id.action_theme_switch)
        if (menuItem != null) {
            if (AppSettings.darkTheme) {
                menuItem.icon = getDrawable(R.drawable.ic_brightness_7_24dp)
            }
            else {
                menuItem.icon = getDrawable(R.drawable.ic_brightness_2_grey_24dp)
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_theme_switch -> {
                AppSettings.darkTheme = !AppSettings.darkTheme
                recreate()
                invalidateOptionsMenu()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val onConnectClicked = View.OnClickListener { view ->
        thread {
            Channel.connect(
                "192.168.1.11",//host.text.toString(),
                8000 + port.text.toString().toInt(),
                "69420" //password.text.toString()
            ) { result ->
                if(result) {
                    val intentController = Intent(this, Controller::class.java)
                    startActivity(intentController)
                }
                else {
                    Snackbar.make(
                        view,
                        Channel.errorMessage.toString(),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun resetTheme() {
        if (AppSettings.darkTheme) {
            setTheme(R.style.AppThemeDark)
        }
        else {
            setTheme(R.style.AppTheme)
        }
    }

    private fun setTextColors() {
        if (AppSettings.darkTheme) {
            host.setTextColor(getColor(R.color.colorBackground))
            port.setTextColor(getColor(R.color.colorBackground))
            password.setTextColor(getColor(R.color.colorBackground))
            connectButton.setTextColor(getColor((R.color.colorBackground)))
        }
    }
}
