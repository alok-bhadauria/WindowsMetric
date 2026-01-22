# Shared BLE Protocol

## UUIDs

| Name | UUID | Type | Description |
| :--- | :--- | :--- | :--- |
| **Service** | `5A401569-F53B-4444-A512-FB7115852555` | Service | The main WindowsMetric service. |
| **Unlock Char** | `5A401569-F53B-4444-A512-FB7115852556` | Write / Indicate | **Phone -> PC**: Send Auth Challenge / PIN.<br>**PC -> Phone**: Ack / Status. |
| **Session Char** | `5A401569-F53B-4444-A512-FB7115852557` | Read / Notify | **PC -> Phone**: Updates on session status (Locked/Unlocked). |

## Data Packets

### 1. Unlock Request (Phone -> PC)
Written to **Unlock Char**.
*   **Format**: `[CommandID: 1 byte] [Data: Variable]`
*   **Command IDs**:
    *   `0x01`: Unlock with PIN.
    *   `0x02`: Unlock with Biometric (Signature).

### 2. Session Status (PC -> Phone)
Notified via **Session Char**.
*   **Format**: `[StatusID: 1 byte]`
*   **Status IDs**:
    *   `0x00`: Locked
    *   `0x01`: Unlocked
