package com.vulsoft.vulsoftos.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.vulsoft.vulsoftos.R;

import java.util.Arrays;
import java.util.List;

public class CategorySelectionDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_SELECTED_CATEGORY = "selected_category";
    private static final String[] CATEGORIES = {"Tout", "Social", "Jeux", "Travail", "Média", "Autre"};

    private String selectedCategory;
    private OnCategorySelectedListener listener;

    public interface OnCategorySelectedListener {
        void onCategorySelected(String category);
    }

    public static CategorySelectionDialogFragment newInstance(String currentCategory) {
        CategorySelectionDialogFragment fragment = new CategorySelectionDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SELECTED_CATEGORY, currentCategory);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnCategorySelectedListener) {
            listener = (OnCategorySelectedListener) context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedCategory = getArguments().getString(ARG_SELECTED_CATEGORY);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_category_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set transparent background for the bottom sheet container so our rounded corners show
        view.post(() -> {
            View parent = (View) view.getParent();
            if (parent != null) {
                parent.setBackgroundResource(android.R.color.transparent);
            }
        });

        RecyclerView rvCategories = view.findViewById(R.id.rvCategories);
        rvCategories.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCategories.setAdapter(new CategoryAdapter(Arrays.asList(CATEGORIES), selectedCategory));
    }

    public void setOnCategorySelectedListener(OnCategorySelectedListener listener) {
        this.listener = listener;
    }

    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

        private final List<String> categories;
        private final String currentSelected;

        public CategoryAdapter(List<String> categories, String currentSelected) {
            this.categories = categories;
            this.currentSelected = currentSelected;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_dialog, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String category = categories.get(position);
            holder.tvCategoryName.setText(category);
            
            // Highlight selected category
            if (category.equals(currentSelected)) {
                holder.tvCategoryName.setTextColor(0xFF2196F3); // Blue
                holder.tvCategoryName.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                holder.tvCategoryName.setTextColor(0xFFFFFFFF); // White
                holder.tvCategoryName.setTypeface(null, android.graphics.Typeface.NORMAL);
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategorySelected(category);
                }
                dismiss();
            });
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCategoryName;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            }
        }
    }
}
