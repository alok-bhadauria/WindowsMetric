# WindowsMetric

WindowsMetric is a cross-platform project bridging Windows (C++) and Android (Kotlin) via Bluetooth Low Energy (BLE).

## Structure

*   `windows/`: C++20 project for Windows 11 using WinRT and CMake.
*   `android/`: Kotlin project for Android (API 26+) using Jetpack Compose.
*   `shared/`: Shared protocols, documentation, and constants.

## Getting Started

Please see `guide/project-guide.txt` (or the `project-raw` folder) for detailed setup instructions.

WindowsMetric - RFCOMM Phase
Starting RFCOMM Server...
Keep window open. Press Ctrl+C to exit.
RFCOMM Advertising Started. Connect via Android App.

[Client Connected]
************************
** SESSION PIN: 1989 **
************************
RX: AUTH:1989
>> AUTH SUCCESS
RX: CMD:LOCK
>> LOCKING...
RX: CMD:UNLOCK:Anshu@Neeraj
>> UNLOCK REQUEST with PIN: Anshu@Neeraj
[InputSimulator] Waking screen...
[Error] SendInput Blocked! (Error: 5)
[Error] SendInput Blocked! (Error: 5)
[InputSimulator] Typing PIN...
[Error] SendInput Blocked! (Error: 5)
[Error] SendInput Blocked! (Error: 5)
[Error] SendInput Blocked! (Error: 5)
[Error] SendInput Blocked! (Error: 5)
[Error] SendInput Blocked! (Error: 5)
[Error] SendInput Blocked! (Error: 5)
[Error] SendInput Blocked! (Error: 5)
[Error] SendInput Blocked! (Error: 5)
[Error] SendInput Blocked! (Error: 5)
[InputSimulator] Sending Enter...
[Error] SendInput Blocked! (Error: 5)
[Error] SendInput Blocked! (Error: 5)
RX: CMD:UNLOCK:Anshu@Neeraj
>> UNLOCK REQUEST with PIN: Anshu@Neeraj
[InputSimulator] Waking screen...
[Error] SendInput Blocked! (Error: 5)
[Error] SendInput Blocked! (Error: 5)
[Error] SendInput Blocked! (Error: 5)
[Error] SendInput Blocked! (Error: 5)
[InputSimulator] Typing PIN...
[Error] SendInput Blocked! (Error: 5)
[Error] SendInput Blocked! (Error: 5)
[Error] SendInput Blocked! (Error: 5)
[InputSimulator] Sending Enter..

Now our error / problem is very clear : We cant type (input) the windows pin/password this way. Need a fix or workaround, whats your plan ?