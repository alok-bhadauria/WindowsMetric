#include "MetricCollector.h"
#include <iostream>

namespace WindowsMetric {

    MetricCollector::MetricCollector() {}

    MetricCollector::~MetricCollector() {
        if (m_queryHandle) {
            PdhCloseQuery(m_queryHandle);
        }
    }

    void MetricCollector::Initialize() {
        if (m_initialized) return;

        PDH_STATUS status = PdhOpenQuery(NULL, NULL, &m_queryHandle);
        if (status != ERROR_SUCCESS) {
            std::cout << "[Metrics] Failed to open PDH Query." << std::endl;
            return;
        }

        // Add CPU Counter: "\Processor(_Total)\% Processor Time"
        // Note: For English, this works. Localization might fail.
        // A safer way uses PdhLookupPerfNameByIndex, but sticking to simple path first.
        status = PdhAddEnglishCounterW(m_queryHandle, L"\\Processor(_Total)\\% Processor Time", 0, &m_cpuCounter);
        if (status != ERROR_SUCCESS) {
             std::cout << "[Metrics] Failed to add CPU Counter." << std::endl;
             return;
        }
        
        // Collect first sample immediately to prime the diff
        PdhCollectQueryData(m_queryHandle);

        m_initialized = true;
    }

    double MetricCollector::GetCpuUsage() {
        if (!m_initialized) return 0.0;

        PDH_FMT_COUNTERVALUE counterVal;
        
        PdhCollectQueryData(m_queryHandle);
        PDH_STATUS status = PdhGetFormattedCounterValue(m_cpuCounter, PDH_FMT_DOUBLE, NULL, &counterVal);
        
        if (status == ERROR_SUCCESS) {
            return counterVal.doubleValue;
        }
        return 0.0;
    }

    double MetricCollector::GetRamUsage() {
        MEMORYSTATUSEX memInfo;
        memInfo.dwLength = sizeof(MEMORYSTATUSEX);
        GlobalMemoryStatusEx(&memInfo);
        
        return (double)memInfo.dwMemoryLoad; // dwMemoryLoad is percentage (0-100)
    }
}
