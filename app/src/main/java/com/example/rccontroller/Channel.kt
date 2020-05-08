package com.example.rccontroller

import org.json.JSONObject
import org.json.JSONTokener
import java.io.OutputStream
import java.lang.Exception
import java.net.Socket
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

object Channel {
    private var sendingSocket: Socket? = null
    private var sendingSocketWriter: OutputStream? = null
    private var sendingSocketReader: Scanner? = null
    private var receivingSocket: Socket? = null
    private var receivingSocketWriter: OutputStream? = null
    private var receivingSocketReader: Scanner? = null

    private val dataTable = JSONObject()

    private val lock = ReentrantLock()

    var isConnectionActive: Boolean = true
    var errorMessage: String? = null

    fun connect(
        host: String,
        port: Int,
        password: String,
        callback: (Boolean) -> Unit) {

        var result = true
        try {
            sendingSocket = Socket(host, port)
            sendingSocketWriter = sendingSocket!!.getOutputStream()
            sendingSocketReader = Scanner(sendingSocket!!.getInputStream())
            receivingSocket = Socket(host, port)
            receivingSocketWriter = receivingSocket!!.getOutputStream()
            receivingSocketReader = Scanner(receivingSocket!!.getInputStream())

            sendingSocketWriter!!.write(
                MessageDigest.getInstance("SHA-256").digest(
                    password.toByteArray(Charset.defaultCharset())
                )
            )

            if (receivingSocketReader!!.nextLine() != "GRANTED") {
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
        lock.lock()
        try {
            dataTable.put(key, value)
            sendingSocketWriter!!.write(dataTable.toString().toByteArray(Charset.defaultCharset()))
        }
        catch (e: Exception) {
            println(e.message)
        }
        finally {
            lock.unlock()
        }
    }

    fun getBoolean(key: String): Boolean {
        lock.lock()
        var value = false
        try {
            value = dataTable.optBoolean(key, false)
        }
        catch (e: Exception) {
            println(e.message)
        }
        finally {
            lock.unlock()
        }
        return value
    }

    fun getDouble(key: String, fallback: Double): Double {
        lock.lock()
        var value = fallback
        try {
            value = dataTable.optDouble(key, fallback)
        }
        catch (e: Exception) {
            println(e.message)
        }
        finally {
            lock.unlock()
        }
        return value
    }

    fun run() {
        while (isConnectionActive) {
            val received = (JSONTokener(receivingSocketReader!!.nextLine()).nextValue() as JSONObject)
            try {
                lock.lock()
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
                lock.unlock()
                receivingSocketWriter!!.write("Done.".toByteArray(Charset.defaultCharset()))
            }
        }
    }

    fun stop() {
        isConnectionActive = false
        sendingSocket!!.close()
        receivingSocket!!.close()
    }
}
