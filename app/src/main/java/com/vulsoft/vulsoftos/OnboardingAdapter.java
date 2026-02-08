package com.vulsoft.vulsoftos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {

    private final List<OnboardingItem> items;

    public OnboardingAdapter(List<OnboardingItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding_page, parent, false);
        return new OnboardingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        OnboardingItem item = items.get(position);
        holder.textTitle.setText(item.title);
        holder.textDescription.setText(item.description);
        holder.imgIcon.setImageResource(item.imageResId);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class OnboardingViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle;
        TextView textDescription;
        ImageView imgIcon;

        public OnboardingViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textDescription = itemView.findViewById(R.id.textDescription);
            imgIcon = itemView.findViewById(R.id.imgOnboarding);
        }
    }

    public static class OnboardingItem {
        String title;
        String description;
        int imageResId;

        public OnboardingItem(String title, String description, int imageResId) {
            this.title = title;
            this.description = description;
            this.imageResId = imageResId;
        }
    }
}
