#pragma once
#include <windows.h>
#include <pdh.h>
#include <pdhmsg.h>
#include <string>

#pragma comment(lib, "pdh.lib")

namespace WindowsMetric {
    class MetricCollector {
    public:
        MetricCollector();
        ~MetricCollector();

        void Initialize();
        double GetCpuUsage();
        double GetRamUsage();

    private:
        PDH_HQUERY m_queryHandle{ nullptr };
        PDH_HCOUNTER m_cpuCounter{ nullptr };
        bool m_initialized = false;
    };
}
