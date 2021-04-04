package com.example.rccontroller

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

//            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
//            val filter2 = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
//            val filter3 = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
//            registerReceiver(receiver, filter)
//            registerReceiver(receiver, filter2)
//            registerReceiver(receiver, filter3)
//
//            bluetoothAdapter?.startDiscovery()
//
//            val inf = getSystemService(Context.WIFI_SERVICE) as WifiManager
//            println(inf.connectionInfo.ssid)
//            println(inf.connectionInfo.ipAddress)
//            println(inf.connectionInfo.networkId)
//            println(inf.connectionInfo.toString())
//            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
//            println("\n#############################################################################\n")
//            pairedDevices?.forEach { device ->
//                print("name: ${device.name}")
//                print(", addr: ${device.address}") // MAC address
//                print(", simplename: ${device::class.simpleName}")    // "Int"
//                print(", qualname: ${device::class.qualifiedName}")
//                print(", bonST: ${device.bondState}")
//                println(", uuids: ${device.uuids[0]}")
//                println(", descCont: ${device.describeContents()}")
//                println("\n#############################################################################\n")
//            }
        }
    }

    private val onConnectClicked = View.OnClickListener { view ->
        thread {
            val ip = "192.168.0.99"
            val port = 69420

            if (true) {
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
