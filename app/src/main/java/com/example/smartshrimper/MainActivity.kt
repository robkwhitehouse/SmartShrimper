package com.example.smartshrimper


import android.R
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.*
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.HandlerCompat
import com.example.smartshrimper.ui.main.*
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject
import java.util.*


var controllerFound = false
lateinit var mBTdevice : BluetoothDevice
lateinit var mBTSocket : BluetoothSocket
var mBtAdapter : BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        revCounter = findViewById(com.example.smartshrimper.R.id.revCounter)
        gaugeThread.start()
        gaugeHandler = Handler(gaugeThread.getLooper())

         val view : View = findViewById(R.id.title)

        //Connect to the ESP-32 sensor controller over Bluetooth

        //1. Check the BT is turned on
        if(!mBtAdapter.isEnabled()) {
            Snackbar.make(view,"Bluetooth is not enabled", Snackbar.LENGTH_LONG).show()
        }
        val pairedDevices = mBtAdapter.bondedDevices

        if (pairedDevices.size > 0) {
            for (mdevice in pairedDevices) {
                if (mdevice.name == "Smart Shrimper") {
                    Snackbar.make(view,"Found paired Bluetooth device", Snackbar.LENGTH_LONG).show()
                    mBTdevice = mdevice
                    controllerFound = true

                }
                if ( controllerFound == false ) {
                    Snackbar.make(view,"Smart Shrimper controller has not been paired", Snackbar.LENGTH_LONG).show()
                    finishAffinity()
                }
            }
        }
        if (controllerFound == true) {
            //Set up a handler to handle incoming Messages from the BT Thread
            val myBThandler = object:  Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    val mtext  = msg.toString()

                    if (msg.what == BT_MSG.TOAST.ordinal) {Snackbar.make(view,"Could not connect to controller", 6000).show()}
                    else {
                        // get JSONObject from the message
                        val obj = JSONObject(msg.data.getByteArray("ShrimperData").toString())
                        // fetch JSONObject named RPM
                        shrimperData.rpm = obj.getString("RPM").toFloat()
                        shrimperData.batteryVoltage = obj.getString("Battery Voltage").toFloat()
                        shrimperData.relayStatus = obj.getString("Relay Status").toBoolean()
                    }
                }
            }
            //Create and run the Bluetooth Thread
            val btService = MyBluetoothService(myBThandler,mBTdevice)
            btService.ConnectThread().start()
            Snackbar.make(view,"Connecting to the controller", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateGauges()
    }

    fun updateGauges() {

        while (true) {
            gaugeHandler.post(revCounter.moveToValue(shrimperData.rpm))
        }
    }
}

