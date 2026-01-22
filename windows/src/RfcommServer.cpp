#include "RfcommServer.h"
#include <iostream>
#include <random>
#include <sstream>
#include <windows.h>
#include "MetricCollector.h"
#include "InputSimulator.h"

using namespace winrt;
using namespace winrt::Windows::Foundation;
using namespace winrt::Windows::Devices::Bluetooth;
using namespace winrt::Windows::Devices::Bluetooth::Rfcomm;
using namespace winrt::Windows::Networking::Sockets;
using namespace winrt::Windows::Storage::Streams;

namespace WindowsMetric {
    
    // ... UUIDs ...
    static const guid RFCOMM_SERVICE_UUID{ 
        0x5A401569, 0xF53B, 0x4444, { 0xA5, 0x12, 0xFB, 0x71, 0x15, 0x85, 0x25, 0x55 } 
    };

    RfcommServer::RfcommServer() {}

    IAsyncAction RfcommServer::Start() {
        // ... (Startup code same as before) ...
        try {
            std::cout << "Starting RFCOMM Server..." << std::endl;
            m_provider = co_await RfcommServiceProvider::CreateAsync(RfcommServiceId::FromUuid(RFCOMM_SERVICE_UUID));
            m_listener = StreamSocketListener();
            m_listener.ConnectionReceived({ this, &RfcommServer::OnConnectionReceived });
            co_await m_listener.BindServiceNameAsync(m_provider.ServiceId().AsString(), SocketProtectionLevel::BluetoothEncryptionAllowNullAuthentication);
            m_provider.StartAdvertising(m_listener);
            std::cout << "RFCOMM Advertising Started. Connect via Android App." << std::endl;
        }
        catch (hresult_error const& ex) {
            std::cout << "Failed to start RFCOMM: " << winrt::to_string(ex.message()) << std::endl;
        }
    }

    void RfcommServer::Stop() {
        if (m_provider) m_provider.StopAdvertising();
        if (m_listener) m_listener.Close();
        m_provider = nullptr;
        m_listener = nullptr;
    }

    IAsyncAction RfcommServer::OnConnectionReceived(
        StreamSocketListener sender,
        StreamSocketListenerConnectionReceivedEventArgs args)
    {
        m_socket = args.Socket();
        std::cout << "\n[Client Connected]" << std::endl;

        m_currentPin = GeneratePin();
        m_isAuthenticated = false;

        std::cout << "************************" << std::endl;
        std::cout << "** SESSION PIN: " << winrt::to_string(m_currentPin) << " **" << std::endl;
        std::cout << "************************" << std::endl;

        co_await HandleClientLoop(m_socket);
    }

    IAsyncAction RfcommServer::HandleClientLoop(StreamSocket socket) {
        try {
            auto input = socket.InputStream();
            auto output = socket.OutputStream();
            m_writer = DataWriter(output);
            auto reader = DataReader(input);
            reader.InputStreamOptions(InputStreamOptions::Partial);
            
            // Metrics Setup
            MetricCollector collector;
            collector.Initialize();

            // Start Metric Broadcaster (Fire & Forget)
            auto metricsOp = [&]() -> fire_and_forget {
                while (true) {
                    co_await winrt::resume_after(std::chrono::seconds(1));
                    if (!m_socket) break;
                    if (!m_isAuthenticated) continue;
                    try {
                        std::stringstream ss;
                        ss << "METRICS:CPU=" << (int)collector.GetCpuUsage() 
                           << ";RAM=" << (int)collector.GetRamUsage() << "\n";
                        m_writer.WriteString(winrt::to_hstring(ss.str()));
                        m_writer.StoreAsync();
                    } catch (...) { break; }
                }
            }();

            std::string accumulator;

            while (true) {
                unsigned int size = co_await reader.LoadAsync(1024);
                if (size == 0) {
                    std::cout << "[Client Disconnected]" << std::endl;
                    break; 
                }

                std::string chunk;
                chunk.resize(size);
                reader.ReadBytes({ (uint8_t*)&chunk[0], size });
                
                accumulator += chunk;

                size_t pos = 0;
                while ((pos = accumulator.find('\n')) != std::string::npos) {
                    std::string cmd = accumulator.substr(0, pos);
                    accumulator.erase(0, pos + 1);
                    
                    // Handle \r if present (CRLF)
                    if (!cmd.empty() && cmd.back() == '\r') {
                        cmd.pop_back();
                    }
                    
                    if (!cmd.empty()) {
                        ProcessCommand(cmd);
                    }
                }
            }
        }
        catch (...) {
            std::cout << "[Connection Error/Closed]" << std::endl;
        }
        
        m_socket = nullptr;
    }

    std::wstring RfcommServer::GeneratePin() {
        std::random_device rd;
        std::mt19937 gen(rd());
        std::uniform_int_distribution<> distr(1000, 9999);
        return std::to_wstring(distr(gen));
    }

    void RfcommServer::ProcessCommand(std::string cmd) {
        cmd.erase(cmd.find_last_not_of(" \n\r\t") + 1);
        std::cout << "RX: " << cmd << std::endl;

        try {
            if (cmd.find("AUTH:") == 0) {
                std::string pinStr = cmd.substr(5);
                std::wstring pinW(pinStr.begin(), pinStr.end());

                if (pinW == m_currentPin) {
                    m_isAuthenticated = true;
                    std::cout << ">> AUTH SUCCESS" << std::endl;
                    m_writer.WriteString(L"AUTH:OK\n");
                    m_writer.StoreAsync();
                } else {
                    std::cout << ">> AUTH FAILED" << std::endl;
                    m_writer.WriteString(L"AUTH:FAIL\n");
                    m_writer.StoreAsync();
                }
            }
            else if (m_isAuthenticated) {
                if (cmd == "CMD:LOCK") {
                    std::cout << ">> LOCKING..." << std::endl;
                    ::LockWorkStation();
                }
                else if (cmd.find("CMD:UNLOCK:") == 0) {
                    // CMD:UNLOCK:1234
                    std::string pin = cmd.substr(11);
                    std::cout << ">> UNLOCK REQUEST with PIN: " << pin << std::endl;
                    InputSimulator::UnlockWithPin(pin);
                }
                else {
                    std::cout << ">> Unknown/Legacy Command" << std::endl;
                }
            }
        }
        catch (...) {}
    }
}
