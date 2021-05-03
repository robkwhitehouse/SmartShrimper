package com.example.smartshrimper.ui.main

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ContentValues
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.example.smartshrimper.R
import com.example.smartshrimper.mBtAdapter
import com.example.smartshrimper.mUUID
import com.google.android.material.snackbar.Snackbar
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

private const val TAG = "SMART_SHRIMPER_DEBUG"

// Defines several constants used when transmitting messages between the
// service and the UI.
const val MESSAGE_READ: Int = 0
const val MESSAGE_WRITE: Int = 1
const val MESSAGE_TOAST: Int = 2
// ... (Add other message types here as needed.)

class MyBluetoothService(private val handler: Handler, private val BtDevice: BluetoothDevice) {

    val btSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
        BtDevice.createRfcommSocketToServiceRecord(mUUID)
    }

    private val mmInStream: InputStream? = btSocket?.inputStream
    private val mmOutStream: OutputStream? = btSocket?.outputStream
    private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream


    inner class ConnectThread() : Thread() {

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBtAdapter.cancelDiscovery()
            try {
                btSocket?.use { socket ->
                    // Connect to the remote device through the socket. This call blocks
                    // until it succeeds or throws an exception.
                    socket.connect()
                    receive()
                }
            } catch (e: IOException) {
                val errortext = "Could not connect to the controller"
                Log.e(TAG, errortext, e)
                val msg = handler.obtainMessage(
                    MESSAGE_TOAST, -1, -1,
                    errortext)
                    msg.sendToTarget()
                    cancel()

            }

        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                btSocket?.close()
            } catch (e: IOException) {
                Log.e(ContentValues.TAG, "Could not close the client socket", e)
            }
        }


        fun receive() {
            var numBytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
                numBytes = try {
                    mmInStream!!.read(mmBuffer)
                } catch (e: IOException) {
                    Log.d(TAG, "Input stream was disconnected", e)
                    break
                }

                // Send the obtained bytes to the UI activity.
                val readMsg = handler.obtainMessage(
                    MESSAGE_READ, numBytes, -1,
                    mmBuffer)
                readMsg.sendToTarget()
            }
        }

        // Call this from the main activity to send data to the remote device.
        fun write(bytes: ByteArray) {
            try {
                mmOutStream!!.write(bytes)
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)

                // Send a failure message back to the activity.
                val writeErrorMsg = handler.obtainMessage(MESSAGE_TOAST)
                val bundle = Bundle().apply {
                    putString("toast", "Couldn't send data to the other device")
                }
                writeErrorMsg.data = bundle
                handler.sendMessage(writeErrorMsg)
                return
            }

            // Share the sent message with the UI activity.
            val writtenMsg = handler.obtainMessage(
                MESSAGE_WRITE, -1, -1, bytes)
            writtenMsg.sendToTarget()
        }
    }
}