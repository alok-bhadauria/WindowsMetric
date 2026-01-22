package com.example.windowsmetric

import java.util.UUID

object BleConstants {
    // Service UUID: 5A401569-F53B-4444-A512-FB7115852555
    val SERVICE_UUID: UUID = UUID.fromString("5A401569-F53B-4444-A512-FB7115852555")

    // Unlock Characteristic (Write/Indicate): 5A401569-F53B-4444-A512-FB7115852556
    val UNLOCK_CHAR_UUID: UUID = UUID.fromString("5A401569-F53B-4444-A512-FB7115852556")

    // Session Characteristic (Read/Notify): 5A401569-F53B-4444-A512-FB7115852557
    val SESSION_CHAR_UUID: UUID = UUID.fromString("5A401569-F53B-4444-A512-FB7115852557")

    // Command IDs
    const val CMD_UNLOCK_PIN: Byte = 0x01
    const val CMD_UNLOCK_BIO: Byte = 0x02

    // Status IDs
    const val STATUS_LOCKED: Byte = 0x00
    const val STATUS_UNLOCKED: Byte = 0x01
}
