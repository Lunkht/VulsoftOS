package com.vulsoft.vulsoftos;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FolderDialogFragment extends DialogFragment {

    private static final String ARG_FOLDER_NAME = "folder_name";
    private List<AppItem> folderApps;
    private AppsAdapter.OnAppClickListener appClickListener;

    public static FolderDialogFragment newInstance(String folderName, List<AppItem> apps) {
        FolderDialogFragment fragment = new FolderDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FOLDER_NAME, folderName);
        fragment.setArguments(args);
        fragment.folderApps = apps;
        return fragment;
    }

    public void setOnAppClickListener(AppsAdapter.OnAppClickListener listener) {
        this.appClickListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(getArguments().getString(ARG_FOLDER_NAME));
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_folder, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String folderName = getArguments().getString(ARG_FOLDER_NAME);
        TextView title = view.findViewById(R.id.folder_title);
        title.setText(folderName);

        RecyclerView recyclerView = view.findViewById(R.id.folder_recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3)); // 3 columns in folder

        if (folderApps == null) {
            folderApps = new java.util.ArrayList<>();
        }

        // Reuse AppsAdapter but disable widgets/folders/drag inside folder for now
        AppsAdapter adapter = new AppsAdapter(folderApps, 100, item -> {
            if (appClickListener != null) {
                appClickListener.onAppClick(item);
            }
            dismiss();
        }, null);
        
        // Configure adapter for folder view
        adapter.setAppIconSize(150); // Slightly larger or standard
        adapter.setAppTextSize(30);
        
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}
