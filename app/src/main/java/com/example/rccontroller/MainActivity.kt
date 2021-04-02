package com.example.rccontroller

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import io.github.rybalkinsd.kohttp.ext.asString
import io.github.rybalkinsd.kohttp.ext.httpGet
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import org.json.JSONTokener
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    fun Int.asBoolean() = this == 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        thread {
            connectButton.setOnClickListener(onConnectClicked)
        }
    }

    private val onConnectClicked = View.OnClickListener { view ->
        thread {
            // TODO: proper error handling
            val response: JSONObject = (
                    JSONTokener(
                        "https://kingbrady.web.elte.hu/rc_car/get_ip.php".httpGet().asString()
                    ).nextValue() as JSONObject
                    )

            val ip = response.getString("ip")
            val port = response.getString("port")
            val avail = response.getInt("available").asBoolean()

            if (avail) {
                Channel.connect(
                    ip,
                    port.toInt(),
                    "69420" //password.text.toString()
                ) { result ->
                    println("callback called")
                    println(result)
                    if (result) {
                        val intentController = Intent(this, Controller::class.java)
                        startActivity(intentController)
                    } else {
                        Snackbar.make(
                            view,
                            Channel.errorMessage.toString(),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Snackbar.make(
                    view,
                    "The car is not available at the moment",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }
}
