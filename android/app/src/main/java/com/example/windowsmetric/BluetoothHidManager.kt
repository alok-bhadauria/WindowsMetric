package com.example.windowsmetric

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executors

@SuppressLint("MissingPermission")
class BluetoothHidManager(private val context: Context) {

    private val TAG = "HidManager"
    private var hidDevice: BluetoothHidDevice? = null
    private var hostDevice: BluetoothDevice? = null
    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val _connectionState = MutableStateFlow("Disconnected")
    val connectionState: StateFlow<String> = _connectionState

    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered

    // Executor for callbacks
    private val executor = Executors.newSingleThreadExecutor()

    // 1. HID Report Map for Keyboard + Mouse (Standard Descriptor)
    // ... (Map is fine, keeping it same) ...
    private val REPORT_MAP = byteArrayOf(
        // Keyboard
        0x05.toByte(), 0x01.toByte(),       // Usage Page (Generic Desktop)
        0x09.toByte(), 0x06.toByte(),       // Usage (Keyboard)
        0xA1.toByte(), 0x01.toByte(),       // Collection (Application)
        0x85.toByte(), 0x01.toByte(),       //   Report ID (1)
        0x05.toByte(), 0x07.toByte(),       //   Usage Page (Key Codes)
        0x19.toByte(), 0xE0.toByte(),       //   Usage Minimum (224)
        0x29.toByte(), 0xE7.toByte(),       //   Usage Maximum (231)
        0x15.toByte(), 0x00.toByte(),       //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(),       //   Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(),       //   Report Size (1)
        0x95.toByte(), 0x08.toByte(),       //   Report Count (8)
        0x81.toByte(), 0x02.toByte(),       //   Input (Data, Variable, Absolute) - Modifier Byte
        0x95.toByte(), 0x01.toByte(),       //   Report Count (1)
        0x75.toByte(), 0x08.toByte(),       //   Report Size (8)
        0x81.toByte(), 0x01.toByte(),       //   Input (Constant) - Reserved Byte
        0x95.toByte(), 0x06.toByte(),       //   Report Count (6)
        0x75.toByte(), 0x08.toByte(),       //   Report Size (8)
        0x15.toByte(), 0x00.toByte(),       //   Logical Minimum (0)
        0x25.toByte(), 0x65.toByte(),       //   Logical Maximum (101)
        0x05.toByte(), 0x07.toByte(),       //   Usage Page (Key Codes)
        0x19.toByte(), 0x00.toByte(),       //   Usage Minimum (0)
        0x29.toByte(), 0x65.toByte(),       //   Usage Maximum (101)
        0x81.toByte(), 0x00.toByte(),       //   Input (Data, Array) - Key Arrays (6 keys)
        0xC0.toByte()                       // End Collection
    )

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.d(TAG, "onAppStatusChanged: registered=$registered")
            _isRegistered.value = registered
            
            if (registered) {
                // If a device was already connected/plugged, track it
                if (pluggedDevice != null) {
                   hostDevice = pluggedDevice
                   _connectionState.value = "Connected"
                } else {
                    _connectionState.value = "Disconnected"
                }
            } else {
                _connectionState.value = "Disconnected"
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            Log.d(TAG, "onConnectionStateChanged: state=$state")
            if (state == BluetoothProfile.STATE_CONNECTED) {
                hostDevice = device
                _connectionState.value = "Connected"
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                hostDevice = null
                _connectionState.value = "Disconnected"
            }
        }
    }

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = proxy as BluetoothHidDevice
                registerApp()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            hidDevice = null
            _isRegistered.value = false
            _connectionState.value = "Disconnected"
        }
    }

    fun initialize(): Boolean {
        if (adapter?.isEnabled == true) {
            val success = adapter.getProfileProxy(context, serviceListener, BluetoothProfile.HID_DEVICE)
            if (!success) {
                _connectionState.value = "Proxy Init Failed"
            }
            return success
        } else {
            _connectionState.value = "Bluetooth Off"
            return false
        }
    }

    private fun registerApp() {
        // Unregister first to ensure clean state (fixes "Other Device" issues)
        try {
            hidDevice?.unregisterApp()
        } catch (e: Exception) {
            Log.w(TAG, "Unregister failed (normal if not registered): ${e.message}")
        }

        val sdpSettings = BluetoothHidDeviceAppSdpSettings(
            "WindowsMetric Keyboard",
            "Android HID Provider",
            "WindowsMetric",
            BluetoothHidDevice.SUBCLASS1_KEYBOARD, // Explicitly Keyboard Only for now to force Icon
            REPORT_MAP
        )

        val qosSettings = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800, 9, 0, 11250, BluetoothHidDeviceAppQosSettings.MAX
        )

        hidDevice?.registerApp(
            sdpSettings,
            null,
            qosSettings,
            executor,
            callback
        )
    }

    fun connect(device: BluetoothDevice) {
        hidDevice?.connect(device)
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): Set<BluetoothDevice> {
        return adapter?.bondedDevices ?: emptySet()
    }

    @SuppressLint("MissingPermission")
    fun connectToAddress(address: String): Boolean {
        // Optimally, look it up in bonded devices to ensure we have the "live" object
        val device = adapter?.bondedDevices?.find { it.address == address } ?: adapter?.getRemoteDevice(address)
        
        if (device != null) {
            Log.d(TAG, "Connecting to saved device: ${device.name} ($address)")
            try {
                hidDevice?.connect(device)
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed: ${e.message}")
            }
        } else {
            Log.e(TAG, "Device not found for address: $address")
        }
        return false
    }

    @SuppressLint("MissingPermission")
    fun connectToPairedHost(): Boolean {
        val bondedDevices = adapter?.bondedDevices
        if (!bondedDevices.isNullOrEmpty()) {
            var attempted = false
            for (device in bondedDevices) {
                try {
                    Log.d(TAG, "Fallback connecting to: ${device.name}")
                    hidDevice?.connect(device)
                    attempted = true
                } catch (e: Exception) {
                    Log.e(TAG, "Sync Connect failed", e)
                }
            }
            return attempted
        }
        return false
    }

    // --- Input Methods ---

    // Send a string as keystrokes
    fun sendString(text: String): Boolean {
        if (hostDevice == null) return false
        
        for (char in text) {
            sendChar(char)
            try {
                Thread.sleep(20) // Small delay for stability
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        return true
    }

    private fun sendChar(char: Char) {
        val (modifier, keycode) = charToHid(char)
        sendReport(modifier, keycode)
        sendReport(0, 0) // Key Up
    }

    // Very basic mapping for demo (Expand for full ASCII)
    private fun charToHid(c: Char): Pair<Byte, Byte> {
        val code = c.code
        // a-z
        if (code in 97..122) return Pair(0, (code - 93).toByte())
        // A-Z
        if (code in 65..90) return Pair(2, (code - 61).toByte()) // 2 = Left Shift
        // Numbers 1-9, 0
        if (code in 49..57) return Pair(0, (code - 19).toByte()) // 1-9
        if (code == 48) return Pair(0, 39) // 0
        // Special
        if (c == ' ') return Pair(0, 44)
        if (c == '\n') return Pair(0, 40) // Enter
        if (c == '@') return Pair(2, 31) // Shift + 2
        
        // Default to space if unknown
        return Pair(0, 44)
    }

    // Send Lock Command (Win + L)
    fun sendLockCommand(): Boolean {
        if (hostDevice == null) return false
        
        // Modifier: 0x08 (Left GUI / Windows Key)
        // Keycode: 0x0F ('l')
        sendReport(0x08.toByte(), 0x0F.toByte())
        
        // Release
        sendReport(0, 0)
        return true
    }

    fun sendCtrlA(): Boolean {
        if (hostDevice == null) return false
        // Modifier: 0x01 (Left Ctrl)
        // Keycode: 0x04 ('a')
        sendReport(0x01.toByte(), 0x04.toByte())
        sendReport(0, 0)
        return true
    }

    fun sendBackspace(): Boolean {
        if (hostDevice == null) return false
        // Modifier: 0
        // Keycode: 0x2A (Backspace)
        sendReport(0, 0x2A.toByte())
        sendReport(0, 0)
        return true
    }

    private fun sendReport(modifier: Byte, keycode: Byte) {
        // Report ID: 1 (Keyboard)
        val report = ByteArray(8)
        report[0] = modifier
        report[1] = 0 // Reserved
        report[2] = keycode
        
        hidDevice?.sendReport(hostDevice, 1, report)
    }
}
