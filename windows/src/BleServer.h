#pragma once
#include <winrt/Windows.Foundation.h>
#include <winrt/Windows.Devices.Bluetooth.h>
#include <winrt/Windows.Devices.Bluetooth.GenericAttributeProfile.h>

namespace WindowsMetric {
    class BleServer {
    public:
        BleServer();
        winrt::Windows::Foundation::IAsyncAction Start();

    private:
        winrt::Windows::Devices::Bluetooth::GenericAttributeProfile::GattServiceProvider m_serviceProvider{ nullptr };
        
        // Helper to create characteristics
        winrt::Windows::Foundation::IAsyncOperation<bool> CreateCharacteristicsAsync();

        void OnWriteRequested(
            winrt::Windows::Devices::Bluetooth::GenericAttributeProfile::GattLocalCharacteristic const& sender,
            winrt::Windows::Devices::Bluetooth::GenericAttributeProfile::GattWriteRequestedEventArgs const& args
        );
    };
}
