#include "BleServer.h"
#include "BleConstants.h"
#include <iostream>
#include <winrt/Windows.Storage.Streams.h>
#include <windows.h> // For LockWorkStation

using namespace winrt;
using namespace winrt::Windows::Devices::Bluetooth;
using namespace winrt::Windows::Devices::Bluetooth::GenericAttributeProfile;
using namespace winrt::Windows::Storage::Streams;

namespace WindowsMetric {

    BleServer::BleServer() {}

    Windows::Foundation::IAsyncAction BleServer::Start() {
        try {
            std::cout << "Starting basic BLE Server..." << std::endl;

            // 1. Create the Service Provider
            GattServiceProviderResult result = co_await GattServiceProvider::CreateAsync(SERVICE_UUID);

            if (result.Error() != BluetoothError::Success) {
                std::cout << "Error: Could not create service provider. Error Code: " << static_cast<int>(result.Error()) << std::endl;
                co_return; // In real code, handle failure
            }

            m_serviceProvider = result.ServiceProvider();
            std::cout << "Service Created Successfully." << std::endl;

            // 2. Create Characteristics
            bool charsCreated = co_await CreateCharacteristicsAsync();
            if (!charsCreated) {
                std::cout << "Failed to create characteristics." << std::endl;
                co_return; 
            }

            // 3. Start Advertising
            GattServiceProviderAdvertisingParameters advParams;
            advParams.IsConnectable(true);
            advParams.IsDiscoverable(true);

            m_serviceProvider.StartAdvertising(advParams);
            std::cout << "BLE Advertising Started! Waiting for connections..." << std::endl;
        }
        catch (hresult_error const& ex) {
            std::wcout << L"WinRT Exception: " << ex.message().c_str() << std::endl;
        }
    }

    Windows::Foundation::IAsyncOperation<bool> BleServer::CreateCharacteristicsAsync() {
        if (!m_serviceProvider) co_return false;

        auto service = m_serviceProvider.Service();

        // --- Unlock Characteristic (Write | Indicate) ---
        GattLocalCharacteristicParameters unlockParams;
        unlockParams.CharacteristicProperties(
            GattCharacteristicProperties::Write | 
            GattCharacteristicProperties::Indicate
        );
        unlockParams.WriteProtectionLevel(GattProtectionLevel::Plain);
        unlockParams.UserDescription(L"Unlock Request");
        
        auto unlockResult = co_await service.CreateCharacteristicAsync(UNLOCK_CHAR_UUID, unlockParams);
        if (unlockResult.Error() != BluetoothError::Success) {
            std::cout << "Failed to create Unlock Characteristic" << std::endl;
        } else {
            auto unlockChar = unlockResult.Characteristic();
            unlockChar.WriteRequested({ this, &BleServer::OnWriteRequested });
        }

        // --- Session Characteristic (Read | Notify) ---
        GattLocalCharacteristicParameters sessionParams;
        sessionParams.CharacteristicProperties(GattCharacteristicProperties::Read | GattCharacteristicProperties::Notify);
        sessionParams.UserDescription(L"Session Status");
        
        auto sessionResult = co_await service.CreateCharacteristicAsync(SESSION_CHAR_UUID, sessionParams);
        if (sessionResult.Error() != BluetoothError::Success) {
            std::cout << "Failed to create Session Characteristic" << std::endl;
        }

        co_return true;
    }

    void BleServer::OnWriteRequested(
        GattLocalCharacteristic const&,
        GattWriteRequestedEventArgs const& args)
    {
        // Get deferral immediately to hold the connection open
        auto deferral = args.GetDeferral();
        auto requestOp = args.GetRequestAsync();

        // Fire and forget coroutine
        [deferral, requestOp]() -> fire_and_forget {
            try {
                // Await request safely
                GattWriteRequest request{ nullptr };
                try {
                    request = co_await requestOp;
                }
                catch (...) {
                     std::cout << "[ERROR] Failed to await RequestAsync." << std::endl;
                     deferral.Complete();
                     co_return; 
                }

                if (!request) {
                    std::cout << "[ERROR] Request object is null." << std::endl;
                    deferral.Complete();
                    co_return;
                }

                // 1. Read Data
                uint8_t cmd = 0;
                try {
                    IBuffer buffer = request.Value();
                    auto reader = DataReader::FromBuffer(buffer);
                    if (reader.UnconsumedBufferLength() > 0) {
                        cmd = reader.ReadByte();
                        std::cout << ">> Command: " << (int)cmd << std::endl;
                    }
                } catch (...) {
                    std::cout << "[ERROR] Failed to read buffer." << std::endl;
                }

                // 2. Respond IMMEDIATELY if needed
                if (request.Option() == GattWriteOption::WriteWithResponse) {
                    try {
                        request.Respond();
                        std::cout << "[ACK] Response Sent." << std::endl;
                    }
                    catch (hresult_error const& ex) {
                         // 0x800710DF = ERROR_DEVICE_NOT_AVAILABLE (common if client disconnects fast)
                        std::wcout << L"[WARN] Respond failed (often harmless): " << ex.message().c_str() << std::endl;
                    }
                }

                // 3. Action
                if (cmd == CMD_UNLOCK_PIN) {
                     std::cout << "   [ACTION] UNLOCK REQUEST (Simulated)" << std::endl;
                } else if (cmd == CMD_LOCK) {
                     std::cout << "   [ACTION] LOCK WORKSTATION" << std::endl;
                     // Important: Flush and small sleep to ensure I/O completes before Lock
                     std::cout.flush(); 
                     ::LockWorkStation();
                }

            }
            catch (...) {
                std::cout << "[ERROR] Critical error in Write Handler." << std::endl;
            }

            // Always complete deferral
            deferral.Complete();
        }(); 
    }
}
