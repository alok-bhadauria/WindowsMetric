#include <iostream>
#include <windows.h>
#include <winrt/Windows.Foundation.h>
#include "RfcommServer.h"

int main()
{
    try {
        std::cout << "Initializing WinRT..." << std::endl;
        // Initialize WinRT Apartment (Required for C++/WinRT)
        winrt::init_apartment();
        std::cout << "WinRT initialized." << std::endl;

        std::cout << "WindowsMetric - RFCOMM Phase" << std::endl;

        WindowsMetric::RfcommServer server;
        
        // Fire and forget the async start
        server.Start();

        std::cout << "Keep window open. Press Ctrl+C to exit." << std::endl;
        
        // Use an infinite loop to keep the server running
        // std::cin.get() can return if the console session changes (e.g. LockScreen), causing the app to exit.
        while (true) {
            Sleep(1000);
        }
    } catch (const std::exception& e) {
        std::cerr << "Exception: " << e.what() << std::endl;
        return 1;
    } catch (...) {
        std::cerr << "Unknown exception" << std::endl;
        return 1;
    }

    return 0;
}
