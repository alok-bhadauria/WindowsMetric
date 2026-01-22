#pragma once
#include <string>
#include <windows.h>

namespace WindowsMetric {
    class InputSimulator {
    public:
        // Converts the string into keystrokes
        static void UnlockWithPin(std::string pin);

    private:
        static void SendKey(WORD wVk);
        static void SendKey(WORD wVk, bool down);
        static void SendChar(char c);
    };
}
