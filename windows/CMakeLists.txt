cmake_minimum_required (VERSION 3.8)

project ("WindowsMetric")

# Add source to this project's executable.
add_executable (WindowsMetric "src/main.cpp")

# TODO: Add C++/WinRT and other dependencies here later.
if (CMAKE_VERSION VERSION_GREATER 3.12)
  set_property(TARGET WindowsMetric PROPERTY CXX_STANDARD 20)
endif()
