package com.example.rccontroller

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        debugIp.text = intent.getStringExtra("IP")
        debugPort.text = intent.getStringExtra("PORT")

        thread {
            connectButton.setOnClickListener(onConnectClicked)
        }
    }

    private val onConnectClicked = View.OnClickListener { view ->
        thread {
            val ip = intent.getStringExtra("IP")
            val port = intent.getStringExtra("PORT")
            if (ip != null && port != null) {
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
                    "The car is not reachable at the moment",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }
}
