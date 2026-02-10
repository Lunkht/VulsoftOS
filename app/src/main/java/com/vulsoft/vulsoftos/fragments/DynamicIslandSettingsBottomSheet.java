package com.vulsoft.vulsoftos.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.vulsoft.vulsoftos.DynamicIslandService;
import com.vulsoft.vulsoftos.R;

public class DynamicIslandSettingsBottomSheet extends BottomSheetDialogFragment {

    private SharedPreferences prefs;
    private SwitchMaterial switchEnable, switchHideInLandscape;
    private LinearLayout styleStandard, styleGlassDark, styleGlassBlur, styleLiquidBlue;
    private android.widget.SeekBar seekDuration, seekYOffset;
    private android.widget.TextView lblDurationValue, lblYOffsetValue;
    private android.widget.RadioGroup rgPosition;
    private android.widget.RadioButton rbPositionCenter, rbPositionLeft;

    private final ActivityResultLauncher<Intent> overlayPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (android.provider.Settings.canDrawOverlays(requireContext())) {
                            prefs.edit().putBoolean("dynamic_island_enabled", true).apply();
                            switchEnable.setChecked(true);
                            requireContext().startForegroundService(new Intent(requireContext(), DynamicIslandService.class));
                        } else {
                            switchEnable.setChecked(false);
                            Toast.makeText(getContext(), "Permission refusée", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_dynamic_island_settings, container, false);

        prefs = requireContext().getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE);

        switchEnable = view.findViewById(R.id.switchEnable);
        switchHideInLandscape = view.findViewById(R.id.switchHideInLandscape);
        styleStandard = view.findViewById(R.id.styleStandard);
        styleGlassDark = view.findViewById(R.id.styleGlassDark);
        styleGlassBlur = view.findViewById(R.id.styleGlassBlur);
        styleLiquidBlue = view.findViewById(R.id.styleLiquidBlue);
        
        seekDuration = view.findViewById(R.id.seekDuration);
        lblDurationValue = view.findViewById(R.id.lblDurationValue);
        seekYOffset = view.findViewById(R.id.seekYOffset);
        lblYOffsetValue = view.findViewById(R.id.lblYOffsetValue);
        
        rgPosition = view.findViewById(R.id.rgPosition);
        rbPositionCenter = view.findViewById(R.id.rbPositionCenter);
        rbPositionLeft = view.findViewById(R.id.rbPositionLeft);

        setupSwitch();
        setupStyles();
        setupSliders();
        setupPosition();
        
        view.findViewById(R.id.btnTestNotification).setOnClickListener(v -> {
            if (switchEnable.isChecked()) {
                Intent intent = new Intent(requireContext(), DynamicIslandService.class);
                intent.setAction("com.vulsoft.vulsoftos.DYNAMIC_ISLAND_SHOW");
                intent.putExtra("title", "Test Premium");
                intent.putExtra("text", "Ceci est une notification de test.");
                intent.putExtra("package", requireContext().getPackageName());
                requireContext().startService(intent);
            } else {
                Toast.makeText(getContext(), "Veuillez d'abord activer Dynamic Island", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void setupSwitch() {
        boolean isEnabled = prefs.getBoolean("dynamic_island_enabled", false);
        switchEnable.setChecked(isEnabled);
        switchEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (android.provider.Settings.canDrawOverlays(requireContext())) {
                    prefs.edit().putBoolean("dynamic_island_enabled", true).apply();
                    requireContext().startForegroundService(new Intent(requireContext(), DynamicIslandService.class));
                } else {
                    buttonView.setChecked(false); // Reset switch until permission granted
                    Intent intent = new Intent(
                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:" + requireContext().getPackageName()));
                    overlayPermissionLauncher.launch(intent);
                    Toast.makeText(getContext(), "Veuillez accorder la permission de superposition", Toast.LENGTH_LONG).show();
                }
            } else {
                prefs.edit().putBoolean("dynamic_island_enabled", false).apply();
                requireContext().stopService(new Intent(requireContext(), DynamicIslandService.class));
            }
        });

        // Hide in Landscape
        boolean isHideInLandscape = prefs.getBoolean("dynamic_island_hide_landscape", true);
        switchHideInLandscape.setChecked(isHideInLandscape);
        switchHideInLandscape.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dynamic_island_hide_landscape", isChecked).apply();
            
            // Restart service to apply change immediately if enabled
            if (switchEnable.isChecked()) {
                requireContext().stopService(new Intent(requireContext(), DynamicIslandService.class));
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    requireContext().startForegroundService(new Intent(requireContext(), DynamicIslandService.class));
                }, 300);
            }
        });
    }

    private void setupStyles() {
        String currentStyle = prefs.getString("dynamic_island_style", "default");
        updateStyleSelection(currentStyle);

        View.OnClickListener styleListener = v -> {
            String style = "default";
            if (v.getId() == R.id.styleGlassDark) style = "glass_dark";
            else if (v.getId() == R.id.styleGlassBlur) style = "glass_blur";
            else if (v.getId() == R.id.styleLiquidBlue) style = "liquid_blue";

            prefs.edit().putString("dynamic_island_style", style).apply();
            updateStyleSelection(style);
            
            // Restart service to apply style if enabled
            if (switchEnable.isChecked()) {
                // Arrêter puis redémarrer le service pour appliquer le nouveau style
                requireContext().stopService(new Intent(requireContext(), DynamicIslandService.class));
                // Petit délai pour s'assurer que le service est bien arrêté
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    requireContext().startForegroundService(new Intent(requireContext(), DynamicIslandService.class));
                    android.widget.Toast.makeText(requireContext(), "Style appliqué", android.widget.Toast.LENGTH_SHORT).show();
                }, 300);
            }
        };

        styleStandard.setOnClickListener(styleListener);
        styleGlassDark.setOnClickListener(styleListener);
        styleGlassBlur.setOnClickListener(styleListener);
        styleLiquidBlue.setOnClickListener(styleListener);
    }

    private void updateStyleSelection(String style) {
        styleStandard.setAlpha(style.equals("default") ? 1.0f : 0.5f);
        styleGlassDark.setAlpha(style.equals("glass_dark") ? 1.0f : 0.5f);
        styleGlassBlur.setAlpha(style.equals("glass_blur") ? 1.0f : 0.5f);
        styleLiquidBlue.setAlpha(style.equals("liquid_blue") ? 1.0f : 0.5f);
        
        styleStandard.setBackgroundResource(style.equals("default") ? R.drawable.bg_widget_selection : 0);
        styleGlassDark.setBackgroundResource(style.equals("glass_dark") ? R.drawable.bg_widget_selection : 0);
        styleGlassBlur.setBackgroundResource(style.equals("glass_blur") ? R.drawable.bg_widget_selection : 0);
        styleLiquidBlue.setBackgroundResource(style.equals("liquid_blue") ? R.drawable.bg_widget_selection : 0);
    }

    private void setupSliders() {
        // Duration: 2s to 10s (Stored in millis)
        int durationMillis = prefs.getInt("dynamic_island_duration", 4000);
        int durationProgress = (durationMillis / 1000) - 2;
        if (durationProgress < 0) durationProgress = 0;
        if (durationProgress > 8) durationProgress = 8;
        
        seekDuration.setProgress(durationProgress);
        lblDurationValue.setText((durationProgress + 2) + "s");
        
        seekDuration.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                int seconds = progress + 2;
                lblDurationValue.setText(seconds + "s");
                if (fromUser) {
                    prefs.edit().putInt("dynamic_island_duration", seconds * 1000).apply();
                }
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        });

        // Y Offset: -50dp to +50dp (Stored in dp)
        int offsetDp = prefs.getInt("dynamic_island_y_offset", 0);
        int offsetProgress = offsetDp + 50;
        if (offsetProgress < 0) offsetProgress = 0;
        if (offsetProgress > 100) offsetProgress = 100;
        
        seekYOffset.setProgress(offsetProgress);
        lblYOffsetValue.setText(offsetDp + "dp");
        
        seekYOffset.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                int dp = progress - 50;
                lblYOffsetValue.setText(dp + "dp");
                if (fromUser) {
                    prefs.edit().putInt("dynamic_island_y_offset", dp).apply();
                }
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            @Override 
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                // Redémarrer le service pour appliquer la nouvelle position
                if (switchEnable.isChecked()) {
                    requireContext().stopService(new Intent(requireContext(), DynamicIslandService.class));
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        requireContext().startForegroundService(new Intent(requireContext(), DynamicIslandService.class));
                    }, 300);
                }
            }
        });
    }

    private void setupPosition() {
        String position = prefs.getString("dynamic_island_position", "center");
        if ("left".equals(position)) {
            rbPositionLeft.setChecked(true);
        } else {
            rbPositionCenter.setChecked(true);
        }

        rgPosition.setOnCheckedChangeListener((group, checkedId) -> {
            String newPos = "center";
            if (checkedId == R.id.rbPositionLeft) {
                newPos = "left";
            }
            prefs.edit().putString("dynamic_island_position", newPos).apply();

            // Restart service to apply change
            if (switchEnable.isChecked()) {
                requireContext().stopService(new Intent(requireContext(), DynamicIslandService.class));
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    requireContext().startForegroundService(new Intent(requireContext(), DynamicIslandService.class));
                }, 300);
            }
        });
    }
}
