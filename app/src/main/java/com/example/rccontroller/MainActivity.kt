package com.example.rccontroller

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {
    private fun Int.asBoolean() = this == 1  // TODO:

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        thread {
            connectButton.setOnClickListener(onConnectClicked)
        }
    }

    private val onConnectClicked = View.OnClickListener { view ->
        thread {
            val ip = intent.getStringExtra("IP")
            val port = intent.getIntExtra("PORT", -1)
            val bluetoothDevice = intent.getParcelableExtra<BluetoothDevice>("DEV")

            Channel.connect(
                ip,
                port,
                bluetoothDevice,
                "69420" /*password.text.toString()*/
            ) { result ->
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
        }
    }
}
