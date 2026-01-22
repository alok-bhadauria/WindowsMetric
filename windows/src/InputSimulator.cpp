#include "InputSimulator.h"
#include <iostream>
#include <vector>

namespace WindowsMetric {

    void InputSimulator::UnlockWithPin(std::string pin) {
        std::cout << "[InputSimulator] Waking screen..." << std::endl;
        
        // 1. Wake up / Dismiss Lock Screen Curtain
        // A single Enter sometimes isn't enough if screen is off.
        SendKey(VK_SPACE); 
        Sleep(1000); // Wait for screen to wake
        
        SendKey(VK_SPACE); // Press again to dismiss curtain
        Sleep(1000); // Wait for animation to finish and focus input

        std::cout << "[InputSimulator] Typing PIN..." << std::endl;

        // 2. Type PIN
        for (char c : pin) {
            SendChar(c);
            Sleep(80); // Increasing delay slightly for reliability
        }

        // 3. Press Enter
        std::cout << "[InputSimulator] Sending Enter..." << std::endl;
        Sleep(500);
        SendKey(VK_RETURN);
    }

    void InputSimulator::SendChar(char c) {
        SHORT vkCode = VkKeyScanA(c);
        if (vkCode == -1) return;

        BYTE virtualKey = LOBYTE(vkCode);
        BYTE shiftState = HIBYTE(vkCode);

        // Check if Shift is required (Bit 0 of high byte)
        bool needShift = (shiftState & 1) != 0;

        if (needShift) {
            SendKey(VK_SHIFT, true); // Press Shift
        }

        SendKey(virtualKey, true);  // Press Key
        SendKey(virtualKey, false); // Release Key

        if (needShift) {
            SendKey(VK_SHIFT, false); // Release Shift
        }
    }

    void InputSimulator::SendKey(WORD wVk, bool down) {
        INPUT input = {};
        input.type = INPUT_KEYBOARD;
        
        // Convert VK to Scan Code
        input.ki.wScan = MapVirtualKey(wVk, MAPVK_VK_TO_VSC);
        input.ki.dwFlags = KEYEVENTF_SCANCODE; // Hardware Scan Code
        
        // Extended keys (Insert, Delete, Home, End, Arrows, etc.) need the Extended flag
        // A simple heuristic: if it's not a standard range, flag it. 
        // But for standard PIN characters (0-9, A-Z, Shift), standard scancodes are fine.
        
        if (!down) {
            input.ki.dwFlags |= KEYEVENTF_KEYUP;
        }

        UINT sent = SendInput(1, &input, sizeof(INPUT));
        if (sent == 0) {
            std::cout << "[Error] SendInput Blocked! (Error: " << GetLastError() << ")" << std::endl;
        }
    }
    
    // For legacy single-call compatibility (Wait/Press/Release)
    void InputSimulator::SendKey(WORD wVk) {
        SendKey(wVk, true);
        SendKey(wVk, false);
    }
}
