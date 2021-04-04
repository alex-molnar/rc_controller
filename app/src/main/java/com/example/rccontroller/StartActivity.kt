package com.example.rccontroller

import android.Manifest.permission.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.skyfishjy.library.RippleBackground
import io.github.rybalkinsd.kohttp.ext.asString
import io.github.rybalkinsd.kohttp.ext.httpGet
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import kotlin.concurrent.thread

class StartActivity : AppCompatActivity() {

    private fun Int.asBoolean() = this == 1

    private val REQUEST_ACCESS_WIFI_STATE: Int = 1
    private val REQUEST_BLUETOOTH: Int = 2
    private val REQUEST_ACCESS_FINE_LOCATION: Int = 3
    private val REQUEST_ACCESS_NETWORK_STATE: Int = 4
    private val REQUEST_BLUETOOTH_ADMIN: Int = 5
    private val REQUEST_INTERNET: Int = 6
    private val REQUEST_ACCESS_BACKGROUND_LOCATION: Int = 7
    private val REQUEST_ENABLE_BT: Int = 8

    private val neededPermissions = hashMapOf<String, Int>(
        ACCESS_WIFI_STATE to REQUEST_ACCESS_WIFI_STATE,
        BLUETOOTH to REQUEST_BLUETOOTH,
        ACCESS_FINE_LOCATION to REQUEST_ACCESS_FINE_LOCATION,
        ACCESS_NETWORK_STATE to REQUEST_ACCESS_NETWORK_STATE,
        BLUETOOTH_ADMIN to REQUEST_BLUETOOTH_ADMIN,
        INTERNET to REQUEST_INTERNET
    )

    private var isNecessaryWifiPermissionsGranted: Boolean = true
    private var isNecessaryBTPermissionsGranted: Boolean = true

    private var pendingRequests = 0

    private var deniedPermissions: ArrayList<String> = ArrayList()

    private var correctableWifiError: Boolean = false
    private var correctableBTError: Boolean = false

    private var ip: String? = null
    private var port: Int? = null
    //and things needed for bluetooth connection

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if (device?.name == "RC_car_raspberrypi") {
                        println("name: ${device.name}")
                        println("addr: ${device.address}")
                        println("bs: ${device.bondState}")
//                        device.createBond()  // Works
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    println("\n\n\n######################DISC FIN###########################\n\n\n")
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    println("\n\n\n#####################DISC START##########################\n\n\n")
                }
                BluetoothAdapter.ACTION_REQUEST_ENABLE -> {
                    println("\n\n\n#####################$contxt, $intent##########################\n\n\n")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        findViewById<RippleBackground>(R.id.content).startRippleAnimation()

        requestPermissions()

        thread {
            while (pendingRequests > 0) {
                Thread.sleep(2000)
            }

            if (!isNecessaryWifiPermissionsGranted || !isNecessaryBTPermissionsGranted) {
                deniedPermissions.forEach { permission ->
                    println("Beg for $permission nicely")
                }
                requestPermissions()
                while (pendingRequests > 0) {
                    Thread.sleep(2000)
                }
            }

            var deviceFound = false

            if (isNecessaryWifiPermissionsGranted) {
                deviceFound = searchForLanDevices()
            }
            if (!deviceFound && isNecessaryBTPermissionsGranted) {
                deviceFound = searchForBTDevices()
            }
            println("After one round deviceFound: $deviceFound")

            if (!deviceFound) {
                if (correctableWifiError) {
                    println("Display some nice message to available WIFI")
                }
                if (correctableBTError) {
                    println("Display some nice message of BT")
                } // TODO: use hasmap so only one message is displayed
                if (isNecessaryWifiPermissionsGranted) {
                    deviceFound = searchForLanDevices()
                }
                if (!deviceFound && isNecessaryBTPermissionsGranted) {
                    deviceFound = searchForBTDevices()
                }
            }

            println("Finished with your shit res: $deviceFound, ip: $ip, port: $port")
        }


//        val intentController = Intent(this, MainActivity::class.java)
//
//        Timer("SettingUp", false).schedule(2500) {
//            startActivity(intentController)
//        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        val permission = when (requestCode) {
            REQUEST_ACCESS_WIFI_STATE -> "ACCESS_WIFI_STATE"
            REQUEST_BLUETOOTH -> "BLUETOOTH"
            REQUEST_ACCESS_FINE_LOCATION -> "ACCESS_FINE_LOCATION"
            REQUEST_ACCESS_NETWORK_STATE -> "ACCESS_NETWORK_STATE"
            REQUEST_BLUETOOTH_ADMIN -> "BLUETOOTH_ADMIN"
            REQUEST_INTERNET -> "INTERNET"
            REQUEST_ACCESS_BACKGROUND_LOCATION -> "ACCESS_BACKGROUND_LOCATION"
            else -> "UNKNOWN"
        }

        val res = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED

        when (requestCode) {
            REQUEST_ACCESS_WIFI_STATE,
            REQUEST_ACCESS_FINE_LOCATION,
            REQUEST_ACCESS_NETWORK_STATE,
            REQUEST_INTERNET ->
                isNecessaryWifiPermissionsGranted = res
            REQUEST_BLUETOOTH,
            REQUEST_BLUETOOTH_ADMIN,
            REQUEST_ACCESS_BACKGROUND_LOCATION ->
                isNecessaryBTPermissionsGranted = res
        }

        val responseString = if (res) {
            "request: $permission, result: GRANTED"
        } else {
            "request: $permission, result: DENIED"
        }

        if (!res) {
            deniedPermissions.add(permission)
        }

        println(responseString)
        pendingRequests--
    }

    private fun requestPermissions() {
        neededPermissions.forEach { (permission, request) ->
            val res = ContextCompat.checkSelfPermission(
                baseContext,
                permission
            ) == PermissionChecker.PERMISSION_GRANTED

            when (permission) {
                ACCESS_WIFI_STATE,
                ACCESS_FINE_LOCATION,
                ACCESS_NETWORK_STATE,
                INTERNET ->
                    isNecessaryWifiPermissionsGranted = isNecessaryWifiPermissionsGranted && res
                BLUETOOTH,
                BLUETOOTH_ADMIN,
                ACCESS_BACKGROUND_LOCATION ->
                    isNecessaryBTPermissionsGranted = isNecessaryBTPermissionsGranted && res
            }

            if (!res) {
                println("Requesting permission for: $permission")
                pendingRequests++
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    request
                )
            } else {
                println("$permission already has been granted")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (
                ContextCompat.checkSelfPermission(
                    baseContext, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                isNecessaryBTPermissionsGranted = false
                pendingRequests++
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    REQUEST_ACCESS_BACKGROUND_LOCATION
                )
            }
        }
    }

    private fun searchForLanDevices(): Boolean {
        println("Searching for LAN devices...")
        val connMgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val iface = connMgr.getLinkProperties(connMgr.activeNetwork)?.interfaceName

        if (iface == null) {
            correctableWifiError = true
        } else {
            val response: JSONArray = (
                    JSONTokener(
                        "https://kingbrady.web.elte.hu/rc_car/get_available.php".httpGet()
                            .asString()
                    ).nextValue() as JSONArray
                    )
            if (response.length() > 0 && iface == "wlan0") {
                val ssid =
                    (getSystemService(Context.WIFI_SERVICE) as WifiManager).connectionInfo.ssid

                for (i: Int in 0 until response.length()) {
                    val resp = (response[i] as JSONObject)
                    if (resp["ssid"].equals(ssid.trim('"'))) {
                        println("$$$$$$$$$$$$$$$$$$$$$$$$$ ${resp["ssid"]} $$$$$$$$$$$$$$$$$$$$$$$")
                        ip = resp["ip"].toString()
                        port = resp["port"].toString().toInt()
                        return true
                    }
                }
            } else if (response.length() > 0) {
                correctableWifiError = true
            }
        }
        return false  // TODO:
    }

    private fun searchForBTDevices(): Boolean {
        println("Searching for BT devices")
//        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
//        if (bluetoothAdapter == null) {
//            println("Bluetooth not supported")
//        }
//        else {
//            if (!bluetoothAdapter.isEnabled) {
//                println("Bluetooth off")
//                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
//            } else {
//                println("Bluetooth on")
//            }
//        }
        return false
    }
}
