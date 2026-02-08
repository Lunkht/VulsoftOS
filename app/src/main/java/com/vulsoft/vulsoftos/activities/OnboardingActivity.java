package com.vulsoft.vulsoftos.activities;

import com.vulsoft.vulsoftos.*;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends BaseActivity {

    private ViewPager2 viewPager;
    private LinearLayout layoutIndicators;
    private Button btnAction;
    private OnboardingAdapter adapter;
    private List<OnboardingAdapter.OnboardingItem> onboardingItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPagerOnboarding);
        layoutIndicators = findViewById(R.id.layoutIndicators);
        btnAction = findViewById(R.id.btnAction);

        setupOnboardingItems();

        adapter = new OnboardingAdapter(onboardingItems);
        viewPager.setAdapter(adapter);

        setupIndicators();
        setCurrentIndicator(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setCurrentIndicator(position);
                if (position == onboardingItems.size() - 1) {
                    btnAction.setText("Commencer");
                } else {
                    btnAction.setText("Suivant");
                }
            }
        });

        btnAction.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() + 1 < onboardingItems.size()) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                completeOnboarding();
            }
        });
    }

    private void setupOnboardingItems() {
        onboardingItems = new ArrayList<>();
        
        // Step 1: Welcome
        onboardingItems.add(new OnboardingAdapter.OnboardingItem(
                "Bienvenue sur Vulsoft OS",
                "Découvrez une nouvelle expérience Android fluide, rapide et respectueuse de votre vie privée.",
                R.mipmap.ic_launcher // Placeholder icon
        ));

        // Step 2: Terms & License
        onboardingItems.add(new OnboardingAdapter.OnboardingItem(
                "Conditions & Licence",
                "Vulsoft OS est un logiciel open-source. En l'utilisant, vous acceptez nos conditions d'utilisation. Aucune donnée personnelle n'est collectée sans votre consentement explicite.",
                android.R.drawable.ic_menu_info_details
        ));

        // Step 3: How it works
        onboardingItems.add(new OnboardingAdapter.OnboardingItem(
                "Fonctionnement",
                "• Appui long espace vide : Apps récentes\n• Double-clic espace vide : Assistant\n• Glisser bas : Recherche\n• Appui long icône : Options",
                android.R.drawable.ic_menu_help
        ));

        // Step 4: Permissions
        onboardingItems.add(new OnboardingAdapter.OnboardingItem(
                "Permissions",
                "Pour fonctionner correctement, Vulsoft OS a besoin d'accéder aux notifications (pour les badges) et à l'usage des applications (pour les apps récentes).",
                android.R.drawable.ic_menu_manage
        ));
    }

    private void setupIndicators() {
        ImageView[] indicators = new ImageView[adapter.getItemCount()];
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(8, 0, 8, 0);

        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new ImageView(getApplicationContext());
            indicators[i].setImageDrawable(ContextCompat.getDrawable(
                    getApplicationContext(),
                    R.drawable.indicator_inactive
            ));
            indicators[i].setLayoutParams(layoutParams);
            layoutIndicators.addView(indicators[i]);
        }
    }

    private void setCurrentIndicator(int index) {
        int childCount = layoutIndicators.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) layoutIndicators.getChildAt(i);
            if (i == index) {
                imageView.setImageDrawable(ContextCompat.getDrawable(
                        getApplicationContext(),
                        R.drawable.indicator_active
                ));
            } else {
                imageView.setImageDrawable(ContextCompat.getDrawable(
                        getApplicationContext(),
                        R.drawable.indicator_inactive
                ));
            }
        }
    }

    private void completeOnboarding() {
        SharedPreferences prefs = getSharedPreferences("launcher_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("tutorial_completed", true).apply();
        
        Intent intent = new Intent(OnboardingActivity.this, InitialSetupActivity.class);
        startActivity(intent);
        finish();
    }
}
