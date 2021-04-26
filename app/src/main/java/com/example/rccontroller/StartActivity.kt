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
import com.github.kittinunf.fuel.httpGet
import com.skyfishjy.library.RippleBackground
import debug
import info
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import warn
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread


class StartActivity : AppCompatActivity() {

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

    private val CAESAR_URL: String = "https://kingbrady.web.elte.hu/rc_car/get_available.php"
    private val BLUETOOTH_NAME: String = "RC_car_raspberrypi"
    private val IFACE_WIFI: String = "wlan0"

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

    private var pendingRequests: Int = 0

    private var deniedPermissions: ArrayList<Int> = ArrayList()

    private var correctableWifiError: Boolean = false
    private var correctableBTError: Boolean = false

    private var bluetoothPairingStatus: PairingStaus = PairingStaus.SCANNING

    private var ip: String? = null
    private var port: Int? = null
    private var bluetoothDevice: BluetoothDevice? = null

    private var response: JSONArray = JSONTokener("[]").nextValue() as JSONArray
    private var caesarReachable = true

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val btdevice =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    btdevice?.let { device ->
                        if (device.name == BLUETOOTH_NAME) {
                            info { "Discovered bluetooth device, with address ${device.address}" }
                            if (device.bondState == BluetoothDevice.BOND_BONDED) {
                                bluetoothDevice = device
                                bluetoothPairingStatus = PairingStaus.FINISHED
                                debug { "Device already paired, set to device" }
                            } else {
                                bluetoothPairingStatus = PairingStaus.BONDING
                                debug { "Device not paired yet, started bonding process now" }
                                device.createBond()
                            }
                            BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                        }
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    if (intent.getIntExtra(
                            BluetoothDevice.EXTRA_BOND_STATE,
                            69
                        ) == BluetoothDevice.BOND_BONDED
                    ) {
                        bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        debug { "Device paired, set to device" }
                        bluetoothPairingStatus = PairingStaus.FINISHED
                    } else if (intent.getIntExtra(
                            BluetoothDevice.EXTRA_BOND_STATE,
                            69
                        ) == BluetoothDevice.BOND_NONE && intent.getIntExtra(
                            BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
                            69
                        ) == BluetoothDevice.BOND_BONDING
                    ) {
                        warn { "Pairing request denied by user" }
                        bluetoothPairingStatus = PairingStaus.FINISHED
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    debug { "Bluetooth discovery finished" }
                    // The if statement needed, because if a device found, we cancel the discovery,
                    // to save resources, hence triggering this event, but in this case it is not
                    // the expected behavior to finish the pairing
                    if (bluetoothPairingStatus == PairingStaus.SCANNING) {
                        bluetoothPairingStatus = PairingStaus.FINISHED
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    debug { "Bluetooth discovery started" }
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

        thread {
            connect()
        }
    }

    override fun onRestart() {
        super.onRestart()
        debug { "Activity restarted" }
        thread {
            ip = null
            port = null
            bluetoothDevice = null
            connect()
        }
    }

    override fun onDestroy() {
        debug { "Activity destroyed" }
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
            REQUEST_ACCESS_WIFI_STATE -> R.string.ACCESS_WIFI_STATE
            REQUEST_BLUETOOTH -> R.string.BLUETOOTH
            REQUEST_ACCESS_FINE_LOCATION -> R.string.ACCESS_FINE_LOCATION
            REQUEST_ACCESS_NETWORK_STATE -> R.string.ACCESS_NETWORK_STATE
            REQUEST_BLUETOOTH_ADMIN -> R.string.BLUETOOTH_ADMIN
            REQUEST_INTERNET -> R.string.INTERNET
            REQUEST_ACCESS_BACKGROUND_LOCATION -> R.string.ACCESS_BACKGROUND_LOCATION
            else -> -1
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
            "request: ${getString(permission)}, result: GRANTED"
        } else {
            "request: ${getString(permission)}, result: DENIED"
        }

        if (!res) {
            deniedPermissions.add(permission)
        }

        info { responseString }
        pendingRequests--
    }

    private fun connect() {
        requestPermissions()
        waitForResults(2)

        if (!isNecessaryWifiPermissionsGranted || !isNecessaryBTPermissionsGranted) {
            pendingRequests++
            var message = ""
            deniedPermissions.forEach { permission ->
                message += "\n" + getString(permission)
            }

            runOnUiThread { dialog.setTitle("About Permissions").setMessage(message.trim()).show() }
            waitForResults(2)

            requestPermissions()
            waitForResults(2)
        }

        val intentMain = Intent(this, MainActivity::class.java)
        val intentError = Intent(this, ErrorActivity::class.java)

        if (searchForDevice()) {
            info { "Device found successfully, launching main activity now" }
            intentMain.putExtra("IP", ip)
            intentMain.putExtra("PORT", port)
            intentMain.putExtra("DEV", bluetoothDevice)
            startActivity(intentMain)
        } else {

            intentError.putExtra(
                getString(R.string.error),
                if (!isNecessaryWifiPermissionsGranted && !isNecessaryBTPermissionsGranted) {
                    getString(R.string.PERMISSION_ERROR)
                } else {
                    getString(R.string.NO_DEVICES_ERROR)
                }
            )
            warn { "No device found, launching error activity now" }
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
                info { "Requesting permission for: $permission" }
                pendingRequests++
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    request
                )
            } else {
                debug { "$permission already has been granted" }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (
                ContextCompat.checkSelfPermission(
                    baseContext, ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                info { "Requesting permission for: $ACCESS_BACKGROUND_LOCATION" }
                isNecessaryBTPermissionsGranted = false
                pendingRequests++
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(ACCESS_BACKGROUND_LOCATION),
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

        if (!deviceFound) {
            val message = when {
                correctableWifiError && correctableBTError ->
                    getString(R.string.BOTH_DEVICES_OFF) +
                            getString(R.string.TURN_WIFI_ON) +
                            "\n" +
                            getString(R.string.TURN_BLUETOOTH_ON)
                correctableWifiError ->
                    getString(R.string.UNSUCCESSFUL_SEARCH) +
                            " BLUETOOTH.\n" +
                            getString(R.string.TURN_WIFI_ON)
                !caesarReachable && correctableBTError ->
                    getString(R.string.CAESAR_UNREACHABLE) +
                            getString(R.string.TURN_BLUETOOTH_ON)
                correctableBTError ->
                    getString(R.string.UNSUCCESSFUL_SEARCH) +
                            " WIFI.\n" +
                            getString(R.string.TURN_BLUETOOTH_ON)
                else -> getString(R.string.UNSUCCESSFUL_SEARCH) + " both WIFI and BLUETOOTH.\n"
            }

            pendingRequests++
            runOnUiThread { dialog.setTitle("Advised Steps").setMessage(message).show() }
            waitForResults(2)

            if (isNecessaryWifiPermissionsGranted) {
                deviceFound = searchForLanDevices()
            }
            if (!deviceFound && isNecessaryBTPermissionsGranted) {
                deviceFound = searchForBTDevices()
            }
        }

        return deviceFound
    }

    private fun searchForLanDevices(): Boolean {
        val connMgr = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val iface = connMgr.getLinkProperties(connMgr.activeNetwork)?.interfaceName

        if (iface == null) {
            correctableWifiError = true
            warn { "No internet connection" }
        } else {
            pendingRequests++
            try {
                CAESAR_URL.httpGet().timeout(1000).response { _, resp, _ ->
                    response =
                        JSONTokener(String(resp.body().toByteArray())).nextValue() as JSONArray
                    pendingRequests--
                }
                waitForResults()
                if (response.length() > 0 && iface == IFACE_WIFI) {
                    return scanForLanDevices(response)
                } else if (response.length() > 0) {
                    correctableWifiError = true
                }
            } catch (e: Exception) {
                pendingRequests = 0
                caesarReachable = false
                warn { e.message!! }
            }
        }
        return false
    }

    private fun searchForBTDevices(): Boolean {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

        return if (bluetoothAdapter == null) {
            warn { "Bluetooth not supported on this device" }
            false
        } else {
            if (!bluetoothAdapter.isEnabled) {
                info { "Bluetooth turned off on this device" }
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
            val date = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            ).parse(device["time_stamp"].toString())
            if (device["ssid"].equals(ssid.trim('"')) && date != null && Calendar.getInstance().timeInMillis - date.time < 60000) {
                ip = device["ip"].toString()
                port = device["port"].toString().toInt()
                info { "LAN device found with ip: $ip, port: $port" }
                return true
            }
        }
        return false
    }

    private fun scanForBTDevices(adapter: BluetoothAdapter): Boolean {
        bluetoothPairingStatus = PairingStaus.SCANNING
        adapter.startDiscovery()
        waitForResults(2, true)

        if (bluetoothDevice != null) {
            info { "BT found with address: ${bluetoothDevice!!.address}" }
        }
        return bluetoothDevice != null
    }

    private fun waitForResults(seconds: Long = 1, isBluetoothState: Boolean = false) {
        while ({
                if (isBluetoothState) bluetoothPairingStatus != PairingStaus.FINISHED
                else pendingRequests > 0
            }()) Thread.sleep(1000 * seconds)
    }
}
