package com.example.rccontroller

import org.json.JSONObject
import org.json.JSONTokener
import java.io.OutputStream
import java.net.Socket
import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object Channel {
    private lateinit var sendingSocket: Socket
    private lateinit var sendingSocketWriter: OutputStream
    private lateinit var sendingSocketReader: Scanner
    private lateinit var receivingSocket: Socket
    private lateinit var receivingSocketWriter: OutputStream
    private lateinit var receivingSocketReader: Scanner

    private val dataTable = JSONObject()

    private val lock = ReentrantLock()

    var isConnectionActive: Boolean = true
    var errorMessage: String? = null

    fun hashPassword(
        password: CharArray?,
        salt: ByteArray?,
        iterations: Int,
        keyLength: Int
    ): ByteArray? {
        return try {
            val skf: SecretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(password, salt, iterations, keyLength)
            val key: SecretKey = skf.generateSecret(spec)
            key.getEncoded()
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: InvalidKeySpecException) {
            throw RuntimeException(e)
        }
    }

    fun connect(
        host: String,
        port: Int,
        password: String,
        callback: (Boolean) -> Unit
    ) {

        var result = true
        try {
            sendingSocket = Socket(host, port)
            sendingSocketWriter = sendingSocket.getOutputStream()
            sendingSocketReader = Scanner(sendingSocket.getInputStream())
            receivingSocket = Socket(host, port)
            receivingSocketWriter = receivingSocket.getOutputStream()
            receivingSocketReader = Scanner(receivingSocket.getInputStream())

            // TODO: better hash algorithm
            // TODO: get everything (salt, key length, iter length...) from db
//            val salt = ByteArray(32)
//            nextBytes(salt)
//            val saltString = "1234"
//            val salt = saltString.toByteArray()
//            val hash = hashPassword(password.toCharArray(), salt, 1000, 64)
//            println("\n#########################################################################\n")
//            println(hash)
//            println("\n#########################################################################\n")
//            sendingSocketWriter!!.write(
//                hash
//            )
            sendingSocketWriter.write(
                MessageDigest.getInstance("SHA-256").digest(
                    password.toByteArray(Charset.defaultCharset())
                )
            )

            if (receivingSocketReader.nextLine() != "GRANTED") {
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
            sendingSocketWriter.write(dataTable.toString().toByteArray(Charset.defaultCharset()))
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
            try {
                lock.lock()
                val received =
                    (JSONTokener(receivingSocketReader.nextLine()).nextValue() as JSONObject)
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
                receivingSocketWriter.write("Done.".toByteArray(Charset.defaultCharset()))
            }
        }
    }

    fun stop() {
        isConnectionActive = false
        sendingSocket.close()
        Thread.sleep(1000)  // lot of reasons this is necessary TODO: explain it (note sockets closing before program exiting causing exceptions, here and on server side as well)
        receivingSocket.close()
    }

    fun sendTurnOffSignal() {
        sendingSocketWriter.write("POWEROFF".toByteArray(Charset.defaultCharset()))
    }
}
