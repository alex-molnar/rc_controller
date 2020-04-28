package com.example.rccontroller

import org.json.JSONObject
import org.json.JSONTokener
import java.io.OutputStream
import java.lang.Exception
import java.net.Socket
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*
import kotlin.collections.HashMap

object Channel {
    private var sendingSocket: Socket? = null
    private var sendingSocketWriter: OutputStream? = null
    private var sendingSocketReader: Scanner? = null
    private var receivingSocket: Socket? = null
    private var receivingSocketWriter: OutputStream? = null
    private var receivingSocketReader: Scanner? = null

    private val dataTable = JSONObject()
    private val messageTable = JSONObject()

    var state: Boolean = true
    var errorMessage: String? = null

    fun connect(
        host: String,
        port: Int,
        password: String,
        callback: (Boolean) -> Unit): Unit {

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

            if (receivingSocketReader!!.nextLine() == "GRANTED") {
                println("GRAAAAANTEEEEEEEEEED")
            }
            else {
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

    fun tmpSend(): Unit {
//        messageTable.put("FORWARD", true)
//        messageTable.put("BACKWARD", false)
//        messageTable.put("LIGHTS", true)
//        sendingSocketWriter!!.write(messageTable.toString().toByteArray(Charset.defaultCharset()))
//        val received = (JSONTokener(receivingSocketReader!!.nextLine()).nextValue() as JSONObject)
//        received.keys().forEach { key ->
//            dataTable.put(key, received.getBoolean(key))
//        }
//        println(dataTable.toString(4))
    }
}