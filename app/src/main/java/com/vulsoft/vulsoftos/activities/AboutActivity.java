package com.vulsoft.vulsoftos.activities;

import com.vulsoft.vulsoftos.*;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.vulsoft.vulsoftos.R;

public class AboutActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Header
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnShare).setOnClickListener(v -> shareApp());

        // Version
        TextView tvVersion = findViewById(R.id.tvVersion);
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            int versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            tvVersion.setText("Version " + versionName + " build " + String.format("%03d", versionCode));
        } catch (Exception e) {
            e.printStackTrace();
            tvVersion.setText("Version 1.0");
        }

        // Socials
        findViewById(R.id.btnReddit).setOnClickListener(v -> openUrl("https://reddit.com"));
        findViewById(R.id.btnInstagram).setOnClickListener(v -> openUrl("https://instagram.com"));
        findViewById(R.id.btnTwitter).setOnClickListener(v -> openUrl("https://twitter.com"));
        findViewById(R.id.btnTelegram).setOnClickListener(v -> openUrl("https://telegram.org"));

        // License Card
        findViewById(R.id.cardLicense).setOnClickListener(v -> {
            Toast.makeText(this, "Fonctionnalité Premium à venir !", Toast.LENGTH_SHORT).show();
        });

        // List Items
        findViewById(R.id.btnChangelog).setOnClickListener(v -> {
            // Show changelog dialog or toast
            Toast.makeText(this, "Pas de nouveaux changements récents.", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnCredits).setOnClickListener(v -> {
            Toast.makeText(this, "Crédits: VulSoft Team, Open Source Libraries.", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnWebsite).setOnClickListener(v -> openUrl("https://www.google.com")); // Placeholder
        findViewById(R.id.btnPrivacy).setOnClickListener(v -> openUrl("https://www.google.com/policies/privacy")); // Placeholder
    }

    private void shareApp() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Découvrez Ruvolute Launcher : https://play.google.com/store/apps/details?id=" + getPackageName());
        sendIntent.setType("text/plain");
        Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivity(shareIntent);
    }

    private void openUrl(String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Impossible d'ouvrir le lien", Toast.LENGTH_SHORT).show();
        }
    }
}
