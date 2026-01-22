package com.example.windowsmetric

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

// Matches RfcommServer.cpp
val RFCOMM_UUID: UUID = UUID.fromString("5A401569-F53B-4444-A512-FB7115852555")

enum class AuthState {
    None,
    Authenticated,
    Failed
}

@SuppressLint("MissingPermission")
class BluetoothClientManager(context: Context) {
    
    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    
    private val _statusMessage = MutableStateFlow("Idle")
    val statusMessage: StateFlow<String> = _statusMessage

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _authState = MutableStateFlow(AuthState.None)
    val authState: StateFlow<AuthState> = _authState

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting

    fun getPairedDevices(): List<BluetoothDevice> {
        return adapter?.bondedDevices?.toList() ?: emptyList()
    }

    fun connect(device: BluetoothDevice) {
        _isConnecting.value = true
        _statusMessage.value = "Connecting to ${device.name}..."
        
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Cancel discovery as it slows down connection
                adapter?.cancelDiscovery()

                socket = device.createRfcommSocketToServiceRecord(RFCOMM_UUID)
                socket?.connect()

                _isConnected.value = true
                _isConnecting.value = false
                _statusMessage.value = "Connected! Enter PIN."
                
                listenForData()
            } catch (e: IOException) {
                Log.e("BTClient", "Connection failed", e)
                _isConnecting.value = false
                _statusMessage.value = "Connection Failed: ${e.message}"
                close()
            }
        }
    }

    fun sendPin(pin: String) {
        send("AUTH:$pin\n")
    }

    fun sendLock() {
        send("CMD:LOCK\n")
    }

    fun sendUnlock() {
        send("CMD:UNLOCK\n")
    }

    private fun send(msg: String) {
        GlobalScope.launch(Dispatchers.IO) {
             try {
                 socket?.outputStream?.write(msg.toByteArray())
                 socket?.outputStream?.flush()
             } catch (e: Exception) {
                 _statusMessage.value = "Send Failed"
             }
        }
    }

    private val _cpuUsage = MutableStateFlow(0)
    val cpuUsage: StateFlow<Int> = _cpuUsage
    
    private val _ramUsage = MutableStateFlow(0)
    val ramUsage: StateFlow<Int> = _ramUsage

    // ... (Existing Connect/Send methods) ...

    fun sendUnlock(pin: String) {
        send("CMD:UNLOCK:$pin\n")
    }

    private fun listenForData() {
        try {
            val buffer = ByteArray(1024)
            while (_isConnected.value) {
                try {
                    val bytes = socket?.inputStream?.read(buffer) ?: -1
                    if (bytes > 0) {
                        val msg = String(buffer, 0, bytes).trim()
                        
                        if (msg.contains("METRICS:")) {
                            // METRICS:CPU=12;RAM=45
                            try {
                                val parts = msg.split(":", ";", "=")
                                // Rough parsing: [METRICS, CPU, 12, RAM, 45]
                                // Better: Split by ; then =
                                val metricPart = msg.substringAfter("METRICS:") // CPU=12;RAM=45
                                val metrics = metricPart.split(";")
                                for (m in metrics) {
                                    val kv = m.split("=")
                                    if (kv.size == 2) {
                                        if (kv[0] == "CPU") _cpuUsage.value = kv[1].toInt().coerceIn(0, 100)
                                        if (kv[0] == "RAM") _ramUsage.value = kv[1].toInt().coerceIn(0, 100)
                                    }
                                }
                            } catch (e: Exception) { Log.e("BTClient", "Parse Error", e) }
                        }
                        else if (msg.contains("AUTH:OK")) {
                            _authState.value = AuthState.Authenticated
                            _statusMessage.value = "Authenticated & Ready"
                        } 
                        else if (msg.contains("AUTH:FAIL")) {
                            _authState.value = AuthState.Failed
                            _statusMessage.value = "Wrong PIN!"
                        }
                        else {
                            Log.d("BTClient", "RX: $msg")
                        }
                    } else {
                        close()
                        break
                    }
                } catch (e: IOException) {
                    close()
                    break
                }
            }
        } catch (e: Exception) {
            Log.e("BTClient", "ListenForData crashed", e)
            close()
        }
    }

    fun close() {
        try {
            socket?.close()
        } catch (e: Exception) {}
        socket = null
        _isConnected.value = false
        _isConnecting.value = false
        _authState.value = AuthState.None
        _statusMessage.value = "Disconnected"
    }
}
