package com.example.windowsmetric

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

// Define UUIDs matching BleConstants.h
object BleUuids {
    val SERVICE_UUID: UUID = UUID.fromString("5A401569-F53B-4444-A512-FB7115852555")
    val UNLOCK_CHAR_UUID: UUID = UUID.fromString("5A401569-F53B-4444-A512-FB7115852558")
}

@SuppressLint("MissingPermission") // Permissions are handled in UI before calling these
class BleClientManager(context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter

    private var gatt: BluetoothGatt? = null

    // State
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _statusMessage = MutableStateFlow("Idle")
    val statusMessage: StateFlow<String> = _statusMessage

    // Scanning
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return
            val name = device.name ?: "Unknown"
            // Log.d("BleClient", "Scanned: $name - ${device.address}") // Optional logging

            // CHECK: Match by Name OR UUID (Manual check is more robust for debugging)
            val matchByName = name.contains("ALOK_LAPTOP", ignoreCase = true)
            
            // Check UUID manually from scanRecord
            val serviceUuids = result.scanRecord?.serviceUuids
            val matchByUuid = serviceUuids?.any { it.uuid == BleUuids.SERVICE_UUID } == true

            if (matchByName || matchByUuid) {
                Log.d("BleClient", "Found Target: $name")
                stopScan()
                _statusMessage.value = "Found $name. Connecting..."
                connectToDevice(context, device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _statusMessage.value = "Scan failed: $errorCode"
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    fun startScan() {
        if (adapter == null || !adapter.isEnabled) {
            _statusMessage.value = "Bluetooth disabled"
            return
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _statusMessage.value = "BLE Scanner unavailable"
            return
        }

        // REMOVED FILTER: Scan everything to ensure we don't miss it due to packet formatting
        // val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(BleUuids.SERVICE_UUID)).build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        _statusMessage.value = "Scanning (No Filter)..."
        _connectionState.value = ConnectionState.Scanning
        
        // Stop scan after 10s if not found
        Handler(Looper.getMainLooper()).postDelayed({
            if (_connectionState.value == ConnectionState.Scanning) {
                stopScan()
                _statusMessage.value = "Device not found (Timeout)."
                _connectionState.value = ConnectionState.Disconnected
            }
        }, 10000)

        // Pass empty list for filters
        scanner.startScan(null, settings, scanCallback)
    }

    fun stopScan() {
        adapter?.bluetoothLeScanner?.stopScan(scanCallback)
    }

    private fun connectToDevice(context: Context, device: BluetoothDevice) {
        _connectionState.value = ConnectionState.Connecting
        // Use TRANSPORT_LE to ensure stable Low Energy connection
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            gatt = device.connectGatt(context, false, gattCallback)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BleClient", "Connected to GATT server. Status: $status")
                _statusMessage.value = "Connected. Discovering Services..."
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w("BleClient", "Disconnected. Status: $status (133=GattError, 8=Timeout, 19=Terminate)")
                _connectionState.value = ConnectionState.Disconnected
                _statusMessage.value = "Disconnected (Status: $status)"
                this@BleClientManager.gatt?.close()
                this@BleClientManager.gatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(BleUuids.SERVICE_UUID)
                if (service != null) {
                    _statusMessage.value = "Ready to Unlock"
                    _connectionState.value = ConnectionState.Connected
                } else {
                    _statusMessage.value = "Service not found!"
                    disconnect()
                }
            } else {
                Log.w("BleClient", "onServicesDiscovered received: $status")
            }
        }
        
        // Handle Characteristic Write confirmation if needed
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
             if (status == BluetoothGatt.GATT_SUCCESS) {
                 _statusMessage.value = "Unlock Command Sent!"
             } else {
                 _statusMessage.value = "Write Failed: $status"
             }
        }
    }

    fun sendCommand(commandId: Byte) {
        val service = gatt?.getService(BleUuids.SERVICE_UUID)
        val characteristic = service?.getCharacteristic(BleUuids.UNLOCK_CHAR_UUID)

        if (characteristic != null) {
            characteristic.value = byteArrayOf(commandId)
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            
            @Suppress("DEPRECATION")
            gatt?.writeCharacteristic(characteristic)
            _statusMessage.value = "Sending Command: $commandId..."
        } else {
             _statusMessage.value = "Char not found"
        }
    }

    fun sendUnlockCommand() {
        sendCommand(0x01)
    }

    fun sendLockCommand() {
        sendCommand(0x03)
    }

    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        _connectionState.value = ConnectionState.Disconnected
    }
}

enum class ConnectionState {
    Disconnected,
    Scanning,
    Connecting,
    Connected
}
