package com.example.rccontroller

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import org.json.JSONObject
import org.json.JSONTokener
import java.io.OutputStream
import java.net.Socket
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*

object Channel {
    private var socket: Socket? = null
    private var btsocket: BluetoothSocket? = null
    private lateinit var socketWriter: OutputStream
    private lateinit var socketReader: Scanner

    private val dataTable = JSONObject()

    var isConnectionActive: Boolean = true
    lateinit var errorMessage: String

    private const val uuid: String = "94f39d29-7d6d-583a-973b-fba39e49d4ee"
    private const val DEVICE_NOT_AVAILABLE =
        "The device discovered is no longer available, try a new device!"
    private const val ACCESS_DENIED =
        "ACCESS DENIED! Try another password, or try to connect ot another device!"
    private const val NEWORK_EXCEPTION =
        "Something came up during the connection! Please check if the device is still available, and try to conenct again!"
    private const val DISTANCE = "distance"
    private const val SPEED = "speed"
    private val POWEROFF_MESSAGE = "POWEROFF".toByteArray(Charset.defaultCharset())

    fun connect(
        host: String?,
        port: Int,
        bluetoothDevice: BluetoothDevice?,
        password: String,
        callback: (Boolean) -> Unit
    ) {

        var result = true
        try {
            if (host != null && port > 0) {
                socket = Socket(host, port)
                socket?.let { notNullableSocket ->
                    socketWriter = notNullableSocket.getOutputStream()
                    socketReader = Scanner(notNullableSocket.getInputStream())
                }
            } else if (bluetoothDevice != null) {
                btsocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(uuid))
                btsocket?.let { notNullableSocket ->
                    notNullableSocket.connect()
                    socketWriter = notNullableSocket.outputStream
                    socketReader = Scanner(notNullableSocket.inputStream)
                }
            } else {
                errorMessage = DEVICE_NOT_AVAILABLE
            }

            // TODO: better hash algorithm

            socketWriter.write(
                MessageDigest.getInstance("SHA-256").digest(
                    password.toByteArray(Charset.defaultCharset())
                )
            )

            if (socketReader.nextLine() != "GRANTED") {
                errorMessage = ACCESS_DENIED
                result = false
            }
        } catch (e: Exception) {
            errorMessage = NEWORK_EXCEPTION
            println(errorMessage)
            result = false
        }
        callback(result)
    }

    fun setMessage(key: String, value: Boolean) {
        try {
            dataTable.put(key, value)
            socketWriter.write(dataTable.toString().toByteArray(Charset.defaultCharset()))
        }
        catch (e: Exception) {
            println(e.message)
        }
    }

    fun getBoolean(key: String): Boolean {
        var value = false
        try {
            value = dataTable.optBoolean(key, false)
        }
        catch (e: Exception) {
            println(e.message)
        }
        return value
    }

    fun getDouble(key: String, fallback: Double): Double {
        var value = fallback
        try {
            value = dataTable.optDouble(key, fallback)
        }
        catch (e: Exception) {
            println(e.message)
        }
        return value
    }

    fun run() {
        while (isConnectionActive) {
            try {
                val received = (JSONTokener(socketReader.nextLine()).nextValue() as JSONObject)
                received.keys().forEach { key ->
                    if (key == DISTANCE || key == SPEED) {
                        dataTable.put(key, received.getDouble(key))
                    } else {
                        dataTable.put(key, received.getBoolean(key))
                    }
                }
            }
            catch (e: Exception) {
                println(e.message)
            }
        }
    }

    fun stop() {
        isConnectionActive = false
        socket?.close()
        btsocket?.close()
    }

    fun sendTurnOffSignal() {
        socketWriter.write(POWEROFF_MESSAGE)
    }
}
