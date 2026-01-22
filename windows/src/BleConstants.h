#pragma once
#include <winrt/Windows.Foundation.h>

namespace WindowsMetric {
    // Service UUID: 5A401569-F53B-4444-A512-FB7115852555
    static const winrt::guid SERVICE_UUID{ 
        0x5A401569, 0xF53B, 0x4444, { 0xA5, 0x12, 0xFB, 0x71, 0x15, 0x85, 0x25, 0x55 } 
    };

    // Unlock Characteristic (Write/Indicate): 5A401569-F53B-4444-A512-FB7115852556
    static const winrt::guid UNLOCK_CHAR_UUID{ 
        0x5A401569, 0xF53B, 0x4444, { 0xA5, 0x12, 0xFB, 0x71, 0x15, 0x85, 0x25, 0x58 } 
    };

    // Session Characteristic (Read/Notify): 5A401569-F53B-4444-A512-FB7115852557
    static const winrt::guid SESSION_CHAR_UUID{ 
        0x5A401569, 0xF53B, 0x4444, { 0xA5, 0x12, 0xFB, 0x71, 0x15, 0x85, 0x25, 0x57 } 
    };

    // Command IDs
    constexpr uint8_t CMD_UNLOCK_PIN = 0x01;
    constexpr uint8_t CMD_UNLOCK_BIO = 0x02;
    constexpr uint8_t CMD_LOCK = 0x03;

    // Status IDs
    constexpr uint8_t STATUS_LOCKED = 0x00;
    constexpr uint8_t STATUS_UNLOCKED = 0x01;
}
