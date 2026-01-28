package com.example.windowsmetric

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.windowsmetric.ui.theme.WindowsMetricTheme
import kotlinx.coroutines.launch
import android.annotation.SuppressLint

class MainActivity : androidx.fragment.app.FragmentActivity() {
    private lateinit var btManager: BluetoothHidManager
    private var savedWindowsPin: String = "" // In real app, use EncryptedSharedPreferences
    private var targetDeviceAddress: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        btManager = BluetoothHidManager(this)
        
        val prefs = getPreferences(MODE_PRIVATE)
        // savedWindowsPin = getPreferences(MODE_PRIVATE).getString("WIN_PIN", "") ?: ""
        savedWindowsPin = prefs.getString("WIN_PIN", "") ?: ""
        targetDeviceAddress = prefs.getString("TARGET_DEVICE", "") ?: ""

        setContent {
            WindowsMetricTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        btManager = btManager,
                        modifier = Modifier.padding(innerPadding),
                        initialTargetAddress = targetDeviceAddress,
                        onTargetSelected = { address ->
                            targetDeviceAddress = address
                            prefs.edit().putString("TARGET_DEVICE", address).apply()
                        },
                        onClearTarget = {
                            targetDeviceAddress = ""
                            prefs.edit().remove("TARGET_DEVICE").apply()
                        },
                        onUnlockRequest = { authenticateAndUnlock() },
                        onSavePin = { pin -> 
                            savedWindowsPin = pin 
                            prefs.edit().putString("WIN_PIN", pin).apply()
                            Toast.makeText(this, "PIN Saved Securely", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
    
    private fun authenticateAndUnlock() {
        if (savedWindowsPin.isEmpty()) {
            Toast.makeText(this, "Please Setup Windows PIN first", Toast.LENGTH_SHORT).show()
            return
        }

        val executor = androidx.core.content.ContextCompat.getMainExecutor(this)
        val biometricPrompt = androidx.biometric.BiometricPrompt(this, executor,
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    
                    // The "Barcode Scanner" Magic: Just type the PIN + Enter
                    lifecycleScope.launch {
                        Toast.makeText(applicationContext, "Unlocking...", Toast.LENGTH_SHORT).show()
                        
                        // 1. Wake (Space)
                        btManager.sendString(" ")
                        kotlinx.coroutines.delay(2000)
                        
                        // 2. Lift Curtain (Space again)
                        btManager.sendString(" ")
                         // 4. Wait for Animation
                        kotlinx.coroutines.delay(500)

                        // 3. Clean Field (Ctrl + A -> Backspace)
                        btManager.sendCtrlA()
                        kotlinx.coroutines.delay(50)
                        btManager.sendBackspace()
                        kotlinx.coroutines.delay(50)

                        // 4. Type PIN
                        val success = btManager.sendString(savedWindowsPin + "\n")
                         if (success) {
                            Toast.makeText(applicationContext, "Credentials Sent", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(applicationContext, "Not Connected to Device", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Auth Error: $errString", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock PC")
            .setSubtitle("Confirm to inject credentials")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

@Composable
fun MainScreen(
    btManager: BluetoothHidManager,
    modifier: Modifier = Modifier,
    initialTargetAddress: String,
    onTargetSelected: (String) -> Unit,
    onClearTarget: () -> Unit,
    onUnlockRequest: () -> Unit,
    onSavePin: (String) -> Unit
) {
    var targetAddress by remember { mutableStateOf(initialTargetAddress) }
    
    // Pass state up
    LaunchedEffect(targetAddress) {
        if (targetAddress != initialTargetAddress && targetAddress.isNotEmpty()) {
             onTargetSelected(targetAddress)
        }
    }

    if (targetAddress.isEmpty()) {
        SetupScreen(btManager, modifier, onDeviceSelected = { 
            targetAddress = it.address 
            onTargetSelected(it.address)
        })
    } else {
        DashboardScreen(
            btManager, 
            modifier, 
            targetAddress, 
            onUnlockRequest, 
            onSavePin,
            onChangeDevice = { 
                targetAddress = ""
                onClearTarget()
            }
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
fun SetupScreen(
    btManager: BluetoothHidManager,
    modifier: Modifier,
    onDeviceSelected: (android.bluetooth.BluetoothDevice) -> Unit
) {
    val context = LocalContext.current
    var pairedDevices by remember { mutableStateOf(emptySet<android.bluetooth.BluetoothDevice>()) }
    
    // Permissions for HID (Advertise is needed to be discoverable as a keyboard)
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE)
    } else {
        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
    }
    
    val hasPermissions = permissionsToRequest.all {
         ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    // Init HID for listing
    LaunchedEffect(hasPermissions) {
        if (hasPermissions) {
            btManager.initialize()
            pairedDevices = btManager.getPairedDevices()
        }
    }
    
    // Refresh list occasionally or on resume
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
             if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                 if (hasPermissions) {
                    btManager.initialize()
                    pairedDevices = btManager.getPairedDevices()
                 }
             }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Text("Setup Windows Metric", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Select your Windows PC from the list below. If it's not listed, pair it in Android Bluetooth Settings first.", style = MaterialTheme.typography.bodyMedium)
        
        Spacer(Modifier.height(24.dp))

        if (!hasPermissions) {
             Button(onClick = { permissionLauncher.launch(permissionsToRequest) }) {
                Text("Grant HID Permissions")
            }
            Spacer(Modifier.height(16.dp))
        }
        
        // Pairing Mode Button
        Button(
            onClick = { 
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                    putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                }
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Make Discoverable (Recommended)")
        }
        Spacer(Modifier.height(8.dp))
        Text("Use this if your PC can't see your phone.", style = MaterialTheme.typography.labelSmall)
        
        Spacer(Modifier.height(24.dp))
        Text("Paired Devices:", style = MaterialTheme.typography.titleMedium)
        
        LazyColumn {
            items(pairedDevices.toList()) { device ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onDeviceSelected(device) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(device.name ?: "Unknown", style = MaterialTheme.typography.titleMedium)
                        Text(device.address, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    btManager: BluetoothHidManager,
    modifier: Modifier,
    targetAddress: String,
    onUnlockRequest: () -> Unit,
    onSavePin: (String) -> Unit,
    onChangeDevice: () -> Unit
) {
    val context = LocalContext.current
    val connectionState by btManager.connectionState.collectAsState()
    val isRegistered by btManager.isRegistered.collectAsState()
    var pinText by remember { mutableStateOf("") }
    
    // Auto-Connect Logic
    LaunchedEffect(isRegistered, connectionState) {
        if (isRegistered && connectionState != "Connected") {
            btManager.connectToAddress(targetAddress)
        }
    }
    
    // Ensure Init
    LaunchedEffect(Unit) {
        btManager.initialize()
    }
    
    // Status text
    // Status text
    val statusText = when {
        connectionState == "Connected" -> "Connected"
        !isRegistered -> "Initializing..."
        connectionState == "Disconnected" -> "Disconnected"
        else -> connectionState // "Connecting..." or others
    }
    val statusColor = if (connectionState == "Connected") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Top Bar Area
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Windows Metric", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onChangeDevice) {
                Text("Change Device")
            }
        }
        
        Spacer(Modifier.height(8.dp))
        Spacer(Modifier.height(8.dp))
        Text("Target: $targetAddress", style = MaterialTheme.typography.bodySmall)
        
        // Status & Manual Connect
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
             Text("Status: $statusText", style = MaterialTheme.typography.titleMedium, color = statusColor)
             
             if (connectionState == "Disconnected" && isRegistered) {
                 Spacer(Modifier.height(8.dp))
                 Button(
                     onClick = { btManager.connectToAddress(targetAddress) },
                     colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                     modifier = Modifier.height(36.dp)
                 ) {
                     Text("Reconnect")
                 }
             }
        }
        
        Spacer(Modifier.weight(1f))
        
        // BIG UNLOCK BUTTON
        Button(
            onClick = { onUnlockRequest() },
            modifier = Modifier.size(200.dp).padding(16.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Icon (using Text for now, use Icon in real app)
                Text("🔓", style = MaterialTheme.typography.displayMedium)
                Text("UNLOCK", style = MaterialTheme.typography.titleLarge)
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Lock Button
        Button(
            onClick = { 
                val success = btManager.sendLockCommand() 
                if (!success) {
                    Toast.makeText(context, "Not Connected to Device", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("LOCK PC", style = MaterialTheme.typography.headlineSmall)
        }
        
        Spacer(Modifier.height(8.dp))
        Text("Requires 'Windows Metric Keyboard' paired", style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.weight(1f))
        
        // PIN Setup (Small, at bottom)
        var showPinSetup by remember { mutableStateOf(false) }
        if (showPinSetup) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = pinText, 
                        onValueChange = { pinText = it },
                        label = { Text("New PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onSavePin(pinText); showPinSetup = false }, modifier = Modifier.align(Alignment.End)) {
                        Text("Save")
                    }
                }
            }
        } else {
             TextButton(onClick = { showPinSetup = true }) {
                Text("Update / Set Windows PIN")
            }
        }
    }
}