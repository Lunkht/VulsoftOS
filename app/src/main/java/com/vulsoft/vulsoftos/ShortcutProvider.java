package com.vulsoft.vulsoftos;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public class ShortcutProvider extends ContentProvider {
    private static final String AUTHORITY = "com.valkunt.os.shortcuts";
    private static final int SHORTCUTS = 1;
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    
    static {
        uriMatcher.addURI(AUTHORITY, "shortcuts", SHORTCUTS);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                       String[] selectionArgs, String sortOrder) {
        
        if (uriMatcher.match(uri) == SHORTCUTS) {
            MatrixCursor cursor = new MatrixCursor(new String[]{
                "id", "title", "intent", "icon"
            });
            
            // Ajouter les raccourcis dynamiques
            cursor.addRow(new Object[]{
                "settings",
                "Paramètres",
                "com.valkunt.os.SettingsActivity",
                "settings_sliders"
            });
            
            cursor.addRow(new Object[]{
                "wallpaper", 
                "Fond d'écran",
                "com.valkunt.os.SettingsActivity?action=wallpaper",
                "picture"
            });
            
            return cursor;
        }
        
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.dir/shortcut";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                     String[] selectionArgs) {
        return 0;
    }
}