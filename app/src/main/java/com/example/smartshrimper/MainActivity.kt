package com.example.smartshrimper


import com.example.smartshrimper.R
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.*
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.HandlerCompat
import com.example.smartshrimper.ui.main.*
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject
import org.json.JSONException
import java.io.IOException
import java.util.*


var controllerFound = false
lateinit var mBTdevice: BluetoothDevice
lateinit var mBTSocket: BluetoothSocket
var mBtAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

//This is the default well-known UUID
val mUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
val mainThreadHandler: Handler = HandlerCompat.createAsync(Looper.getMainLooper())

class ShrimperData {
    var rpm = 0f
    var batteryVoltage = 0f
    var relayStatus = false
}

class MainActivity : AppCompatActivity() {
    val shrimperData = ShrimperData()
    val gaugeThread = HandlerThread("GaugeDemoThread")
    lateinit var gaugeHandler: Handler
    lateinit var revCounter: Gauge
    lateinit var voltmeter: Gauge
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        revCounter = findViewById(com.example.smartshrimper.R.id.revCounter)
        voltmeter = findViewById(com.example.smartshrimper.R.id.voltmeter)
        gaugeThread.start()
        gaugeHandler = Handler(gaugeThread.looper)

        val view: View = findViewById(R.id.constraintLayout)

        //Connect to the ESP-32 sensor controller over Bluetooth

        //1. Check the BT is turned on
        if (!mBtAdapter.isEnabled) {
            Snackbar.make(view, "Bluetooth is not enabled", Snackbar.LENGTH_LONG).show()
        }
        val pairedDevices = mBtAdapter.bondedDevices

        if (pairedDevices.size > 0) {
            for (mdevice in pairedDevices) {
                if (mdevice.name == "Smart Shrimper") {
                    Snackbar.make(view, "Found paired Bluetooth device", Snackbar.LENGTH_LONG)
                        .show()
                    mBTdevice = mdevice
                    controllerFound = true

                }
                if (controllerFound == false) {
                    Snackbar.make(
                        view,
                        "Smart Shrimper controller has not been paired",
                        Snackbar.LENGTH_LONG
                    ).show()
                    finishAffinity()
                }
            }
        }
        if (controllerFound) {
            //Set up a handler to handle incoming Messages from the BT Thread
            val myBThandler = object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    if (msg.what == BT_MSG.TOAST.ordinal) {//Could not connect
                        Snackbar.make(view, "Could not connect to controller", 6000).show()
                    } else { //Message is a valid BT message
                        // extract the ByteArray from the message
                        val msgByteArray = msg.data.getByteArray(null)
                        var jObj: JSONObject? = null
                        // Create the JSON object from the ByteArray
                        try {
                            jObj = JSONObject(msgByteArray?.let { String(it) })
                        } catch (e: JSONException) {
                            e.message?.let { Snackbar.make(view, it, 4000).show() }
                            Snackbar.make(view, msgByteArray.toString(), 4000).show()
                        }
                        if (jObj != null) {
                            // fetch fields named the JSON objects
                            shrimperData.rpm = jObj.getString("RPM").toFloat() ?: 0F
                            shrimperData.batteryVoltage =
                                jObj.getString("Voltage").toFloat() ?: 0F
                            shrimperData.relayStatus =
                                jObj.getString("Coolbox Status").toBoolean() ?: false
                            updateGauges()
                        }
                    }
                }
            }
            //Create and run the Bluetooth Thread
            val btService = MyBluetoothService(myBThandler, mBTdevice)
            btService.ConnectThread().start()
            Snackbar.make(view, "Connecting to the controller", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateGauges()
    }

    fun updateGauges() {

            revCounter.moveToValue(shrimperData.rpm)
            voltmeter.moveToValue(shrimperData.batteryVoltage)

    }
}

