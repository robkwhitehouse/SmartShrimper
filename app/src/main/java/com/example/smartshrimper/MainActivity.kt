package com.example.smartshrimper


import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.example.smartshrimper.ui.main.Gauge
import com.example.smartshrimper.ui.main.SectionsPagerAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.delay



interface SendChannel<in E> {
    suspend fun send(element: E)
    fun close(): Boolean
}

interface ReceiveChannel<out E> {
    suspend fun receive(): E
}

interface Channel<E> : SendChannel<E>, ReceiveChannel<E>

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mBtAdapter = BluetoothAdapter.getDefaultAdapter()
        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)
        val fab: FloatingActionButton = findViewById(R.id.fab)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()

        }
        //Connect to Sensor controller over Bluetooth

        val pairedDevices = mBtAdapter.bondedDevices

        if (pairedDevices.size > 0) {
            for (mdevice in pairedDevices) {
                if (mdevice.name == "Smart Shrimper") {
                    val view : View = findViewById(R.id.title)
                    Snackbar.make(view,"Found paired Bluetooth device", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show()
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()

         suspend fun updateGauges() {
             while (true) {
 //
     //
         //
             //                revCounter.moveToValue(streamBuffer.readNext().toFloat())
                 delay(200L)
             }
         }
    }
}