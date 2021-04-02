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

    override fun onDestroy() {
        isActive = false
        Channel.stop()
        super.onDestroy()
    }

    override fun onStop() {
        userReturned = false
        Handler().postDelayed({
            if (!userReturned) {
                finishAffinity()
            }
        }, 30000)
        super.onStop()
    }

    override fun onRestart() {
        userReturned = true
        super.onRestart()
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
            val forwardString = hashes(resources.getResourceEntryName(forwardButton.id))
            val backwardString = hashes(resources.getResourceEntryName(backwardButton.id))
            val leftString = hashes(resources.getResourceEntryName(leftButton.id))
            val rightString = hashes(resources.getResourceEntryName(rightButton.id))
            val lightString = hashes(resources.getResourceEntryName(lightSwitch.id))
            val hazardString = hashes(resources.getResourceEntryName(hazardWarning.id))
            val leftIndicatorString = hashes(resources.getResourceEntryName(leftIndicator.id))
            val rightIndicatorString = hashes(resources.getResourceEntryName(rightIndicator.id))
            val reverseString = hashes(resources.getResourceEntryName(reverseSwitch.id))
            val distanceString = hashes(resources.getResourceEntryName(distanceLabel.id))
            val speedString = hashes(resources.getResourceEntryName(speedLabel.id))
            val lineString = hashes(resources.getResourceEntryName(lineLabel.id))

            if (Channel.getBoolean(forwardString) && !states.optBoolean(forwardString)) {
                states.put(forwardString, true)
                forwardButton.background = getDrawable(R.drawable.ic_keyboard_arrow_up_red_24dp)
            }
            else if(!Channel.getBoolean(forwardString) && states.optBoolean(forwardString)) {
                states.put(forwardString, false)
                forwardButton.background = getDrawable(R.drawable.ic_keyboard_arrow_up_blue_24dp)
            }

            if (Channel.getBoolean(backwardString) && !states.optBoolean(backwardString)) {
                states.put(backwardString, true)
                backwardButton.background = getDrawable(R.drawable.ic_keyboard_arrow_down_red_24dp)
            }
            else if(!Channel.getBoolean(backwardString) && states.optBoolean(backwardString)) {
                states.put(backwardString, false)
                backwardButton.background = getDrawable(R.drawable.ic_keyboard_arrow_down_blue_24dp)
            }

            if (Channel.getBoolean(leftString) && !states.optBoolean(leftString)) {
                states.put(leftString, true)
                leftButton.background = getDrawable(R.drawable.ic_keyboard_arrow_left_red_24dp)
            }
            else if(!Channel.getBoolean(leftString) && states.optBoolean(leftString)) {
                states.put(leftString, false)
                leftButton.background = getDrawable(R.drawable.ic_keyboard_arrow_left_blue_24dp)
            }

            if (Channel.getBoolean(rightString) && !states.optBoolean(rightString)) {
                states.put(rightString, true)
                rightButton.background = getDrawable(R.drawable.ic_keyboard_arrow_right_red_24dp)
            }
            else if(!Channel.getBoolean(rightString) && states.optBoolean(rightString)) {
                states.put(rightString, false)
                rightButton.background = getDrawable(R.drawable.ic_keyboard_arrow_right_blue_24dp)
            }

            if (Channel.getBoolean(lightString) && !states.optBoolean(lightString)) {
                states.put(lightString, true)
                lightSwitch.background = getDrawable(R.drawable.ic_highlight_on_24dp)
            }
            else if(!Channel.getBoolean(lightString) && states.optBoolean(lightString)) {
                states.put(lightString, false)
                lightSwitch.background = getDrawable(R.drawable.ic_highlight_off_24dp)
            }

            if (Channel.getBoolean(hazardString) && !states.optBoolean(hazardString)) {
                states.put(hazardString, true)
                hazardWarning.background = getDrawable(R.drawable.ic_warning_on_24dp)
            }
            else if(!Channel.getBoolean(hazardString) && states.optBoolean(hazardString)) {
                states.put(hazardString, false)
                hazardWarning.background = getDrawable(R.drawable.ic_warning_off_24dp)
            }

            if (Channel.getBoolean(leftIndicatorString) && !states.optBoolean(leftIndicatorString)) {
                states.put(leftIndicatorString, true)
                leftIndicator.background = getDrawable(R.drawable.indicator_on_24dp)
            }
            else if(!Channel.getBoolean(leftIndicatorString) && states.optBoolean(leftIndicatorString)) {
                states.put(leftIndicatorString, false)
                leftIndicator.background = getDrawable(R.drawable.indicator_off_24dp)
            }

            if (Channel.getBoolean(rightIndicatorString) && !states.optBoolean(rightIndicatorString)) {
                states.put(rightIndicatorString, true)
                rightIndicator.background = getDrawable(R.drawable.indicator_on_24dp)
            }
            else if(!Channel.getBoolean(rightIndicatorString) && states.optBoolean(rightIndicatorString)) {
                states.put(rightIndicatorString, false)
                rightIndicator.background = getDrawable(R.drawable.indicator_off_24dp)
            }

            if (Channel.getBoolean(reverseString) && !states.optBoolean(reverseString)) {
                states.put(reverseString, true)
                reverseSwitch.setTextColor(getColor(R.color.pure_red))
            }
            else if(!Channel.getBoolean(reverseString) && states.optBoolean(reverseString)) {
                states.put(reverseString, false)
                reverseSwitch.setTextColor(getColor(R.color.lightOFF))
            }

            if (Channel.getBoolean(lineString) && !states.optBoolean(lineString)) {
                states.put(lineString, true)
                lineLabel.setImageDrawable(getDrawable(R.drawable.ic_more_vert_black_24dp))
            }
            else if(!Channel.getBoolean(lineString) && states.optBoolean(lineString)) {
                states.put(lineString, false)
                lineLabel.setImageDrawable(getDrawable(R.drawable.ic_more_vert_white_24dp))
            }

            val distance = Channel.getDouble(distanceString, 100.0)
            var text = "${getString(R.string.distance_text)} ${distance}${getString(R.string.distance_measure)}"
            distanceLabel.text = text
            if (distance < 10) {
                distanceLabel.setTextColor(getColor(R.color.pure_red))
            }
            else if (distance < 25) {
                distanceLabel.setTextColor(getColor(R.color.indicatorON))
            } else {
                distanceLabel.setTextColor(getColor(R.color.colorBackground))
            }

            val speed = Channel.getDouble(speedString, 0.0)
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
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.exitItem) {
            finishAffinity()
        } else {
            val id = hashes(resources.getResourceEntryName(item.itemId))
            thread {
                Channel.setMessage(id, !Channel.getBoolean(id))
            }
        }
        return true
    }

    private val touchEvent = View.OnTouchListener { view, event ->
        thread {
            if (event.action == downEvent && !states.optBoolean(resources.getResourceEntryName(view.id))) {
                Channel.setMessage(hashes(resources.getResourceEntryName(view.id)), true)
                states.put(resources.getResourceEntryName(view.id), true)
            } else if (event.action == upEvent && states.optBoolean(
                    resources.getResourceEntryName(
                        view.id
                    )
                )
            ) {
                Channel.setMessage(hashes(resources.getResourceEntryName(view.id)), false)
                states.put(resources.getResourceEntryName(view.id), false)
            }
        }
        true
    }

    private val toggleEvent = View.OnTouchListener { view, event ->
        if (event.action == downEvent) {
            view as CompoundButton
            thread {
                Channel.setMessage(hashes(resources.getResourceEntryName(view.id)), !view.isChecked)
            }
        }
        false
    }

    private fun hashes(key: String): String {
        return when(key) {
            "forwardButton" -> "forward"
            "backwardButton" -> "backward"
            "leftButton" -> "turn_left"
            "rightButton" -> "turn_right"
            "hornButton" -> "horn"
            "lightSwitch" -> "lights"
            "reverseSwitch" -> "reverse"
            "hazardWarning" -> "hazard_warning"
            "leftIndicator" -> "left_indicator"
            "rightIndicator" -> "right_indicator"
            "distanceLabel" -> "distance"
            "speedLabel" -> "speed"
            "lineLabel" -> "line"
            "distanceKeepingItem" -> "distance_keeping"
            "keepContainedItem" -> "keep_contained"
            "changeDirectionItem" -> "change_direction"
            else -> ""
        }
    }
}
