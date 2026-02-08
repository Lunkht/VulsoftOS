package com.vulsoft.vulsoftos;

import android.content.Context;
import android.content.pm.PackageManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.util.List;
import java.util.ArrayList;

public class AppLayoutManager {
    private static final String LAYOUT_FILE = "app_layout.json";

    public static class SavedLayout {
        public List<AppItem> gridApps = new ArrayList<>();
        public List<AppItem> dockApps = new ArrayList<>();
    }

    public static SavedLayout loadLayout(Context context, PackageManager pm) {
        SavedLayout layout = new SavedLayout();
        File file = new File(context.getFilesDir(), LAYOUT_FILE);
        if (!file.exists()) {
            return null; // No saved layout
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            if (sb.length() == 0) return null;

            JSONObject root = new JSONObject(sb.toString());
            JSONArray gridJson = root.optJSONArray("grid");
            JSONArray dockJson = root.optJSONArray("dock");

            if (gridJson != null) {
                for (int i = 0; i < gridJson.length(); i++) {
                    Object itemObj = gridJson.get(i);
                    AppItem item = parseItem(itemObj);
                    if (item != null) {
                        layout.gridApps.add(item);
                    }
                }
            }
            
            if (dockJson != null) {
                for (int i = 0; i < dockJson.length(); i++) {
                    Object itemObj = dockJson.get(i);
                    AppItem item = parseItem(itemObj);
                    if (item != null) {
                        layout.dockApps.add(item);
                    }
                }
            }
            
            return layout;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static AppItem parseItem(Object jsonItem) {
        try {
            AppItem item = new AppItem();
            if (jsonItem instanceof String) {
                // Legacy format: just package name
                item.type = AppItem.Type.APP;
                item.packageName = (String) jsonItem;
            } else if (jsonItem instanceof JSONObject) {
                JSONObject obj = (JSONObject) jsonItem;
                String typeStr = obj.optString("type", "APP");
                item.type = AppItem.Type.valueOf(typeStr);
                item.packageName = obj.optString("package");
                item.className = obj.optString("class", null);
                item.label = obj.optString("label"); // Optional
                
                if (item.type == AppItem.Type.WIDGET) {
                    item.widgetId = obj.optInt("widgetId", -1);
                    item.spanX = obj.optInt("spanX", 1);
                    item.spanY = obj.optInt("spanY", 1);
                } else if (item.type == AppItem.Type.FOLDER) {
                    JSONArray itemsJson = obj.optJSONArray("items");
                    if (itemsJson != null) {
                        for (int k = 0; k < itemsJson.length(); k++) {
                            AppItem child = parseItem(itemsJson.get(k));
                            if (child != null) item.folderItems.add(child);
                        }
                    }
                }
            }
            return item;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveLayout(Context context, List<AppItem> gridApps, List<AppItem> dockApps) {
        try {
            JSONObject root = new JSONObject();
            JSONArray gridJson = new JSONArray();
            JSONArray dockJson = new JSONArray();

            for (AppItem item : gridApps) {
                gridJson.put(toJson(item));
            }
            
            for (AppItem item : dockApps) {
                dockJson.put(toJson(item));
            }

            root.put("grid", gridJson);
            root.put("dock", dockJson);

            File file = new File(context.getFilesDir(), LAYOUT_FILE);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(root.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static JSONObject toJson(AppItem item) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("type", item.type.name());
            obj.put("package", item.packageName);
            if (item.className != null) obj.put("class", item.className);
            if (item.label != null) obj.put("label", item.label);
            
            if (item.type == AppItem.Type.WIDGET) {
                obj.put("widgetId", item.widgetId);
                obj.put("spanX", item.spanX);
                obj.put("spanY", item.spanY);
            } else if (item.type == AppItem.Type.FOLDER) {
                JSONArray itemsJson = new JSONArray();
                for (AppItem child : item.folderItems) {
                    itemsJson.put(toJson(child));
                }
                obj.put("items", itemsJson);
            }
            return obj;
        } catch (Exception e) {
            return null;
        }
    }
}
