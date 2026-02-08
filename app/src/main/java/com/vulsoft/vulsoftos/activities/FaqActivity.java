package com.vulsoft.vulsoftos.activities;

import com.vulsoft.vulsoftos.*;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class FaqActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faq);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        // Ajouter des actions pour les liens utiles
        setupHelpfulLinks();
        
        // Vérifier le statut d'intégration système
        checkSystemIntegrationStatus();
    }
    
    private void setupHelpfulLinks() {
        // Bouton pour ouvrir les paramètres système
        View btnSystemSettings = findViewById(R.id.btnSystemSettings);
        if (btnSystemSettings != null) {
            btnSystemSettings.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_HOME_SETTINGS);
                    startActivity(intent);
                } catch (Exception e) {
                    // Fallback vers les paramètres généraux
                    Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                    startActivity(intent);
                }
            });
        }
        
        // Bouton pour les permissions de notification
        View btnNotificationSettings = findViewById(R.id.btnNotificationSettings);
        if (btnNotificationSettings != null) {
            btnNotificationSettings.setOnClickListener(v -> {
                SystemIntegrationManager.requestNotificationAccess(this);
            });
        }
    }
    
    private void checkSystemIntegrationStatus() {
        SystemIntegrationManager.IntegrationStatus status = 
            SystemIntegrationManager.checkIntegrationStatus(this);
            
        // Mettre à jour l'UI avec le statut
        TextView statusText = findViewById(R.id.txtIntegrationStatus);
        if (statusText != null) {
            if (status.isDefaultLauncher && status.hasNotificationAccess) {
                statusText.setText("Intégration système complète");
                statusText.setTextColor(getResources().getColor(R.color.widget_green, null));
            } else {
                statusText.setText("Intégration système incomplète");
                statusText.setTextColor(getResources().getColor(R.color.widget_orange, null));
            }
        }
    }
}
