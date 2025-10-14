package de.marcandreher.fusionkit.core.cmd.implementations;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.marcandreher.fusionkit.core.cmd.Command;
import de.marcandreher.fusionkit.core.cmd.CommandInfo;

@CommandInfo(name = "jvm", description = "Shows comprehensive JVM, memory, CPU and system information.")
public class JVMCommand implements Command {
    
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("#0.0");
    private static final String SEPARATOR = "─".repeat(80);
    
    @Override
    public void execute(String[] args) {
        // Parse command arguments
        boolean showHelp = args.length > 0 && ("help".equals(args[0]) || "-h".equals(args[0]));
        boolean showDetailed = args.length > 0 && ("detailed".equals(args[0]) || "-d".equals(args[0]));
        boolean showGc = args.length > 0 && ("gc".equals(args[0]) || "-g".equals(args[0]));

        if (showHelp) {
            showHelpMessage();
            return;
        }

        getLogger().info("[*] JVM System Report");
        getLogger().info(SEPARATOR);

        try {
            showJvmInfo();
            showMemoryInfo();
            showCpuInfo();
            showThreadInfo();
            
            if (showGc || showDetailed) {
                showGarbageCollectionInfo();
            }
            
            if (showDetailed) {
                showDetailedSystemInfo();
            }
            
        } catch (Exception e) {
            getLogger().error("Failed to retrieve JVM information: " + e.getMessage(), e);
        }

        getLogger().info(SEPARATOR);
        getLogger().info("(i) Use 'jvm detailed' for more info, 'jvm gc' for GC stats, or 'jvm help' for usage");
    }

    private void showHelpMessage() {
        getLogger().info("[?] JVM Command Usage:");
        getLogger().info("  jvm           - Show basic JVM status and system information");
        getLogger().info("  jvm detailed  - Show comprehensive system metrics");
        getLogger().info("  jvm gc        - Show garbage collection statistics");
        getLogger().info("  jvm help      - Show this help message");
    }

    private void showJvmInfo() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        
        getLogger().info("[J] JVM Information:");
        getLogger().info(String.format("  ├─ JVM Name:              %s", runtimeBean.getVmName()));
        getLogger().info(String.format("  ├─ JVM Version:           %s", runtimeBean.getVmVersion()));
        getLogger().info(String.format("  ├─ JVM Vendor:            %s", runtimeBean.getVmVendor()));
        getLogger().info(String.format("  ├─ Java Version:          %s", System.getProperty("java.version")));
        getLogger().info(String.format("  ├─ Java Home:             %s", System.getProperty("java.home")));
        getLogger().info(String.format("  ├─ Process ID:            %d", runtimeBean.getPid()));
        getLogger().info(String.format("  ├─ Uptime:               %s", formatUptime(runtimeBean.getUptime())));
        getLogger().info(String.format("  └─ Start Time:            %s", new java.util.Date(runtimeBean.getStartTime())));
    }

    private void showMemoryInfo() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemory = memoryBean.getNonHeapMemoryUsage();
        
        // Calculate heap utilization
        double heapUsedPercent = (double) heapMemory.getUsed() / heapMemory.getMax() * 100;
        String heapStatus = getMemoryStatus(heapUsedPercent);
        
        getLogger().info("");
        getLogger().info("[M] Memory Information:");
        getLogger().info(String.format("  [H] Heap Memory:"));
        getLogger().info(String.format("    ├─ Used:               %s MB", DECIMAL_FORMAT.format(heapMemory.getUsed() / 1024.0 / 1024.0)));
        getLogger().info(String.format("    ├─ Committed:          %s MB", DECIMAL_FORMAT.format(heapMemory.getCommitted() / 1024.0 / 1024.0)));
        getLogger().info(String.format("    ├─ Max:                %s MB", DECIMAL_FORMAT.format(heapMemory.getMax() / 1024.0 / 1024.0)));
        getLogger().info(String.format("    └─ Utilization:        %s%% %s", PERCENT_FORMAT.format(heapUsedPercent), heapStatus));
        
        getLogger().info(String.format("  [N] Non-Heap Memory:"));
        getLogger().info(String.format("    ├─ Used:               %s MB", DECIMAL_FORMAT.format(nonHeapMemory.getUsed() / 1024.0 / 1024.0)));
        getLogger().info(String.format("    ├─ Committed:          %s MB", DECIMAL_FORMAT.format(nonHeapMemory.getCommitted() / 1024.0 / 1024.0)));
        getLogger().info(String.format("    └─ Max:                %s", nonHeapMemory.getMax() == -1 ? "Unlimited" : DECIMAL_FORMAT.format(nonHeapMemory.getMax() / 1024.0 / 1024.0) + " MB"));
        
        // Show total system memory
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long usedMemory = totalMemory - freeMemory;
        
        getLogger().info(String.format("  [R] Runtime Memory:"));
        getLogger().info(String.format("    ├─ Used:               %s MB", DECIMAL_FORMAT.format(usedMemory / 1024.0 / 1024.0)));
        getLogger().info(String.format("    ├─ Free:               %s MB", DECIMAL_FORMAT.format(freeMemory / 1024.0 / 1024.0)));
        getLogger().info(String.format("    ├─ Total:              %s MB", DECIMAL_FORMAT.format(totalMemory / 1024.0 / 1024.0)));
        getLogger().info(String.format("    └─ Max Available:       %s MB", DECIMAL_FORMAT.format(maxMemory / 1024.0 / 1024.0)));
    }

    private void showCpuInfo() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        
        getLogger().info("");
        getLogger().info("[S] CPU & System Information:");
        getLogger().info(String.format("  ├─ OS Name:               %s", osBean.getName()));
        getLogger().info(String.format("  ├─ OS Version:            %s", osBean.getVersion()));
        getLogger().info(String.format("  ├─ OS Architecture:       %s", osBean.getArch()));
        getLogger().info(String.format("  ├─ Available Processors:  %d", osBean.getAvailableProcessors()));
        
        // Try to get more detailed CPU info if available (some JVMs provide this)
        try {
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
                double processCpuLoad = sunOsBean.getProcessCpuLoad();
                double systemCpuLoad = sunOsBean.getCpuLoad();
                long totalPhysicalMemory = sunOsBean.getTotalMemorySize();
                long freePhysicalMemory = sunOsBean.getFreeMemorySize();
                long totalSwapSpace = sunOsBean.getTotalSwapSpaceSize();
                long freeSwapSpace = sunOsBean.getFreeSwapSpaceSize();
                
                if (processCpuLoad >= 0) {
                    getLogger().info(String.format("  ├─ Process CPU Load:      %s%% %s", PERCENT_FORMAT.format(processCpuLoad * 100), getCpuStatus(processCpuLoad * 100)));
                }
                if (systemCpuLoad >= 0) {
                    getLogger().info(String.format("  ├─ System CPU Load:       %s%%", PERCENT_FORMAT.format(systemCpuLoad * 100)));
                }
                
                getLogger().info(String.format("  ├─ Physical Memory:       %s GB", DECIMAL_FORMAT.format(totalPhysicalMemory / 1024.0 / 1024.0 / 1024.0)));
                getLogger().info(String.format("  ├─ Free Physical Memory:  %s GB", DECIMAL_FORMAT.format(freePhysicalMemory / 1024.0 / 1024.0 / 1024.0)));
                if (totalSwapSpace > 0) {
                    getLogger().info(String.format("  ├─ Total Swap Space:      %s GB", DECIMAL_FORMAT.format(totalSwapSpace / 1024.0 / 1024.0 / 1024.0)));
                    getLogger().info(String.format("  └─ Free Swap Space:       %s GB", DECIMAL_FORMAT.format(freeSwapSpace / 1024.0 / 1024.0 / 1024.0)));
                }
            }
        } catch (Exception e) {
            getLogger().debug("Extended CPU info not available: " + e.getMessage());
        }
        
        double loadAverage = osBean.getSystemLoadAverage();
        if (loadAverage >= 0) {
            getLogger().info(String.format("  └─ System Load Average:   %.2f", loadAverage));
        }
    }

    private void showThreadInfo() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        
        getLogger().info("");
        getLogger().info("[T] Thread Information:");
        getLogger().info(String.format("  ├─ Live Threads:          %d", threadBean.getThreadCount()));
        getLogger().info(String.format("  ├─ Daemon Threads:        %d", threadBean.getDaemonThreadCount()));
        getLogger().info(String.format("  ├─ Peak Threads:          %d", threadBean.getPeakThreadCount()));
        getLogger().info(String.format("  └─ Total Started:         %d", threadBean.getTotalStartedThreadCount()));
    }

    private void showGarbageCollectionInfo() {
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        
        getLogger().info("");
        getLogger().info("[G] Garbage Collection:");
        
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            getLogger().info(String.format("  [C] %s:", gcBean.getName()));
            getLogger().info(String.format("    ├─ Collection Count:    %d", gcBean.getCollectionCount()));
            getLogger().info(String.format("    └─ Collection Time:     %d ms", gcBean.getCollectionTime()));
        }
        
        if (gcBeans.isEmpty()) {
            getLogger().info("  └─ No GC information available");
        }
    }

    private void showDetailedSystemInfo() {
        getLogger().info("");
        getLogger().info("[D] Detailed System Properties:");
        getLogger().info(String.format("  ├─ User Name:             %s", System.getProperty("user.name")));
        getLogger().info(String.format("  ├─ User Home:             %s", System.getProperty("user.home")));
        getLogger().info(String.format("  ├─ Working Directory:     %s", System.getProperty("user.dir")));
        getLogger().info(String.format("  ├─ Temporary Directory:   %s", System.getProperty("java.io.tmpdir")));
        getLogger().info(String.format("  ├─ File Separator:        '%s'", System.getProperty("file.separator")));
        getLogger().info(String.format("  ├─ Path Separator:        '%s'", System.getProperty("path.separator")));
        getLogger().info(String.format("  ├─ Line Separator:        %s", escapeString(System.getProperty("line.separator"))));
        getLogger().info(String.format("  └─ Class Path:            %s", truncateString(System.getProperty("java.class.path"), 100)));
    }

    private String formatUptime(long uptimeMs) {
        long days = TimeUnit.MILLISECONDS.toDays(uptimeMs);
        long hours = TimeUnit.MILLISECONDS.toHours(uptimeMs) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(uptimeMs) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(uptimeMs) % 60;
        
        if (days > 0) {
            return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    private String getMemoryStatus(double percentUsed) {
        if (percentUsed < 50) {
            return "[OK] Low";
        } else if (percentUsed < 80) {
            return "[!] Medium";
        } else if (percentUsed < 95) {
            return "[!!] High";
        } else {
            return "[!!!] Critical";
        }
    }

    private String getCpuStatus(double percentUsed) {
        if (percentUsed < 25) {
            return "[OK] Low";
        } else if (percentUsed < 50) {
            return "[!] Medium";
        } else if (percentUsed < 75) {
            return "[!!] High";
        } else {
            return "[!!!] Critical";
        }
    }

    private String escapeString(String str) {
        if (str == null) return "null";
        return str.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private String truncateString(String str, int maxLength) {
        if (str == null) return "null";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
}
