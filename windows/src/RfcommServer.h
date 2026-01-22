#pragma once

#include <winrt/Windows.Foundation.h>
#include <winrt/Windows.Networking.Sockets.h>
#include <winrt/Windows.Devices.Bluetooth.h>
#include <winrt/Windows.Devices.Bluetooth.Rfcomm.h>
#include <winrt/Windows.Storage.Streams.h>
#include <string>

namespace WindowsMetric {

    class RfcommServer {
    public:
        RfcommServer();
        winrt::Windows::Foundation::IAsyncAction Start();
        void Stop();

    private:
        winrt::Windows::Devices::Bluetooth::Rfcomm::RfcommServiceProvider m_provider{ nullptr };
        winrt::Windows::Networking::Sockets::StreamSocketListener m_listener{ nullptr };
        
        // Active Session
        winrt::Windows::Networking::Sockets::StreamSocket m_socket{ nullptr };
        winrt::Windows::Storage::Streams::DataWriter m_writer{ nullptr };
        
        std::wstring m_currentPin;
        bool m_isAuthenticated = false;

        winrt::Windows::Foundation::IAsyncAction OnConnectionReceived(
            winrt::Windows::Networking::Sockets::StreamSocketListener sender,
            winrt::Windows::Networking::Sockets::StreamSocketListenerConnectionReceivedEventArgs args
        );

        winrt::Windows::Foundation::IAsyncAction HandleClientLoop(
            winrt::Windows::Networking::Sockets::StreamSocket socket
        );

        std::wstring GeneratePin();
        void ProcessCommand(std::string cmd);
    };
}
