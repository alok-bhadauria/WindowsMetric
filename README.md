# WindowsMetric: Android Biometric Unlock for Windows

**Unlock your Windows PC using your Android phone's fingerprint sensor.**

WindowsMetric turns your Android device into a **Bluetooth Human Interface Device (HID)**—specifically a keyboard. It uses your phone's biometrics (Fingerprint/Face Unlock) to securely authenticate you, then automatically types your Windows PIN/Password via Bluetooth to unlock your PC.

**NO Windows Client Required!** Use your phone as a pure hardware keyboard emulator.

## Features
*   **Zero-Install on Windows**: Uses standard Bluetooth HID profile. Your PC sees a generic Bluetooth keyboard.
*   **Secure**: Your PIN is stored securely in Android Keystore and only released after Biometric authentication.
*   **Convenient**: One-tap unlock. No more typing long passwords.

## How it Works
1.  **Pairing**: Pair your Android phone with your Windows PC via Bluetooth (just like a wireless keyboard).
2.  **Setup**: Open the app, enter your Windows PIN, and save it (secured by Biometrics).
3.  **Unlock**:
    *   Wake your PC.
    *   Open the App and tap "Unlock PC".
    *   Authenticate with Fingerprint.
    *   The App sends `Enter` -> `Types PIN` -> `Enter` to your PC instantly.

## Installation

### Android
1.  Go to the [**Releases**](../../releases) page.
2.  Download the latest `app-release.apk`.
3.  Install it on your Android Device.

### Windows
*   No installation required. Just ensure Bluetooth is On.

## Setup Guide
1.  **Phone**: Turn on Bluetooth.
2.  **PC**: Go to **Settings > Bluetooth & devices > Add device**.
3.  **Phone**: Open the WindowsMetric App. It should make itself discoverable as "WindowsMetric Keyboard".
4.  **Pair**: Select the device on your PC and pair them.
5.  **Configure App**:
    *   Open App.
    *   Tap **"Set Windows PIN"**.
    *   Enter your login PIN/Password.
    *   Confirm with Biometrics.
6.  **Test**: Lock your PC (`Win + L`) and try the **"Unlock"** button in the app!

## Requirements
*   **Android**: Android 8.0 (Oreo) or higher. Bluetooth capability.
*   **Windows**: Windows 10 or 11 with Bluetooth support.


