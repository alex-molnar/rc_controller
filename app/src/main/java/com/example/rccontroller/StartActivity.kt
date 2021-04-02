package com.example.rccontroller

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.skyfishjy.library.RippleBackground
import io.github.rybalkinsd.kohttp.ext.asString
import io.github.rybalkinsd.kohttp.ext.httpGet
import org.json.JSONObject
import org.json.JSONTokener
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.thread

class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        findViewById<RippleBackground>(R.id.content).startRippleAnimation()

        thread {
            // TODO: lot of error handling
            val response: JSONObject = (
                    JSONTokener(
                        "https://kingbrady.web.elte.hu/rc_car/get_ip.php".httpGet().asString()
                    ).nextValue() as JSONObject
                    )

            val intentController = Intent(this, MainActivity::class.java)

            Timer("SettingUp", false).schedule(2500) {
                intentController.putExtra("IP", response.getString("ip"))
                intentController.putExtra("PORT", response.getString("port"))
                startActivity(intentController)
            }

        }
    }
}
