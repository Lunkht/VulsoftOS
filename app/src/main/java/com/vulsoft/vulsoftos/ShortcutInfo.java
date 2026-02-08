package com.vulsoft.vulsoftos;

public class ShortcutInfo {
    private String label;
    private int iconRes;
    private Runnable action;

    public ShortcutInfo(String label, int iconRes, Runnable action) {
        this.label = label;
        this.iconRes = iconRes;
        this.action = action;
    }

    public String getLabel() {
        return label;
    }

    public int getIconRes() {
        return iconRes;
    }

    public Runnable getAction() {
        return action;
    }
}
