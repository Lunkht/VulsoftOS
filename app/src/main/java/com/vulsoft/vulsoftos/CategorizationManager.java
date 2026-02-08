package com.vulsoft.vulsoftos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategorizationManager {

    public static Map<String, List<AppItem>> categorize(List<AppItem> apps) {
        Map<String, List<AppItem>> categories = new HashMap<>();

        String[] socialKeywords = { "facebook", "twitter", "instagram", "whatsapp", "messenger", "telegram", "snapchat",
                "tiktok", "viber", "linkedin" };
        String[] multimediaKeywords = { "spotify", "youtube", "netflix", "music", "player", "gallery", "video",
                "camera", "photo", "vlc", "deezer" };
        String[] googleKeywords = { "google", "chrome", "drive", "maps", "gmail", "calendar", "photos", "keep", "docs",
                "sheets", "slides", "chrome" };
        String[] systemKeywords = { "setting", "android", "system", "launcher", "valkunt", "phone", "contact", "dialer",
                "message", "file", "calculator", "clock" };
        String[] productivityKeywords = { "note", "task", "office", "word", "excel", "powerpoint", "pdf", "mail",
                "outlook", "slack", "zoom", "teams" };
        String[] gamesKeywords = { "game", "pubg", "freefire", "candy", "roblox", "minecraft", "clash", "brawl",
                "asphalt", "ea", "ps", "xbox" };

        for (AppItem app : apps) {
            // Skip widgets or existing folders for categorization
            if (app.type != AppItem.Type.APP) continue;

            String pkg = app.packageName != null ? app.packageName.toLowerCase() : "";
            String label = app.label != null ? app.label.toLowerCase() : "";

            if (containsAny(pkg, label, googleKeywords)) {
                addToCategory(categories, "Google+", app);
            } else if (containsAny(pkg, label, socialKeywords)) {
                addToCategory(categories, "Réseaux", app);
            } else if (containsAny(pkg, label, multimediaKeywords)) {
                addToCategory(categories, "Médias", app);
            } else if (containsAny(pkg, label, systemKeywords)) {
                addToCategory(categories, "Système", app);
            } else if (containsAny(pkg, label, productivityKeywords)) {
                addToCategory(categories, "Outils", app);
            } else if (containsAny(pkg, label, gamesKeywords)) {
                addToCategory(categories, "Jeux", app);
            }
        }

        return categories;
    }

    private static boolean containsAny(String pkg, String label, String[] keywords) {
        for (String k : keywords) {
            if (pkg.contains(k) || label.contains(k))
                return true;
        }
        return false;
    }

    private static void addToCategory(Map<String, List<AppItem>> categories, String name, AppItem app) {
        if (!categories.containsKey(name)) {
            categories.put(name, new ArrayList<>());
        }
        categories.get(name).add(app);
    }
}
