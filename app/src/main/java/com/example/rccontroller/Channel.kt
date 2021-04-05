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
import java.util.concurrent.locks.ReentrantLock

object Channel {
    private var socket: Socket? = null
    private var btsocket: BluetoothSocket? = null
    private lateinit var socketWriter: OutputStream
    private lateinit var socketReader: Scanner

    private val dataTable = JSONObject()

    private val lock = ReentrantLock()  // TODO: remove this?

    var isConnectionActive: Boolean = true
    var errorMessage: String? = null

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
                btsocket =
                    bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"))
                btsocket?.let { notNullableSocket ->
                    notNullableSocket.connect()
                    socketWriter = notNullableSocket.outputStream
                    socketReader = Scanner(notNullableSocket.inputStream)
                }
            } else {
                errorMessage = "The device discovered is no longer available, try a new device"
            }

            // TODO: better hash algorithm

            socketWriter.write(
                MessageDigest.getInstance("SHA-256").digest(
                    password.toByteArray(Charset.defaultCharset())
                )
            )

            if (socketReader.nextLine() != "GRANTED") {
                errorMessage = "Access DENIED"
                result = false
            }
        } catch (e: Exception) {
            errorMessage = e.message
            println(errorMessage)
            result = false
        }
        callback(result)
    }

    fun setMessage(key: String, value: Boolean) {
//        lock.lock()
        try {
            dataTable.put(key, value)
            socketWriter.write(dataTable.toString().toByteArray(Charset.defaultCharset()))
        }
        catch (e: Exception) {
            println(e.message)
        }
        finally {
//            lock.unlock()
        }
    }

    fun getBoolean(key: String): Boolean {
//        lock.lock()
        var value = false
        try {
            value = dataTable.optBoolean(key, false)
        }
        catch (e: Exception) {
            println(e.message)
        }
        finally {
//            lock.unlock()
        }
        return value
    }

    fun getDouble(key: String, fallback: Double): Double {
//        lock.lock()
        var value = fallback
        try {
            value = dataTable.optDouble(key, fallback)
        }
        catch (e: Exception) {
            println(e.message)
        }
        finally {
//            lock.unlock()
        }
        return value
    }

    fun run() {
        while (isConnectionActive) {
            try {
//                lock.lock()
                val received =
                    (JSONTokener(socketReader.nextLine()).nextValue() as JSONObject)
                received.keys().forEach { key ->
                    if (key == "distance" || key == "speed") {
                        dataTable.put(key, received.getDouble(key))
                    }
                    else {
                        dataTable.put(key, received.getBoolean(key))
                    }
                }
            }
            catch (e: Exception) {
                println(e.message)
            }
            finally {
//                lock.unlock()
            }
        }
    }

    fun stop() {
        isConnectionActive = false
        socket?.close()
        btsocket?.close()
    }

    fun sendTurnOffSignal() {
        socketWriter.write("POWEROFF".toByteArray(Charset.defaultCharset()))
    }
}
