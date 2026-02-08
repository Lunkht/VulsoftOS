package com.vulsoft.vulsoftos;

import android.graphics.drawable.Drawable;
import java.util.List;

public class AppInfo {
    private String label;
    private String packageName;
    private Drawable icon;
    private boolean isPlaceholder;
    private boolean isFolder;
    private List<AppInfo> folderItems;
    private boolean isHidden;

    public AppInfo() {}

    public AppInfo(String packageName, List<AppInfo> items) {
        this.packageName = packageName;
        this.folderItems = items;
        this.isFolder = true;
    }
    
    public String getPackageName() { return packageName; }
    public String getLabel() { return label; }
    public Drawable getIcon() { return icon; }
    public boolean isPlaceholder() { return isPlaceholder; }
    public boolean isFolder() { return isFolder; }
    public boolean isHidden() { return isHidden; }
    public List<AppInfo> getFolderItems() { return folderItems; }
    
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public void setLabel(String label) { this.label = label; }
    public void setIcon(Drawable icon) { this.icon = icon; }
    public void setHidden(boolean hidden) { this.isHidden = hidden; }

    public static AppInfo createPlaceholder() {
        AppInfo info = new AppInfo();
        info.isPlaceholder = true;
        return info;
    }
}
