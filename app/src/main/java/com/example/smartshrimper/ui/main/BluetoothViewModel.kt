package com.example.smartshrimper.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartshrimper.BluetoothStreamWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BluetoothViewModel(

        private val bluetoothStreamWorker: BluetoothStreamWorker
): ViewModel() {

    private val simulator = true
    private var rpmVal = 1000

    fun connect(BTdeviceName: String) {
        // Create a new coroutine to move the execution off the UI thread
        viewModelScope.launch(Dispatchers.IO) {
            //Make BT connection
        }
    }

    fun lastValue() :Int {
        if (simulator == true) {
            viewModelScope.launch { // launch new coroutine in background and continue
                delay(200L) // non-blocking delay for 1 second (default time unit is ms)
                rpmVal += 10
                return@launch (rpmVal)
            }
        } else
        // Create a new coroutine to move the execution off the UI thread
                viewModelScope.launch(Dispatchers.IO) {
                    //
                }
    }
}
