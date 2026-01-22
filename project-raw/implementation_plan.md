# Implementation Plan - Phase 2: Metrics & Real Unlock

## Goal Description
1.  **System Monitor**: Display real-time CPU/RAM usage on the Android app.
2.  **Simple Unlock**: Allow the Android app to send a saved Windows PIN to the PC, which simulates keystrokes to unlock the session (Simulated Input method).

## Protocol Updates
*   `METRICS:CPU=xx;RAM=yy` (Server -> Client)
*   `CMD:UNLOCK:<PIN>` (Client -> Server)

## Proposed Changes

### Windows (C++/WinRT)
#### [NEW] [MetricCollector.h / .cpp](file:///e:/Projects/WindowsMetric/windows/src/MetricCollector.h)
*   Uses Pdh (Performance Data Helper) for CPU.
*   Uses GlobalMemoryStatusEx for RAM.

#### [NEW] [InputSimulator.h / .cpp](file:///e:/Projects/WindowsMetric/windows/src/InputSimulator.h)
*   `UnlockWithPin(std::string pin)`
*   Uses `SendInput` to press Keys corresponding to the PIN + Enter.
*   *Note*: This works only if the Lock Screen focuses the password field (default behavior).

#### [MODIFY] [RfcommServer.cpp](file:///e:/Projects/WindowsMetric/windows/src/RfcommServer.cpp)
*   Integrate `MetricCollector` loop (1Hz).
*   Parse `CMD:UNLOCK:<PIN>` and call `InputSimulator`.

### Android (Kotlin)
#### [MODIFY] [BluetoothClientManager.kt](file:///e:/Projects/WindowsMetric/android/app/src/main/java/com/example/windowsmetric/BluetoothClientManager.kt)
*   Parse Metrics packets.
*   Function `sendUnlock(pin: String)`.

#### [MODIFY] [MainActivity.kt](file:///e:/Projects/WindowsMetric/android/app/src/main/java/com/example/windowsmetric/MainActivity.kt)
*   Add "Windows PIN" text field (saved in preferences ideally, state for now).
*   Add Circular Progress Indicators for CPU/RAM.

## Verification Plan
1.  **Metrics**: Verify numbers change on Android.
2.  **Unlock**:
    *   Lock PC (`CMD:LOCK`).
    *   Enter correct PIN in Android.
    *   Hit Unlock.
    *   Watch PC type key-by-key and unlock.
