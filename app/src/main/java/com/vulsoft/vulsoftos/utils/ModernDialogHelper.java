package com.vulsoft.vulsoftos.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.vulsoft.vulsoftos.R;

public class ModernDialogHelper {

    public interface OnOptionSelectedListener {
        void onOptionSelected(int index, String value);
    }

    public static class Builder {
        private final Context context;
        private final AlertDialog dialog;
        private final View dialogView;
        private final TextView titleView;
        private final TextView messageView;
        private final FrameLayout contentFrame;
        private final TextView positiveBtn;
        private final TextView negativeBtn;
        private final TextView neutralBtn;

        public Builder(Context context) {
            this.context = context;
            dialog = new AlertDialog.Builder(context).create();
            dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_modern, null);
            
            titleView = dialogView.findViewById(R.id.dialogTitle);
            messageView = dialogView.findViewById(R.id.dialogMessage);
            contentFrame = dialogView.findViewById(R.id.dialogContent);
            positiveBtn = dialogView.findViewById(R.id.dialogPositive);
            negativeBtn = dialogView.findViewById(R.id.dialogNegative);
            neutralBtn = dialogView.findViewById(R.id.dialogNeutral);

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            dialog.setView(dialogView);
        }

        public Builder setTitle(String title) {
            titleView.setText(title);
            titleView.setVisibility(View.VISIBLE);
            return this;
        }

        public Builder setTitle(int resId) {
            return setTitle(context.getString(resId));
        }

        public Builder setMessage(String message) {
            messageView.setText(message);
            messageView.setVisibility(View.VISIBLE);
            return this;
        }

        public Builder setMessage(int resId) {
            return setMessage(context.getString(resId));
        }

        public Builder setPositiveButton(String text, View.OnClickListener listener) {
            positiveBtn.setText(text);
            positiveBtn.setVisibility(View.VISIBLE);
            positiveBtn.setOnClickListener(v -> {
                if (listener != null) listener.onClick(v);
                dialog.dismiss();
            });
            return this;
        }

        public Builder setPositiveButton(int resId, View.OnClickListener listener) {
            return setPositiveButton(context.getString(resId), listener);
        }

        public Builder setNegativeButton(String text, View.OnClickListener listener) {
            negativeBtn.setText(text);
            negativeBtn.setVisibility(View.VISIBLE);
            negativeBtn.setOnClickListener(v -> {
                if (listener != null) listener.onClick(v);
                dialog.dismiss();
            });
            return this;
        }

        public Builder setNegativeButton(int resId, View.OnClickListener listener) {
            return setNegativeButton(context.getString(resId), listener);
        }

        public Builder setNeutralButton(String text, View.OnClickListener listener) {
            neutralBtn.setText(text);
            neutralBtn.setVisibility(View.VISIBLE);
            neutralBtn.setOnClickListener(v -> {
                if (listener != null) listener.onClick(v);
                dialog.dismiss();
            });
            return this;
        }

        public Builder setNeutralButton(int resId, View.OnClickListener listener) {
            return setNeutralButton(context.getString(resId), listener);
        }
        
        public Builder setSingleChoiceItems(String[] items, int checkedItem, OnOptionSelectedListener listener) {
             RecyclerView list = new RecyclerView(context);
             list.setLayoutManager(new LinearLayoutManager(context));
             
             ModernDialogAdapter adapter = new ModernDialogAdapter(items, checkedItem, true, (index, value) -> {
                 if (listener != null) listener.onOptionSelected(index, value);
                 dialog.dismiss();
             });
             list.setAdapter(adapter);
             contentFrame.addView(list);
             contentFrame.setVisibility(View.VISIBLE);
             
             // If we have items, we often don't need a positive button, or it might be confusing
             // But usually single choice dialogs dismiss on selection.
             return this;
        }

        public Builder setItems(String[] items, OnOptionSelectedListener listener) {
             RecyclerView list = new RecyclerView(context);
             list.setLayoutManager(new LinearLayoutManager(context));
             
             ModernDialogAdapter adapter = new ModernDialogAdapter(items, -1, false, (index, value) -> {
                 if (listener != null) listener.onOptionSelected(index, value);
                 dialog.dismiss();
             });
             list.setAdapter(adapter);
             contentFrame.addView(list);
             contentFrame.setVisibility(View.VISIBLE);
             return this;
        }

        public void show() {
            dialog.show();
        }
    }

    private static class ModernDialogAdapter extends RecyclerView.Adapter<ModernDialogAdapter.ViewHolder> {
        private final String[] items;
        private final int checkedItem;
        private final boolean showRadio;
        private final OnOptionSelectedListener listener;

        public ModernDialogAdapter(String[] items, int checkedItem, boolean showRadio, OnOptionSelectedListener listener) {
            this.items = items;
            this.checkedItem = checkedItem;
            this.showRadio = showRadio;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dialog_choice, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.text.setText(items[position]);
            
            if (showRadio) {
                holder.radio.setVisibility(View.VISIBLE);
                holder.radio.setChecked(position == checkedItem);
                
                holder.radio.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onOptionSelected(position, items[position]);
                    }
                });
            } else {
                holder.radio.setVisibility(View.GONE);
            }
            
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOptionSelected(position, items[position]);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.length;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            RadioButton radio;
            TextView text;

            ViewHolder(View itemView) {
                super(itemView);
                radio = itemView.findViewById(R.id.choiceRadio);
                text = itemView.findViewById(R.id.choiceText);
            }
        }
    }
}
