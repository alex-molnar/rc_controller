package com.example.rccontroller

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_controller.*
import org.json.JSONObject
import kotlin.concurrent.thread


class Controller : AppCompatActivity() {

    data class ViewAttributes(
        val message: String,
        val drawableOn: Drawable,
        val drawableOff: Drawable
    ) {
        var textId: Int? = null
    }

    private val downEvent = 0
    private val upEvent = 1

    private val states: JSONObject = JSONObject()

    private var isActive: Boolean = true
    private var userReturned: Boolean = true

    private lateinit var idToAttributes: HashMap<Int, ViewAttributes>
    private lateinit var views: ArrayList<View>

    private fun Int.draw(): Drawable {
        return checkNotNull(getDrawable(this))
    }

    private fun View.update() {
        val lambdaOn: () -> Unit = {
            when (this.id) {
                R.id.reverseSwitch -> (this as ToggleButton).setTextColor(getColor(R.color.pure_red))
                R.id.lineLabel -> (this as ImageView).setImageDrawable(getDrawable(R.drawable.ic_more_vert_black_24dp))
                else -> this.background = idToAttributes[this.id]?.drawableOn
            }
        }
        val lambdaOff: () -> Unit = {
            when (this.id) {
                R.id.reverseSwitch -> (this as ToggleButton).setTextColor(getColor(R.color.lightOFF))
                R.id.lineLabel -> (this as ImageView).setImageDrawable(getDrawable(R.drawable.ic_more_vert_white_24dp))
                else -> this.background = idToAttributes[this.id]?.drawableOff
            }
        }

        val message = checkNotNull(idToAttributes[this.id]?.message)
        if (Channel.getBoolean(message) && !states.optBoolean(message)) {
            states.put(message, true)
            lambdaOn()
        } else if (!Channel.getBoolean(message) && states.optBoolean(message)) {
            states.put(message, false)
            lambdaOff()
        }
    }

    private fun TextView.update() {
        val value = Channel.getDouble(checkNotNull(idToAttributes[this.id]?.message), 0.0)
        this.text = getString(checkNotNull(idToAttributes[this.id]?.textId), value)
        this.setTextColor(
            getColor(
                when {
                    this.id == R.id.distanceLabel && value < 10 || this.id == R.id.speedLabel && value > 30 -> {
                        R.color.pure_red
                    }
                    this.id == R.id.distanceLabel && value < 25 || this.id == R.id.speedLabel && value > 20 -> {
                        R.color.indicatorON
                    }
                    else -> R.color.colorBackground
                }
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_controller)

        views = arrayListOf(
            forwardButton, backwardButton, leftButton, rightButton, reverseSwitch, hornButton,
            lightSwitch, leftIndicator, rightIndicator, hazardWarning,
            distanceLabel, lineLabel, speedLabel
        )

        thread {
            Channel.run()
        }

        thread {
            views.forEach { view ->
                view.setOnTouchListener(
                    when (view) {
                        forwardButton, backwardButton, leftButton, rightButton, hornButton -> touchEvent
                        else -> toggleEvent
                    }
                )
            }

            Thread.sleep(1000)

            while (isActive) {
                views.forEach { view ->
                    when (view) {
                        distanceLabel, speedLabel -> (view as TextView).update()
                        else -> view.update()
                    }
                }
            }
        }
    }

    override fun onStop() {
        userReturned = false
        Handler().postDelayed({
            if (!userReturned) {
                cleanupAndFinish()
            }
        }, 30000)
        super.onStop()
    }

    override fun onRestart() {
        userReturned = true
        super.onRestart()
    }

    private fun cleanupAndFinish() {
        isActive = false
        Channel.stop()
        Thread.sleep(500)
        finishAffinity()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        menu?.let { optionsMenu ->
            idToAttributes = hashMapOf(
                forwardButton.id to ViewAttributes(
                    "forward",
                    R.drawable.ic_keyboard_arrow_up_red_24dp.draw(),
                    R.drawable.ic_keyboard_arrow_up_blue_24dp.draw()
                ),
                backwardButton.id to ViewAttributes(
                    "backward",
                    R.drawable.ic_keyboard_arrow_down_red_24dp.draw(),
                    R.drawable.ic_keyboard_arrow_down_blue_24dp.draw()
                ),
                leftButton.id to ViewAttributes(
                    "turn_left",
                    R.drawable.ic_keyboard_arrow_left_red_24dp.draw(),
                    R.drawable.ic_keyboard_arrow_left_blue_24dp.draw()
                ),
                rightButton.id to ViewAttributes(
                    "turn_right",
                    R.drawable.ic_keyboard_arrow_right_red_24dp.draw(),
                    R.drawable.ic_keyboard_arrow_right_blue_24dp.draw()
                ),
                hornButton.id to ViewAttributes(
                    "horn",
                    R.drawable.ic_horn_svgrepo_com.draw(),
                    R.drawable.ic_horn_svgrepo_com.draw()
                ),
                lightSwitch.id to ViewAttributes(
                    "lights",
                    R.drawable.ic_highlight_on_24dp.draw(),
                    R.drawable.ic_highlight_off_24dp.draw()
                ),
                reverseSwitch.id to ViewAttributes(
                    "reverse",
                    R.drawable.ic_hotroad.draw(),
                    R.drawable.ic_hotroad.draw()
                ),
                hazardWarning.id to ViewAttributes(
                    "hazard_warning",
                    R.drawable.ic_warning_on_24dp.draw(),
                    R.drawable.ic_warning_off_24dp.draw()
                ),
                leftIndicator.id to ViewAttributes(
                    "left_indicator",
                    R.drawable.indicator_on_24dp.draw(),
                    R.drawable.indicator_off_24dp.draw()
                ),
                rightIndicator.id to ViewAttributes(
                    "right_indicator",
                    R.drawable.indicator_on_24dp.draw(),
                    R.drawable.indicator_off_24dp.draw()
                ),
                distanceLabel.id to ViewAttributes(
                    "distance",
                    R.drawable.ic_hotroad.draw(),
                    R.drawable.ic_hotroad.draw()
                ),
                speedLabel.id to ViewAttributes(
                    "speed",
                    R.drawable.ic_hotroad.draw(),
                    R.drawable.ic_hotroad.draw()
                ),
                lineLabel.id to ViewAttributes(
                    "line",
                    R.drawable.ic_more_vert_black_24dp.draw(),
                    R.drawable.ic_more_vert_black_24dp.draw()
                ),
                optionsMenu.getItem(0).itemId to ViewAttributes(
                    "keep_contained",
                    R.drawable.ic_hotroad.draw(),
                    R.drawable.ic_hotroad.draw()
                ),
                optionsMenu.getItem(1).itemId to ViewAttributes(
                    "distance_keeping",
                    R.drawable.ic_hotroad.draw(),
                    R.drawable.ic_hotroad.draw()
                )
            )
            idToAttributes[speedLabel.id]?.textId = R.string.speed_text
            idToAttributes[distanceLabel.id]?.textId = R.string.distance_text
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.exitItem -> {
                cleanupAndFinish()
            }
            R.id.powerOffItem -> {
                thread {
                    Channel.sendTurnOffSignal()
                }
                Thread.sleep(500)
                cleanupAndFinish()
            }
            R.id.changePasswordItem -> {
                val intentChangePassword = Intent(this, PasswordActivity::class.java)
                startActivity(intentChangePassword)
            }
            else -> {
                val message = checkNotNull(idToAttributes[item.itemId]?.message)
                thread {
                    Channel.setMessage(message, !Channel.getBoolean(message))
                }
            }
        }
        return true
    }

    private val touchEvent = View.OnTouchListener { view, event ->
        thread {
            val message = checkNotNull(idToAttributes[view.id]?.message)
            if (event.action == downEvent && !states.optBoolean(resources.getResourceEntryName(view.id))) {
                Channel.setMessage(message, true)
                states.put(message, true)
            } else if (event.action == upEvent && states.optBoolean(message)) {
                Channel.setMessage(message, false)
                states.put(message, false)
            }
        }
        true
    }

    private val toggleEvent = View.OnTouchListener { view, event ->
        if (event.action == downEvent) {
            view as CompoundButton
            thread {
                Channel.setMessage(checkNotNull(idToAttributes[view.id]?.message), !view.isChecked)
            }
        }
        false
    }
}
