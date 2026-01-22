cmake_minimum_required (VERSION 3.13)

project ("WindowsMetric")

# Add source to this project's executable.
# Set C++ Standard to C++20 for WinRT support
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Source files
add_executable (WindowsMetric 
    "src/main.cpp"
    "src/RfcommServer.cpp" 
    "src/RfcommServer.h"
    "src/MetricCollector.cpp"
    "src/MetricCollector.h"
    "src/InputSimulator.cpp"
    "src/InputSimulator.h"
)

# Link against the Windows App SDK (WinRT)
target_link_libraries(WindowsMetric PRIVATE windowsapp)

# Ensure strictly compliant C++ for WinRT conformance
if (MSVC)
    target_compile_options(WindowsMetric PRIVATE /permissive-)
    # Request uiAccess=true and asInvoker level via linker flag to avoid manifest merge conflicts
    target_link_options(WindowsMetric PRIVATE "/MANIFESTUAC:level='asInvoker' uiAccess='true'")
endif()
