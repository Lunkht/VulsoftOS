package com.vulsoft.vulsoftos.activities;

import com.vulsoft.vulsoftos.*;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.vulsoft.vulsoftos.activities.AppHealthActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsActivity extends BaseActivity {

    public static final String KEY_GESTURE_SWIPE_UP = "gesture_swipe_up";
    public static final String KEY_GESTURE_SWIPE_DOWN = "gesture_swipe_down";
    public static final String KEY_GESTURE_DOUBLE_TAP = "gesture_double_tap";
    public static final String KEY_ZEN_MODE = "zen_mode_enabled";

    public static final String ACTION_NONE = "action_none";
    public static final String ACTION_NOTIFICATIONS = "action_notifications";
    public static final String ACTION_SETTINGS = "action_settings";
    public static final String ACTION_WALLPAPER = "action_wallpaper";
    public static final String ACTION_ASSISTANT = "action_assistant";
    public static final String ACTION_APP_SEARCH = "action_app_search";

    public static final String PREF_DRAWER_STYLE = "drawer_style";
    public static final String DRAWER_STYLE_GRID = "grid";
    public static final String DRAWER_STYLE_LIST = "list";
    public static final String DRAWER_STYLE_WP = "windows_phone";

    public static final String PREF_STATUS_BAR_STYLE = "status_bar_style";
    public static final String STATUS_BAR_AUTO = "auto";
    public static final String STATUS_BAR_DARK = "dark";
    public static final String STATUS_BAR_LIGHT = "light";

    public static final String PREF_DOCK_STYLE = "dock_style";
    public static final String DOCK_STYLE_AUTO = "auto";
    public static final String DOCK_STYLE_DARK = "dark";
    public static final String DOCK_STYLE_LIGHT = "light";

    private final String[] gestureOptions = { "Aucune action", "Notifications", "Paramètres", "Changer fond d'écran", "Assistant", "Recherche" };
    private final String[] gestureValues = { ACTION_NONE, ACTION_NOTIFICATIONS, ACTION_SETTINGS, ACTION_WALLPAPER, ACTION_ASSISTANT, ACTION_APP_SEARCH };

    private androidx.activity.result.ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest> pickMedia;
    private androidx.activity.result.ActivityResultLauncher<String> createBackupLauncher;
    private androidx.activity.result.ActivityResultLauncher<String[]> restoreBackupLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        createBackupLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json"),
                uri -> {
                    if (uri != null) {
                        try {
                            BackupHelper.backupSettings(this, uri);
                            android.widget.Toast
                                    .makeText(this, R.string.backup_success, android.widget.Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            android.widget.Toast.makeText(this, getString(R.string.backup_error) + e.getMessage(),
                                    android.widget.Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    }
                });

        restoreBackupLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        try {
                            BackupHelper.restoreSettings(this, uri);
                            android.widget.Toast
                                    .makeText(this, R.string.restore_success, android.widget.Toast.LENGTH_SHORT).show();
                            finishAffinity();
                            startActivity(new android.content.Intent(this, MainActivity.class));
                        } catch (Exception e) {
                            android.widget.Toast.makeText(this, getString(R.string.backup_error) + e.getMessage(),
                                    android.widget.Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    }
                });

        pickMedia = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri != null) {
                        // Persist permission (optional but good practice for some URI types, though
                        // PickVisualMedia usually grants temp access)
                        // For long term persistence, we should copy it or take persistable permission
                        // if possible.
                        // For now, let's try passing it directly to WallpaperManager which copies logic
                        // or uses it.
                        // WallpaperManager.saveCustomWallpaper handles stream copying if needed (it
                        // sets stream).
                        // Actually our updated WallpaperManager uses openInputStream.
                        // To ensure it persists across reboots if it's a content URI that requires
                        // permission,
                        // we might need to take persistable URI permission if supported, or copy the
                        // file.
                        // The Photo Picker returns a URI that we can read. Let's trust
                        // WallpaperManager.saveCustomWallpaper
                        // to set it as system wallpaper (which persists it system-wide).
                        // But for our app's background 'drawing', we need read access later.
                        // The most robust way is to copy it to app files.
                        try {
                            int takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
                            getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        } catch (Exception e) {
                            // Photo picker URIs might not support this, but standard open document does.
                            // Fallback: Just save it.
                        }

                        WallpaperManager.saveCustomWallpaper(this, uri);
                        android.widget.Toast
                                .makeText(this, R.string.wallpaper_applied, android.widget.Toast.LENGTH_SHORT)
                                .show();
                    }
                });

        LinearLayout btnLight = findViewById(R.id.btnLight);
        LinearLayout btnAmoled = findViewById(R.id.btnAmoled);
        LinearLayout btnSystem = findViewById(R.id.dark_night);
        LinearLayout btnGlass = findViewById(R.id.btnGlass);
        ImageView btnBack = findViewById(R.id.btnBack);
        LinearLayout btnChangeWallpaper = findViewById(R.id.btnChangeWallpaper);
        LinearLayout btnAssistant = findViewById(R.id.btnAssistant);

        btnChangeWallpaper.setOnClickListener(v -> showWallpaperBottomSheet());

        // Premium Features
        LinearLayout btnDynamicIslandSettings = findViewById(R.id.btnDynamicIslandSettings);
        if (btnDynamicIslandSettings != null) {
            btnDynamicIslandSettings.setOnClickListener(v -> {
                new com.vulsoft.vulsoftos.fragments.DynamicIslandSettingsBottomSheet().show(getSupportFragmentManager(), "DynamicIslandSettings");
            });
        }
        


        LinearLayout btnUniversalSearchSettings = findViewById(R.id.btnUniversalSearchSettings);
        if (btnUniversalSearchSettings != null) {
            btnUniversalSearchSettings.setOnClickListener(v -> {
                 new com.vulsoft.vulsoftos.UniversalSearchDialogFragment().show(getSupportFragmentManager(), "UniversalSearch");
            });
        }
        
        // Search Position Logic
        android.widget.RadioGroup radioSearchPosition = findViewById(R.id.radioSearchPosition);
        if (radioSearchPosition != null) {
            SharedPreferences prefs = getSharedPreferences("launcher_prefs", MODE_PRIVATE);
            boolean searchBarTop = prefs.getBoolean("search_bar_top", false);
            boolean showSearchBar = prefs.getBoolean("show_search_bar", false);
            
            if (searchBarTop) {
                radioSearchPosition.check(R.id.radioSearchTop);
            } else {
                radioSearchPosition.check(R.id.radioSearchBottom);
            }
            
            setRadioGroupEnabled(radioSearchPosition, showSearchBar);
            
            radioSearchPosition.setOnCheckedChangeListener((group, checkedId) -> {
                boolean isTop = (checkedId == R.id.radioSearchTop);
                prefs.edit().putBoolean("search_bar_top", isTop).apply();
            });
        }
        
        // Categories Button
        LinearLayout btnCategories = findViewById(R.id.btnCategories);
        if (btnCategories != null) {
            btnCategories.setOnClickListener(v -> {
                new com.vulsoft.vulsoftos.fragments.CategorySelectionDialogFragment().show(getSupportFragmentManager(), "CategorySelection");
            });
        }

        /* Icon Theme Button removed (Moved to GridSettingsBottomSheet)
        LinearLayout btnChangeIconTheme = findViewById(R.id.btnChangeIconTheme);
        if (btnChangeIconTheme != null) {
            btnChangeIconTheme.setOnClickListener(v -> showIconPackDialog());
        }
        */

        if (btnAssistant != null) {
            btnAssistant.setOnClickListener(v -> {
                startActivity(new android.content.Intent(this, AssistantActivity.class));
            });
        }

        btnLight.setOnClickListener(v -> {
            ThemeManager.saveTheme(this, ThemeManager.THEME_LIGHT);
            recreate();
        });

        btnAmoled.setOnClickListener(v -> {
            ThemeManager.saveTheme(this, ThemeManager.THEME_AMOLED);
            recreate();
        });

        btnSystem.setOnClickListener(v -> {
            ThemeManager.saveTheme(this, ThemeManager.THEME_SYSTEM);
            recreate();
        });

        btnGlass.setOnClickListener(v -> {
            ThemeManager.saveTheme(this, ThemeManager.THEME_GLASS);
            recreate();
        });

        // Drawer Style Settings
        LinearLayout btnStyleGrid = findViewById(R.id.btnStyleGrid);
        LinearLayout btnStyleList = findViewById(R.id.btnStyleList);

        btnStyleGrid.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("launcher_prefs", MODE_PRIVATE);
            prefs.edit().putString(PREF_DRAWER_STYLE, DRAWER_STYLE_GRID).apply();
            Toast.makeText(this, "Style Grille activé", Toast.LENGTH_SHORT).show();
            // Optional: update UI to reflect selection
        });

        btnStyleList.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("launcher_prefs", MODE_PRIVATE);
            prefs.edit().putString(PREF_DRAWER_STYLE, DRAWER_STYLE_LIST).apply();
            Toast.makeText(this, "Style Liste activé", Toast.LENGTH_SHORT).show();
        });

        LinearLayout btnStyleWP = findViewById(R.id.btnStyleWP);
        if (btnStyleWP != null) {
            btnStyleWP.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("launcher_prefs", MODE_PRIVATE);
                prefs.edit().putString(PREF_DRAWER_STYLE, DRAWER_STYLE_WP).apply();
                Toast.makeText(this, "Style Windows Phone activé", Toast.LENGTH_SHORT).show();
            });
        }

        // About & FAQ & Feedback
        findViewById(R.id.btnAbout)
                .setOnClickListener(v -> startActivity(new android.content.Intent(this, AboutActivity.class)));
        findViewById(R.id.btnFaq)
                .setOnClickListener(v -> startActivity(new android.content.Intent(this, FaqActivity.class)));
        
        View btnFeedback = findViewById(R.id.btnFeedback);
        if (btnFeedback != null) {
            btnFeedback.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("message/rfc822");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@vulsoft.com"});
                intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback Ruvolute OS");
                try {
                    startActivity(Intent.createChooser(intent, "Envoyer un email..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(SettingsActivity.this, "Aucun client mail installé.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Icon Customization (Moved to GridSettingsBottomSheet)
        /*
        findViewById(R.id.btnIconShape).setOnClickListener(v -> showIconShapeDialog());
        updateIconShapeSummary();

        findViewById(R.id.btnIconPack).setOnClickListener(v -> showIconPackDialog());
        updateIconPackSummary();
        */

        // Backup & Restore
        findViewById(R.id.btnBackup).setOnClickListener(v -> createBackupLauncher.launch("vulsoftos_backup.json"));
        findViewById(R.id.btnRestore)
                .setOnClickListener(v -> restoreBackupLauncher.launch(new String[] { "application/json" }));

        // Reset Params
        findViewById(R.id.btnResetParams).setOnClickListener(v -> showResetConfirmationDialog());

        /*
        findViewById(R.id.settingsLockScreen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Lock screen disabled
            }
        });
        */
        btnBack.setOnClickListener(v -> finish());

        LinearLayout btnSetDefault = findViewById(R.id.idSetDefaultLauncher);
        btnSetDefault.setOnClickListener(v -> {
            SystemIntegrationManager.requestDefaultLauncherRole(this);
        });

        // Ajouter boutons pour l'intégration système (si disponibles dans le layout)
        View btnSystemPermissions = findViewById(R.id.btnSystemPermissions);
        if (btnSystemPermissions != null) {
            btnSystemPermissions.setOnClickListener(v -> requestSystemPermissions());
        }

        View btnNotificationAccess = findViewById(R.id.btnNotificationAccess);
        if (btnNotificationAccess != null) {
            btnNotificationAccess.setOnClickListener(v -> requestNotificationAccess());
        }

        View btnHealthReport = findViewById(R.id.btnHealthReport);
        if (btnHealthReport != null) {
            btnHealthReport.setOnClickListener(v -> {
                startActivity(new android.content.Intent(this, AppHealthActivity.class));
            });
        }

        // Gestures Setup
        setupGesturePreference(findViewById(R.id.btnGestureSwipeUp), findViewById(R.id.txtGestureSwipeUp), KEY_GESTURE_SWIPE_UP, ACTION_NONE);
        setupGesturePreference(findViewById(R.id.btnGestureSwipeDown), findViewById(R.id.txtGestureSwipeDown), KEY_GESTURE_SWIPE_DOWN, ACTION_NOTIFICATIONS);
        setupGesturePreference(findViewById(R.id.btnGestureDoubleTap), findViewById(R.id.txtGestureDoubleTap), KEY_GESTURE_DOUBLE_TAP, ACTION_NONE);

        // Zen Mode Switch
        com.google.android.material.switchmaterial.SwitchMaterial switchZenMode = findViewById(R.id.switchZenMode);
        if (switchZenMode != null) {
            boolean isZenEnabled = getSharedPreferences("launcher_prefs", MODE_PRIVATE).getBoolean(KEY_ZEN_MODE, false);
            switchZenMode.setChecked(isZenEnabled);
            switchZenMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                getSharedPreferences("launcher_prefs", MODE_PRIVATE).edit()
                        .putBoolean(KEY_ZEN_MODE, isChecked).apply();
            });
        }

        // Search Bar Switch
        com.google.android.material.switchmaterial.SwitchMaterial switchSearchBar = findViewById(R.id.switchSearchBar);
        if (switchSearchBar != null) {
            boolean isSearchBarEnabled = getSharedPreferences("launcher_prefs", MODE_PRIVATE).getBoolean("show_search_bar", false);
            switchSearchBar.setChecked(isSearchBarEnabled);
            switchSearchBar.setOnCheckedChangeListener((buttonView, isChecked) -> {
                getSharedPreferences("launcher_prefs", MODE_PRIVATE).edit()
                        .putBoolean("show_search_bar", isChecked).apply();
                
                android.widget.RadioGroup rg = findViewById(R.id.radioSearchPosition);
                if (rg != null) {
                    setRadioGroupEnabled(rg, isChecked);
                }
            });
        }



        // Dock Background Switch
        com.google.android.material.switchmaterial.SwitchMaterial switchDockBg = findViewById(R.id.switchDockBg);
        if (switchDockBg != null) {
            boolean isDockBgEnabled = getSharedPreferences("launcher_prefs", MODE_PRIVATE).getBoolean("dock_bg_enabled",
                    true);
            switchDockBg.setChecked(isDockBgEnabled);
            switchDockBg.setOnCheckedChangeListener((buttonView, isChecked) -> {
                getSharedPreferences("launcher_prefs", MODE_PRIVATE).edit()
                        .putBoolean("dock_bg_enabled", isChecked).apply();
            });
        }

        // Shake to Change Wallpaper Switch
        com.google.android.material.switchmaterial.SwitchMaterial switchShakeWallpaper = findViewById(R.id.switchShakeWallpaper);
        if (switchShakeWallpaper != null) {
            boolean isShakeEnabled = getSharedPreferences("launcher_prefs", MODE_PRIVATE).getBoolean("shake_to_change_wallpaper", false);
            switchShakeWallpaper.setChecked(isShakeEnabled);
            switchShakeWallpaper.setOnCheckedChangeListener((buttonView, isChecked) -> {
                getSharedPreferences("launcher_prefs", MODE_PRIVATE).edit()
                        .putBoolean("shake_to_change_wallpaper", isChecked).apply();
            });
        }

        // Custom Lock Screen Switch removed as requested

        // Custom Lock Screen Customization Button
        /*
        LinearLayout btnCustomizeLockScreen = findViewById(R.id.btnCustomizeLockScreen);
        if (btnCustomizeLockScreen != null) {
            btnCustomizeLockScreen.setOnClickListener(v -> {
                // LockScreenSettingsActivity removed
            });
        }
        */

        // Screen Recorder Button
        LinearLayout btnScreenRecorder = findViewById(R.id.btnScreenRecorder);
        if (btnScreenRecorder != null) {
            btnScreenRecorder.setOnClickListener(v -> {
                startScreenRecorderService();
                Toast.makeText(this, "Lancement de l'enregistreur...", Toast.LENGTH_SHORT).show();
            });
        }

        // Grid Customization Button
        LinearLayout btnGridSettings = findViewById(R.id.btnGridSettings);
        if (btnGridSettings != null) {
            btnGridSettings.setOnClickListener(v -> {
                new com.vulsoft.vulsoftos.fragments.GridSettingsBottomSheet().show(getSupportFragmentManager(), "GridSettings");
            });
        }

        // Hide Notch Switch
        com.google.android.material.switchmaterial.SwitchMaterial switchHideNotch = findViewById(R.id.switchHideNotch);
        if (switchHideNotch != null) {
            boolean isHideNotchEnabled = getSharedPreferences("launcher_prefs", MODE_PRIVATE).getBoolean("hide_notch",
                    false);
            switchHideNotch.setChecked(isHideNotchEnabled);
            switchHideNotch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                getSharedPreferences("launcher_prefs", MODE_PRIVATE).edit().putBoolean("hide_notch", isChecked).apply();
            });
        }

        // Status Bar Style
        android.widget.RadioGroup radioStatusBarStyle = findViewById(R.id.radioStatusBarStyle);
        if (radioStatusBarStyle != null) {
            String currentStyle = getSharedPreferences("launcher_prefs", MODE_PRIVATE).getString(PREF_STATUS_BAR_STYLE, STATUS_BAR_AUTO);

            if (STATUS_BAR_DARK.equals(currentStyle)) {
                radioStatusBarStyle.check(R.id.statusDark);
            } else if (STATUS_BAR_LIGHT.equals(currentStyle)) {
                radioStatusBarStyle.check(R.id.statusLight);
            } else {
                radioStatusBarStyle.check(R.id.statusAuto);
            }

            radioStatusBarStyle.setOnCheckedChangeListener((group, checkedId) -> {
                String newStyle = STATUS_BAR_AUTO;
                if (checkedId == R.id.statusDark) {
                    newStyle = STATUS_BAR_DARK;
                } else if (checkedId == R.id.statusLight) {
                    newStyle = STATUS_BAR_LIGHT;
                }
                getSharedPreferences("launcher_prefs", MODE_PRIVATE).edit().putString(PREF_STATUS_BAR_STYLE, newStyle).apply();
            });
        }

        // Dock Style
        android.widget.RadioGroup radioDockStyle = findViewById(R.id.radioDockStyle);
        if (radioDockStyle != null) {
            String currentStyle = getSharedPreferences("launcher_prefs", MODE_PRIVATE).getString(PREF_DOCK_STYLE, DOCK_STYLE_AUTO);

            if (DOCK_STYLE_DARK.equals(currentStyle)) {
                radioDockStyle.check(R.id.dockDark);
            } else if (DOCK_STYLE_LIGHT.equals(currentStyle)) {
                radioDockStyle.check(R.id.dockLight);
            } else {
                radioDockStyle.check(R.id.dockAuto);
            }

            radioDockStyle.setOnCheckedChangeListener((group, checkedId) -> {
                String newStyle = DOCK_STYLE_AUTO;
                if (checkedId == R.id.dockDark) {
                    newStyle = DOCK_STYLE_DARK;
                } else if (checkedId == R.id.dockLight) {
                    newStyle = DOCK_STYLE_LIGHT;
                }
                getSharedPreferences("launcher_prefs", MODE_PRIVATE).edit().putString(PREF_DOCK_STYLE, newStyle).apply();
            });
        }

        /* Icon Customization moved to GridSettingsBottomSheet
        // Icon Shape SeekBar (Removed)
        // Icon Scale SeekBar (Removed)
        // Text Scale SeekBar (Removed)
        */
        android.widget.SeekBar seekBarBlur = findViewById(R.id.seekBarBlur);
        TextView txtBlurPercentage = findViewById(R.id.txtBlurPercentage);
        if (seekBarBlur != null && txtBlurPercentage != null) {
            int currentBlur = getSharedPreferences("launcher_prefs", MODE_PRIVATE).getInt("wallpaper_blur_radius", 0);
            seekBarBlur.setProgress(currentBlur);
            txtBlurPercentage.setText(currentBlur + "%");

            seekBarBlur.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                    txtBlurPercentage.setText(progress + "%");
                }

                @Override
                public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                    getSharedPreferences("launcher_prefs", MODE_PRIVATE).edit()
                            .putInt("wallpaper_blur_radius", seekBar.getProgress()).apply();
                }
            });
        }

        // Icon/Text Scale logic moved to GridSettingsBottomSheet


        // Language Selection
        android.widget.RadioGroup radioLanguage = findViewById(R.id.radioLanguage);
        if (radioLanguage != null) {
            String currentLang = LocaleHelper.getLanguage(this);
            if ("fr".equals(currentLang)) {
                radioLanguage.check(R.id.langFr);
            } else if ("zh".equals(currentLang)) {
                radioLanguage.check(R.id.langZh);
            } else {
                radioLanguage.check(R.id.langEn);
            }

            radioLanguage.setOnCheckedChangeListener((group, checkedId) -> {
                String newLang;
                if (checkedId == R.id.langFr) {
                    newLang = "fr";
                } else if (checkedId == R.id.langZh) {
                    newLang = "zh";
                } else {
                    newLang = "en";
                }

                if (!newLang.equals(currentLang)) {
                    LocaleHelper.setLocale(SettingsActivity.this, newLang);

                    // Restart app to apply language change
                    android.content.Intent intent = new android.content.Intent(SettingsActivity.this,
                            MainActivity.class);
                    intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            });
        }

        // Label Visibility Toggle
        com.google.android.material.switchmaterial.SwitchMaterial switchShowLabels = findViewById(
                R.id.switchShowLabels);
        if (switchShowLabels != null) {
            boolean showLabels = getSharedPreferences("launcher_prefs", MODE_PRIVATE).getBoolean("show_labels", true);
            switchShowLabels.setChecked(showLabels);
            switchShowLabels.setOnCheckedChangeListener((buttonView, isChecked) -> {
                getSharedPreferences("launcher_prefs", MODE_PRIVATE).edit()
                        .putBoolean("show_labels", isChecked).apply();
                android.widget.Toast.makeText(this, "Noms d'apps " + (isChecked ? "visibles" : "masqués"),
                        android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        // Biometric Security Switch
        com.google.android.material.switchmaterial.SwitchMaterial switchBiometric = findViewById(R.id.switchBiometric);
        if (switchBiometric != null) {
            SharedPreferences prefs = getSharedPreferences("launcher_prefs", MODE_PRIVATE);
            boolean isBiometricEnabled = prefs.getBoolean("biometric_enabled", false);
            switchBiometric.setChecked(isBiometricEnabled);

            // Check availability initially
            if (!BiometricHelper.isBiometricAvailable(this)) {
                switchBiometric.setEnabled(false);
            }

            switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    // Verify biometric before enabling
                    if (BiometricHelper.isBiometricAvailable(this)) {
                        BiometricHelper.authenticate(this, "Authentification requise",
                                "Confirmez votre identité pour activer la sécurité",
                                new BiometricHelper.AuthenticationCallback() {
                                    @Override
                                    public void onAuthenticationSucceeded() {
                                        prefs.edit().putBoolean("biometric_enabled", true).apply();
                                        android.widget.Toast.makeText(SettingsActivity.this,
                                                "Sécurité biométrique activée", android.widget.Toast.LENGTH_SHORT)
                                                .show();
                                    }

                                    @Override
                                    public void onAuthenticationFailed() {
                                        // Keep toggle off
                                        switchBiometric.setChecked(false);
                                        android.widget.Toast.makeText(SettingsActivity.this,
                                                "Échec de l'authentification", android.widget.Toast.LENGTH_SHORT)
                                                .show();
                                    }

                                    @Override
                                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                                        switchBiometric.setChecked(false);
                                        android.widget.Toast.makeText(SettingsActivity.this, "Erreur: " + errString,
                                                android.widget.Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        switchBiometric.setChecked(false);
                        android.widget.Toast.makeText(SettingsActivity.this,
                                "Biométrie non disponible sur cet appareil", android.widget.Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Disable directly
                    prefs.edit().putBoolean("biometric_enabled", false).apply();
                }
            });
        }

        // Hidden Apps Management
        findViewById(R.id.btnHiddenApps).setOnClickListener(v -> {
            SharedPreferences hiddenAppsPrefs = getSharedPreferences("launcher_prefs", MODE_PRIVATE);
            boolean isBioEnabled = hiddenAppsPrefs.getBoolean("biometric_enabled", false);

            if (isBioEnabled && BiometricHelper.isBiometricAvailable(this)) {
                BiometricHelper.authenticate(this, "Accès Sécurisé",
                        "Authentification requise pour voir les applications masquées",
                        new BiometricHelper.AuthenticationCallback() {
                            @Override
                            public void onAuthenticationSucceeded() {
                                showHiddenAppsDialog();
                            }

                            @Override
                            public void onAuthenticationFailed() {
                                android.widget.Toast.makeText(SettingsActivity.this, "Authentification échouée",
                                        android.widget.Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onAuthenticationError(int errorCode, CharSequence errString) {
                                android.widget.Toast.makeText(SettingsActivity.this, "Erreur: " + errString,
                                        android.widget.Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                showHiddenAppsDialog();
            }
        });

        // Transition Effect Logic
        LinearLayout btnTransitionEffect = findViewById(R.id.btnTransitionEffect);
        TextView txtCurrentTransition = findViewById(R.id.txtCurrentTransition);
        if (btnTransitionEffect != null && txtCurrentTransition != null) {
            String currentTransition = getSharedPreferences("launcher_prefs", MODE_PRIVATE)
                    .getString("transition_effect", "default");
            txtCurrentTransition.setText(getTransitionName(currentTransition));

            btnTransitionEffect.setOnClickListener(v -> showTransitionSelectionDialog(txtCurrentTransition));
        }

        // Focus Mode
        com.google.android.material.switchmaterial.SwitchMaterial switchFocus = findViewById(R.id.switchFocusMode);
        boolean isFocusEnabled = getSharedPreferences("launcher_prefs", MODE_PRIVATE).getBoolean("focus_mode_enabled",
                false);
        switchFocus.setChecked(isFocusEnabled);

        findViewById(R.id.btnFocusMode).setOnClickListener(v -> {
            boolean newState = !switchFocus.isChecked();
            switchFocus.setChecked(newState);
            getSharedPreferences("launcher_prefs", MODE_PRIVATE).edit()
                    .putBoolean("focus_mode_enabled", newState).apply();

            android.widget.Toast.makeText(this, "Focus Mode " + (newState ? "activé" : "désactivé"),
                    android.widget.Toast.LENGTH_SHORT).show();
            // We should ideally tell MainActivity to refresh, but for now just saving is
            // enough
            // as it will refresh on resume if implemented.
        });

        // Smart Folders
        int btnSmartFoldersId = getResources().getIdentifier("btnSmartFolders", "id", getPackageName());
        if (btnSmartFoldersId != 0) {
            findViewById(btnSmartFoldersId).setOnClickListener(v -> {
                android.content.pm.PackageManager pm = getPackageManager();
                AppLayoutManager.SavedLayout layout = AppLayoutManager.loadLayout(this, pm);
                if (layout != null) {
                    // Combine grid and dock for categorization
                    List<AppItem> allApps = new ArrayList<>();
                    if (layout.gridApps != null)
                        allApps.addAll(layout.gridApps);
                    if (layout.dockApps != null)
                        allApps.addAll(layout.dockApps);

                    // Only categorize real apps (not placeholders, not folders, not widgets)
                    List<AppItem> appsToCategorize = new ArrayList<>();
                    for (AppItem app : allApps) {
                        if (app.type == AppItem.Type.APP) {
                            appsToCategorize.add(app);
                        } else if (app.type == AppItem.Type.FOLDER) {
                            // If we want to re-categorize existing folders, we should unpack them
                            if (app.folderItems != null) {
                                appsToCategorize.addAll(app.folderItems);
                            }
                        }
                        // Widgets are left alone (but we need to preserve them in the grid if they were
                        // there)
                    }

                    java.util.Map<String, List<AppItem>> categories = CategorizationManager
                            .categorize(appsToCategorize);

                    if (categories.isEmpty()) {
                        android.widget.Toast
                                .makeText(this, "Aucune application à catégoriser", android.widget.Toast.LENGTH_SHORT)
                                .show();
                        return;
                    }

                    // Create new Grid list
                    List<AppItem> newGrid = new ArrayList<>();

                    // Add existing widgets first (preserve them)
                    if (layout.gridApps != null) {
                        for (AppItem item : layout.gridApps) {
                            if (item.type == AppItem.Type.WIDGET) {
                                newGrid.add(item);
                            }
                        }
                    }

                    // Create folders
                    for (java.util.Map.Entry<String, List<AppItem>> entry : categories.entrySet()) {
                        if (entry.getValue().size() >= 2) {
                            AppItem folder = new AppItem();
                            folder.type = AppItem.Type.FOLDER;
                            folder.label = entry.getKey();
                            folder.folderItems = entry.getValue();
                            newGrid.add(folder);
                        } else {
                            // If only 1 item, don't make a folder
                            newGrid.addAll(entry.getValue());
                        }
                    }

                    // Add remaining apps that were not categorized
                    java.util.Set<String> categorizedPkgs = new java.util.HashSet<>();
                    for (List<AppItem> list : categories.values()) {
                        for (AppItem a : list) {
                            if (a.packageName != null)
                                categorizedPkgs.add(a.packageName);
                        }
                    }

                    for (AppItem app : appsToCategorize) {
                        if (app.packageName != null && !categorizedPkgs.contains(app.packageName)) {
                            newGrid.add(app);
                        }
                    }

                    // Save layout (Dock is cleared as apps moved to grid/folders)
                    AppLayoutManager.saveLayout(this, newGrid, new ArrayList<>());

                    android.widget.Toast.makeText(this, "Applications organisées en Smart Folders !",
                            android.widget.Toast.LENGTH_SHORT).show();
                    finish(); // Return to home to see changes
                }
            });
        }

        // Handle Insets
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content),
                (v, insets) -> {
                    androidx.core.graphics.Insets systemBars = insets
                            .getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                });
        
        setupSearch();
    }

    /* Icon Customization moved to GridSettingsBottomSheet
    private void showIconShapeDialog() {
        // ...
    }
    // ...
    */


    private String getTransitionName(String key) {
        switch (key) {
            case "zoom":
                return "Zoom";
            case "depth":
                return "Profondeur";
            default:
                return "Par défaut";
        }
    }

    private void showTransitionSelectionDialog(TextView txtStatus) {
        final String[] options = { "Par défaut", "Zoom", "Profondeur" };
        final String[] keys = { "default", "zoom", "depth" };

        String currentKey = getSharedPreferences("launcher_prefs", MODE_PRIVATE).getString("transition_effect",
                "default");
        int checkedItem = 0;
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].equals(currentKey)) {
                checkedItem = i;
                break;
            }
        }

        new com.vulsoft.vulsoftos.utils.ModernDialogHelper.Builder(this)
                .setTitle("Animation de défilement")
                .setSingleChoiceItems(options, checkedItem, (index, value) -> {
                    String selectedKey = keys[index];
                    getSharedPreferences("launcher_prefs", MODE_PRIVATE).edit()
                            .putString("transition_effect", selectedKey).apply();
                    txtStatus.setText(options[index]);
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void showResetConfirmationDialog() {
        new com.vulsoft.vulsoftos.utils.ModernDialogHelper.Builder(this)
                .setTitle("Réinitialisation")
                .setMessage(
                        "Êtes-vous sûr de vouloir réinitialiser tous les paramètres ?\nCette action est irréversible.")
                .setPositiveButton("Réinitialiser", (v) -> resetToFactorySettings())
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void resetToFactorySettings() {
        android.content.SharedPreferences prefs = getSharedPreferences("launcher_prefs", MODE_PRIVATE);
        prefs.edit().clear().apply();
        WallpaperManager.saveWallpaper(this, R.drawable.wallpaper_blue);
        ThemeManager.saveTheme(this, ThemeManager.THEME_SYSTEM);
        finishAffinity();
        startActivity(new android.content.Intent(this, MainActivity.class));
    }

    private void setupGesturePreference(View btn, TextView statusView, String key, String defaultValue) {
        if (btn == null || statusView == null) return;
        
        SharedPreferences prefs = getSharedPreferences("launcher_prefs", MODE_PRIVATE);
        String currentVal = prefs.getString(key, defaultValue);
        statusView.setText(getLabelForValue(currentVal));

        btn.setOnClickListener(v -> {
            int checkedItem = 0;
            String current = prefs.getString(key, defaultValue);
            for (int i = 0; i < gestureValues.length; i++) {
                if (gestureValues[i].equals(current)) {
                    checkedItem = i;
                    break;
                }
            }

            new com.vulsoft.vulsoftos.utils.ModernDialogHelper.Builder(this)
                .setTitle("Choisir une action")
                .setSingleChoiceItems(gestureOptions, checkedItem, (index, value) -> {
                    String selectedValue = gestureValues[index];
                    prefs.edit().putString(key, selectedValue).apply();
                    statusView.setText(gestureOptions[index]);
                })
                .setNegativeButton("Annuler", null)
                .show();
        });
    }

    private String getLabelForValue(String value) {
        for (int i = 0; i < gestureValues.length; i++) {
            if (gestureValues[i].equals(value)) {
                return gestureOptions[i];
            }
        }
        return gestureOptions[0];
    }

    private void showWallpaperBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_wallpaper_bottom_sheet, null);
        bottomSheetDialog.setContentView(view);

        androidx.recyclerview.widget.RecyclerView recyclerWallpapers = view.findViewById(R.id.recyclerWallpapers);
        // btnApplyWallpaper removed from layout

        List<Integer> wallpapers = new ArrayList<>(Arrays.asList(
                R.drawable.wallpaper_purple,
                R.drawable.wallpaper_blue,
                R.drawable.wallpaper_dark,
                R.drawable.wallpaper_black,
                R.drawable.wallpaper_orange,
                R.drawable.wallpaper_green,
                R.drawable.gradient_fire,

                R.drawable.gradient_bg,
                R.drawable.gradient_night,
                R.drawable.gradient_radial,
                R.drawable.gradient_ocean,
                R.drawable.gradient_border_glass,
                R.drawable.gradient_moun,
                R.drawable.gradient_forest,
                R.drawable.gradient_sunset,
                R.drawable.gradient_purple_pink,

                R.drawable.benin,
                R.drawable.burkina_faso,
                R.drawable.ghana,
                R.drawable.guinea,
                R.drawable.ivory_cost,
                R.drawable.mali,
                R.drawable.morocco,
                R.drawable.nageria));

        // Get current wallpaper to show selection
        int currentWallpaper = WallpaperManager.getSavedWallpaper(this);
        int initialSelection = wallpapers.indexOf(currentWallpaper);

        WallpaperAdapter adapter = new WallpaperAdapter(wallpapers, position -> {
            // Update selection UI
            ((WallpaperAdapter) recyclerWallpapers.getAdapter()).setSelectedPosition(position);

            // Apply wallpaper immediately
            int selectedWallpaperRes = wallpapers.get(position);
            WallpaperManager.saveWallpaper(this, selectedWallpaperRes);
            android.widget.Toast.makeText(this, "Fond d'écran appliqué", android.widget.Toast.LENGTH_SHORT).show();
        });

        recyclerWallpapers.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 3));
        recyclerWallpapers.setAdapter(adapter);

        if (initialSelection != -1) {
            adapter.setSelectedPosition(initialSelection);
            recyclerWallpapers.scrollToPosition(initialSelection);
        }

        LinearLayout btnPickGallery = view.findViewById(R.id.btnPickGallery);

        if (btnPickGallery != null) {
            btnPickGallery.setOnClickListener(v -> {
                pickMedia.launch(new androidx.activity.result.PickVisualMediaRequest.Builder()
                        .setMediaType(
                                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
                bottomSheetDialog.dismiss();
            });
        }

        bottomSheetDialog.show();
    }

    private void requestDefaultLauncherRole() {
        SystemIntegrationManager.requestDefaultLauncherRole(this);
    }

    private void requestSystemPermissions() {
        SystemIntegrationManager.requestSystemPermissions(this);
    }

    private void requestNotificationAccess() {
        SystemIntegrationManager.requestNotificationAccess(this);
    }

    private void openHomeSettings() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            android.app.role.RoleManager roleManager = (android.app.role.RoleManager) getSystemService(
                    android.content.Context.ROLE_SERVICE);
            if (roleManager != null && roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME)) {
                if (!roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_HOME)) {
                    android.content.Intent intent = roleManager
                            .createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME);
                    startActivityForResult(intent, 123);
                    return;
                }
            }
        }

        // Fallback for older versions or if RoleManager fails
        android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_HOME_SETTINGS);
        try {
            startActivity(intent);
        } catch (Exception e) {
            // If ACTION_HOME_SETTINGS is not supported, try the generic main home intent
            android.content.Intent homeIntent = new android.content.Intent(android.content.Intent.ACTION_MAIN);
            homeIntent.addCategory(android.content.Intent.CATEGORY_HOME);
            homeIntent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(homeIntent);
        }
    }

    private void savePreference(String key, String value) {
        getSharedPreferences("launcher_prefs", MODE_PRIVATE)
                .edit()
                .putString(key, value)
                .apply();
        android.widget.Toast.makeText(this, "Réglage enregistré", android.widget.Toast.LENGTH_SHORT).show();
    }

    private void saveWidgetColorPreference(String color) {
        WidgetCustomizationManager.saveWidgetTextColor(this, color);
        android.widget.Toast.makeText(this, "Couleur de widget enregistrée", android.widget.Toast.LENGTH_SHORT).show();
    }

    private class HiddenAppAdapter
            extends androidx.recyclerview.widget.RecyclerView.Adapter<HiddenAppAdapter.ViewHolder> {
        private List<AppInfo> apps;
        private BottomSheetDialog dialog;

        public HiddenAppAdapter(List<AppInfo> apps, BottomSheetDialog dialog) {
            this.apps = apps;
            this.dialog = dialog;
        }

        @androidx.annotation.NonNull
        @Override
        public ViewHolder onCreateViewHolder(@androidx.annotation.NonNull android.view.ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_item, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull ViewHolder holder, int position) {
            AppInfo app = apps.get(position);
            holder.text.setText(app.getLabel());
            holder.text.setVisibility(View.VISIBLE);
            holder.icon.setImageDrawable(app.getIcon());
            holder.itemView.setOnClickListener(v -> {
                // Remove from SharedPreferences
                SharedPreferences prefs = holder.itemView.getContext().getSharedPreferences("launcher_prefs",
                        Context.MODE_PRIVATE);
                java.util.Set<String> hiddenApps = prefs.getStringSet("hidden_apps", new java.util.HashSet<>());
                java.util.Set<String> newHidden = new java.util.HashSet<>(hiddenApps);

                if (newHidden.remove(app.getPackageName())) {
                    prefs.edit().putStringSet("hidden_apps", newHidden).apply();
                }

                android.widget.Toast.makeText(SettingsActivity.this, app.getLabel() + " restaurée",
                        android.widget.Toast.LENGTH_SHORT).show();

                int actualPos = holder.getBindingAdapterPosition();
                if (actualPos != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                    apps.remove(actualPos);
                    notifyItemRemoved(actualPos);
                    // notifyItemRangeChanged is often needed to update positions of subsequent
                    // items
                    notifyItemRangeChanged(actualPos, apps.size());
                }

                if (apps.isEmpty()) {
                    dialog.dismiss();
                }
            });
        }

        @Override
        public int getItemCount() {
            return apps.size();
        }

        class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            ImageView icon;
            TextView text;

            public ViewHolder(@androidx.annotation.NonNull View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.icon);
                text = itemView.findViewById(R.id.label);
            }
        }
    }

    private void setupSearch() {
        androidx.appcompat.widget.SearchView searchView = findViewById(R.id.settingsSearch);
        
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSettings(newText);
                return true;
            }
        });
    }

    private void filterSettings(String query) {
        LinearLayout container = findViewById(R.id.settingsContainer);
        if (container == null) return;
        
        String lowerQuery = query.toLowerCase().trim();
        boolean isSearching = !lowerQuery.isEmpty();
        
        // Handle title visibility
        TextView titleView = findViewById(R.id.settingsTitle);
        if (titleView != null) titleView.setVisibility(isSearching ? View.GONE : View.VISIBLE);

        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child.getId() == R.id.settingsSearch || child.getId() == R.id.settingsTitle) continue;
            
            if (isSearching) {
                // If Divider (height 1dp), hide
                if (child.getLayoutParams().height == 1) {
                    child.setVisibility(View.GONE);
                    continue;
                }
                
                // If Section Header (TextView), hide
                if (child instanceof TextView) {
                    child.setVisibility(View.GONE); 
                    continue;
                }
                
                // Process Item/Group
                boolean match = checkAndFilterView(child, lowerQuery);
                child.setVisibility(match ? View.VISIBLE : View.GONE);
            } else {
                // Restore
                restoreVisibility(child);
            }
        }
    }

    private boolean checkAndFilterView(View view, String query) {
        // If it's a clickable item (Button/Switch), check its text content
        // We consider it a "leaf unit" if it's clickable or a compound button
        if (view.hasOnClickListeners() || view instanceof android.widget.CompoundButton) {
            boolean hasText = checkTextDeep(view, query);
            return hasText;
        }
        
        // If it's a container (Layout) but NOT clickable itself
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            boolean anyChildVisible = false;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                boolean childMatch = checkAndFilterView(child, query);
                child.setVisibility(childMatch ? View.VISIBLE : View.GONE);
                if (childMatch) anyChildVisible = true;
            }
            return anyChildVisible;
        }
        
        // Leaf view (TextView) that is not clickable
        if (view instanceof TextView) {
             return ((TextView) view).getText().toString().toLowerCase().contains(query);
        }
        
        return false;
    }

    private boolean checkTextDeep(View view, String query) {
        if (view instanceof TextView) {
            return ((TextView) view).getText().toString().toLowerCase().contains(query);
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (checkTextDeep(group.getChildAt(i), query)) return true;
            }
        }
        return false;
    }

    private void restoreVisibility(View view) {
        view.setVisibility(View.VISIBLE);
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                restoreVisibility(group.getChildAt(i));
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1235) { // Screen Recorder Overlay Permission
             if (android.provider.Settings.canDrawOverlays(this)) {
                 startScreenRecorderService();
             } else {
                 android.widget.Toast.makeText(this, "Permission de superposition refusée", android.widget.Toast.LENGTH_SHORT).show();
             }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1236) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                startScreenRecorderService();
            } else {
                android.widget.Toast.makeText(this, "Permissions nécessaires refusées", android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startScreenRecorderService() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 1235);
            return;
        }
        
        java.util.List<String> permissionsNeeded = new java.util.ArrayList<>();
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.RECORD_AUDIO);
        }
        
        // WRITE_EXTERNAL_STORAGE is only needed for API < 29
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
        
        if (!permissionsNeeded.isEmpty()) {
            requestPermissions(permissionsNeeded.toArray(new String[0]), 1236);
            return;
        }
        
        android.content.Intent serviceIntent = new android.content.Intent(this, com.vulsoft.vulsoftos.ScreenRecorderService.class);
        serviceIntent.setAction(com.vulsoft.vulsoftos.ScreenRecorderService.ACTION_SHOW_CONTROLS);
        startService(serviceIntent);
    }

    private void stopScreenRecorderService() {
        android.content.Intent serviceIntent = new android.content.Intent(this, com.vulsoft.vulsoftos.ScreenRecorderService.class);
        serviceIntent.setAction(com.vulsoft.vulsoftos.ScreenRecorderService.ACTION_STOP_SERVICE);
        startService(serviceIntent);
    }

    private void showHiddenAppsDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_recent_apps, null); // Reuse recents layout for
        // list
        dialog.setContentView(view);

        TextView title = view.findViewById(R.id.txtRecentAppsTitle);
        if (title != null)
            title.setText("Restaurer des applications");

        androidx.recyclerview.widget.RecyclerView recycler = view.findViewById(R.id.recyclerRecentApps);
        recycler.setLayoutManager(new GridLayoutManager(this, 4));

        // Load hidden apps from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("launcher_prefs", MODE_PRIVATE);
        java.util.Set<String> hiddenPackages = prefs.getStringSet("hidden_apps", new java.util.HashSet<>());

        if (hiddenPackages.isEmpty()) {
            android.widget.Toast.makeText(this, "Aucune application masquée", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        List<AppInfo> hiddenAppsList = new ArrayList<>();
        android.content.pm.PackageManager pm = getPackageManager();

        for (String pkg : hiddenPackages) {
            try {
                android.content.pm.ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                AppInfo appInfo = new AppInfo();
                appInfo.setPackageName(pkg);
                appInfo.setLabel(pm.getApplicationLabel(ai).toString());
                appInfo.setIcon(pm.getApplicationIcon(ai));
                appInfo.setHidden(true);
                hiddenAppsList.add(appInfo);
            } catch (android.content.pm.PackageManager.NameNotFoundException e) {
                // App might have been uninstalled
                continue;
            }
        }

        if (hiddenAppsList.isEmpty()) {
            android.widget.Toast.makeText(this, "Aucune application masquée trouvée", android.widget.Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        // Use custom HiddenAppAdapter for unhiding
        HiddenAppAdapter adapter = new HiddenAppAdapter(hiddenAppsList, dialog);
        recycler.setAdapter(adapter);

        dialog.show();
    }

    private void setRadioGroupEnabled(android.widget.RadioGroup radioGroup, boolean enabled) {
        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            radioGroup.getChildAt(i).setEnabled(enabled);
        }
    }

}