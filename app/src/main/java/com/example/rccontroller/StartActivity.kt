package com.example.rccontroller

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.skyfishjy.library.RippleBackground
import java.util.*
import kotlin.concurrent.schedule

class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        findViewById<RippleBackground>(R.id.content).startRippleAnimation()

        val intentController = Intent(this, MainActivity::class.java)

        Timer("SettingUp", false).schedule(2500) {
            startActivity(intentController)
        }
    }
}
