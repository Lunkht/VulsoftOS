package com.vulsoft.vulsoftos.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.app.AlertDialog;
import com.vulsoft.vulsoftos.IconShapeHelper;
import com.vulsoft.vulsoftos.IconPackManager;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.vulsoft.vulsoftos.R;
import com.vulsoft.vulsoftos.MainActivity;

public class GridSettingsBottomSheet extends BottomSheetDialogFragment {

    private SharedPreferences prefs;
    private static final String PREFS_NAME = "launcher_prefs";
    
    // UI Elements
    private TextView txtColumns;
    private TextView txtIconSizeValue;
    private TextView txtVerticalSpacingValue;
    private TextView txtFontSizeValue;
    private TextView txtIconShape;
    private TextView txtIconPack;
    
    private SeekBar seekBarIconSize;
    private SeekBar seekBarVerticalSpacing;
    private SeekBar seekBarFontSize;
    
    private SwitchMaterial switchIconTitleVisibility;
    private SwitchMaterial switchTwoLineTitles;
    
    private int currentColumns;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_grid_settings, container, false);
        
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        bindViews(view);
        setupListeners();
        loadValues();
        
        return view;
    }
    
    private void bindViews(View view) {
        txtColumns = view.findViewById(R.id.txtColumns);
        txtIconSizeValue = view.findViewById(R.id.txtIconSizeValue);
        txtVerticalSpacingValue = view.findViewById(R.id.txtVerticalSpacingValue);
        txtFontSizeValue = view.findViewById(R.id.txtFontSizeValue);
        txtIconShape = view.findViewById(R.id.txtIconShape);
        txtIconPack = view.findViewById(R.id.txtIconPack);
        
        seekBarIconSize = view.findViewById(R.id.seekBarIconSize);
        seekBarVerticalSpacing = view.findViewById(R.id.seekBarVerticalSpacing);
        seekBarFontSize = view.findViewById(R.id.seekBarFontSize);
        
        switchIconTitleVisibility = view.findViewById(R.id.switchIconTitleVisibility);
        switchTwoLineTitles = view.findViewById(R.id.switchTwoLineTitles);
        
        // Buttons
        view.findViewById(R.id.btnColumnsMinus).setOnClickListener(v -> changeColumns(-1));
        view.findViewById(R.id.btnColumnsPlus).setOnClickListener(v -> changeColumns(1));
        view.findViewById(R.id.btnResetIconSize).setOnClickListener(v -> resetIconSize());
        view.findViewById(R.id.btnResetVerticalSpacing).setOnClickListener(v -> resetVerticalSpacing());
        view.findViewById(R.id.btnResetFontSize).setOnClickListener(v -> resetFontSize());
        view.findViewById(R.id.btnIconShape).setOnClickListener(v -> showIconShapeDialog());
        view.findViewById(R.id.btnIconPack).setOnClickListener(v -> showIconPackDialog());
    }
    
    private void setupListeners() {
        seekBarIconSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 20) progress = 20; // Minimum size constraint
                txtIconSizeValue.setText(progress + "%");
                if (fromUser) saveIntPreference("icon_scale", progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        seekBarVerticalSpacing.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txtVerticalSpacingValue.setText(progress + " dp");
                if (fromUser) saveIntPreference("grid_vertical_spacing", progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        seekBarFontSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 20) progress = 20; // Minimum size constraint
                txtFontSizeValue.setText(progress + "%");
                if (fromUser) saveIntPreference("text_scale", progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        switchIconTitleVisibility.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveBooleanPreference("show_labels", isChecked);
        });
        
        switchTwoLineTitles.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveBooleanPreference("two_line_titles", isChecked);
        });
    }
    
    private void loadValues() {
        // Columns
        currentColumns = prefs.getInt("grid_columns", -1);
        if (currentColumns == -1) {
            String gridPref = prefs.getString("icon_grid_size", "4x6");
            currentColumns = "5x6".equals(gridPref) ? 5 : 4;
        }
        txtColumns.setText(String.valueOf(currentColumns));
        
        // Icon Size
        int iconScale = prefs.getInt("icon_scale", 100);
        seekBarIconSize.setProgress(iconScale);
        txtIconSizeValue.setText(iconScale + "%");
        
        // Vertical Spacing
        int spacing = prefs.getInt("grid_vertical_spacing", 0);
        seekBarVerticalSpacing.setProgress(spacing);
        txtVerticalSpacingValue.setText(spacing + " dp");
        
        // Font Size
        int textScale = prefs.getInt("text_scale", 100);
        seekBarFontSize.setProgress(textScale);
        txtFontSizeValue.setText(textScale + "%");
        
        // Switches
        switchIconTitleVisibility.setChecked(prefs.getBoolean("show_labels", true));
        switchTwoLineTitles.setChecked(prefs.getBoolean("two_line_titles", false));
        
        updateIconShapeSummary();
        updateIconPackSummary();
    }
    
    private void changeColumns(int delta) {
        int newColumns = currentColumns + delta;
        if (newColumns < 3) newColumns = 3;
        if (newColumns > 6) newColumns = 6;
        
        if (newColumns != currentColumns) {
            currentColumns = newColumns;
            txtColumns.setText(String.valueOf(currentColumns));
            saveIntPreference("grid_columns", currentColumns);
        }
    }
    
    private void resetIconSize() {
        seekBarIconSize.setProgress(100);
        saveIntPreference("icon_scale", 100);
    }
    
    private void resetVerticalSpacing() {
        seekBarVerticalSpacing.setProgress(0);
        saveIntPreference("grid_vertical_spacing", 0);
    }
    
    private void resetFontSize() {
        seekBarFontSize.setProgress(100);
        saveIntPreference("text_scale", 100);
    }
    
    private void saveIntPreference(String key, int value) {
        prefs.edit().putInt(key, value).apply();
        notifyChanges();
    }
    
    private void saveBooleanPreference(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
        notifyChanges();
    }
    
    private void notifyChanges() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).refreshAdapters();
        }
    }

    private void showIconShapeDialog() {
        final String[] options = { "Original", "Cercle", "Carré Arrondi", "Squircle", "Goutte d'eau" };
        final String[] values = { IconShapeHelper.SHAPE_ORIGINAL, IconShapeHelper.SHAPE_CIRCLE, IconShapeHelper.SHAPE_ROUNDED_SQUARE, IconShapeHelper.SHAPE_SQUIRCLE, IconShapeHelper.SHAPE_TEARDROP };

        String currentShape = IconShapeHelper.getSavedShape(requireContext());
        int checkedItem = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(currentShape)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Forme des icônes")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    IconShapeHelper.saveShape(requireContext(), values[which]);
                    updateIconShapeSummary();
                    dialog.dismiss();
                    if (getActivity() != null) getActivity().recreate();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void updateIconShapeSummary() {
        if (txtIconShape == null) return;
        String current = IconShapeHelper.getSavedShape(requireContext());
        switch (current) {
            case IconShapeHelper.SHAPE_CIRCLE: txtIconShape.setText("Cercle"); break;
            case IconShapeHelper.SHAPE_ROUNDED_SQUARE: txtIconShape.setText("Carré Arrondi"); break;
            case IconShapeHelper.SHAPE_SQUIRCLE: txtIconShape.setText("Squircle"); break;
            case IconShapeHelper.SHAPE_TEARDROP: txtIconShape.setText("Goutte d'eau"); break;
            default: txtIconShape.setText("Original"); break;
        }
    }

    private void showIconPackDialog() {
        final String[] options = { "Défaut", "Ruvolute", "Afriqui" };
        final String[] values = { IconPackManager.PACK_DEFAULT, IconPackManager.PACK_RUVOLUTE, IconPackManager.PACK_AFRIQUI };

        String currentPack = IconPackManager.getSavedIconPack(requireContext());
        int checkedItem = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(currentPack)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Pack d'icônes")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    IconPackManager.saveIconPack(requireContext(), values[which]);
                    updateIconPackSummary();
                    dialog.dismiss();
                    if (getActivity() != null) getActivity().recreate();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void updateIconPackSummary() {
        if (txtIconPack == null) return;
        String current = IconPackManager.getSavedIconPack(requireContext());
        if (IconPackManager.PACK_RUVOLUTE.equals(current)) txtIconPack.setText("Ruvolute");
        else if (IconPackManager.PACK_AFRIQUI.equals(current)) txtIconPack.setText("Afriqui");
        else txtIconPack.setText("Défaut");
    }
}
