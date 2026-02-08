package com.vulsoft.vulsoftos;

import android.content.Intent;
import android.graphics.drawable.Drawable;

public class AppItem {
    public enum Type {
        APP,
        WIDGET,
        FOLDER
    }

    public Type type = Type.APP;
    public String label;
    public String packageName;
    public String className;
    public Drawable icon;
    public Intent launchIntent;
    public String category; // Added for Smart App Drawer
    
    // Widget specific fields
    public int widgetId = -1;
    public int spanX = 1;
    public int spanY = 1;

    // Folder specific fields
    public java.util.List<AppItem> folderItems = new java.util.ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppItem appItem = (AppItem) o;
        if (type != appItem.type) return false;
        if (type == Type.APP) {
            return packageName != null && packageName.equals(appItem.packageName) &&
                   (className == null ? appItem.className == null : className.equals(appItem.className));
        }
        if (type == Type.WIDGET) {
            return widgetId == appItem.widgetId;
        }
        if (type == Type.FOLDER) {
            // Compare label and child count as a reasonable proxy for equality
            // Deep comparison of children might be too expensive or prone to circular issues if not careful
            boolean labelsMatch = (label == null && appItem.label == null) || (label != null && label.equals(appItem.label));
            if (!labelsMatch) return false;
            
            // Check size
            return folderItems.size() == appItem.folderItems.size();
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        if (type == Type.APP && packageName != null) {
            return packageName.hashCode() + (className != null ? className.hashCode() : 0);
        }
        if (type == Type.WIDGET) {
            return widgetId;
        }
        if (type == Type.FOLDER) {
            return label != null ? label.hashCode() : 0;
        }
        return super.hashCode();
    }
}
