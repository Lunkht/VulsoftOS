package com.vulsoft.vulsoftos.health;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppHealthManager {
    private static final String TAG = "AppHealthManager";
    private static final String FILE_NAME = "health_reports.json";
    private static final int MAX_REPORTS = 50;

    public static void analyzeSystemHealth(Context context, boolean isBoot) {
        List<AppHealthReport> newReports = new ArrayList<>();
        PackageManager pm = context.getPackageManager();

        // 1. Log System Boot
        if (isBoot) {
            newReports.add(new AppHealthReport(
                    "android",
                    "Système",
                    System.currentTimeMillis(),
                    "BOOT_SUCCESS",
                    "Redémarrage du système détecté",
                    0
            ));
        }

        // 2. Check for Crashes/ANRs (API 30+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!com.vulsoft.vulsoftos.RecentAppsHelper.hasUsageStatsPermission(context)) {
                newReports.add(new AppHealthReport(
                        context.getPackageName(),
                        "Ruvoluti Health",
                        System.currentTimeMillis(),
                        "PERMISSION_ERROR",
                        "Impossible d'analyser les crashs (Permission Usage Stats requise)",
                        1
                ));
            } else {
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    try {
                        // Requires PACKAGE_USAGE_STATS or DUMP permission
                        List<ApplicationExitInfo> exitInfos = am.getHistoricalProcessExitReasons(null, 0, 10);
                        for (ApplicationExitInfo info : exitInfos) {
                            // Filter for recent events (e.g., last 24h or since last boot if we could track it)
                            // For now, we just log recent ones that are errors
                            if (isErrorReason(info.getReason())) {
                                String pkg = info.getProcessName(); // Fallback if getPackageName() is not available or typically same as process
                                String label = pkg;
                                try {
                                    if (pkg != null) {
                                        ApplicationInfo appInfo = pm.getApplicationInfo(pkg, 0);
                                        label = pm.getApplicationLabel(appInfo).toString();
                                    }
                                } catch (PackageManager.NameNotFoundException e) {
                                    // ignore
                                }

                                newReports.add(new AppHealthReport(
                                        pkg,
                                        label,
                                        info.getTimestamp(),
                                        getReasonString(info.getReason()),
                                        info.getDescription() != null ? info.getDescription() : "Arrêt inattendu",
                                        2 // Critical
                                ));
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur lors de l'analyse", e);
                    }
                }
            }
        }

        // Save reports
        saveReports(context, newReports);
    }

    private static boolean isErrorReason(int reason) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return reason == ApplicationExitInfo.REASON_CRASH ||
                   reason == ApplicationExitInfo.REASON_ANR ||
                   reason == ApplicationExitInfo.REASON_CRASH_NATIVE ||
                   reason == ApplicationExitInfo.REASON_INITIALIZATION_FAILURE;
        }
        return false;
    }

    private static String getReasonString(int reason) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            switch (reason) {
                case ApplicationExitInfo.REASON_CRASH: return "CRASH";
                case ApplicationExitInfo.REASON_ANR: return "ANR (Bloqué)";
                case ApplicationExitInfo.REASON_CRASH_NATIVE: return "CRASH NATIF";
                case ApplicationExitInfo.REASON_INITIALIZATION_FAILURE: return "ÉCHEC INIT";
                case ApplicationExitInfo.REASON_EXIT_SELF: return "EXIT";
                case ApplicationExitInfo.REASON_USER_REQUESTED: return "ARRÊT UTILISATEUR";
                case ApplicationExitInfo.REASON_LOW_MEMORY: return "MÉMOIRE INSUFFISANTE";
                default: return "AUTRE (" + reason + ")";
            }
        }
        return "UNKNOWN";
    }

    private static void saveReports(Context context, List<AppHealthReport> newReports) {
        List<AppHealthReport> existing = loadReports(context);
        existing.addAll(0, newReports); // Add new at top

        // Limit size
        if (existing.size() > MAX_REPORTS) {
            existing = existing.subList(0, MAX_REPORTS);
        }

        // Serialize
        try {
            JSONArray jsonArray = new JSONArray();
            for (AppHealthReport report : existing) {
                JSONObject obj = new JSONObject();
                obj.put("pkg", report.getPackageName());
                obj.put("name", report.getAppName());
                obj.put("time", report.getTimestamp());
                obj.put("type", report.getIssueType());
                obj.put("desc", report.getDescription());
                obj.put("severity", report.getSeverity());
                jsonArray.put(obj);
            }

            File file = new File(context.getFilesDir(), FILE_NAME);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(jsonArray.toString().getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "Error saving reports", e);
        }
    }

    public static List<AppHealthReport> loadReports(Context context) {
        List<AppHealthReport> reports = new ArrayList<>();
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) return reports;

        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();

            String json = new String(data, StandardCharsets.UTF_8);
            JSONArray jsonArray = new JSONArray(json);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                reports.add(new AppHealthReport(
                        obj.optString("pkg"),
                        obj.optString("name"),
                        obj.optLong("time"),
                        obj.optString("type"),
                        obj.optString("desc"),
                        obj.optInt("severity")
                ));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading reports", e);
        }
        
        // Sort by time desc
        Collections.sort(reports, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
        return reports;
    }
}
