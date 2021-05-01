package com.example.rccontroller

import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_pin.*
import kotlin.concurrent.thread


class PinActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        thread {
            connectButton.setOnClickListener(onConnectClicked)
            restartDiscoveryButton.setOnClickListener() {
                finish()
            }
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
                "69420"
//                password.text.toString()
            ) { result, fatalError ->
                when {
                    result -> {
                        val intentController = Intent(this, MainActivity::class.java)
                        startActivity(intentController)
                    }
                    fatalError -> {
                        runOnUiThread {
                            AlertDialog.Builder(this)
                                .setIcon(R.drawable.ic_hotroad)
                                .setNeutralButton("OK") { _: DialogInterface, _: Int -> finish() }
                                .setTitle("SUCCESS")
                                .setMessage(Channel.errorMessage)
                                .show()
                        }
                    }
                    else -> {
                        Snackbar.make(view, Channel.errorMessage, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
