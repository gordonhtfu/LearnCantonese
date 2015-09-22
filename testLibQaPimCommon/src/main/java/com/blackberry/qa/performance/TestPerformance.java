package com.blackberry.qa.performance;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.os.Debug.MemoryInfo;
import android.util.Log;

/**
 * Test class for getting system/application performance metrics.
 */
public class TestPerformance {

    private static String TAG = TestPerformance.class.getSimpleName();

    /**
     * Context of the calling app/service/test
     */
    private Context mContext;

    /**
     * Activity manager to interact with and get info for currently running
     * activities/processes on the system
     */
    private ActivityManager mActivityManager;

    /**
     * Flag for determining if process filtering (by proc name) has been
     * requested
     */
    private boolean mPidFilter = false;

    /**
     * Process filter string
     */
    private String mFilterName = "";

    public TestPerformance(Context context) {
        mContext = context;
        mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
    }

    /**
     * Set an optional filter on the process(es) of interest. If a filter is
     * not set, performance metrics for all active processes will be returned.
     * @param filter: The string to search the process names by. This could
     * be the package name/prefix, e.g. 'com.blackberry' or a complete
     * process name, e.g. 'com.blackberry.hub'.
     */
    public void setPidFilter(String filter) {
        mPidFilter = true;
        mFilterName = filter.toLowerCase();
    }

    /**
     * Return cpu usage for processes.
     * Will apply filter on processes if set via setPidFilter.
     * @return HashMap containing process names (string) and cpu usage (int)
     */
    public HashMap<String, Integer> getProcessesCpuUsage() {
        HashMap<String, Integer> cpuUsageMap = new LinkedHashMap<String, Integer>();
        String line;
        boolean procLineFlag = false;
        try {
            Process process = Runtime.getRuntime().exec("top -n 1");
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            while ((line = in.readLine()) != null) {
                Log.d(TAG, "PRINTING LINE " + line);
                if (line.contains("CPU%")) {
                    procLineFlag = true;
                    continue;
                }
                if (procLineFlag) {
                    if (mPidFilter && !line.contains(mFilterName)) {
                        continue;
                    } else {
                        String[] splitLine = line.trim().split("\\s+");
                        //TODO: This loop probably not needed after trim, removal
                        //      will result in better performance as well.
                        for (int x = 0; x < 4; x++) {
                            if (splitLine[x].contains("%")) {
                                String procName = splitLine[splitLine.length - 1];
                                int cpuVal = Integer.parseInt(splitLine[x].replace("%", ""));
                                cpuUsageMap.put(procName, cpuVal);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return cpuUsageMap;
    }

    /**
     * Return memory usage for processes. Returns a HashMap of
     * String (proc name), MemoryInfo objects - each object holds memory information
     * for an individual process. See android.os.debug.MemoryInfo.
     * Will apply filter on processes if set via setPidFilter.
     * @return HashMap containing process names (string) and memory info (MemoryInfo)
     */
    public HashMap<String, MemoryInfo> getProcessesMemUsage() {
        HashMap<String, MemoryInfo> memoryMap = new HashMap<String, MemoryInfo>();
        Map<Integer, String> pidMap = this.getPidMap();
        for (Integer procPid : pidMap.keySet()) {
            String procName = pidMap.get(procPid);
            memoryMap.put(procName, this.getProcMemUsage(procName));
        }
        return memoryMap;
    }

    /**
     * Return cpu usage for a single process.
     * @param name: The name of the process
     * @return cpu usage as an int
     */
    public int getProcCpuUsage(String name) {
        HashMap<String, Integer> cpuMap = this.getProcessesCpuUsage();
        int cpuUsage = cpuMap.get(name);
        return cpuUsage;
    }

    /**
     * Return memory usage for a single process.
     * @param name: The name of the process
     * @return MemoryInfo object.
     */
    public MemoryInfo getProcMemUsage(String name) {
        int pid = this.getPid(name);
        int[] pidArr = new int[1];
        pidArr[0] = pid;
        MemoryInfo[] memoryInfoArray = mActivityManager.getProcessMemoryInfo(pidArr);
        return memoryInfoArray[0];
    }

    /**
     * Return info on currently running processes. See
     * ActivityManager.RunningAppProcessInfo.
     * @return A list of RunningAppProcessInfo objects
     */
    private List<RunningAppProcessInfo> getRunningProcesses() {
        List<RunningAppProcessInfo> runningAppProcesses = mActivityManager.getRunningAppProcesses();
        return runningAppProcesses;
    }

    /**
     * Return map of currently running processes.
     * Will apply filter on processes if set via setPidFilter.
     * @return Map<Integer, String> containing the process IDs and names of
     * currently running processes
     */
    public Map<Integer, String> getPidMap() {
        Map<Integer, String> pidMap = new TreeMap<Integer, String>();
        List<RunningAppProcessInfo> runningAppProcesses = this.getRunningProcesses();
        for (RunningAppProcessInfo runningAppProcessInfo : runningAppProcesses) {
            if (!mPidFilter) {
                pidMap.put(runningAppProcessInfo.pid, runningAppProcessInfo.processName);
            } else if (mPidFilter && runningAppProcessInfo.processName.toLowerCase().contains(mFilterName)) {
                pidMap.put(runningAppProcessInfo.pid, runningAppProcessInfo.processName);
            }
        }
        return pidMap;
    }

    /**
     * Returns process id for a process name
     * @param name: Name of the process
     * @return process id as an int
     */
    public int getPid(String name) {
        Map<Integer, String> pidMap = this.getPidMap();
        int pid = -1;
        for (Map.Entry<Integer, String> entry: pidMap.entrySet()) {
            if (name.equals(entry.getValue())) {
                pid = (int) entry.getKey();
                break;
            }
        }
        return pid;
    }
}
