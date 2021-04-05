package com.example.rccontroller

import android.Manifest.permission.*
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
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

    private enum class PairingStaus {
        SCANNING, BONDING, FINISHED
    }

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

    private lateinit var permissionExplanations: HashMap<String, String>

    private var isNecessaryWifiPermissionsGranted: Boolean = true
    private var isNecessaryBTPermissionsGranted: Boolean = true

    private var pendingRequests = 0

    private var deniedPermissions: ArrayList<String> = ArrayList()

    private var correctableWifiError: Boolean = false
    private var correctableBTError: Boolean = false

    private var bluetoothPairingStatus: PairingStaus = PairingStaus.SCANNING

    private var ip: String? = null
    private var port: Int? = null
    private var bluetoothDevice: BluetoothDevice? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if (device?.name == "RC_car_raspberrypi") {
                        println("discovered ${device.name} with address ${device.address} already paired: ${device.bondState == BluetoothDevice.BOND_BONDED}")
                        if (device.bondState == BluetoothDevice.BOND_BONDED) {
                            bluetoothDevice = device
                            bluetoothPairingStatus = PairingStaus.FINISHED
                            println("Device already bonded, set to device")
                        } else {
                            bluetoothPairingStatus = PairingStaus.BONDING
                            println("Started bonding process")
                            device.createBond()
                        }
                        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    println("\n\n\n######################BOND STATE CHNAGE###########################\n\n\n")
                    if (intent.getIntExtra(
                            BluetoothDevice.EXTRA_BOND_STATE,
                            69
                        ) == BluetoothDevice.BOND_BONDED
                    ) {
                        bluetoothDevice =
                            intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        println("Device paired, set to device")
                        bluetoothPairingStatus = PairingStaus.FINISHED
                    } else if (intent.getIntExtra(
                            BluetoothDevice.EXTRA_BOND_STATE,
                            69
                        ) == BluetoothDevice.BOND_NONE && intent.getIntExtra(
                            BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
                            69
                        ) == BluetoothDevice.BOND_BONDING
                    ) {
                        println("Pairing request denied by user")
                        bluetoothPairingStatus = PairingStaus.FINISHED
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    println("\n\n\n######################DISC FIN###########################\n\n\n")
                    // The if statement needed, because if a device found, we cancel the discovery,
                    // to save resources, hence triggering this event, but in this case it is not
                    // the expected behavior to finish the pairing
                    if (bluetoothPairingStatus == PairingStaus.SCANNING) {
                        bluetoothPairingStatus = PairingStaus.FINISHED
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    println("\n\n\n#####################DISC START##########################\n\n\n")
                }
            }
        }
    }

    private lateinit var dialog: AlertDialog.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        findViewById<RippleBackground>(R.id.content).startRippleAnimation()
        dialog = AlertDialog.Builder(this)
            .setIcon(R.drawable.ic_hotroad)
            .setNeutralButton("OK") { _: DialogInterface, _: Int -> pendingRequests-- }

        registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
        registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED))
        registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))

        permissionExplanations = hashMapOf(
            "ACCESS_WIFI_STATE" to getString(R.string.ACCESS_WIFI_STATE),
            "BLUETOOTH" to getString(R.string.BLUETOOTH),
            "ACCESS_FINE_LOCATION" to getString(R.string.ACCESS_FINE_LOCATION),
            "ACCESS_NETWORK_STATE" to getString(R.string.ACCESS_NETWORK_STATE),
            "BLUETOOTH_ADMIN" to getString(R.string.BLUETOOTH_ADMIN),
            "INTERNET" to getString(R.string.INTERNET),
            "ACCESS_BACKGROUND_LOCATION" to getString(R.string.ACCESS_BACKGROUND_LOCATION)
        )

        thread {
            connect()
        }
    }

    override fun onRestart() {
        super.onRestart()
        thread {
            connect()
        }
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_ENABLE_BT -> pendingRequests--
        }
        super.onActivityResult(requestCode, resultCode, data)
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
            REQUEST_ACCESS_FINE_LOCATION -> {
                isNecessaryWifiPermissionsGranted = res
                isNecessaryBTPermissionsGranted = res
            }
            REQUEST_ACCESS_WIFI_STATE,
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

    private fun connect() {
        requestPermissions()
        while (pendingRequests > 0) {
            Thread.sleep(2000)
        }

        if (!isNecessaryWifiPermissionsGranted || !isNecessaryBTPermissionsGranted) {
            pendingRequests++
            var message = ""

            deniedPermissions.forEach { permission ->
                message += "\n" + permissionExplanations[permission]
            }

            runOnUiThread {
                dialog.setTitle("About Permissions").setMessage(message.trim()).show()
            }

            while (pendingRequests > 0) {
                Thread.sleep(2000)
            }

            requestPermissions()
            while (pendingRequests > 0) {
                Thread.sleep(2000)
            }
        }

        val intentMain = Intent(this, MainActivity::class.java)
        val intentError = Intent(this, ErrorActivity::class.java)

        if (searchForDevice()) {
            println("DEVICE FOUND LAUNCHING MAIN ACTIVITY")
            intentMain.putExtra("IP", ip)
            intentMain.putExtra("PORT", port)
            intentMain.putExtra("DEV", bluetoothDevice)
            startActivity(intentMain)
        } else {
            println("DEVICE NOT FOUND LAUNCHING ERROR ACTIVITY")
            intentError.putExtra(
                getString(R.string.error),
                if (!isNecessaryWifiPermissionsGranted && !isNecessaryBTPermissionsGranted) {
                    getString(R.string.PERMISSION_ERROR)
                } else {
                    getString(R.string.NO_DEVICES_ERROR)
                }
            )
            startActivity(intentError)
        }
    }

    private fun requestPermissions() {
        neededPermissions.forEach { (permission, request) ->
            val res = ContextCompat.checkSelfPermission(
                baseContext,
                permission
            ) == PermissionChecker.PERMISSION_GRANTED

            when (permission) {
                ACCESS_FINE_LOCATION -> {
                    isNecessaryWifiPermissionsGranted = isNecessaryWifiPermissionsGranted && res
                    isNecessaryBTPermissionsGranted = isNecessaryBTPermissionsGranted && res
                }
                ACCESS_WIFI_STATE,
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

    private fun searchForDevice(): Boolean {
        var deviceFound = false

        if (isNecessaryWifiPermissionsGranted) {
            deviceFound = searchForLanDevices()
        }
        if (!deviceFound && isNecessaryBTPermissionsGranted) {
            deviceFound = searchForBTDevices()
        }
        println("After one round deviceFound: $deviceFound")

        if (!deviceFound) {
            var message: String = ""
            val shouldDisplayMessage = correctableWifiError || correctableBTError
            if (correctableWifiError) {
                message = getString(R.string.TURN_WIFI_ON)
                correctableWifiError = false
            }
            if (correctableBTError) {
                message += "\n" + getString(R.string.TURN_BLUETOOTH_ON)
                correctableBTError = false
            }

            if (shouldDisplayMessage) {
                runOnUiThread {
                    dialog.setTitle("About Permissions").setMessage(message.trim()).show()
                }
                pendingRequests++

                while (pendingRequests > 0) {
                    Thread.sleep(2000)
                }
            }

            if (isNecessaryWifiPermissionsGranted) {
                deviceFound = searchForLanDevices()
            }
            if (!deviceFound && isNecessaryBTPermissionsGranted) {
                deviceFound = searchForBTDevices()
            }
        }

        println("Finished with your shit res: $deviceFound, ip: $ip, port: $port, dev: ${bluetoothDevice?.address}")
        return deviceFound
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
                return scanForLanDevices(response)
            } else if (response.length() > 0) {
                correctableWifiError = true
            }
        }
        return false
    }

    private fun searchForBTDevices(): Boolean {
        println("Searching for BT devices")
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

        return if (bluetoothAdapter == null) {
            println("Bluetooth not supported")
            false
        } else {
            if (!bluetoothAdapter.isEnabled) {
                println("Bluetooth off")
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)

                pendingRequests++
                while (pendingRequests > 0) Thread.sleep(2000)
                if (!bluetoothAdapter.isEnabled) correctableBTError = true

                bluetoothAdapter.isEnabled && scanForBTDevices(bluetoothAdapter)
            } else {
                scanForBTDevices(bluetoothAdapter)
            }
        }
    }

    private fun scanForLanDevices(devices: JSONArray): Boolean {
        val ssid = (getSystemService(Context.WIFI_SERVICE) as WifiManager).connectionInfo.ssid

        for (i: Int in 0 until devices.length()) {
            val device = (devices[i] as JSONObject)
            if (device["ssid"].equals(ssid.trim('"'))) {
                println("$$$$$$$$$$$$$$$$$$$$$$$$$ ${device["ssid"]} $$$$$$$$$$$$$$$$$$$$$$$")
                ip = device["ip"].toString()
                port = device["port"].toString().toInt()
                return true
            }
        }
        return false
    }

    private fun scanForBTDevices(adapter: BluetoothAdapter): Boolean {
        bluetoothPairingStatus = PairingStaus.SCANNING
        adapter.startDiscovery()

        while (bluetoothPairingStatus != PairingStaus.FINISHED) {
            println("WAITING FOR BT discovery")
            Thread.sleep(3000)
        }

        println("BT Discovery process ended. result: ${bluetoothDevice != null}")
        return bluetoothDevice != null
    }
}
