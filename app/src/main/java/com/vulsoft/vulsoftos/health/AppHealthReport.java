package com.vulsoft.vulsoftos.health;

public class AppHealthReport {
    private String packageName;
    private String appName;
    private long timestamp;
    private String issueType; // "CRASH", "ANR", "BOOT_SUCCESS", "INFO"
    private String description;
    private int severity; // 0=Info, 1=Warning, 2=Critical

    public AppHealthReport(String packageName, String appName, long timestamp, String issueType, String description, int severity) {
        this.packageName = packageName;
        this.appName = appName;
        this.timestamp = timestamp;
        this.issueType = issueType;
        this.description = description;
        this.severity = severity;
    }

    public String getPackageName() { return packageName; }
    public String getAppName() { return appName; }
    public long getTimestamp() { return timestamp; }
    public String getIssueType() { return issueType; }
    public String getDescription() { return description; }
    public int getSeverity() { return severity; }
}
