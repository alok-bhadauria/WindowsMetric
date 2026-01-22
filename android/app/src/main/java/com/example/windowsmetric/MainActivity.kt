package com.example.windowsmetric

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.windowsmetric.ui.theme.WindowsMetricTheme

class MainActivity : ComponentActivity() {
    private lateinit var btManager: BluetoothClientManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        btManager = BluetoothClientManager(this)

        setContent {
            WindowsMetricTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BluetoothScreen(
                        btManager = btManager,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        btManager.close()
    }
}

@Composable
fun BluetoothScreen(btManager: BluetoothClientManager, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val status by btManager.statusMessage.collectAsState()
    val isConnected by btManager.isConnected.collectAsState()
    val isConnecting by btManager.isConnecting.collectAsState()
    val authState by btManager.authState.collectAsState()
    val cpu by btManager.cpuUsage.collectAsState()
    val ram by btManager.ramUsage.collectAsState()
    
    var sessionPinText by remember { mutableStateOf("") }
    var windowsPinText by remember { mutableStateOf("") }
    
    // ... Permissions Code ...

    // Permissions
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Status: $status", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))

        if (!isConnected) {
            Button(onClick = { permissionLauncher.launch(permissionsToRequest) }) {
                Text("Refresh Permissions")
            }
            Spacer(Modifier.height(16.dp))
            
            Text("Select Paired PC:", style = MaterialTheme.typography.titleMedium)
            
            val devices = remember { btManager.getPairedDevices() }
            LazyColumn {
                items(devices) { device ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(enabled = !isConnecting && !isConnected) { btManager.connect(device) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = when {
                                isConnecting -> "Connecting..."
                                isConnected -> "Connected"
                                else -> device.name ?: "Unknown"
                            },
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        } else if (authState != AuthState.Authenticated) {
            Text("Enter SESSION PIN from Terminal:", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = sessionPinText,
                onValueChange = { sessionPinText = it },
                label = { Text("Session PIN") }
            )
            Button(onClick = { btManager.sendPin(sessionPinText) }) { Text("Verify") }
        } else {
            // --- DASHBOARD ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                // CPU Gauge
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(progress = cpu / 100f, color = MaterialTheme.colorScheme.primary)
                    Text("CPU: $cpu%")
                }
                // RAM Gauge
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(progress = ram / 100f, color = MaterialTheme.colorScheme.secondary)
                    Text("RAM: $ram%")
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            // --- CONTROLS ---
            Text("Windows Security", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = windowsPinText,
                onValueChange = { windowsPinText = it },
                label = { Text("Windows PIN (to Unlock)") },
                placeholder = { Text("e.g. 1234") },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password)
            )
            
            Spacer(Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { btManager.sendLock() }) { Text("LOCK") }
                Button(onClick = { btManager.sendUnlock(windowsPinText) }) { Text("UNLOCK") }
            }
            
            Spacer(Modifier.height(32.dp))
            Button(onClick = { btManager.close() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Disconnect")
            }
        }
    }
}