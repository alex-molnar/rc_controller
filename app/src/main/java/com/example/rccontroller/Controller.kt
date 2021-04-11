package com.example.rccontroller

import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_controller.*
import org.json.JSONObject
import kotlin.concurrent.thread


class Controller : AppCompatActivity() {

    private val downEvent = 0
    private val upEvent = 1

    private val states: JSONObject = JSONObject()

    private var isActive: Boolean = true
    private var userReturned: Boolean = true

    private val FORWARD = "forward"
    private val BACKWARD = "backward"
    private val LEFT = "turn_left"
    private val RIGHT = "turn_right"
    private val HORN = "horn"
    private val LIGHTS = "lights"
    private val HAZARD_WARNING = "hazard_warning"
    private val LEFT_INDICATOR = "left_indicator"
    private val RIGHT_INDICATOR = "right_indicator"
    private val REVERSE = "reverse"
    private val DISTANCE = "distance"
    private val SPEED = "speed"
    private val LINE = "line"

    private lateinit var idToMessageMap: HashMap<Int, String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_controller)

        thread {
            Channel.run()
        }

        thread {
            setEventListeners()
            listen()
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

    private fun setEventListeners() {
        forwardButton.setOnTouchListener(touchEvent)
        backwardButton.setOnTouchListener(touchEvent)
        leftButton.setOnTouchListener(touchEvent)
        rightButton.setOnTouchListener(touchEvent)
        hornButton.setOnTouchListener(touchEvent)
        lightSwitch.setOnTouchListener(toggleEvent)
        hazardWarning.setOnTouchListener(toggleEvent)
        leftIndicator.setOnTouchListener(toggleEvent)
        rightIndicator.setOnTouchListener(toggleEvent)
        reverseSwitch.setOnTouchListener(toggleEvent)
    }

    private fun listen() {
        while (isActive) {

            if (Channel.getBoolean(FORWARD) && !states.optBoolean(FORWARD)) {
                states.put(FORWARD, true)
                forwardButton.background = getDrawable(R.drawable.ic_keyboard_arrow_up_red_24dp)
            } else if (!Channel.getBoolean(FORWARD) && states.optBoolean(FORWARD)) {
                states.put(FORWARD, false)
                forwardButton.background = getDrawable(R.drawable.ic_keyboard_arrow_up_blue_24dp)
            }

            if (Channel.getBoolean(BACKWARD) && !states.optBoolean(BACKWARD)) {
                states.put(BACKWARD, true)
                backwardButton.background = getDrawable(R.drawable.ic_keyboard_arrow_down_red_24dp)
            } else if (!Channel.getBoolean(BACKWARD) && states.optBoolean(BACKWARD)) {
                states.put(BACKWARD, false)
                backwardButton.background = getDrawable(R.drawable.ic_keyboard_arrow_down_blue_24dp)
            }

            if (Channel.getBoolean(LEFT) && !states.optBoolean(LEFT)) {
                states.put(LEFT, true)
                leftButton.background = getDrawable(R.drawable.ic_keyboard_arrow_left_red_24dp)
            } else if (!Channel.getBoolean(LEFT) && states.optBoolean(LEFT)) {
                states.put(LEFT, false)
                leftButton.background = getDrawable(R.drawable.ic_keyboard_arrow_left_blue_24dp)
            }

            if (Channel.getBoolean(RIGHT) && !states.optBoolean(RIGHT)) {
                states.put(RIGHT, true)
                rightButton.background = getDrawable(R.drawable.ic_keyboard_arrow_right_red_24dp)
            } else if (!Channel.getBoolean(RIGHT) && states.optBoolean(RIGHT)) {
                states.put(RIGHT, false)
                rightButton.background = getDrawable(R.drawable.ic_keyboard_arrow_right_blue_24dp)
            }

            if (Channel.getBoolean(LIGHTS) && !states.optBoolean(LIGHTS)) {
                states.put(LIGHTS, true)
                lightSwitch.background = getDrawable(R.drawable.ic_highlight_on_24dp)
            } else if (!Channel.getBoolean(LIGHTS) && states.optBoolean(LIGHTS)) {
                states.put(LIGHTS, false)
                lightSwitch.background = getDrawable(R.drawable.ic_highlight_off_24dp)
            }

            if (Channel.getBoolean(HAZARD_WARNING) && !states.optBoolean(HAZARD_WARNING)) {
                states.put(HAZARD_WARNING, true)
                hazardWarning.background = getDrawable(R.drawable.ic_warning_on_24dp)
            } else if (!Channel.getBoolean(HAZARD_WARNING) && states.optBoolean(HAZARD_WARNING)) {
                states.put(HAZARD_WARNING, false)
                hazardWarning.background = getDrawable(R.drawable.ic_warning_off_24dp)
            }

            if (Channel.getBoolean(LEFT_INDICATOR) && !states.optBoolean(LEFT_INDICATOR)) {
                states.put(LEFT_INDICATOR, true)
                leftIndicator.background = getDrawable(R.drawable.indicator_on_24dp)
            } else if (!Channel.getBoolean(LEFT_INDICATOR) && states.optBoolean(LEFT_INDICATOR)) {
                states.put(LEFT_INDICATOR, false)
                leftIndicator.background = getDrawable(R.drawable.indicator_off_24dp)
            }

            if (Channel.getBoolean(RIGHT_INDICATOR) && !states.optBoolean(RIGHT_INDICATOR)) {
                states.put(RIGHT_INDICATOR, true)
                rightIndicator.background = getDrawable(R.drawable.indicator_on_24dp)
            } else if (!Channel.getBoolean(RIGHT_INDICATOR) && states.optBoolean(RIGHT_INDICATOR)) {
                states.put(RIGHT_INDICATOR, false)
                rightIndicator.background = getDrawable(R.drawable.indicator_off_24dp)
            }

            if (Channel.getBoolean(REVERSE) && !states.optBoolean(REVERSE)) {
                states.put(REVERSE, true)
                reverseSwitch.setTextColor(getColor(R.color.pure_red))
            } else if (!Channel.getBoolean(REVERSE) && states.optBoolean(REVERSE)) {
                states.put(REVERSE, false)
                reverseSwitch.setTextColor(getColor(R.color.lightOFF))
            }

            if (Channel.getBoolean(LINE) && !states.optBoolean(LINE)) {
                states.put(LINE, true)
                lineLabel.setImageDrawable(getDrawable(R.drawable.ic_more_vert_black_24dp))
            } else if (!Channel.getBoolean(LINE) && states.optBoolean(LINE)) {
                states.put(LINE, false)
                lineLabel.setImageDrawable(getDrawable(R.drawable.ic_more_vert_white_24dp))
            }

            val distance = Channel.getDouble(DISTANCE, 100.0)
            var text =
                "${getString(R.string.distance_text)} ${distance}${getString(R.string.distance_measure)}"
            distanceLabel.text = text
            if (distance < 10) {
                distanceLabel.setTextColor(getColor(R.color.pure_red))
            }
            else if (distance < 25) {
                distanceLabel.setTextColor(getColor(R.color.indicatorON))
            } else {
                distanceLabel.setTextColor(getColor(R.color.colorBackground))
            }

            val speed = Channel.getDouble(SPEED, 0.0)
            text = "${getString(R.string.speed_text)} ${speed}${getString(R.string.speed_measure)}"
            speedLabel.text = text
            if (speed > 30) {
                speedLabel.setTextColor(getColor(R.color.pure_red))
            }
            else if (speed > 20) {
                speedLabel.setTextColor(getColor(R.color.indicatorON))
            } else {
                speedLabel.setTextColor(getColor(R.color.colorBackground))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        menu?.let { optionsMenu ->
            idToMessageMap = hashMapOf(
                optionsMenu.getItem(0).itemId to "keep_contained",
                optionsMenu.getItem(1).itemId to "distance_keeping",
                optionsMenu.getItem(2).itemId to "change_direction",
                forwardButton.id to FORWARD,
                backwardButton.id to BACKWARD,
                leftButton.id to LEFT,
                rightButton.id to RIGHT,
                hornButton.id to HORN,
                lightSwitch.id to LIGHTS,
                reverseSwitch.id to REVERSE,
                hazardWarning.id to HAZARD_WARNING,
                leftIndicator.id to LEFT_INDICATOR,
                rightIndicator.id to RIGHT_INDICATOR,
                distanceLabel.id to DISTANCE,
                speedLabel.id to SPEED,
                lineLabel.id to LINE
            )
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.exitItem) {
            cleanupAndFinish()
        } else if (item.itemId == R.id.powerOffItem) {
            thread {
                Channel.sendTurnOffSignal()
            }
            Thread.sleep(500)
            cleanupAndFinish()
        } else {
            val message = idToMessageMap[item.itemId]
            thread {
                Channel.setMessage(message, !Channel.getBoolean(message))
            }
        }
        return true
    }

    private val touchEvent = View.OnTouchListener { view, event ->
        thread {
            if (event.action == downEvent && !states.optBoolean(resources.getResourceEntryName(view.id))) {
                Channel.setMessage(idToMessageMap[view.id], true)
                states.put(idToMessageMap[view.id], true)
            } else if (event.action == upEvent && states.optBoolean(idToMessageMap[view.id])) {
                Channel.setMessage(idToMessageMap[view.id], false)
                states.put(idToMessageMap[view.id], false)
            }
        }
        true
    }

    private val toggleEvent = View.OnTouchListener { view, event ->
        if (event.action == downEvent) {
            view as CompoundButton
            thread {
                Channel.setMessage(idToMessageMap[view.id], !view.isChecked)
            }
        }
        false
    }
}
