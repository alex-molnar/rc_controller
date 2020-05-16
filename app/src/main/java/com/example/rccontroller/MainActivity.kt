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

        thread {
            connectButton.setOnClickListener(onConnectClicked)
        }
    }

    private val onConnectClicked = View.OnClickListener { view ->
        thread {
            var porty: Int
            try {
                porty = port.text.toString().toInt()
            } catch (e: NumberFormatException) {
                print(e.message)
                porty = 0
            }
            Channel.connect(
                "192.168.1.11",//host.text.toString(),
                8000 + porty, //port.text.toString().toInt(),
                "69420" //password.text.toString()
            ) { result ->
                if(result) {
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
        }
    }
}
